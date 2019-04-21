package http.proxy;

import http.proxy.cache.CacheManager;
import http.proxy.exeptions.BadRequestException;
import http.proxy.exeptions.MethodNotAllowedException;
import http.proxy.exeptions.RequestTimeoutException;
import http.proxy.logger.Logger;
import http.proxy.utils.RequestReader;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import static http.proxy.constants.Constants.*;
import static http.proxy.ssl.UnsafeOkHttpClient.getUnsafeOkHttpClientBuilder;

public final class SocketHandler implements Runnable {

    private CacheManager cm;
    private Socket socket;
    private InputStream is;
    private OutputStream os;
    private Logger l;

    public SocketHandler(final Socket socket, final Logger logger, final CacheManager cacheManager) throws IOException {
        this.socket = socket;
        is = socket.getInputStream();
        os = socket.getOutputStream();
        l = logger;
        cm = cacheManager;
    }

    @Override
    public void run() {
        try {
            RequestReader requestReader = new RequestReader(is);
            l.log(Logger.Level.INFO, socket, requestReader.getMethod(), requestReader.getUrl(), true, null);
            //Берем из кэша
            final Response response = cm.getResponse(requestReader.getUrl());
            if (response != null) {
                try {
                    new StandardCallback(l, socket, os, true).onResponse(null, response);
                } catch (IOException e) {
                    l.log(Logger.Level.EXCEPTION, ON_FAILURE + SPACE + e.getMessage());
                    try {
                        socket.close();
                    } catch (IOException ee) {
                        l.log(Logger.Level.EXCEPTION, VERY_BAD_EXCEPTION + SPACE + ee.getMessage());
                    }
                }
            } else getUnsafeOkHttpClientBuilder()
                    .proxy(Proxy.NO_PROXY)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .callTimeout(30, TimeUnit.SECONDS)
                    .build()
                    .newCall(requestReader.toRequest())
                    .enqueue(new StandardCallback(l, socket, os, false));

        } catch (RequestTimeoutException | BadRequestException e) {
            l.log(Logger.Level.EXCEPTION, socket, e.getMessage());
            try {
                if (e instanceof RequestTimeoutException) {
                    os.write(firstLine(REQUEST_TIMEOUT_CODE, REQUEST_TIMEOUT).getBytes());
                } else {
                    os.write(firstLine(BAD_REQUEST_CODE, BAD_REQUEST).getBytes());
                }
                os.write((CONNECTION + HEADER_DELIM + SPACE + CLOSE + CRLF).getBytes());
                os.flush();
                socket.close();
            } catch (IOException ee) {
                l.log(Logger.Level.EXCEPTION, socket, VERY_BAD_EXCEPTION + SPACE + e.getMessage());
            }
        } catch (MethodNotAllowedException e) {
            l.log(Logger.Level.WARNING, socket, e.getMessage() + SPACE + e.getRequestedMethod());
            try {
                String string = DEFAULT_HTTP_VERSION +
                        SPACE +
                        METHOD_NOT_ALLOWED_CODE +
                        SPACE +
                        METHOD_NOT_ALLOWED +
                        CRLF +
                        ALLOW +
                        HEADER_DELIM +
                        SPACE +
                        e.getAllowedMethods().toString() +
                        CRLF;
                os.write(string.getBytes());
                os.write((CONNECTION + HEADER_DELIM + SPACE + CLOSE + CRLF).getBytes());
                os.flush();
                socket.close();
            } catch (IOException ee) {
                l.log(Logger.Level.EXCEPTION, socket, VERY_BAD_EXCEPTION + SPACE + e.getMessage());
            }
        }
    }

    private String firstLine(int code, String message) {
        return DEFAULT_HTTP_VERSION + SPACE + code + SPACE + message + SPACE + CRLF;
    }


    class StandardCallback implements Callback {

        private final Logger l;
        private final Socket socket;
        private final OutputStream os;
        private final boolean cached;

        public StandardCallback(Logger logger, Socket socket, OutputStream os, boolean cached) {
            this.l = logger;
            this.socket = socket;
            this.os = os;
            this.cached = cached;
        }

        @Override
        public void onFailure(Call call, IOException e) {
            if (e instanceof SocketTimeoutException) {
                l.log(Logger.Level.EXCEPTION, socket, GATEWAY_TIMEOUT);
                try {
                    os.write(firstLine(GATEWAY_TIMEOUT_CODE, GATEWAY_TIMEOUT).getBytes());
                    os.write((CONNECTION + HEADER_DELIM + SPACE + CLOSE + CRLF).getBytes());
                    os.flush();
                } catch (IOException ee) {
                    l.log(Logger.Level.EXCEPTION, socket, ee.getMessage());
                }
            } else {
                try {
                    os.write(firstLine(BAD_REQUEST_CODE, BAD_REQUEST).getBytes());
                    os.write((CONNECTION + HEADER_DELIM + SPACE + CLOSE + CRLF).getBytes());
                    os.flush();
                } catch (IOException ee) {
                    l.log(Logger.Level.EXCEPTION, socket, ON_FAILURE + SPACE + BAD_REQUEST);
                }
            }
            try {
                socket.close();
            } catch (IOException ee) {
                l.log(Logger.Level.EXCEPTION, socket, VERY_BAD_EXCEPTION + SPACE + ee.getMessage());
            }
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            byte[] body = response.body().bytes();
            try {
                String builder = response.protocol().toString() +
                        SPACE +
                        response.code() +
                        SPACE +
                        response.message() +
                        LF +
                        //Убираем Transfer-Encoding, наш прокси его не использует
                        response
                                .headers()
                                .newBuilder()
                                .removeAll(TRANSFER_ENCODING)
                                .build()
                                .toString() +
                        CRLF;
                os.write(builder.getBytes());

                if (body != null) {
                    os.write(body);
                }
                os.flush();
                socket.close();

                if (cached) {
                } else {
                    //TODO check it
                    if (GET_METHOD.equalsIgnoreCase(response.request().method())) {
                        cm.put(call.request().url().toString(), response, body);
                    }
                    l.log(Logger.Level.INFO, socket, response.request().method(), response.request().url().toString(), false, null);
                }

            } catch (IOException e) {
                l.log(Logger.Level.EXCEPTION, socket, VERY_BAD_EXCEPTION + SPACE + e.getMessage());
            } finally {
                socket.close();
            }
        }

    }

}