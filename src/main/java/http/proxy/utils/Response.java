package http.proxy.utils;

import http.proxy.exceptions.BadRequestException;

import java.io.InputStream;
import java.util.Arrays;

import static http.proxy.constants.Constants.HEAD_METHOD;
import static http.proxy.constants.Constants.SPACE;

public final class Response extends HttpReader {

    private String protocol;
    private int code;
    private String message;
    private long createdTime;
    private Request request;

    public Response(Request request) {
        this.request = request;
    }

    @Override
    public void read(InputStream is) throws Exception {
        super.readTopLine(is);
        createdTime = System.currentTimeMillis();
        final String[] s = getFirstLine().split(SPACE);
        if (s.length < 3) throw new BadRequestException();
        protocol = s[0];
        code = Integer.valueOf(s[1]);
        message = String.join(SPACE, Arrays.copyOfRange(s, 2, s.length));

        //Если ответ содержит редирект или это ответ на метод HEAD, то не читаем
        //тело
        if (
                Arrays.asList(201, 301, 302, 303, 307, 308).contains(code)
                        || HEAD_METHOD.equalsIgnoreCase(request.getMethod())
        ) {
            readBody = false;
        }
        super.readHeaders(is);
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public String getUrl() {
        return request.getUrl();
    }
}
