package http.proxy.cache;

import http.proxy.logger.Logger;
import okhttp3.Response;

import java.util.Timer;
import java.util.TimerTask;

import static http.proxy.constants.Constants.*;

public final class CacheManager {

    private long maxSize;
    private long lifetime;
    private Logger logger;
    private final Cache cache;
    private TimerTask cleaner;

    /**
     * @param maxSize  максимальный размер кэша в байтах
     * @param lifetime максимальная длительность жизни кешированного файла в секундах
     */
    public CacheManager(final long maxSize, final long lifetime) {
        this.cache = new Cache();
        long minLifeTime = 5;
        if (lifetime < minLifeTime) {
            this.lifetime = minLifeTime * 1000;
        } else {
            this.lifetime = lifetime * 1000;
        }
        if (maxSize > 0) {
            this.maxSize = maxSize;
            initCleaner();
            final Timer timer = new Timer();
            /** Запускаем чистку кэша по расписанию через каждые lifetime миллисекунд */
            timer.schedule(cleaner, 0, this.lifetime);
        }
    }

    public void registerLogger(final Logger logger) {
        this.logger = logger;
    }

    /**
     * Инициализирует чистильшик кэша
     * Через каждые lifetime миллисекунд удаляет из кэша
     * данные, которые живут уже больше lifetime
     */
    private void initCleaner() {
        cleaner = new TimerTask() {
            @Override
            public void run() {
                /** Выполняем синхронизацию по кэшу, чтобы он не менялся пока идет чистка*/
                synchronized (cache) {
                    if (cache.isEmpty()) return;
                    try {
                        while (cache.isNotEmpty() && System.currentTimeMillis() - cache.oldestCreatedTime() > lifetime) {
                            final String removedUrl = cache.removeOldest();
                            if (logger != null)
                                logger.log(Logger.Level.INFO,
                                        CACHE_OUTDATED +
                                                SPACE +
                                                cache.getSize() +
                                                SPACE +
                                                removedUrl
                                );
                        }
                    } catch (IllegalStateException e) {
                        if (logger != null)
                            logger.log(Logger.Level.EXCEPTION, VERY_BAD_CACHE_EXCEPTION + SPACE + e.getMessage());
                    }
                }
            }
        };
    }

    public void put(final String url, final Response response, byte[] body) {
        synchronized (cache) {
            if (cache.contains(url)) return;
            ResponseWrapper responseWrapper = new ResponseWrapper(response, body);
            if (responseWrapper.isValid()) {
                if (maxSize < responseWrapper.length()) {
                    if (logger != null)
                        logger.log(Logger.Level.WARNING,
                                CACHE_TOO_BIG +
                                        SPACE +
                                        responseWrapper.length() +
                                        " bytes");
                    return;
                }
            }

            while (cache.getSize() + responseWrapper.length() > maxSize) {
                final String removedUrl = cache.removeOldest();
                if (logger != null)
                    logger.log(Logger.Level.INFO,
                            CACHE_NO_SPACE +
                                    SPACE +
                                    cache.getSize() +
                                    SPACE +
                                    HEADER_DELIM +
                                    SPACE +
                                    removedUrl);
            }
            cache.put(url, responseWrapper);
            logger.log(Logger.Level.INFO, CACHE_INSERTED + SPACE + response.request().url());
        }
    }

    public Response getResponse(final String url) {
        synchronized (cache) {
            if (!contains(url)) return null;
            logger.log(Logger.Level.INFO, CACHE_RETURNED + SPACE + url);
            return cache.get(url).getResponse();
        }
    }


    public boolean contains(final String url) {
        synchronized (cache) {
            return cache.contains(url);
        }
    }

}
