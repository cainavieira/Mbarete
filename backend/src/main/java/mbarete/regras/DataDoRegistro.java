package mbarete.regras;

import java.time.LocalDate;

public final class DataDoRegistro {

  /** A mesma frase recusa a linha importada com data futura (RN-60). */
  static final String MENSAGEM = "A data do registro não pode estar no futuro.";

  private DataDoRegistro() {}

  /**
   * Recusa o registro depois de hoje, que afirmaria uma medição que ainda não aconteceu (RN-05).
   * Hoje é o dia do dispositivo, e não o do servidor (RN-04).
   */
  public static void recusarDataFutura(LocalDate data, LocalDate hoje) {
    if (data.isAfter(hoje)) {
      throw new RegraVioladaException(Recusa.DATA_FUTURA, "data", MENSAGEM);
    }
  }
}
