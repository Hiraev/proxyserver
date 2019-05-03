package http.proxy;

import http.proxy.cache.CacheManager;
import http.proxy.logger.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Proxy сервер
 * Хранит в себе ExecutorService с размером 10.
 * Это значит, что 10 задач могут выполняться параллельно или конкуретно (в
 * зависимости от количества ядер процессора)
 * Executor сервис получает на выполнение Runnable, помещает его в очередь
 * и выполняет, когда один из потоков освобождается
 * <p>
 * Здесь используется FixedThreadPool - это значит, что создается фиксированное количество потоков,
 * которые переиспользуются для выполнения новых задач
 * <p>
 * В качестве задачи в нашем случаем выступает SocketHandler, который реализует Runnable
 */
public final class ProxyServer {

    private final ExecutorService executorService;
    private final ExecutorService onResponseExecutorService;
    private final CacheManager cacheManager;
    private final ServerSocket serverSocket;
    private final Logger logger;
    private final int soTimeout = 30000; //30 секунд, разрешенное время бездействия входного потока сокета

    ProxyServer(final int port, final int cacheSize, final int lifetime, final Logger logger) throws IOException {
        executorService = Executors.newFixedThreadPool(10);
        onResponseExecutorService = Executors.newFixedThreadPool(10);
        cacheManager = new CacheManager(cacheSize, lifetime);
        serverSocket = new ServerSocket(port);
        this.logger = logger;
        cacheManager.registerLogger(logger);
    }

    public void start() {
        while (true) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();
                socket.setSoTimeout(soTimeout);
                executorService.submit(new SocketHandler(socket, logger, cacheManager, onResponseExecutorService));
            } catch (IOException e) {
                logger.log(Logger.Level.EXCEPTION, socket, "Something went wrong when accepting a socket");
            }
        }
    }

}
