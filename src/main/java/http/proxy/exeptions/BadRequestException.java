package http.proxy.exeptions;

import static http.proxy.constants.Constants.BAD_REQUEST;

public class BadRequestException extends Exception {

    @Override
    public String getMessage() {
        return BAD_REQUEST;
    }

}
