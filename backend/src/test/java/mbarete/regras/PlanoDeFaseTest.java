package mbarete.regras;

import static mbarete.regras.PlanoDeFase.editarPlano;
import static mbarete.regras.PlanoDeFase.montarPlano;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PlanoDeFaseTest {

  private static final BigDecimal PESO_ALVO = new BigDecimal("75.0");

  // Pega o plano gravado na manutenção, que não tem nível, taxa alvo nem peso
  // alvo (RN-33, RN-41, UC-05 1a)
  @Test
  void manutencaoDescartaOPlano() {
    assertThat(
            montarPlano(
                TipoDeFase.MANUTENCAO,
                Nivel.MODERADO,
                new BigDecimal("0.50"),
                UnidadeDaTaxa.PERCENTUAL_SEMANA,
                PESO_ALVO))
        .isEqualTo(new PlanoDeFase(null, null, null));
  }

  // Pega cada parte do plano aceita ausente em cutting e bulking, e o campo
  // destacado trocado (RN-28, RN-29, RN-41)
  @ParameterizedTest
  @CsvSource({
    ",         0.50, PERCENTUAL_SEMANA, 75.0, NIVEL_AUSENTE,     nivel",
    "MODERADO,     , PERCENTUAL_SEMANA, 75.0, TAXA_ALVO_AUSENTE, taxaAlvoValor",
    "MODERADO, 0.50,                  , 75.0, TAXA_ALVO_AUSENTE, taxaAlvoUnidade",
    "MODERADO, 0.50, PERCENTUAL_SEMANA,     , PESO_ALVO_AUSENTE, pesoAlvo"
  })
  void cuttingSemParteDoPlanoERecusado(
      @Nullable Nivel nivel,
      @Nullable BigDecimal valor,
      @Nullable UnidadeDaTaxa unidade,
      @Nullable BigDecimal pesoAlvo,
      Recusa recusa,
      String campo) {
    assertThatThrownBy(() -> montarPlano(TipoDeFase.CUTTING, nivel, valor, unidade, pesoAlvo))
        .isInstanceOfSatisfying(
            RegraVioladaException.class,
            excecao -> {
              assertThat(excecao.recusa()).isEqualTo(recusa);
              assertThat(excecao.campo()).isEqualTo(campo);
            });
  }

  // Pega a taxa comparada com as casas, que tornaria personalizado o 0,5
  // digitado no lugar do 0,50 do moderado (RN-30)
  @Test
  void taxaIgualAoPadraoMantemONivel() {
    assertThat(
            montarPlano(
                    TipoDeFase.CUTTING,
                    Nivel.MODERADO,
                    new BigDecimal("0.5"),
                    UnidadeDaTaxa.PERCENTUAL_SEMANA,
                    PESO_ALVO)
                .nivel())
        .isEqualTo(Nivel.MODERADO);
  }

  // Pega a taxa editada que mantém o nível escolhido, e a unidade trocada que
  // não conta como edição (RN-30, UC-05 3a)
  @ParameterizedTest
  @CsvSource({"0.75, PERCENTUAL_SEMANA", "0.50, KG_SEMANA"})
  void taxaForaDoPadraoTornaONivelPersonalizado(BigDecimal valor, UnidadeDaTaxa unidade) {
    assertThat(montarPlano(TipoDeFase.CUTTING, Nivel.MODERADO, valor, unidade, PESO_ALVO).nivel())
        .isEqualTo(Nivel.PERSONALIZADO);
  }

  private static final TaxaAlvo MEIO_POR_CENTO =
      new TaxaAlvo(new BigDecimal("0.50"), UnidadeDaTaxa.PERCENTUAL_SEMANA);

  // Pega a taxa do cutting que fica na bulking, virando personalizado com a
  // unidade do outro tipo, em vez do padrão do mesmo nível (RN-28, UC-06 2j)
  @Test
  void trocarDeTipoComATaxaComoEstavaAplicaOPadraoDoNivel() {
    assertThat(
            editarPlano(
                TipoDeFase.CUTTING,
                MEIO_POR_CENTO,
                TipoDeFase.BULKING,
                Nivel.MODERADO,
                new BigDecimal("0.50"),
                UnidadeDaTaxa.PERCENTUAL_SEMANA,
                PESO_ALVO))
        .isEqualTo(
            new PlanoDeFase(
                Nivel.MODERADO,
                new TaxaAlvo(new BigDecimal("0.30"), UnidadeDaTaxa.KG_SEMANA),
                PESO_ALVO));
  }

  // Pega a taxa do outro tipo aceita no personalizado, que não tem padrão para
  // pôr no lugar (RN-28, UC-06 2j)
  @Test
  void trocarDeTipoNoPersonalizadoPedeATaxaDeNovo() {
    assertThatThrownBy(
            () ->
                editarPlano(
                    TipoDeFase.CUTTING,
                    MEIO_POR_CENTO,
                    TipoDeFase.BULKING,
                    Nivel.PERSONALIZADO,
                    new BigDecimal("0.50"),
                    UnidadeDaTaxa.PERCENTUAL_SEMANA,
                    PESO_ALVO))
        .isInstanceOfSatisfying(
            RegraVioladaException.class,
            excecao -> assertThat(excecao.recusa()).isEqualTo(Recusa.TAXA_ALVO_AUSENTE));
  }

  // Pega o padrão posto por cima da taxa que a pessoa digitou ao trocar de
  // tipo, que vale e torna o nível personalizado (RN-30)
  @Test
  void trocarDeTipoComTaxaNovaValeATaxaDigitada() {
    var digitada = new TaxaAlvo(new BigDecimal("0.25"), UnidadeDaTaxa.KG_SEMANA);

    assertThat(
            editarPlano(
                TipoDeFase.CUTTING,
                MEIO_POR_CENTO,
                TipoDeFase.BULKING,
                Nivel.MODERADO,
                digitada.valor(),
                digitada.unidade(),
                PESO_ALVO))
        .isEqualTo(new PlanoDeFase(Nivel.PERSONALIZADO, digitada, PESO_ALVO));
  }

  // Pega a edição sem troca de tipo tratada como troca, que pediria de novo a
  // taxa do personalizado ao editar só o peso alvo
  @Test
  void semTrocarDeTipoOPersonalizadoFica() {
    var oitentaCentesimos = new TaxaAlvo(new BigDecimal("0.80"), UnidadeDaTaxa.PERCENTUAL_SEMANA);

    assertThat(
            editarPlano(
                TipoDeFase.CUTTING,
                oitentaCentesimos,
                TipoDeFase.CUTTING,
                Nivel.PERSONALIZADO,
                oitentaCentesimos.valor(),
                oitentaCentesimos.unidade(),
                PESO_ALVO))
        .isEqualTo(new PlanoDeFase(Nivel.PERSONALIZADO, oitentaCentesimos, PESO_ALVO));
  }
}
