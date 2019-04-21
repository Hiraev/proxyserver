package http.proxy.utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * Класс для чтения настроек из файла
 * При ошибке чтении (отсутсвие файла, неверный формат и т.п.)
 * программа будет завершена с печатью stack trace
 */
public final class ProxyServerPropertiesReader {

    private int port;
    private int lifetime;
    private int cacheSize;
    private final File file;

    public ProxyServerPropertiesReader(final String path) {
        this.file = new File(path);
        read();
    }

    private void read() {
        try {
            final Properties properties = new Properties();
            properties.load(new FileReader(file));
            cacheSize = Integer.valueOf(properties.getProperty("cache_size"));
            lifetime = Integer.valueOf(properties.getProperty("lifetime"));
            port = Integer.valueOf(properties.getProperty("port"));
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public int getPort() {
        return port;
    }

    public int getLifetime() {
        return lifetime;
    }

    public int getCacheSize() {
        return cacheSize;
    }

}
