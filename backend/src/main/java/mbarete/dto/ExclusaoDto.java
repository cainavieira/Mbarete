package mbarete.dto;

import jakarta.validation.constraints.NotNull;

/**
 * A confirmação da exclusão: o e-mail que a pessoa digitou. Vai no corpo, e nunca na URL, que entra
 * no log de acesso (endpoints.md, RNF-17).
 */
public record ExclusaoDto(@NotNull String email) {}
