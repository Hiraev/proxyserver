package http.proxy.exeptions;

import java.net.SocketException;

import static http.proxy.constants.Constants.REQUEST_TIMEOUT;

public class RequestTimeoutException extends SocketException {

    @Override
    public String getMessage() {
        return REQUEST_TIMEOUT;
    }

}
