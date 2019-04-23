package http.proxy.utils;

import http.proxy.exceptions.BadRequestException;
import http.proxy.exceptions.BadSyntaxException;
import http.proxy.exceptions.MethodNotAllowedException;
import http.proxy.exceptions.RequestTimeoutException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
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
            super.readInputStream(is);
            //Первая строка содержит метод, запрос и версию протокола
            final String[] s = getFirstLine().split(SPACE);
            if (s.length != 3) throw new BadRequestException();
            method = s[0];
            url = s[1];
            protocol = s[2];
            if (!Arrays.asList(GET_METHOD, HEAD_METHOD, POST_METHOD).contains(method))
                throw new MethodNotAllowedException(method);

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
     */
    public void execute(ExecutorService service, Callback callback) {
        service.submit(
                () -> {
                    try {
//                        //TODO Здесь получаем Gateway Timeout
                        URL urlObject = new URL(url);
                        HttpURLConnection.setFollowRedirects(true);
                        HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection(Proxy.NO_PROXY);
                        connection.setRequestMethod(method);
                        connection.setUseCaches(false);
                        getHeaders().forEach(connection::addRequestProperty);
                        connection.setReadTimeout(10 * 1000);
                        connection.setConnectTimeout(10 * 1000);
                        if (getContentLength() > 0) {
                            connection.setDoOutput(true);
                            connection.getOutputStream().write(getBody());
                        }

                        int responseCode = connection.getResponseCode();

                        String[] firstLine = connection.getHeaderField(null).split(SPACE);
                        Response response = new Response(
                                url,
                                firstLine[0],
                                firstLine[1],
                                firstLine[2],
                                connection.getHeaderFields()
                        );

                        if (responseCode > 299) {
                            response.read(connection.getErrorStream());
                        } else {
                            response.read(connection.getInputStream());
                        }
                        callback.onSuccess(this, response);
                    } catch (Exception e) {
                        callback.onFailure(this, e);
                    }
                }
        );
    }
}