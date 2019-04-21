package http.proxy.logger;

import java.net.Socket;
import java.util.logging.Level;

public interface Logger {

    enum Level {

        INFO("I"), WARNING("W"), EXCEPTION("E");

        private String prefix;

        Level(String prefix) {
            this.prefix = prefix + ": ";
        }

        public String getPrefix() {
            return prefix;
        }

    }

    void log(Level level, Socket socket, String method, String url, boolean isRequest, String message);

    void log(Level level, Socket socket, String message);

    void log(Level level, String message);
}