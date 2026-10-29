package mbarete.regras;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public final class EncaixeDeFase {

  private static final DateTimeFormatter DATA_EXIBIDA = DateTimeFormatter.ofPattern("dd/MM/uuuu");

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
      recusarSobreposicao(fases, inicio, fim, invadida -> caminhoDoPeriodo(invadida, inicio));
      return Optional.empty();
    }
    Optional<PeriodoDeFase> encerrada =
        fases.stream()
            .filter(fase -> fase.fim() == null && fase.inicio().isBefore(inicio))
            .findFirst()
            .map(
                aberta ->
                    new PeriodoDeFase(
                        aberta.id(), aberta.tipo(), aberta.inicio(), inicio.minusDays(1)));
    recusarSobreposicao(
        fases.stream()
            .filter(fase -> encerrada.isEmpty() || fase.id() != encerrada.get().id())
            .toList(),
        inicio,
        null,
        // UC-05 2g: só a em aberto que começa no mesmo dia ou depois chega aqui
        invadida ->
            invadida.fim() == null
                ? " Para adiantar o início de um ciclo, edite a data de início dessa fase."
                : "");
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
    Optional<PeriodoDeFase> queAcompanha =
        outras.stream()
            .filter(fase -> !inicio.equals(antes.inicio()))
            .filter(fase -> acompanha(fase, antes, hoje))
            .findFirst();
    if (queAcompanha.isPresent() && !inicio.isAfter(queAcompanha.get().inicio())) {
      throw sobreposta(queAcompanha.get(), "dataInicio", "");
    }
    Optional<PeriodoDeFase> movida =
        queAcompanha.map(
            anterior ->
                new PeriodoDeFase(
                    anterior.id(), anterior.tipo(), anterior.inicio(), inicio.minusDays(1)));
    recusarSobreposicao(
        outras.stream().filter(fase -> movida.isEmpty() || fase.id() != movida.get().id()).toList(),
        inicio,
        fim,
        // UC-06 2b: o início novo caiu dentro da anterior, que tem fim
        invadida ->
            invadida.fim() != null && invadida.inicio().isBefore(inicio)
                ? " Para adiantar o início, encurte antes essa fase."
                : "");
    return movida;
  }

  /**
   * UC-05 2e: o período dentro do ciclo atual cabe depois de encerrar a fase em aberto; começando
   * no início dela ou antes, encerrá-la não basta, e o caminho é adiar o início dela.
   */
  private static String caminhoDoPeriodo(PeriodoDeFase invadida, LocalDate inicio) {
    if (invadida.fim() != null) {
      return "";
    }
    return invadida.inicio().isBefore(inicio)
        ? " Para classificar um período dentro do ciclo atual, encerre antes essa fase."
        : " Para classificar esse período, adie antes o início dessa fase.";
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
    return new PeriodoDeFase(fase.id(), fase.tipo(), fase.inicio(), fim);
  }

  /**
   * A fase anterior à apagada, quando o fim dela no futuro estava ali por causa da apagada: ele vai
   * para a véspera da próxima fase ou some, se não houver próxima, e a anterior volta a ser a fase
   * em aberto (RN-67, UC-07 3b).
   */
  public static Optional<PeriodoDeFase> apagarFase(
      List<PeriodoDeFase> fases, PeriodoDeFase apagada, LocalDate hoje) {
    List<PeriodoDeFase> outras = fases.stream().filter(fase -> fase.id() != apagada.id()).toList();
    @Nullable LocalDate fimNovo =
        outras.stream()
            .filter(fase -> fase.inicio().isAfter(apagada.inicio()))
            .map(PeriodoDeFase::inicio)
            .min(LocalDate::compareTo)
            .map(inicio -> inicio.minusDays(1))
            .orElse(null);
    return outras.stream()
        .filter(fase -> acompanha(fase, apagada, hoje))
        .findFirst()
        .map(
            anterior ->
                new PeriodoDeFase(anterior.id(), anterior.tipo(), anterior.inicio(), fimNovo));
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
   * fim, ou a falta dele, que invade a seguinte. Ela nomeia a primeira fase invadida e, onde o caso
   * de uso dá caminho, diz o caminho (UC-05 2e e 2g, UC-06 2b).
   */
  private static void recusarSobreposicao(
      List<PeriodoDeFase> fases, LocalDate inicio, @Nullable LocalDate fim, Caminho caminho) {
    for (PeriodoDeFase fase :
        fases.stream().sorted(Comparator.comparing(PeriodoDeFase::inicio)).toList()) {
      LocalDate fimDaOutra = fase.fim();
      boolean comecaAntesDoFim = fim == null || !fase.inicio().isAfter(fim);
      boolean terminaDepoisDoInicio = fimDaOutra == null || !fimDaOutra.isBefore(inicio);
      if (comecaAntesDoFim && terminaDepoisDoInicio) {
        throw sobreposta(fase, fase.cobre(inicio) ? "dataInicio" : "dataFim", caminho.para(fase));
      }
    }
  }

  /** O que a pessoa faz antes para a fase caber, ou nada, quando o caso de uso não dá caminho. */
  @FunctionalInterface
  private interface Caminho {
    String para(PeriodoDeFase invadida);
  }

  private static RegraVioladaException sobreposta(
      PeriodoDeFase invadida, String campo, String caminho) {
    return new RegraVioladaException(
        Recusa.FASE_SOBREPOSTA, campo, "As datas invadem " + nomear(invadida) + "." + caminho);
  }

  /**
   * A fase como a recusa a nomeia, com as datas como a tela escreve (RN-63): "a manutenção de
   * 05/05/2026 a 02/08/2026", ou "o cutting de 01/09/2026 em diante", quando está em aberto.
   */
  private static String nomear(PeriodoDeFase fase) {
    String tipo =
        switch (fase.tipo()) {
          case CUTTING -> "o cutting";
          case BULKING -> "o bulking";
          case MANUTENCAO -> "a manutenção";
        };
    LocalDate fim = fase.fim();
    return tipo
        + " de "
        + DATA_EXIBIDA.format(fase.inicio())
        + (fim == null ? " em diante" : " a " + DATA_EXIBIDA.format(fim));
  }
}
