package util;

import java.util.Objects;

public record Pair<K, V>(K key, V value) {
    @Override
    public String toString() {
        return key + "=" + value;
    }

    @Override
    public int hashCode() {
        return key.hashCode() * 13 + (value == null ? 0 : value.hashCode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            if (o instanceof Pair<?, ?> pair) {
                return Objects.equals(key, pair.key) && Objects.equals(value, pair.value);
            } else {
                return false;
            }
        }
    }
}
