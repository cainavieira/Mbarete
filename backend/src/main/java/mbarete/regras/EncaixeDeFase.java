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
      recusarFim(inicio, fim, null, hoje);
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
   * Confere as datas novas da fase editada contra as outras fases da pessoa (RN-18, RN-19, RN-24) e
   * devolve a fase anterior que acompanha a mudança do início, já com o fim na nova véspera
   * (RN-67).
   *
   * <p>Editar nunca encerra outra fase: qualquer sobreposição é recusada. A exceção é a anterior
   * com fim no futuro, que está ali por causa desta (RN-22) e o move junto, inclusive para hoje ou
   * para o passado; antecipar até o início dela, ou antes, é recusado. O fim no futuro que a fase
   * editada já tinha fica, porque ele também nasceu da fase seguinte.
   */
  public static Optional<PeriodoDeFase> encaixarEdicao(
      List<PeriodoDeFase> fases,
      PeriodoDeFase antes,
      LocalDate inicio,
      @Nullable LocalDate fim,
      LocalDate hoje) {
    if (fim != null) {
      recusarFim(inicio, fim, antes.fim(), hoje);
    }
    List<PeriodoDeFase> outras = fases.stream().filter(fase -> fase.id() != antes.id()).toList();
    Optional<PeriodoDeFase> movida =
        outras.stream()
            .filter(fase -> !inicio.equals(antes.inicio()))
            .filter(fase -> acompanha(fase, antes, hoje))
            .findFirst()
            .map(
                anterior ->
                    new PeriodoDeFase(anterior.id(), anterior.inicio(), inicio.minusDays(1)));
    if (movida.isPresent() && !inicio.isAfter(movida.get().inicio())) {
      throw new RegraVioladaException(Recusa.FASE_SOBREPOSTA, "dataInicio", SOBREPOSTA);
    }
    recusarSobreposicao(
        outras.stream().filter(fase -> movida.isEmpty() || fase.id() != movida.get().id()).toList(),
        inicio,
        fim);
    return movida;
  }

  /**
   * A fase com o fim do encerramento (UC-14). Só a fase corrente, a que cobre hoje, se encerra, e
   * ela pode ter fim no futuro, posto pela seguinte agendada; a fase em aberto que ainda não
   * começou não é corrente (RN-27). O fim não pode ser futuro nem anterior ao início (RN-18,
   * RN-67).
   */
  public static PeriodoDeFase encerrarFase(PeriodoDeFase fase, LocalDate fim, LocalDate hoje) {
    if (!fase.cobre(hoje)) {
      throw new RegraVioladaException(
          Recusa.FASE_NAO_CORRENTE, "Só a fase corrente, a que cobre hoje, pode ser encerrada.");
    }
    recusarFim(fase.inicio(), fim, null, hoje);
    return new PeriodoDeFase(fase.id(), fase.inicio(), fim);
  }

  /**
   * A anterior com fim no futuro, na véspera da editada: esse fim acompanha o início dela (RN-67).
   */
  private static boolean acompanha(PeriodoDeFase fase, PeriodoDeFase antes, LocalDate hoje) {
    LocalDate fimDaOutra = fase.fim();
    return fimDaOutra != null
        && fimDaOutra.isAfter(hoje)
        && fimDaOutra.equals(antes.inicio().minusDays(1));
  }

  /**
   * O fim não pode ser futuro nem anterior ao início (RN-18, RN-67). O fim no futuro que a fase já
   * tinha passa, porque ele nasceu da fase seguinte, e não de uma declaração (RN-22).
   */
  private static void recusarFim(
      LocalDate inicio, LocalDate fim, @Nullable LocalDate fimQueJaEstava, LocalDate hoje) {
    if (fim.isAfter(hoje) && !fim.equals(fimQueJaEstava)) {
      throw new RegraVioladaException(
          Recusa.DATA_FIM_FUTURA, "dataFim", "A data de fim não pode estar no futuro.");
    }
    if (fim.isBefore(inicio)) {
      throw new RegraVioladaException(
          Recusa.DATA_FIM_ANTES_DO_INICIO,
          "dataFim",
          "A data de fim não pode ser anterior à de início.");
    }
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
