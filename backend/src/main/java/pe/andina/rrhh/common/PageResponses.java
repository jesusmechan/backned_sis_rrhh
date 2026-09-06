package pe.andina.rrhh.common;

import pe.andina.rrhh.dto.AppDtos.PageResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PageResponses {

    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 100;

    private PageResponses() {}

    public static <T> PageResponse<T> of(List<T> items, Integer page, Integer size) {
        int s = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        int p = page == null || page < 1 ? 1 : page;
        long total = items.size();
        int pages = (int) Math.max(1, Math.ceil(total / (double) s));
        if (p > pages) {
            p = pages;
        }
        int from = Math.min((p - 1) * s, items.size());
        int to = Math.min(from + s, items.size());
        return new PageResponse<>(List.copyOf(items.subList(from, to)), p, s, total, pages);
    }

    public static String text(Object... parts) {
        StringBuilder sb = new StringBuilder();
        for (Object part : parts) {
            if (part == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(part);
        }
        return sb.toString();
    }

    public static <T> List<T> search(List<T> items, String q, Function<T, String> haystack) {
        if (q == null || q.isBlank()) {
            return items;
        }
        String needle = q.toLowerCase(Locale.ROOT);
        return items.stream()
                .filter(item -> {
                    String text = haystack.apply(item);
                    return text != null && text.toLowerCase(Locale.ROOT).contains(needle);
                })
                .toList();
    }

    public static <T> List<T> withEstados(List<T> items, String estado, Function<T, Enum<?>> estadoOf) {
        if (estado == null || estado.isBlank()) {
            return items;
        }
        Set<String> wanted = Arrays.stream(estado.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .map(part -> part.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (wanted.isEmpty()) {
            return items;
        }
        return items.stream()
                .filter(item -> {
                    Enum<?> value = estadoOf.apply(item);
                    return value != null && wanted.contains(value.name());
                })
                .toList();
    }
}
