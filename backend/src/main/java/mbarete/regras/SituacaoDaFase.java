package mbarete.regras;

import java.time.LocalDate;

/**
 * Onde a fase está em relação a hoje. A corrente cobre hoje, inclusive com o fim agendado, e pode
 * não existir (RN-27); a encerrada terminou antes de hoje (RN-40); a futura ainda não começou
 * (RN-69).
 */
public enum SituacaoDaFase {
  ENCERRADA,
  CORRENTE,
  FUTURA;

  public static SituacaoDaFase situacaoDaFase(PeriodoDeFase fase, LocalDate hoje) {
    if (fase.inicio().isAfter(hoje)) {
      return FUTURA;
    }
    return fase.cobre(hoje) ? CORRENTE : ENCERRADA;
  }
}
