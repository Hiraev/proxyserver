package http.proxy.exceptions;

import java.util.Arrays;
import java.util.List;

/**
 * Исключение для не поддерживаемых запросов
 */
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

    /**
     * @return список доступных методоа
     */
    public List<String> getAllowedMethods() {
        return Arrays.asList(GET_METHOD, POST_METHOD, HEAD_METHOD);
    }

    /**
     * @return метод, который был запошен
     */
    public String getRequestedMethod() {
        return method;
    }
}
