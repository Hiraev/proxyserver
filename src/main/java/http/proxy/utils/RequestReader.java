package http.proxy.utils;

import http.proxy.exceptions.BadRequestException;
import http.proxy.exceptions.RequestTimeoutException;
import http.proxy.exceptions.MethodNotAllowedException;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static http.proxy.constants.Constants.*;

/**
 * Класс для чтения запросов
 * Хранит в себе тело запроса, длину тела запроса
 * адрес, метод, версию http, заголовки и Content-Type (MediaType)
 * <p>
 * Умеет превращаться в Request, который из OkHttp
 */
public final class RequestReader {

    private byte[] body;
    private int contentLength;

    private String url;
    private String method;
    private String httpVersion;

    private Headers headers;
    private MediaType mediaType;

    /**
     * В констуркторе сразу считываем все, что можем
     *
     * @param is - входной поток
     * @throws RequestTimeoutException   выбрасывается, когда клиент слишком доло не отправляет
     *                                   байты
     * @throws MethodNotAllowedException выбрасывается, когда метод, указанный в запросе не
     *                                   поддерживатеся
     * @throws BadRequestException       выбрасывается, когда в запросе присутсвует какая-то ошибка
     */
    public RequestReader(final InputStream is)
            throws RequestTimeoutException, MethodNotAllowedException, BadRequestException {
        try {
            final List<String> lines = new ArrayList<>();
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));

            /**Читаем строчки до пустой строки
             * Пустая строка является разделителем между заголовка и телом запроса */
            String line;
            while (!(line = bufferedReader.readLine()).isEmpty()) {
                lines.add(line);
            }

            //Первая строка содержит метод, запрос и версию протокола
            final Iterator<String> iterator = lines.iterator();
            final String[] s = iterator.next().split(SPACE);
            if (s.length != 3) throw new BadRequestException();
            method = s[0];
            url = s[1];
            httpVersion = s[2];

            if (!Arrays.asList(GET_METHOD, HEAD_METHOD, POST_METHOD).contains(method))
                throw new MethodNotAllowedException(method);

            final Headers.Builder headersBuilder = new Headers.Builder();

            /**
             * Обрабатыаем прочитанные залоговки и суем их в Headers из OkHttp
             */
            while (iterator.hasNext()) {
                String[] header = iterator.next().split(HEADER_DELIM);
                if (header.length < 2) throw new BadRequestException();
                headersBuilder.add(header[0].trim(), header[1].trim());
            }

            headers = headersBuilder.build();
            final String contentLengthString = headers.get(CONTENT_LENGTH);
            contentLength = (contentLengthString == null) ? 0 : Integer.valueOf(contentLengthString);

            mediaType = (contentLength != 0) ? MediaType.parse(headers.get(CONTENT_TYPE)) : null;

            int i = 0;
            /** Считываем только contentLength символов */
            body = new byte[contentLength];
            while (i < contentLength) {
                body[i++] = (byte) bufferedReader.read();
            }
        } catch (IOException e) {
            throw new RequestTimeoutException();
        }
    }

    @Override
    public String toString() {
        //Конструируем запрос
        return method +
                SPACE +
                url +
                SPACE +
                httpVersion +
                LF +
                headers.toString() +
                CRLF +
                new String(body);
    }

    /**
     * Превращает данный запрос в запрос из OkHttp
     *
     * @return OkHttp запрос
     */
    public Request toRequest() {
        final Request.Builder builder = new Request.Builder();
        final RequestBody requestBody = (contentLength != 0) ? RequestBody.create(mediaType, body) : null;
        return builder.headers(headers).method(method, requestBody).url(url).build();
    }

    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }
}
