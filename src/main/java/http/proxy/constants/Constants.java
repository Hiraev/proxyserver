package http.proxy.constants;

public final class Constants {

    public static final String CACHE_RETURNED = "Cache returned";
    public static final String CACHE_INSERTED = "Cache inserted";
    public static final String CACHE_REMOVED = "Cache removed";
    public static final String CACHE_OUTDATED = CACHE_REMOVED + " (OUTDATED)";
    public static final String CACHE_NO_SPACE = CACHE_REMOVED + " (NO SPACE)";
    public static final String CACHE_TOO_BIG = "Cache doesn't have enough space for this";
    public static final String VERY_BAD_CACHE_EXCEPTION = "Caught EXCEPTION when clearing the cache";

    public static final String VERY_BAD_EXCEPTION = "Caught an exception when trying to report another exception";

    public static final String DEFAULT_HTTP_VERSION = "HTTP/1.1";

    public static final String GET_METHOD = "GET";
    public static final String POST_METHOD = "POST";
    public static final String HEAD_METHOD = "HEAD";

    public static final String REQUEST_TIMEOUT = "Request Timeout";
    public static final String GATEWAY_TIMEOUT = "Gateway Timeout";
    public static final String BAD_REQUEST = "Bad Request";
    public static final String METHOD_NOT_ALLOWED = "Method Not Allowed";

    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String TRANSFER_ENCODING = "Transfer-Encoding";
    public static final String CONNECTION = "Connection";
    public static final String ALLOW = "Allow";
    public static final String CHUNKED = "Chunked";

    public static final String CLOSE = "Close";

    public static final String LF = "\n";
    public static final String CRLF = "\r\n";
    public static final String SPACE = " ";
    public static final String HEADER_DELIM = ":";

    public static final int BAD_REQUEST_CODE = 400;
    public static final int METHOD_NOT_ALLOWED_CODE = 405;
    public static final int REQUEST_TIMEOUT_CODE = 408;
    public static final int GATEWAY_TIMEOUT_CODE = 504;

}
