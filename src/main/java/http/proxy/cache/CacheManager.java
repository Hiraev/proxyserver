package http.proxy.cache;

import http.proxy.logger.Logger;
import http.proxy.utils.Response;

import java.util.Timer;
import java.util.TimerTask;

import static http.proxy.constants.Constants.*;

/**
 * Менеджер для работы с кэшем, всем действия, выполняемые с ним синхронизированы
 * принимает в конструкторе размер кэша и время хранения в кеше
 * При инициализации запускает timer.schedule(), который каждые lifetime секунд
 * проверяет кэш на наличие устаревших значений и удаляет их, если таковые находятся.
 * Timer Schedule работает в фоне, не мешяю основному потоку.
 * Все обращения синхронизированы по кэшу, чтобы действия надо кэшем могу выполнять только
 * один поток.
 */
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
                        while (cache.isNotEmpty()
                                && System.currentTimeMillis() - cache.oldestCreatedTime() > lifetime
                        ) {
                            final String removedUrl = cache.removeOldest();
                            if (logger != null)
                                logger.log(Logger.Level.INFO, CACHE_OUTDATED +
                                        SPACE +
                                        cache.getSize() +
                                        SPACE +
                                        removedUrl
                                );
                        }
                    } catch (IllegalStateException e) {
                        if (logger != null)
                            logger.log(Logger.Level.EXCEPTION, VERY_BAD_CACHE_EXCEPTION +
                                    SPACE +
                                    e.getMessage()
                            );
                    }
                }
            }
        };
    }

    /**
     * Кладем новое значение в кэш, если раньше его там не было.
     * <p>
     * Ответ и тело ответа передаются отдельно, чтобы было
     * удобнее копировать кэшированный ответ.
     * <p>
     * Если в кэше недостатоно места, то будем удалять оттуда
     * элементы, пока место не появится.
     * <p>
     * Если всталяемый файл слишком большой для кеша, то вставка
     * не произойдет
     *
     * @param url      адрес
     * @param response ответ
     */
    public void put(final String url, final Response response) {
        synchronized (cache) {
            if (cache.contains(url)) return;
            if (maxSize < response.getContentLength()) {
                if (logger != null)
                    logger.log(Logger.Level.WARNING, CACHE_TOO_BIG +
                            SPACE +
                            response.getContentLength() +
                            " bytes"
                    );
                return;
            }

            while (cache.getSize() + response.getContentLength() > maxSize) {
                final String removedUrl = cache.removeOldest();
                if (logger != null)
                    logger.log(Logger.Level.INFO, CACHE_NO_SPACE +
                            SPACE +
                            cache.getSize() +
                            SPACE +
                            HEADER_DELIM +
                            SPACE +
                            removedUrl
                    );
            }
            cache.put(url, response);
            logger.log(Logger.Level.INFO,
                    CACHE_INSERTED +
                            SPACE +
                            response.getUrl()
            );
        }
    }

    /**
     * Получить кэшированный ответ из кэша
     *
     * @param url адрес
     * @return кэшированный ответ
     */
    public Response getResponse(final String url) {
        synchronized (cache) {
            if (!contains(url)) return null;
            logger.log(Logger.Level.INFO, CACHE_RETURNED + SPACE + url);
            return cache.get(url);
        }
    }


    public boolean contains(final String url) {
        synchronized (cache) {
            return cache.contains(url);
        }
    }

}
