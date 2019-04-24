package http.proxy.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import static http.proxy.constants.Constants.*;

/**
 * Собственная реализация класса, аналогичного тому, что бы взят из OkHttp
 */
public class Headers {

    private List<String> names;
    private List<String> values;

    public Headers() {
        names = new ArrayList<>();
        values = new ArrayList<>();
    }

    public Headers(Map<String, List<String>> map) {
        Objects.requireNonNull(map);
        names = new ArrayList<>();
        values = new ArrayList<>();
        map.forEach((k, v) -> {
            if (k != null && v != null) {
                names.add(k);
                values.add(v.stream().reduce((acc, it) -> acc + ", " + it).orElse(""));
            }
        });
        assert (names.size() == values.size());
    }

    public void add(String name, String value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
        names.add(name);
        values.add(value);
        assert (names.size() == values.size());
    }

    public String get(String name) {
        for (int i = 0; i < names.size(); i++) {
            if (names.get(i).equalsIgnoreCase(name)) {
                return values.get(i);
            }
        }
        return null;
    }

    public void remove(String name) {
        for (int i = 0; i < names.size(); i++) {
            if (names.get(i).equalsIgnoreCase(name)) {
                names.remove(i);
                values.remove(i);
                break;
            }
        }
        assert (names.size() == values.size());
    }

    public void forEach(BiConsumer<String, String> action) {
        Objects.requireNonNull(action);
        for (int i = 0; i < names.size(); i++) {
            action.accept(names.get(i), values.get(i));
        }
    }

    @Override
    public String toString() {
        assert (names.size() == values.size());
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            builder.append(names.get(i))
                    .append(HEADER_DELIM + SPACE)
                    .append(values.get(i))
                    .append(CRLF);
        }
        return builder.toString();
    }
}
