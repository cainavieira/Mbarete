package mbarete.exception;

import org.jspecify.annotations.Nullable;

/**
 * O formato único de erro (convencoes-backend.md). O campo e a regra ficam nulos quando a recusa
 * não é de um campo, ou não vem de uma regra.
 */
public record RespostaDeErro(Erro erro) {

  public record Erro(
      String codigo, @Nullable String campo, String mensagem, @Nullable String regra) {}
}
