package http.proxy.utils;

import http.proxy.exeptions.BadRequestException;
import http.proxy.exeptions.RequestTimeoutException;
import http.proxy.exeptions.MethodNotAllowedException;
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

public final class RequestReader {

    private byte[] body;
    private int contentLength;

    private String url;
    private String method;
    private String httpVersion;

    private Headers headers;
    private MediaType mediaType;

    public RequestReader(final InputStream is)
            throws RequestTimeoutException, MethodNotAllowedException, BadRequestException {
        try {
            final List<String> lines = new ArrayList<>();
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));

            //Читаем строчки до пустой строки
            String line;
            while (!(line = bufferedReader.readLine()).isEmpty()) {
                lines.add(line);
            }

            //Первая строка содержит метод, запрос и версия протокола
            final Iterator<String> iterator = lines.iterator();
            final String[] s = iterator.next().split(SPACE);
            if (s.length != 3) throw new BadRequestException();
            method = s[0];
            url = s[1];
            httpVersion = s[2];

            if (!Arrays.asList(GET_METHOD, HEAD_METHOD, POST_METHOD).contains(method))
                throw new MethodNotAllowedException(method);

            final Headers.Builder headersBuilder = new Headers.Builder();

            while (iterator.hasNext()) {
                String[] header = iterator.next().split(HEADER_DELIM);
                if (header.length < 2) throw new BadRequestException();
                headersBuilder.add(header[0].trim(), header[1].trim());
            }

            headers = headersBuilder.build();
            final String contentLengthString = headers.get(CONTENT_LENGTH);
            contentLength = (contentLengthString == null) ? 0 : Integer.valueOf(contentLengthString);

            mediaType = (contentLength != 0) ? MediaType.parse(headers.get(CONTENT_TYPE)) : null;

            //Ставим отметку для чтения тела запроса. Будем считывать только contentLength символов
            int i = 0;
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
