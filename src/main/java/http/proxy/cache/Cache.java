package http.proxy.cache;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Класс для сохранение ответов. Ответы оборнуты в ResponseWrapper
 * для удобной работы с ними.
 */
public class Cache {

    private final Map<String, ResponseWrapper> cache = new ConcurrentHashMap<>();
    private final Deque<String> orderedKeys = new LinkedBlockingDeque<>();

    private int size;

    /**
     * Если responseWrapper валидный кладем его в кэш
     * @param url url по, которому был получен ответ
     * @param response ответ, соответвующий данному url
     */
    void put(final String url, final ResponseWrapper response) {
        synchronized (orderedKeys) {
            if (response.isValid()) {
                orderedKeys.add(url);
                cache.put(url, response);
                size += response.length();
            }
        }
    }

    synchronized boolean contains(final String url) {
        return cache.containsKey(url);
    }

    /**
     * Возвращаем копию так как буфер в Response,
     * который находится внутри ResponseWrapper
     * можно читать только один раз, а копия из кэша
     * может запрашивать несколько раз
     *
     * @param url адрес
     * @return новый экземпляр ResponseWrapper
     */
    synchronized ResponseWrapper get(String url) {
        return cache.get(url).copy();
    }

    /**
     * @param url
     */
    private void remove(final String url) {
        synchronized (orderedKeys) {
            if (cache.containsKey(url)) {
                size -= cache.get(url).length();
                cache.remove(url);
            }
        }
    }

    int getSize() {
        return size;
    }

    /**
     * Удалить самый первый элемент, то есть тот
     * который дольше всех находится в кэше
     *
     * @return ключ - соответсвующий удаленному элементу
     */
    String removeOldest() {
        synchronized (orderedKeys) {
            final String first = orderedKeys.pollFirst();
            remove(first);
            return first;
        }
    }

    /**
     * Время, когда был добавлен самый старые элемент в
     * кэше
     *
     * @return время в миллисекундах
     */
    long oldestCreatedTime() {
        synchronized (orderedKeys) {
            return get(orderedKeys.getFirst()).getCreatedTime();
        }
    }

    synchronized boolean isNotEmpty() {
        return !cache.isEmpty();
    }

    synchronized boolean isEmpty() {
        return cache.isEmpty();
    }

    public Deque<String> getOrderedKeys() {
        return orderedKeys;
    }
}
