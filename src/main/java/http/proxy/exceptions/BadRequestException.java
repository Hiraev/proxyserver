package http.proxy.exceptions;

import static http.proxy.constants.Constants.BAD_REQUEST;

/**
 * Выбрасываем это исключения, когда получаем невалидный запрос
 */
public class BadRequestException extends Exception {

    @Override
    public String getMessage() {
        return BAD_REQUEST;
    }

}
