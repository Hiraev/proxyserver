package http.proxy;

import http.proxy.cache.CacheManager;
import http.proxy.exceptions.BadRequestException;
import http.proxy.exceptions.MethodNotAllowedException;
import http.proxy.exceptions.RequestTimeoutException;
import http.proxy.logger.Logger;
import http.proxy.utils.RequestReader;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import static http.proxy.constants.Constants.*;

/**
 * Класс для обработки сокетов. Читает запрос из сокета
 * выполняет его, записывает в сокет ответ и закрывает его.
 */
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
            /**Создаем экземпляр RequestReader, который сразу же и считает
             * данные из входного потока (inputStream)*/
            RequestReader requestReader = new RequestReader(is);
            l.log(Logger.Level.INFO, socket, requestReader.getMethod(), requestReader.getUrl(), true, null);
            /** Пытаем взять значение из кэша, если его там нет, то получим null*/
            final Response response = cm.getResponse(requestReader.getUrl());
            if (response != null) {
                try {
                    /**Получили ответ, создает StandardCallBack, который запишет ответ в сокет*/
                    new StandardCallback(l, socket, os, true).onResponse(null, response);
                } catch (IOException e) {
                    l.log(Logger.Level.EXCEPTION, ON_FAILURE + SPACE + e.getMessage());
                    try {
                        socket.close();
                    } catch (IOException ee) {
                        l.log(Logger.Level.EXCEPTION, VERY_BAD_EXCEPTION + SPACE + ee.getMessage());
                    }
                }
                /**
                 * Если ответ из кэша не получен, то выполняем OkHttp запрос
                 * Отключаем Proxy, чтобы с этого же компьютера можно было использовать
                 * данную программу в качестве прокси. ставим ограничение на время запроса и ответа
                 * задаем запрос и в enqueue задаем то, что нужно делать асинхронно
                 * при получении ответа
                 *
                 * !!!Если возникунт ошибки, связанные с безопасностью, то здесь вместо
                 * new OkHttpClient.Builder() вызвать getUnsafeOkHttpClientBuilder()
                 * из класса http.proxy.ssl.UnsafeOkHttpClient!!!
                 */
            } else new OkHttpClient.Builder()
                    .proxy(Proxy.NO_PROXY)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .callTimeout(30, TimeUnit.SECONDS)
                    .build()
                    .newCall(requestReader.toRequest())
                    .enqueue(new StandardCallback(l, socket, os, false));

            /** Ловим исключения, которыем могут возникнуть при создании RequestReader
             * И отправляем клиенту соответвующие заголовки*/
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
                /** Если при записи происзошла ошибка (сокет неожиданно закрылся )*/
                l.log(Logger.Level.EXCEPTION, socket, VERY_BAD_EXCEPTION + SPACE + e.getMessage());
            }
        } catch (MethodNotAllowedException e) {
            /** Получили ошибку о том, что метод не поддерживается, сообщаем об этом клиенту и отправляем
             * в заголовке Allow спиок доступных методов*/
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
            } catch (IOException ee) {/** Если при записи происзошла ошибка (сокет неожиданно закрылся )*/
                l.log(Logger.Level.EXCEPTION, socket, VERY_BAD_EXCEPTION + SPACE + e.getMessage());
            }
        }
    }

    /**
     * Формирует первую строку заголовка HTTP/1.1. CODE MESSAGE
     */
    private String firstLine(int code, String message) {
        return DEFAULT_HTTP_VERSION + SPACE + code + SPACE + message + SPACE + CRLF;
    }

    /**
     * Калбэк для асинхронного вызова при получении ответа от сервера
     * Полученный ответ кэшируем, если надо и отправляем клиенту
     */
    class StandardCallback implements Callback {

        private final Logger l;
        private final Socket socket;
        private final OutputStream os;
        private final boolean cached;

        /**
         * @param logger логгер
         * @param socket сокет
         * @param os     outputStream для записи ответа
         * @param cached если true, значени отправляем клиенту кшированный ответ
         */
        public StandardCallback(Logger logger, Socket socket, OutputStream os, boolean cached) {
            this.l = logger;
            this.socket = socket;
            this.os = os;
            this.cached = cached;
        }

        /**
         * Если при выполнении запроса или получении ответа от сервера, что-то пошло не так
         *
         * @param call вызов
         * @param e    исключение
         */
        @Override
        public void onFailure(Call call, IOException e) {
            if (e instanceof SocketTimeoutException) {
                l.log(Logger.Level.EXCEPTION, socket, GATEWAY_TIMEOUT);
                try {
                    os.write(firstLine(GATEWAY_TIMEOUT_CODE, GATEWAY_TIMEOUT).getBytes());
                    os.write((CONNECTION + HEADER_DELIM + SPACE + CLOSE + CRLF).getBytes());
                    os.flush();
                } catch (IOException ee) {
                    /** Если при записи происзошла ошибка (сокет неожиданно закрылся )*/
                    l.log(Logger.Level.EXCEPTION, socket, ee.getMessage());
                }
            } else {
                try {
                    os.write(firstLine(BAD_REQUEST_CODE, BAD_REQUEST).getBytes());
                    os.write((CONNECTION + HEADER_DELIM + SPACE + CLOSE + CRLF).getBytes());
                    os.flush();
                } catch (IOException ee) {
                    /** Если при записи происзошла ошибка (сокет неожиданно закрылся )*/
                    l.log(Logger.Level.EXCEPTION, socket, ON_FAILURE + SPACE + BAD_REQUEST);
                }
            }
            try {
                socket.close();
            } catch (IOException ee) {
                /** Если при записи происзошла ошибка (сокет неожиданно закрылся )*/
                l.log(Logger.Level.EXCEPTION, socket, VERY_BAD_EXCEPTION + SPACE + ee.getMessage());
            }
        }

        /**
         * Вызывается, когда был получен ответ от сервера
         *
         * @param call     вызов
         * @param response ответ
         * @throws IOException если что-то пошло не так
         */
        @Override
        public void onResponse(Call call, Response response) throws IOException {
            /**
             * СРАЗУ БЕРЕМ ТЕЛО ЗАПРОСА, ЧТОБЫ МОЖНО БЫЛО И КЭШИРОВАТЬ ЕГО
             * И ОТПРАВИТЬ КЛИЕНТУ, ИНАЧЕ Response НЕ ПОЗВОЛИТ ОБРАТЬТСЯ К ТЕЛУ
             * ВТОРОЙ РАЗ
             */
            byte[] body = response.body().bytes();
            try {
                /**
                 * Строим ответ, заполняем заголовки
                 */
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
                /** Записываем заголовки для отправки клиенту*/
                os.write(builder.getBytes());

                /** Записываем тело ответа*/
                if (body != null) {
                    os.write(body);
                }
                /** Отправляем и закрываем сокет*/
                os.flush();
                socket.close();

                if (!cached) {
                    /** Если ответ был кэширован и если он получен методом GET, то кэшируем его*/
                    if (GET_METHOD.equalsIgnoreCase(response.request().method())) {
                        cm.put(call.request().url().toString(), response, body);
                    }
                    l.log(Logger.Level.INFO,
                            socket,
                            response.request().method(),
                            response.request().url().toString(),
                            false,
                            null
                    );
                }

            } catch (IOException e) {
                /** Если при записи происзошла ошибка (сокет неожиданно закрылся )*/
                l.log(Logger.Level.EXCEPTION, socket, VERY_BAD_EXCEPTION + SPACE + e.getMessage());
            } finally {
                socket.close();
            }
        }

    }

}