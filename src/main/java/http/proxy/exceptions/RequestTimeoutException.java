package http.proxy.exceptions;

import java.net.SocketException;

import static http.proxy.constants.Constants.REQUEST_TIMEOUT;

/**
 * Когда запрос приходит слишком долго
 */
public class RequestTimeoutException extends SocketException {

    @Override
    public String getMessage() {
        return REQUEST_TIMEOUT;
    }

}
