package tn.esprit.backend_esprittech.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Enveloppe de pagination stable exposée par l'API (évite de sérialiser
 * directement {@code Page}/{@code PageImpl}, dont le format n'est pas garanti).
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    /** Construit une réponse paginée en convertissant chaque élément via {@code mapper}. */
    public static <E, T> PagedResponse<T> from(Page<E> source, Function<E, T> mapper) {
        return new PagedResponse<>(
                source.getContent().stream().map(mapper).toList(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.isFirst(),
                source.isLast()
        );
    }
}
