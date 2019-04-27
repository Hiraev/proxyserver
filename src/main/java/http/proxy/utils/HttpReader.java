package http.proxy.utils;

import http.proxy.exceptions.BadSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static http.proxy.constants.Constants.*;

/**
 * Считывает данные из входного потока,
 * это может быть либо запрос, либо ответ
 */
public abstract class HttpReader {

    protected String firstLine;
    protected Headers headers;
    private int contentLength = -1;
    private byte[] body;
    private boolean isRequest = false;

    protected void readInputStream(InputStream is) throws IOException, BadSyntaxException {
        if (is == null) return;
        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        //Если headers не null, значит наследники, которые реализут этот класс уже прочитали их
        if (headers == null) {
            isRequest = true;
            final List<String> lines = new ArrayList<>();

            /**Читаем строчки до пустой строки
             * Пустая строка является разделителем между заголовка и телом запроса */
            String line;
            while (!(line = bufferedReader.readLine()).isEmpty()) {
                lines.add(line);
            }

            final Iterator<String> iterator = lines.iterator();
            firstLine = iterator.next();

            headers = new Headers();
            while (iterator.hasNext()) {
                String[] header = iterator.next().split(HEADER_DELIM);
                if (header.length < 2) throw new BadSyntaxException();
                headers.add(header[0].trim(), header[1].trim());
            }
        }

        final String contentLengthString = headers.get(CONTENT_LENGTH);
        contentLength = (contentLengthString == null) ? 0 : Integer.valueOf(contentLengthString);

        if (contentLength > 0) {
            int i = 0;
            /** Считываем только contentLength символов */
            body = new byte[contentLength];
            while (i < contentLength) {
                if (isRequest) {
                    body[i++] = (byte) bufferedReader.read();
                } else {
                    body[i++] = (byte) is.read();
                }
            }
        }
        if (CHUNKED.equalsIgnoreCase(headers.get(TRANSFER_ENCODING))) {
            //Удялем информацию о чанках
            headers.remove(TRANSFER_ENCODING);
            readChuncked(is, bufferedReader);
            //Записыаем информацию о размере
            headers.add(CONTENT_LENGTH, String.valueOf(body.length));
        }
    }

    /**
     * Если размер входного потока данных заранее не известен, то будет читать его до получения
     * символа конца потока, которые в форме int равен -1
     *
     * @param is входной поток
     * @throws IOException
     */
    private void readChuncked(InputStream is, BufferedReader br) throws IOException {
        final List<Byte> chunkedBody = new ArrayList<>();
        byte b;
        if (isRequest) {
            while ((b = (byte) br.read()) != -1) {
                chunkedBody.add(b);
            }
        } else {
            while ((b = (byte) is.read()) != -1) {
                chunkedBody.add(b);
            }
        }
        body = new byte[chunkedBody.size()];
        //Копируем считанное тело к себе.
        for (int i = 0; i < chunkedBody.size(); i++) {
            body[i] = chunkedBody.get(i);
        }
    }

    protected String getFirstLine() {
        return firstLine;
    }

    /**
     * НЕ ВКЛЮЧАЕТ В СЕБЯ ТЕЛО
     * ТЕЛО УДОБНЕЕ ПОСЫЛАТЬ ОТДЕЛЬНО
     *
     * @return первая строка + заголовки + пустая строка
     */
    @Override
    public String toString() {
        //Конструируем запрос
        return firstLine +
                CRLF +
                getHeaders().toString() +
                CRLF;
    }

    public abstract String getProtocol();

    public abstract void read(InputStream is) throws Exception;

    public Headers getHeaders() {
        return headers;
    }

    public int getContentLength() {
        return contentLength;
    }

    public byte[] getBody() {
        return body;
    }
}
