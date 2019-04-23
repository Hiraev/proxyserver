package http.proxy;

import http.proxy.cache.CacheManager;
import http.proxy.exceptions.BadRequestException;
import http.proxy.exceptions.MethodNotAllowedException;
import http.proxy.exceptions.RequestTimeoutException;
import http.proxy.logger.Logger;
import http.proxy.utils.Callback;
import http.proxy.utils.Request;
import http.proxy.utils.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;

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
    private ExecutorService es;

    public SocketHandler(final Socket socket,
                         final Logger logger,
                         final CacheManager cacheManager,
                         final ExecutorService executorService
    ) throws IOException {
        this.socket = socket;
        is = socket.getInputStream();
        os = socket.getOutputStream();
        l = logger;
        cm = cacheManager;
        es = executorService;
    }

    private void writeResponse(final String string, final byte[] body) {
        try {
            os.write(string.getBytes());
            os.write(CRLF.getBytes());
            if (body != null) {
                os.write(body);
            }
            os.flush();
            socket.close();
        } catch (IOException e) {
            /** Если при записи происзошла ошибка (сокет неожиданно закрылся )*/
            l.log(Logger.Level.EXCEPTION, socket, VERY_BAD_EXCEPTION + SPACE + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            /**Создаем экземпляр Request, который сразу же и считает
             * данные из входного потока (inputStream)*/
            Request request = new Request();
            request.read(is);
            l.log(Logger.Level.INFO, socket, request.getMethod(), request.getUrl(), true, null);
            /** Пытаем взять значение из кэша, если его там нет, то получим null*/
            final Response response = cm.getResponse(request.getUrl());
            if (response != null) {
                /**Получили ответ, создает StandardCallBack, который запишет ответ в сокет*/
                new StandardCallback(l, socket, os, true).onSuccess(null, response);

            } else request.execute(es, new StandardCallback(l, socket, os, false));

            /** Ловим исключения, которыем могут возникнуть при создании Request
             * И отправляем клиенту соответвующие заголовки*/
        } catch (RequestTimeoutException | BadRequestException e) {
            StringBuilder resp = new StringBuilder();
            if (e instanceof RequestTimeoutException) {
                resp.append(firstLine(REQUEST_TIMEOUT_CODE, REQUEST_TIMEOUT));
                l.log(Logger.Level.EXCEPTION, socket, REQUEST_TIMEOUT + SPACE + e.getMessage());
            } else {
                resp.append(firstLine(BAD_REQUEST_CODE, BAD_REQUEST));
                l.log(Logger.Level.EXCEPTION, socket, BAD_REQUEST + SPACE + e.getMessage());
            }
            resp.append(CONNECTION + HEADER_DELIM + SPACE + CLOSE);
            writeResponse(resp.toString(), null);
        } catch (MethodNotAllowedException e) {
            /** Получили ошибку о том, что метод не поддерживается, сообщаем об этом клиенту и отправляем
             * в заголовке Allow спиок доступных методов*/
            l.log(Logger.Level.WARNING, socket, e.getMessage() + SPACE + e.getRequestedMethod());
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
                    CRLF + CONNECTION + HEADER_DELIM + SPACE + CLOSE + CRLF;
            writeResponse(string, null);
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
         * @param request вызов
         * @param e       исключение
         */
        @Override
        public void onFailure(Request request, Exception e) {
            l.log(Logger.Level.EXCEPTION, socket, e.getMessage());
            if (e instanceof SocketTimeoutException) {
                writeResponse(firstLine(GATEWAY_TIMEOUT_CODE, GATEWAY_TIMEOUT) +
                        CONNECTION + HEADER_DELIM + SPACE + CLOSE, null
                );
            } else {
                writeResponse(firstLine(BAD_REQUEST_CODE, BAD_REQUEST) +
                        CONNECTION + HEADER_DELIM + SPACE + CLOSE, null
                );
            }
        }

        /**
         * Вызывается, когда был получен ответ от сервера
         *
         * @param request  вызов
         * @param response ответ
         * @throws IOException если что-то пошло не так
         */
        @Override
        public void onSuccess(Request request, Response response) {
            /** Строим ответ, заполняем заголовки */
            String builder = response.getProtocol() +
                    SPACE +
                    response.getCode() +
                    SPACE +
                    response.getMessage() +
                    CRLF +
                    response.getHeaders();

            /** Записываем заголовки для отправки клиенту*/
            writeResponse(builder, response.getBody());
            if (!cached) {
                /** Если ответ был кэширован и если он получен методом GET, то кэшируем его*/
                if (GET_METHOD.equalsIgnoreCase(request.getMethod())) {
                    cm.put(request.getUrl(), response);
                }
                l.log(Logger.Level.INFO,
                        socket,
                        request.getMethod(),
                        request.getUrl(),
                        false,
                        null
                );
            }
        }
    }
}