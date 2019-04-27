package http.proxy.logger;

import java.net.Socket;
import java.util.Date;

import static http.proxy.constants.Constants.HEADER_DELIM;
import static http.proxy.constants.Constants.SPACE;

abstract class AbstractLogger implements Logger {

    protected String getString(Level level, Socket socket, String method, String url, boolean isRequest, String message) {
        return level.getPrefix() +
                SPACE +
                new Date(System.currentTimeMillis()).toInstant() +
                SPACE +
                socket.getInetAddress() +
                HEADER_DELIM +
                socket.getPort() +
                SPACE +
                method +
                SPACE +
                ((isRequest) ? ">>>>>>" : "<<<<<<") +
                SPACE +
                url +
                SPACE +
                ((message != null) ? message : "");
    }

    protected String getString(Level level, Socket socket, String message) {
        return level.getPrefix() +
                SPACE +
                new Date(System.currentTimeMillis()).toInstant() +
                SPACE +
                ((socket != null) ? socket.getInetAddress() + HEADER_DELIM + socket.getPort() : "") +
                SPACE +
                ((message != null) ? message : "");
    }

    protected String getString(Level level, String message) {
        return level.getPrefix() +
                SPACE +
                new Date(System.currentTimeMillis()).toInstant() +
                SPACE +
                ((message != null) ? message : "");
    }

}
