package http.proxy.exeptions;

import java.util.Arrays;
import java.util.List;

import static http.proxy.constants.Constants.*;

public class MethodNotAllowedException extends Exception {

    private final String method;

    public MethodNotAllowedException(String method) {
        this.method = method;
    }

    @Override
    public String getMessage() {
        return METHOD_NOT_ALLOWED;
    }

    public List<String> getAllowedMethods() {
        return Arrays.asList(GET_METHOD, POST_METHOD, HEAD_METHOD);
    }

    public String getRequestedMethod() {
        return method;
    }
}
