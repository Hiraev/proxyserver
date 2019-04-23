package http.proxy.utils;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public final class Response extends HttpReader {

    private String protocol;
    private int code;
    private String message;
    private long createdTime;
    private String url;

    public Response(String url, String protocol, String code, String message, Map<String, List<String>> headers) {
        this.url = url;
        this.protocol = protocol;
        this.code = Integer.parseInt(code);
        this.message = message;
        super.headers = new Headers(headers);
        firstLine = protocol + " " + code + " " + message;
    }

    @Override
    public void read(InputStream is) throws Exception {
        //TODO Bad Gateway
        super.readInputStream(is);
        createdTime = System.currentTimeMillis();
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
        return url;
    }
}
