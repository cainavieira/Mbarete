package mbarete.exception;

import mbarete.regras.Recusa;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;

/** O status e a regra de cada código (convencoes-backend.md, endpoints.md). */
final class CatalogoDeRecusas {

  record Descricao(HttpStatus status, @Nullable String regra) {}

  private CatalogoDeRecusas() {}

  static Descricao descricao(Recusa recusa) {
    return switch (recusa) {
      case PESO_FORA_DE_FAIXA -> new Descricao(HttpStatus.BAD_REQUEST, "RN-03");
      case DATA_LOCAL_INVALIDA -> new Descricao(HttpStatus.BAD_REQUEST, "RN-04");
      case NAO_ENCONTRADO -> new Descricao(HttpStatus.NOT_FOUND, "RN-50");
      case CORPO_MALFORMADO -> new Descricao(HttpStatus.UNPROCESSABLE_CONTENT, null);
      case ERRO_INTERNO -> new Descricao(HttpStatus.INTERNAL_SERVER_ERROR, "RNF-13");
      case NAO_AUTENTICADO -> new Descricao(HttpStatus.UNAUTHORIZED, "ADR-0041");
      case CINTURA_FORA_DE_FAIXA -> new Descricao(HttpStatus.BAD_REQUEST, "RN-11");
      case DATA_FUTURA -> new Descricao(HttpStatus.BAD_REQUEST, "RN-05");
      case REGISTRO_JA_EXISTE -> new Descricao(HttpStatus.CONFLICT, "RN-01");
    };
  }

  /**
   * A frase que a pessoa lê quando a recusa vem de um campo do corpo: a anotação do DTO traz só o
   * código, e a frase mora aqui, uma vez por código.
   */
  static String mensagemDoCampo(Recusa recusa) {
    return switch (recusa) {
      case PESO_FORA_DE_FAIXA -> "O peso deve estar entre 20,0 e 400,0 kg.";
      case CINTURA_FORA_DE_FAIXA -> "A cintura deve estar entre 40,0 e 200,0 cm.";
      default -> "Confira o campo destacado.";
    };
  }
}
