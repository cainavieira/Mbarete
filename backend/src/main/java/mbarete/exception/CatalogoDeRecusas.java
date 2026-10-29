package mbarete.exception;

import mbarete.regras.FaixaDoRegistro;
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
      case NIVEL_AUSENTE -> new Descricao(HttpStatus.BAD_REQUEST, "RN-28");
      case TAXA_ALVO_AUSENTE, TAXA_ALVO_INVALIDA -> new Descricao(HttpStatus.BAD_REQUEST, "RN-29");
      case PESO_ALVO_AUSENTE -> new Descricao(HttpStatus.BAD_REQUEST, "RN-41");
      case FASE_SOBREPOSTA -> new Descricao(HttpStatus.CONFLICT, "RN-19");
      case DATA_FIM_FUTURA -> new Descricao(HttpStatus.BAD_REQUEST, "RN-67");
      case DATA_FIM_ANTES_DO_INICIO -> new Descricao(HttpStatus.BAD_REQUEST, "RN-18");
      case FASE_NAO_CORRENTE -> new Descricao(HttpStatus.CONFLICT, "RN-27");
      case CSV_COLUNA_AUSENTE -> new Descricao(HttpStatus.BAD_REQUEST, "RN-53");
      case CSV_SEPARADOR_DESCONHECIDO -> new Descricao(HttpStatus.BAD_REQUEST, "RN-54");
      case EMAIL_NAO_CONFERE -> new Descricao(HttpStatus.BAD_REQUEST, "UC-12");
      case PREVISUALIZACAO_DESATUALIZADA -> new Descricao(HttpStatus.CONFLICT, "RN-01");
    };
  }

  /**
   * A frase que a pessoa lê quando a recusa vem de um campo do corpo: a anotação do DTO traz só o
   * código, e a frase mora aqui, uma vez por código.
   */
  static String mensagemDoCampo(Recusa recusa) {
    return switch (recusa) {
      case PESO_FORA_DE_FAIXA -> FaixaDoRegistro.PESO_FORA_DE_FAIXA;
      case CINTURA_FORA_DE_FAIXA -> FaixaDoRegistro.CINTURA_FORA_DE_FAIXA;
      case TAXA_ALVO_INVALIDA -> "A taxa alvo é digitada sem sinal e deve ser maior que zero.";
      default -> "Confira o campo destacado.";
    };
  }
}
