package mbarete.regras;

import static mbarete.regras.Pesagens.diarias;
import static mbarete.regras.TaxaAlvo.converterTaxaAlvo;
import static mbarete.regras.TaxaAlvo.padrao;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class TaxaAlvoTest {

  private static final LocalDate HOJE = LocalDate.of(2026, 10, 20);

  // Pega a unidade trocada entre os tipos: o padrão do cutting é em porcentagem
  // e o do bulking em quilogramas (RN-31, RN-32)
  @ParameterizedTest
  @CsvSource({
    "CUTTING, CONSERVADOR, 0.25, PERCENTUAL_SEMANA",
    "CUTTING, MODERADO, 0.50, PERCENTUAL_SEMANA",
    "CUTTING, AGRESSIVO, 1.00, PERCENTUAL_SEMANA",
    "BULKING, CONSERVADOR, 0.20, KG_SEMANA",
    "BULKING, MODERADO, 0.30, KG_SEMANA",
    "BULKING, AGRESSIVO, 0.40, KG_SEMANA"
  })
  void padraoDoNivel(TipoDeFase tipo, Nivel nivel, BigDecimal valor, UnidadeDaTaxa unidade) {
    assertThat(padrao(tipo, nivel))
        .hasValueSatisfying(
            taxa -> {
              assertThat(taxa.valor()).isEqualByComparingTo(valor);
              assertThat(taxa.unidade()).isEqualTo(unidade);
            });
  }

  @ParameterizedTest
  @EnumSource(names = {"CUTTING", "BULKING"})
  void personalizadoNaoTemPadrao(TipoDeFase tipo) {
    assertThat(padrao(tipo, Nivel.PERSONALIZADO)).isEmpty();
  }

  // Pega o sinal ausente ou invertido (RN-29)
  @Test
  void oSinalVemDoTipo() {
    var taxa = new TaxaAlvo(new BigDecimal("0.50"), UnidadeDaTaxa.KG_SEMANA);

    assertThat(converterTaxaAlvo(TipoDeFase.CUTTING, taxa, List.of(), HOJE))
        .hasValueSatisfying(kg -> assertThat(kg).isEqualByComparingTo("-0.50"));
    assertThat(converterTaxaAlvo(TipoDeFase.BULKING, taxa, List.of(), HOJE))
        .hasValueSatisfying(kg -> assertThat(kg).isEqualByComparingTo("0.50"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"0.00", "-0.50"})
  void zeroOuValorComSinalERecusado(String valor) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new TaxaAlvo(new BigDecimal(valor), UnidadeDaTaxa.KG_SEMANA));
  }

  // Pega a porcentagem sobre o peso do começo (100,0, que daria -0,50) em vez
  // do peso semanal de hoje (80,0), e a porcentagem lida como quilogramas (RN-34)
  @Test
  void percentualIncideSobreOPesoSemanalDeHoje() {
    var pesagens = new ArrayList<Pesagem>();
    pesagens.addAll(diarias(HOJE.minusDays(30), HOJE.minusDays(7), "100.0"));
    pesagens.addAll(diarias(HOJE.minusDays(6), HOJE, "80.0"));
    var taxa = new TaxaAlvo(new BigDecimal("0.50"), UnidadeDaTaxa.PERCENTUAL_SEMANA);

    assertThat(converterTaxaAlvo(TipoDeFase.CUTTING, taxa, pesagens, HOJE))
        .hasValueSatisfying(kg -> assertThat(kg).isEqualByComparingTo("-0.40"));
  }

  // Pega a manutenção quebrando por não ter taxa gravada (RN-33)
  @Test
  void manutencaoConverteParaZero() {
    assertThat(converterTaxaAlvo(TipoDeFase.MANUTENCAO, null, List.of(), HOJE))
        .hasValueSatisfying(kg -> assertThat(kg).isEqualByComparingTo("0"));
  }
}
