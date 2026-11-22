package com.library.librarysystem.controller;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

// One page of results, in a shape we control (Spring's own Page is not meant to be sent as JSON).
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
