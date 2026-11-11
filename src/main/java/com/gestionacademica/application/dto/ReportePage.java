package com.gestionacademica.application.dto;

import java.util.List;

public record ReportePage<T>(List<T> items, int page, int size, long totalElements, int totalPages) {
    public ReportePage {
        items = List.copyOf(items);
    }
}
