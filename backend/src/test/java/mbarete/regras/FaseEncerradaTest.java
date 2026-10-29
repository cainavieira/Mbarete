package mbarete.regras;

import static mbarete.regras.FaseEncerrada.resumirFaseEncerrada;
import static mbarete.regras.Pesagens.ate;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import mbarete.regras.FaseEncerrada.Alvo;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class FaseEncerradaTest {

  private static final LocalDate INICIO = LocalDate.of(2026, 9, 1);
  private static final LocalDate FIM = LocalDate.of(2026, 9, 30);
  private static final LocalDate BASE = LocalDate.of(2026, 9, 4);

  // Pega a duração sem um dos dois dias e a variação contada do fim para a
  // base (RN-40)
  @Test
  void variaDaBaseAoPesoSemanalDoFim() {
    var encerrada = manutencao(BASE, comBase(ate(FIM, "79.0", "79.0", "79.0", "79.0")));

    assertThat(encerrada.duracaoEmDias()).isEqualTo(30);
    assertThat(encerrada.variacaoTotal())
        .isNotNull()
        .satisfies(
            variacao -> {
              assertThat(variacao.valor()).isEqualByComparingTo("-1.0");
              assertThat(variacao.de()).isEqualTo(BASE);
              assertThat(variacao.ate()).isEqualTo(FIM);
              assertThat(variacao.poucosPontos()).isFalse();
            });
  }

  // Pega o fim sem peso semanal virando poucos pontos, e a procura que avança
  // da base em vez de recuar do fim (RN-40)
  @Test
  void recuaDoFimAteOUltimoDiaComPesoSemanal() {
    var encerrada =
        manutencao(BASE, comBase(ate(FIM.minusDays(5), "78.0", "78.0", "78.0", "78.0")));

    assertThat(encerrada.variacaoTotal())
        .isNotNull()
        .satisfies(
            variacao -> {
              assertThat(variacao.valor()).isEqualByComparingTo("-2.0");
              assertThat(variacao.ate()).isEqualTo(FIM.minusDays(2));
            });
  }

  // Pega a procura que para antes da base, e estoura quando só ela tem peso
  // semanal
  @Test
  void aBaseSozinhaDaVariacaoZero() {
    var diaDaBase = LocalDate.of(2026, 9, 7);
    var pesagens = new ArrayList<Pesagem>(ate(diaDaBase, "80.0", "80.0"));
    pesagens.add(new Pesagem(LocalDate.of(2026, 9, 1), new BigDecimal("80.0")));
    pesagens.add(new Pesagem(diaDaBase.minusDays(2), new BigDecimal("80.0")));
    pesagens.add(new Pesagem(LocalDate.of(2026, 9, 20), new BigDecimal("79.0")));

    assertThat(manutencao(diaDaBase, pesagens).variacaoTotal())
        .isNotNull()
        .satisfies(
            variacao -> {
              assertThat(variacao.valor()).isEqualByComparingTo("0");
              assertThat(variacao.ate()).isEqualTo(diaDaBase);
            });
  }

  // Pega os registros de fora da fase, a ordem em que chegam e a falta da
  // marca (RN-40, UC-09 2b)
  @Test
  void semBaseVaiDoPrimeiroAoUltimoRegistroDaFase() {
    var pesagens =
        List.of(
            pesagem("2026-09-20", "79.0"),
            pesagem("2026-09-02", "80.5"),
            pesagem("2026-08-31", "85.0"),
            pesagem("2026-10-01", "70.0"),
            pesagem("2026-09-10", "80.0"));

    var encerrada = cutting("78.0", null, pesagens);

    assertThat(encerrada.variacaoTotal())
        .isNotNull()
        .satisfies(
            variacao -> {
              assertThat(variacao.valor()).isEqualByComparingTo("-1.5");
              assertThat(variacao.de()).isEqualTo(LocalDate.of(2026, 9, 2));
              assertThat(variacao.ate()).isEqualTo(LocalDate.of(2026, 9, 20));
              assertThat(variacao.poucosPontos()).isTrue();
            });
    assertThat(encerrada.alvo()).isEqualTo(Alvo.SEM_AVALIACAO);
  }

  // Pega o dia da base gravado contado como base, quando a janela dele caiu
  // para três registros (RN-68)
  @Test
  void aBaseQuePerdeuAJanelaContaComoSemBase() {
    var pesagens = new ArrayList<Pesagem>(ate(BASE, "80.0", "80.0", "80.0"));
    pesagens.add(pesagem("2026-09-20", "79.0"));

    var encerrada = cutting("78.0", BASE, pesagens);

    assertThat(encerrada.variacaoTotal())
        .isNotNull()
        .satisfies(variacao -> assertThat(variacao.poucosPontos()).isTrue());
    assertThat(encerrada.alvo()).isEqualTo(Alvo.SEM_AVALIACAO);
  }

  // Pega a variação de um registro contra ele mesmo (RN-40)
  @Test
  void comUmRegistroSoNaoHaVariacao() {
    var pesagens = List.of(pesagem("2026-08-31", "81.0"), pesagem("2026-09-15", "80.0"));

    assertThat(manutencao(null, pesagens).variacaoTotal()).isNull();
  }

  // Pega a variação e o alvo feitos só de pesagens de antes da fase, quando a
  // janela do dia da base as alcança (RN-40, UC-09 2d)
  @Test
  void comBaseEUmRegistroNaFaseNaoHaVariacaoNemAvaliacao() {
    var pesagens = new ArrayList<Pesagem>(ate(INICIO.minusDays(1), "80.0", "80.0", "80.0", "80.0"));
    pesagens.add(pesagem("2026-09-02", "79.0"));

    var encerrada = cutting("78.0", INICIO, pesagens);

    assertThat(encerrada.variacaoTotal()).isNull();
    assertThat(encerrada.alvo()).isEqualTo(Alvo.SEM_AVALIACAO);
  }

  // Pega o alvo avaliado só no fim, quando o peso já voltou, e o último dia no
  // alvo no lugar do primeiro (RN-40)
  @Test
  void oAlvoContaNoPrimeiroDiaEmQueFoiAlcancado() {
    var pesagens = comBase(ate(LocalDate.of(2026, 9, 13), "77.5", "77.5", "77.5", "77.5"));
    pesagens.addAll(ate(FIM, "79.0", "79.0", "79.0", "79.0"));

    var encerrada = cutting("78.0", BASE, pesagens);

    assertThat(encerrada.alvo()).isEqualTo(Alvo.ATINGIDO);
    assertThat(encerrada.alvoAtingidoEm()).isEqualTo(LocalDate.of(2026, 9, 13));
  }

  // Pega a direção do cutting aplicada ao bulking (RN-42)
  @Test
  void oBulkingAtingeSubindo() {
    var pesagens = comBase(ate(LocalDate.of(2026, 9, 13), "82.5", "82.5", "82.5", "82.5"));

    var encerrada =
        resumirFaseEncerrada(
            TipoDeFase.BULKING, new BigDecimal("82.0"), INICIO, FIM, BASE, pesagens);

    assertThat(encerrada.alvo()).isEqualTo(Alvo.ATINGIDO);
  }

  @Test
  void semAlcancarNaoFoiAtingido() {
    var encerrada = cutting("78.0", BASE, comBase(ate(FIM, "79.0", "79.0", "79.0", "79.0")));

    assertThat(encerrada.alvo()).isEqualTo(Alvo.NAO_ATINGIDO);
    assertThat(encerrada.alvoAtingidoEm()).isNull();
  }

  // Pega o alvo igual à base dado como não atingido, em vez de do lado errado
  // (RN-40, RN-44)
  @Test
  void oAlvoDoLadoErradoDaBaseNaoEAvaliado() {
    var encerrada = cutting("80.0", BASE, comBase(ate(FIM, "79.0", "79.0", "79.0", "79.0")));

    assertThat(encerrada.alvo()).isEqualTo(Alvo.LADO_ERRADO);
    assertThat(encerrada.alvoAtingidoEm()).isNull();
  }

  // Pega a manutenção com alvo sem avaliação, ou levada à conta da barra, que
  // ela não tem (RN-41)
  @Test
  void aManutencaoNaoTemAlvo() {
    var pesagens = comBase(ate(FIM, "79.0", "79.0", "79.0", "79.0"));

    assertThat(manutencao(BASE, pesagens).alvo()).isNull();
    assertThat(manutencao(null, pesagens).alvo()).isNull();
  }

  /** Quatro pesagens de 80,0 terminando no dia da base, e as dadas. */
  private static List<Pesagem> comBase(List<Pesagem> depois) {
    var pesagens = new ArrayList<Pesagem>(ate(BASE, "80.0", "80.0", "80.0", "80.0"));
    pesagens.addAll(depois);
    return pesagens;
  }

  private static Pesagem pesagem(String data, String peso) {
    return new Pesagem(LocalDate.parse(data), new BigDecimal(peso));
  }

  private static FaseEncerrada manutencao(@Nullable LocalDate diaDaBase, List<Pesagem> pesagens) {
    return resumirFaseEncerrada(TipoDeFase.MANUTENCAO, null, INICIO, FIM, diaDaBase, pesagens);
  }

  private static FaseEncerrada cutting(
      String alvo, @Nullable LocalDate diaDaBase, List<Pesagem> pesagens) {
    return resumirFaseEncerrada(
        TipoDeFase.CUTTING, new BigDecimal(alvo), INICIO, FIM, diaDaBase, pesagens);
  }
}
