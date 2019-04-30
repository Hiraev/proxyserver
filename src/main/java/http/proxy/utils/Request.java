package http.proxy.utils;

import http.proxy.exceptions.BadRequestException;
import http.proxy.exceptions.BadSyntaxException;
import http.proxy.exceptions.MethodNotAllowedException;
import http.proxy.exceptions.RequestTimeoutException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

import static http.proxy.constants.Constants.*;

/**
 * Класс для чтения запросов
 * Хранит в себе тело запроса, длину тела запроса
 * адрес, метод, версию http, заголовки
 * <p>
 */
public final class Request extends HttpReader {

    private String url;
    private String method;
    private String protocol;

    /**
     * @param is - входной поток
     * @throws RequestTimeoutException   выбрасывается, когда клиент слишком доло не отправляет
     *                                   байты
     * @throws MethodNotAllowedException выбрасывается, когда метод, указанный в запросе не
     *                                   поддерживатеся
     * @throws BadRequestException       выбрасывается, когда в запросе присутсвует какая-то ошибка
     */
    @Override
    public void read(final InputStream is)
            throws RequestTimeoutException, MethodNotAllowedException, BadRequestException {
        try {
            //Заставляем родительский класс читать входной поток
            super.readTopLine(is);
            //Первая строка содержит метод, запрос и версию протокола
            final String[] s = getFirstLine().split(SPACE);
            if (s.length != 3) {
                System.out.println(Arrays.toString(s));
                throw new BadRequestException();
            }
            method = s[0];
            url = s[1];
            protocol = s[2];

            if (!Arrays.asList(GET_METHOD, HEAD_METHOD, POST_METHOD).contains(method)) {
                throw new MethodNotAllowedException(method);
            }

            super.readHeaders(is);
        } catch (IOException e) {
            throw new RequestTimeoutException();
        } catch (BadSyntaxException e) {
            throw new BadRequestException();
        }
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    /**
     * Выполняем execute в ExecutorService, который нам предоставят
     * CallBack аналогичен тому, что бы взят из OkHttp
     */
    public void execute(ExecutorService service, Callback callback) {
        service.submit(
                () -> {
                    try {
//                        //TODO Здесь получаем Gateway Timeout
                        Socket socket = new Socket(Proxy.NO_PROXY);
                        String host = new URL(url).getHost();

                        socket.connect(
                                new InetSocketAddress(
                                        InetAddress.getByName(host),
                                        80
                                ),
                                30000
                        );


                        final InputStream is = socket.getInputStream();
                        final OutputStream os = socket.getOutputStream();

                        os.write(toString().getBytes());
                        if (getBody() != null) os.write(getBody());
                        os.flush();

                        final Response response = new Response(url);
                        response.read(is);
                        socket.close();
                        {
                            callback.onSuccess(this, response);
                        }
                    } catch (Exception e) {
                        callback.onFailure(this, e);
                    }
                }
        );
    }

    private void redirect(ExecutorService service, Callback callback, Response response) {
        try {
            String host = new URL(url).getHost();
            String protocol = new URL(url).getProtocol();
            Request request = new Request();
            request.method = method;
            request.protocol = protocol;
            request.body = getBody();
            request.headers = new Headers();
            headers.forEach((a, b) -> headers.add(a, b));
            request.headers.remove("Referer");
            request.headers.add("Referer", url);

            final String location = response.getHeaders().get("Location");
            if (location.startsWith("http://") || location.startsWith("https://")) {
                url = location;
            } else if (location.startsWith("https")) {
                url = url.replaceFirst("http", "https");
            } else {
                url = protocol + "://" + host;
                if (!location.startsWith("/")) {
                    url += "/";
                }
                url += location;
            }
            execute(service, callback);
        } catch (IOException e) {
        }
    }
}