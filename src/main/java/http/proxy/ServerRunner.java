package http.proxy;

import http.proxy.logger.FileOutLogger;
import http.proxy.logger.Logger;
import http.proxy.logger.STDOutLogger;
import http.proxy.utils.ProxyServerPropertiesReader;

import java.io.IOException;

public final class ServerRunner {

    public static void main(String[] args) {
        if (args.length < 1)
            throw new IllegalArgumentException("You have to specify the path to the configuration file");

        final ProxyServerPropertiesReader props = new ProxyServerPropertiesReader(args[0]);
        Logger logger;

        /** Если указан путь к файлу лога, то используем логер в файл*/
        if (props.getLogFile() != null) {
            try {
                logger = new FileOutLogger(props.getLogFile());
                System.out.println();
            } catch (IOException e) {
                logger = new STDOutLogger();
                logger.log(Logger.Level.WARNING, "Can't create FileOutLogger because of " + e.getMessage());
            }
        } else {
            logger = new STDOutLogger();
        }

        logger.log(Logger.Level.INFO, "Server started on port: " + props.getPort());
        logger.log(Logger.Level.INFO, "Cache lifetime: " +
                props.getLifetime() +
                " seconds, cache size " +
                props.getCacheSize() +
                " bytes"
        );
        try {
            final ProxyServer proxyServer = new ProxyServer(
                    props.getPort(),
                    props.getCacheSize(),
                    props.getLifetime(),
                    logger
            );
            proxyServer.start();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}
