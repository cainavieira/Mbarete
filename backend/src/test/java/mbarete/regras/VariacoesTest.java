package mbarete.regras;

import static mbarete.regras.Variacoes.calcularVariacoes;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class VariacoesTest {

  private static final LocalDate HOJE = LocalDate.of(2026, 10, 20);

  // Um registro por dia desde 40 dias atrás, 0,1 kg mais leve a cada dia, do
  // mais novo para o mais antigo. O peso semanal de n dias atrás é
  // 80,3 + 0,1 n, e o primeiro é o de 37 dias atrás, quando a janela chega a 4
  // registros
  private static final List<Pesagem> PESAGENS =
      IntStream.rangeClosed(0, 40)
          .mapToObj(
              dias ->
                  new Pesagem(
                      HOJE.minusDays(dias),
                      new BigDecimal("80.0").add(BigDecimal.valueOf(dias, 1))))
          .toList();

  // Pega as partidas de 6 ou 8 dias, de 13 ou 15, de 27 ou 29, e o primeiro
  // registro tomado como o mais recente, porque a lista chega do mais novo para
  // o mais antigo (RN-46, RN-47)
  @Test
  void semFaseParteDeUmaDuasEQuatroSemanasEDoPrimeiroRegistro() {
    var variacoes = calcularVariacoes(PESAGENS, false, null, HOJE);

    assertThat(variacoes.semana())
        .isNotNull()
        .satisfies(
            semana -> {
              assertThat(semana.de()).isEqualTo(HOJE.minusDays(7));
              assertThat(semana.valor()).isEqualByComparingTo("-0.7");
            });
    assertThat(variacoes.duasSemanas())
        .isNotNull()
        .extracting(Variacao::de)
        .isEqualTo(HOJE.minusDays(14));
    assertThat(variacoes.quatroSemanas())
        .isNotNull()
        .extracting(Variacao::de)
        .isEqualTo(HOJE.minusDays(28));
    assertThat(variacoes.desdeOInicio())
        .isNotNull()
        .extracting(Variacao::de)
        .isEqualTo(HOJE.minusDays(37));
  }

  // Pega a fase ignorada, com a partida no primeiro registro da conta: a base
  // de 20 dias atrás pesa 82,3, e hoje, 80,3 (RN-46)
  @Test
  void comFaseParteDaBase() {
    var variacoes = calcularVariacoes(PESAGENS, true, HOJE.minusDays(20), HOJE);

    assertThat(variacoes.desdeOInicio())
        .isNotNull()
        .satisfies(
            desde -> {
              assertThat(desde.de()).isEqualTo(HOJE.minusDays(20));
              assertThat(desde.valor()).isEqualByComparingTo("-2.0");
            });
  }

  // Pega a fase sem base caindo para o primeiro registro, e a base sem peso
  // semanal avançando como as outras partidas: sem os registros de 20 a 23 dias
  // atrás, a janela da base fica com 3, e a primeira que chega a 4 é a de 16
  // dias atrás (RN-46, RN-68)
  @Test
  void comFaseESemABaseNaoHaVariacaoDesdeOInicio() {
    var semAJanelaDaBase =
        PESAGENS.stream()
            .filter(
                pesagem ->
                    pesagem.data().isAfter(HOJE.minusDays(20))
                        || pesagem.data().isBefore(HOJE.minusDays(23)))
            .toList();

    assertThat(calcularVariacoes(PESAGENS, true, null, HOJE).desdeOInicio()).isNull();
    assertThat(calcularVariacoes(semAJanelaDaBase, true, HOJE.minusDays(20), HOJE).desdeOInicio())
        .isNull();
  }

  // Pega o limite contado um dia antes: sem os registros de 11 a 20 dias
  // atrás, o primeiro peso semanal desde a partida de 14 dias é o de 7 dias
  // atrás, o último que a janela de hoje não alcança (RN-47)
  @Test
  void aPartidaAvancaAteUmaSemanaAtras() {
    var variacoes = calcularVariacoes(semOsRegistros(11, 20), false, null, HOJE);

    assertThat(variacoes.duasSemanas())
        .isNotNull()
        .extracting(Variacao::de)
        .isEqualTo(HOJE.minusDays(7));
  }

  // Pega a partida que passa de 1 semana atrás e compara a semana com ela
  // mesma: sem os registros de 10 a 20 dias atrás, o primeiro peso semanal
  // desde as partidas de 7 e de 14 dias é o de 6 dias atrás (RN-47)
  @Test
  void aPartidaNaoAvancaAlemDeUmaSemanaAtras() {
    var variacoes = calcularVariacoes(semOsRegistros(10, 20), false, null, HOJE);

    assertThat(variacoes.semana()).isNull();
    assertThat(variacoes.duasSemanas()).isNull();
  }

  /** As pesagens sem as de tantos a tantos dias atrás, os dois inclusive. */
  private static List<Pesagem> semOsRegistros(int deDiasAtras, int ateDiasAtras) {
    return PESAGENS.stream()
        .filter(
            pesagem ->
                pesagem.data().isAfter(HOJE.minusDays(deDiasAtras))
                    || pesagem.data().isBefore(HOJE.minusDays(ateDiasAtras)))
        .toList();
  }
}
