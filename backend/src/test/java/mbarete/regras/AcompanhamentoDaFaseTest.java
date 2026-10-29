package mbarete.regras;

import static mbarete.regras.AcompanhamentoDaFase.acompanharFase;
import static mbarete.regras.Pesagens.diarias;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class AcompanhamentoDaFaseTest {

  private static final LocalDate HOJE = LocalDate.of(2026, 10, 20);

  /** 1% da semana, a taxa do cutting agressivo (RN-31). */
  private static final TaxaAlvo UM_POR_CENTO =
      new TaxaAlvo(new BigDecimal("1.00"), UnidadeDaTaxa.PERCENTUAL_SEMANA);

  /**
   * Um registro por dia desde 40 dias atrás, 0,1 kg mais leve a cada dia: o peso semanal de hoje é
   * 76,3, e o de 28 dias atrás, 79,1.
   */
  private static final List<Pesagem> PERDENDO =
      IntStream.rangeClosed(0, 40)
          .mapToObj(
              dias ->
                  new Pesagem(
                      HOJE.minusDays(dias),
                      new BigDecimal("76.0").add(BigDecimal.valueOf(dias, 1))))
          .toList();

  // Pega a taxa real fora do mês e a barra ou a faixa trocadas de tipo: -2,8
  // kg em 4 semanas dá -0,70 por semana, no ritmo de um alvo de -0,76 (RN-36,
  // RN-38)
  @Test
  void aCuttingComMaisDe14DiasTemVereditoTaxaRealEBarra() {
    var daFase = cutting(HOJE.minusDays(30));

    assertThat(daFase.ritmo()).isEqualTo(Ritmo.NO_RITMO);
    assertThat(daFase.taxaReal())
        .isNotNull()
        .satisfies(
            taxa -> {
              assertThat(taxa.valor()).isEqualByComparingTo("-0.7");
              assertThat(taxa.de()).isEqualTo(HOJE.minusDays(28));
            });
    assertThat(daFase.taxaAlvoKg()).isEqualByComparingTo("-0.763");
    assertThat(daFase.faixa()).isNull();
    assertThat(daFase.progresso()).isInstanceOf(Progresso.Barra.class);
  }

  // Pega a taxa real mostrada antes do veredito, quando ainda é mais água do
  // que ritmo, e o dia em que ele aparece perdido (RN-39, UC-08 4a)
  @Test
  void antesDos14DiasNaoHaVereditoNemTaxaReal() {
    var daFase = cutting(HOJE.minusDays(10));

    assertThat(daFase.ritmo()).isNull();
    assertThat(daFase.taxaReal()).isNull();
    assertThat(daFase.vereditoAPartirDe()).isEqualTo(HOJE.plusDays(4));
    assertThat(daFase.taxaAlvoKg()).isNotNull();
    assertThat(daFase.progresso()).isInstanceOf(Progresso.Barra.class);
  }

  // Pega a barra sem base sumindo em vez de dizer dados insuficientes, e a
  // taxa alvo levada junto (UC-08 1a)
  @Test
  void semDiaDaBaseABarraTemDadosInsuficientes() {
    var daFase = cutting(null);

    assertThat(daFase.ritmo()).isNull();
    assertThat(daFase.taxaReal()).isNull();
    assertThat(daFase.taxaAlvoKg()).isEqualByComparingTo("-0.763");
    assertThat(daFase.progresso()).isInstanceOf(Progresso.DadosInsuficientes.class);
  }

  // Pega a faixa da manutenção perdida, a barra dada a quem não tem peso alvo
  // e a taxa real ao lado de um veredito de posição (RN-33, RN-41)
  @Test
  void aManutencaoTemAFaixaEOVereditoPelaPosicao() {
    var daFase = manutencao(HOJE.minusDays(10));

    assertThat(daFase.faixa())
        .isEqualTo(new FaixaDeTolerancia(new BigDecimal("79.2"), new BigDecimal("80.8")));
    assertThat(daFase.ritmo()).isEqualTo(Ritmo.NO_RITMO);
    assertThat(daFase.taxaReal()).isNull();
    assertThat(daFase.progresso()).isNull();
    assertThat(daFase.taxaAlvoKg()).isEqualByComparingTo("0");
  }

  // Pega a barra de dados insuficientes dada à manutenção sem base, que não
  // tem barra nenhuma (RN-41)
  @Test
  void aManutencaoSemBaseNaoTemBarraNemFaixa() {
    var daFase = manutencao(null);

    assertThat(daFase.progresso()).isNull();
    assertThat(daFase.faixa()).isNull();
  }

  private static AcompanhamentoDaFase cutting(@Nullable LocalDate diaDaBase) {
    return acompanharFase(
        TipoDeFase.CUTTING, UM_POR_CENTO, new BigDecimal("71.0"), diaDaBase, null, PERDENDO, HOJE);
  }

  private static AcompanhamentoDaFase manutencao(@Nullable LocalDate diaDaBase) {
    return acompanharFase(
        TipoDeFase.MANUTENCAO,
        null,
        null,
        diaDaBase,
        null,
        diarias(HOJE.minusDays(20), HOJE, "80.0"),
        HOJE);
  }
}
