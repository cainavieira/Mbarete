package mbarete.regras;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public final class EncaixeDeFase {

  private static final String SOBREPOSTA = "As datas invadem outra fase.";

  private EncaixeDeFase() {}

  /**
   * Confere as datas da fase nova contra as outras fases da pessoa (RN-18, RN-19, RN-67) e devolve
   * a fase em aberto que ela encerra, já com o fim na véspera do início dela (RN-22).
   *
   * <p>Com data de fim, a fase nova classifica um período e não encerra ninguém: invadir qualquer
   * fase é recusado. Sem data de fim, ela encerra a fase em aberto quando começa depois dela, e
   * invadir qualquer outra é recusado.
   */
  public static Optional<PeriodoDeFase> encaixarFaseNova(
      List<PeriodoDeFase> fases, LocalDate inicio, @Nullable LocalDate fim, LocalDate hoje) {
    if (fim != null) {
      if (fim.isAfter(hoje)) {
        throw new RegraVioladaException(
            Recusa.DATA_FIM_FUTURA, "dataFim", "A data de fim não pode estar no futuro.");
      }
      if (fim.isBefore(inicio)) {
        throw new RegraVioladaException(
            Recusa.DATA_FIM_ANTES_DO_INICIO,
            "dataFim",
            "A data de fim não pode ser anterior à de início.");
      }
      recusarSobreposicao(fases, inicio, fim);
      return Optional.empty();
    }
    Optional<PeriodoDeFase> encerrada =
        fases.stream()
            .filter(fase -> fase.fim() == null && fase.inicio().isBefore(inicio))
            .findFirst()
            .map(aberta -> new PeriodoDeFase(aberta.id(), aberta.inicio(), inicio.minusDays(1)));
    recusarSobreposicao(
        fases.stream()
            .filter(fase -> encerrada.isEmpty() || fase.id() != encerrada.get().id())
            .toList(),
        inicio,
        null);
    return encerrada;
  }

  /**
   * A recusa destaca o início quando ele cai dentro de outra fase; quando o início está livre, é o
   * fim, ou a falta dele, que invade a seguinte.
   */
  private static void recusarSobreposicao(
      List<PeriodoDeFase> fases, LocalDate inicio, @Nullable LocalDate fim) {
    for (PeriodoDeFase fase : fases) {
      LocalDate fimDaOutra = fase.fim();
      boolean comecaAntesDoFim = fim == null || !fase.inicio().isAfter(fim);
      boolean terminaDepoisDoInicio = fimDaOutra == null || !fimDaOutra.isBefore(inicio);
      if (comecaAntesDoFim && terminaDepoisDoInicio) {
        throw new RegraVioladaException(
            Recusa.FASE_SOBREPOSTA, fase.cobre(inicio) ? "dataInicio" : "dataFim", SOBREPOSTA);
      }
    }
  }
}
