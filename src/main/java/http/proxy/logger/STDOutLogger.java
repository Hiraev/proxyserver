package http.proxy.logger;

import java.net.Socket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class STDOutLogger extends AbstractLogger {

    private Lock lock = new ReentrantLock();

    @Override
    public void log(Level level, Socket socket, String method, String url, boolean isRequest, String message) {
        lock.lock();
        System.out.println(getString(level, socket, method, url, isRequest, message));
        lock.unlock();
    }

    @Override
    public void log(Level level, Socket socket, String message) {
        lock.lock();
        System.out.println(getString(level, socket, message));
        lock.unlock();
    }

    @Override
    public void log(Level level, String message) {
        lock.lock();
        System.out.println(getString(level, message));
        lock.unlock();
    }
}
