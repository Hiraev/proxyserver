package http.proxy.exceptions;

public class BadSyntaxException extends Exception {

    @Override
    public String getMessage() {
        return "Bad syntax exception";
    }
}
