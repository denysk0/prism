package sample;

import java.util.ArrayList;
import java.util.List;

public class TinyService {
    private final List<String> events = new ArrayList<>();
    private int rejected;

    public String normalize(String input) {
        if (input == null) {
            rejected++;
            throw new IllegalArgumentException("input");
        }
        String value = input.trim().toLowerCase();
        if (value.isEmpty()) {
            rejected++;
            throw new IllegalArgumentException("empty");
        }
        if (value.length() > 32) {
            rejected++;
            throw new IllegalArgumentException("too long");
        }
        events.add(value);
        return value;
    }

    public boolean hasEvent(String value) {
        if (value == null) {
            return false;
        }
        return events.contains(value.trim().toLowerCase());
    }

    public List<String> events() {
        return List.copyOf(events);
    }

    public int rejected() {
        return rejected;
    }

    public void clear() {
        events.clear();
        rejected = 0;
    }
}
