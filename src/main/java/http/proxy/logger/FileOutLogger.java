package http.proxy.logger;

import http.proxy.constants.Constants;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FileOutLogger extends AbstractLogger {

    private Lock lock;
    private final BufferedWriter writer;

    public FileOutLogger(final String path) throws IOException {
        final File file = new File(path);
        file.getParentFile().mkdirs();
        if (!file.exists()) file.createNewFile();
        this.writer = new BufferedWriter(new FileWriter(file));
        lock = new ReentrantLock();
        System.out.println("Log messages will be written in " + file.getAbsolutePath());
    }

    @Override
    public void log(Level level, Socket socket, String method, String url, boolean isRequest, String message) {
        lock.lock();
        try {
            writer.write(getString(level, socket, method, url, isRequest, message) + Constants.LF);
            writer.flush();
        } catch (IOException e) {
        }
        lock.unlock();
    }

    @Override
    public void log(Level level, Socket socket, String message) {
        lock.lock();
        try {
            writer.write(getString(level, socket, message) + Constants.LF);
            writer.flush();
        } catch (IOException e) {
        }
        lock.unlock();
    }

    @Override
    public void log(Level level, String message) {
        lock.lock();
        try {
            writer.write(getString(level, message) + Constants.LF);
            writer.flush();
        } catch (IOException e) {
        }
        lock.unlock();
    }

}
