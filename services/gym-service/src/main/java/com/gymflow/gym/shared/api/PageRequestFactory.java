package com.gymflow.gym.shared.api;

import com.gymflow.gym.shared.error.InvalidRequestException;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class PageRequestFactory {
    private PageRequestFactory() {}

    public static PageRequest create(int page, int size, String sort, Set<String> allowedSorts) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidRequestException("Paginação inválida: page >= 0 e size entre 1 e 100");
        }
        String[] parts = sort.split(",", 2);
        if (!allowedSorts.contains(parts[0])) {
            throw new InvalidRequestException("Campo de ordenação não permitido: " + parts[0]);
        }
        Sort.Direction direction = parts.length == 2 && "desc".equalsIgnoreCase(parts[1])
            ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, parts[0]));
    }
}

