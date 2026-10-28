package mbarete.regras;

import static mbarete.regras.EncaixeDeFase.encaixarFaseNova;
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
      new PeriodoDeFase(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
  private static final PeriodoDeFase DESDE_MARCO =
      new PeriodoDeFase(2, LocalDate.of(2026, 3, 1), null);
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

  // Pega a fase com fim que encerra a em aberto em vez de ser recusada: ela
  // classifica um período, e não começa um ciclo (UC-05 2e)
  @Test
  void comFimDentroDaEmAbertoERecusada() {
    assertRecusa(
        () -> encaixar(FASES, "2026-03-10", "2026-03-20"), Recusa.FASE_SOBREPOSTA, "dataInicio");
  }

  // Pega a fase em aberto que não é encerrada, ou encerrada no próprio dia do
  // início da nova em vez da véspera (RN-22, UC-05 2a)
  @Test
  void semFimDepoisDaEmAbertoEncerraNaVespera() {
    assertThat(encaixar(FASES, "2026-04-10", null))
        .contains(new PeriodoDeFase(2, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 9)));
  }

  // Pega a em aberto encerrada antes do próprio início, quando a nova começa no
  // mesmo dia ou antes dela (UC-05 2g)
  @ParameterizedTest
  @CsvSource({"2026-03-01, dataInicio", "2026-02-15, dataFim"})
  void semFimNoMesmoDiaOuAntesDaEmAbertoERecusada(String inicio, String campo) {
    assertRecusa(() -> encaixar(FASES, inicio, null), Recusa.FASE_SOBREPOSTA, campo);
  }

  // Pega a fase sem fim conferida só contra a em aberto: começa no vão e invade
  // a seguinte, que tem fim (UC-05 2h)
  @Test
  void semFimNoVaoQueInvadeASeguinteERecusada() {
    var marco = new PeriodoDeFase(2, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

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

  private static Optional<PeriodoDeFase> encaixar(
      List<PeriodoDeFase> fases, String inicio, @Nullable String fim) {
    return encaixarFaseNova(
        fases, LocalDate.parse(inicio), fim == null ? null : LocalDate.parse(fim), HOJE);
  }

  private static void assertRecusa(Runnable chamada, Recusa recusa, String campo) {
    assertThatThrownBy(chamada::run)
        .isInstanceOfSatisfying(
            RegraVioladaException.class,
            excecao -> {
              assertThat(excecao.recusa()).isEqualTo(recusa);
              assertThat(excecao.campo()).isEqualTo(campo);
            });
  }
}
