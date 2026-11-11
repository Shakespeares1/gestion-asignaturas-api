package com.gestionacademica.infrastructure.web.dto;

import com.gestionacademica.application.dto.ReportePage;
import java.util.List;
import java.util.function.Function;

public record ReportePageResponse<T>(List<T> items, int page, int size, long totalElements, int totalPages) {
    public ReportePageResponse { items = List.copyOf(items); }

    public static <S, T> ReportePageResponse<T> from(ReportePage<S> page, Function<S, T> mapper) {
        return new ReportePageResponse<>(page.items().stream().map(mapper).toList(), page.page(), page.size(),
                page.totalElements(), page.totalPages());
    }
}
