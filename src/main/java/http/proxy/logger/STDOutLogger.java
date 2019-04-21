package http.proxy.logger;

import java.net.Socket;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static http.proxy.constants.Constants.HEADER_DELIM;
import static http.proxy.constants.Constants.SPACE;

public class STDOutLogger implements Logger {

    private Lock lock = new ReentrantLock();

    @Override
    public void log(Level level, Socket socket, String method, String url, boolean isRequest, String message) {
        lock.lock();
        final String result = level.getPrefix() +
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

        System.out.println(result);
        lock.unlock();
    }

    @Override
    public void log(Level level, Socket socket, String message) {
        lock.lock();
        System.out.println(level.getPrefix() +
                SPACE +
                new Date(System.currentTimeMillis()).toInstant() +
                SPACE +
                ((socket != null) ? socket.getInetAddress() + HEADER_DELIM + socket.getPort() : "") +
                SPACE +
                ((message != null) ? message : "")
        );
        lock.unlock();
    }

    @Override
    public void log(Level level, String message) {
        lock.lock();
        System.out.println(level.getPrefix() +
                SPACE +
                new Date(System.currentTimeMillis()).toInstant() +
                SPACE +
                ((message != null) ? message : "")
        );
        lock.unlock();
    }
}
