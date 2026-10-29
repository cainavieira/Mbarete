package mbarete.regras;

import static mbarete.regras.EncaixeDeFase.apagarFase;
import static mbarete.regras.EncaixeDeFase.encaixarEdicao;
import static mbarete.regras.EncaixeDeFase.encaixarFaseNova;
import static mbarete.regras.EncaixeDeFase.encerrarFase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EncaixeDeFaseTest {

  private static final LocalDate HOJE = LocalDate.of(2026, 10, 20);
  private static final PeriodoDeFase JANEIRO =
      new PeriodoDeFase(
          1, TipoDeFase.MANUTENCAO, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
  private static final PeriodoDeFase DESDE_MARCO =
      new PeriodoDeFase(2, TipoDeFase.CUTTING, LocalDate.of(2026, 3, 1), null);
  private static final List<PeriodoDeFase> FASES = List.of(JANEIRO, DESDE_MARCO);

  // Pega a fase vizinha tomada como sobreposta: fevereiro encosta nas duas, e
  // é assim que se classifica o vão (UC-05 2c)
  @Test
  void comFimNoVaoEncaixaSemEncerrarNinguem() {
    assertThat(encaixar(FASES, "2026-02-01", "2026-02-28")).isEmpty();
  }

  // Pega a borda de um dia dentro da outra fase aceita, e o campo destacado
  // trocado: o início dentro de outra fase, ou o fim invadindo a seguinte
  // (RN-19)
  @ParameterizedTest
  @CsvSource({"2026-01-31, 2026-02-10, dataInicio", "2026-02-01, 2026-03-01, dataFim"})
  void umDiaDentroDeOutraFaseERecusado(String inicio, String fim, String campo) {
    assertRecusa(() -> encaixar(FASES, inicio, fim), Recusa.FASE_SOBREPOSTA, campo);
  }

  // Pega a fase invadida sem nome, com o fim que ela não tem, ou com caminho
  // onde o caso de uso não dá nenhum (UC-05 2b)
  @Test
  void aRecusaNomeiaAFaseInvadida() {
    assertRecusa(
        () -> encaixar(FASES, "2026-01-31", "2026-02-10"),
        Recusa.FASE_SOBREPOSTA,
        "dataInicio",
        "As datas invadem a manutenção de 01/01/2026 a 31/01/2026.");
  }

  // Pega a fase com fim que encerra a em aberto em vez de ser recusada, e o
  // caminho trocado: dentro do ciclo, encerrar antes a em aberto; no início
  // dela, encerrar não basta, e o caminho é adiar o início (UC-05 2e)
  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "2026-03-10 | Para classificar um período dentro do ciclo atual, encerre antes essa fase.",
        "2026-03-01 | Para classificar esse período, adie antes o início dessa fase."
      })
  void comFimDentroDaEmAbertoERecusada(String inicio, String caminho) {
    assertRecusa(
        () -> encaixar(FASES, inicio, "2026-03-20"),
        Recusa.FASE_SOBREPOSTA,
        "dataInicio",
        "As datas invadem o cutting de 01/03/2026 em diante. " + caminho);
  }

  // Pega a fase invadida tomada pela ordem da lista, e não das datas: o
  // histórico se classifica depois do fato, e o id não segue o calendário
  @Test
  void nomeiaAFaseInvadidaQueVemPrimeiro() {
    var abril =
        new PeriodoDeFase(
            3, TipoDeFase.BULKING, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30));
    var marco =
        new PeriodoDeFase(
            2, TipoDeFase.CUTTING, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

    assertRecusa(
        () -> encaixar(List.of(abril, marco), "2026-02-01", "2026-04-10"),
        Recusa.FASE_SOBREPOSTA,
        "dataFim",
        "As datas invadem o cutting de 01/03/2026 a 31/03/2026.");
  }

  // Pega a fase em aberto que não é encerrada, ou encerrada no próprio dia do
  // início da nova em vez da véspera (RN-22, UC-05 2a)
  @Test
  void semFimDepoisDaEmAbertoEncerraNaVespera() {
    assertThat(encaixar(FASES, "2026-04-10", null))
        .contains(
            new PeriodoDeFase(
                2, TipoDeFase.CUTTING, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 9)));
  }

  // Pega a em aberto encerrada antes do próprio início, quando a nova começa no
  // mesmo dia ou antes dela, e a recusa sem o caminho de editar o início
  // (UC-05 2g)
  @ParameterizedTest
  @CsvSource({"2026-03-01, dataInicio", "2026-02-15, dataFim"})
  void semFimNoMesmoDiaOuAntesDaEmAbertoERecusada(String inicio, String campo) {
    assertRecusa(
        () -> encaixar(FASES, inicio, null),
        Recusa.FASE_SOBREPOSTA,
        campo,
        "As datas invadem o cutting de 01/03/2026 em diante. Para adiantar o início de um ciclo,"
            + " edite a data de início dessa fase.");
  }

  // Pega a fase sem fim conferida só contra a em aberto: começa no vão e invade
  // a seguinte, que tem fim (UC-05 2h)
  @Test
  void semFimNoVaoQueInvadeASeguinteERecusada() {
    var marco =
        new PeriodoDeFase(
            2, TipoDeFase.CUTTING, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

    assertRecusa(
        () -> encaixar(List.of(JANEIRO, marco), "2026-02-01", null),
        Recusa.FASE_SOBREPOSTA,
        "dataFim");
  }

  // Pega a última fase tomada como a em aberto: janeiro já tem fim, e esticá-lo
  // até a véspera da nova cobriria o vão
  @Test
  void semFimSemFaseEmAbertoNaoEncerraNinguem() {
    assertThat(encaixar(List.of(JANEIRO), "2026-02-10", null)).isEmpty();
  }

  // Pega a data de fim conferida contra o dia do servidor, ou com a borda
  // errada: hoje pode, amanhã não (RN-67)
  @Test
  void fimHojeEAceitoEFimAmanhaERecusado() {
    assertThat(encaixar(List.of(), "2026-10-01", "2026-10-20")).isEmpty();
    assertRecusa(
        () -> encaixar(List.of(), "2026-10-01", "2026-10-21"), Recusa.DATA_FIM_FUTURA, "dataFim");
  }

  // Pega a fase de um dia só recusada, e o fim antes do início aceito (RN-18)
  @Test
  void fimNoDiaDoInicioEAceitoEAntesERecusado() {
    assertThat(encaixar(List.of(), "2026-10-01", "2026-10-01")).isEmpty();
    assertRecusa(
        () -> encaixar(List.of(), "2026-10-01", "2026-09-30"),
        Recusa.DATA_FIM_ANTES_DO_INICIO,
        "dataFim");
  }

  /** Encerrada em 31-10 porque a seguinte está agendada para 01-11 (RN-22, RN-67). */
  private static final PeriodoDeFase ATE_A_AGENDADA =
      new PeriodoDeFase(
          3, TipoDeFase.CUTTING, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 31));

  private static final PeriodoDeFase AGENDADA =
      new PeriodoDeFase(4, TipoDeFase.CUTTING, LocalDate.of(2026, 11, 1), null);
  private static final List<PeriodoDeFase> COM_A_AGENDADA = List.of(ATE_A_AGENDADA, AGENDADA);

  // Pega a fase editada conferida contra ela mesma, que recusaria toda edição
  @Test
  void editarSemMudarAsDatasNaoSobrepoeASiMesma() {
    assertThat(editar(COM_A_AGENDADA, AGENDADA, "2026-11-01", null)).isEmpty();
  }

  // Pega o fim no futuro que não acompanha o início da seguinte, que fica no
  // dia do início dela, ou que só a acompanha para o futuro: a véspera nova
  // pode já ter passado (RN-67, UC-06 2i)
  @ParameterizedTest
  @CsvSource({"2026-10-15, 2026-10-14", "2026-11-10, 2026-11-09"})
  void oInicioDaSeguinteMoveOFimNoFuturoDaAnterior(String inicio, String fimDaAnterior) {
    assertThat(editar(COM_A_AGENDADA, AGENDADA, inicio, null))
        .contains(
            new PeriodoDeFase(
                3, TipoDeFase.CUTTING, LocalDate.of(2026, 9, 1), LocalDate.parse(fimDaAnterior)));
  }

  // Pega a anterior empurrada para antes do próprio início, quando a seguinte
  // é antecipada até o início dela ou antes, e a recusa que a nomeia com o fim
  // já movido, que ela não tem (RN-67, UC-06 2i)
  @ParameterizedTest
  @CsvSource({"2026-09-01", "2026-08-20"})
  void anteciparAteOInicioDaAnteriorERecusado(String inicio) {
    assertRecusa(
        () -> editar(COM_A_AGENDADA, AGENDADA, inicio, null),
        Recusa.FASE_SOBREPOSTA,
        "dataInicio",
        "As datas invadem o cutting de 01/09/2026 a 31/10/2026.");
  }

  // Pega a anterior com fim já passado movida junto: só o fim no futuro
  // acompanha, e para adiantar sobre a anterior encurta-se ela antes (UC-06 2b)
  @Test
  void aAnteriorComFimPassadoNaoAcompanha() {
    var fevereiro = new PeriodoDeFase(5, TipoDeFase.CUTTING, LocalDate.of(2026, 2, 1), null);

    assertRecusa(
        () -> editar(List.of(JANEIRO, fevereiro), fevereiro, "2026-01-20", null),
        Recusa.FASE_SOBREPOSTA,
        "dataInicio",
        "As datas invadem a manutenção de 01/01/2026 a 31/01/2026. Para adiantar o início,"
            + " encurte antes essa fase.");
  }

  // Pega a edição tratada como criação, que encerraria a fase em aberto em vez
  // de recusar (RN-19, UC-06 2b)
  @Test
  void editarNuncaEncerraAEmAberto() {
    assertRecusa(
        () -> editar(FASES, JANEIRO, "2026-04-01", null), Recusa.FASE_SOBREPOSTA, "dataInicio");
  }

  // Pega o fim no futuro que a fase já tinha recusado, que impediria editar o
  // plano dela, e o fim futuro novo aceito (RN-67)
  @Test
  void oFimNoFuturoQueJaEstavaFicaEUmNovoERecusado() {
    assertThat(editar(COM_A_AGENDADA, ATE_A_AGENDADA, "2026-09-01", "2026-10-31")).isEmpty();
    assertRecusa(
        () -> editar(COM_A_AGENDADA, ATE_A_AGENDADA, "2026-09-01", "2026-10-25"),
        Recusa.DATA_FIM_FUTURA,
        "dataFim");
  }

  // Pega a fase em aberto tomada como a corrente: a agendada está em aberto e
  // ainda não começou, e janeiro já acabou (RN-27)
  @Test
  void soAFaseQueCobreHojeEEncerrada() {
    assertRecusa(
        () -> encerrarFase(AGENDADA, LocalDate.of(2026, 11, 15), HOJE),
        Recusa.FASE_NAO_CORRENTE,
        null);
    assertRecusa(
        () -> encerrarFase(JANEIRO, LocalDate.of(2026, 1, 20), HOJE),
        Recusa.FASE_NAO_CORRENTE,
        null);
  }

  // Pega a corrente tomada como a sem fim: ela pode ter fim no futuro, posto
  // pela seguinte agendada (RN-22, RN-27)
  @Test
  void aCorrenteComFimNoFuturoPodeSerEncerrada() {
    assertThat(encerrarFase(ATE_A_AGENDADA, LocalDate.of(2026, 10, 15), HOJE))
        .isEqualTo(
            new PeriodoDeFase(
                3, TipoDeFase.CUTTING, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 15)));
  }

  // Pega o encerramento sem as recusas do fim: futuro ou antes do início
  // (RN-18, RN-67, UC-14 2a e 2b)
  @ParameterizedTest
  @CsvSource({"2026-10-21, DATA_FIM_FUTURA", "2026-02-28, DATA_FIM_ANTES_DO_INICIO"})
  void oEncerramentoNoFuturoOuAntesDoInicioERecusado(String fim, Recusa recusa) {
    assertRecusa(() -> encerrarFase(DESDE_MARCO, LocalDate.parse(fim), HOJE), recusa, "dataFim");
  }

  // Pega o fim no futuro da anterior que fica na véspera da apagada, e o que
  // vai para a véspera da última fase em vez da próxima (RN-67, UC-07 3b)
  @Test
  void apagarASeguinteLevaOFimDaAnteriorParaAVesperaDaProxima() {
    var novembro =
        new PeriodoDeFase(
            4, TipoDeFase.CUTTING, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
    var dezembro =
        new PeriodoDeFase(
            5, TipoDeFase.CUTTING, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31));
    var desdeJaneiro = new PeriodoDeFase(6, TipoDeFase.CUTTING, LocalDate.of(2027, 1, 1), null);

    assertThat(
            apagarFase(List.of(ATE_A_AGENDADA, novembro, dezembro, desdeJaneiro), novembro, HOJE))
        .contains(
            new PeriodoDeFase(
                3, TipoDeFase.CUTTING, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 30)));
  }

  // Pega o fim no futuro que fica quando não há próxima: a anterior volta a
  // ser a fase em aberto (RN-67, UC-07 3b)
  @Test
  void semProximaOFimNoFuturoDaAnteriorSome() {
    assertThat(apagarFase(COM_A_AGENDADA, AGENDADA, HOJE))
        .contains(new PeriodoDeFase(3, TipoDeFase.CUTTING, LocalDate.of(2026, 9, 1), null));
  }

  // Pega a anterior com fim já passado esticada sobre o vão da apagada: só o
  // fim no futuro estava ali por causa dela (RN-67, RN-70)
  @Test
  void aAnteriorComFimPassadoFicaComoEstava() {
    var fevereiro = new PeriodoDeFase(5, TipoDeFase.CUTTING, LocalDate.of(2026, 2, 1), null);

    assertThat(apagarFase(List.of(JANEIRO, fevereiro), fevereiro, HOJE)).isEmpty();
  }

  private static Optional<PeriodoDeFase> editar(
      List<PeriodoDeFase> fases, PeriodoDeFase antes, String inicio, @Nullable String fim) {
    return encaixarEdicao(
        fases, antes, LocalDate.parse(inicio), fim == null ? null : LocalDate.parse(fim), HOJE);
  }

  private static Optional<PeriodoDeFase> encaixar(
      List<PeriodoDeFase> fases, String inicio, @Nullable String fim) {
    return encaixarFaseNova(
        fases, LocalDate.parse(inicio), fim == null ? null : LocalDate.parse(fim), HOJE);
  }

  private static void assertRecusa(Runnable chamada, Recusa recusa, @Nullable String campo) {
    assertThatThrownBy(chamada::run)
        .isInstanceOfSatisfying(
            RegraVioladaException.class,
            excecao -> {
              assertThat(excecao.recusa()).isEqualTo(recusa);
              assertThat(excecao.campo()).isEqualTo(campo);
            });
  }

  private static void assertRecusa(Runnable chamada, Recusa recusa, String campo, String mensagem) {
    assertRecusa(chamada, recusa, campo);
    assertThatThrownBy(chamada::run).hasMessage(mensagem);
  }
}
