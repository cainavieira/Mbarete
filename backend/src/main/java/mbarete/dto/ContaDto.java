package mbarete.dto;

import org.jspecify.annotations.Nullable;

/** A conta como o Google a deu no último login; a foto pode faltar (UC-12). */
public record ContaDto(String nome, String email, @Nullable String foto) {}
