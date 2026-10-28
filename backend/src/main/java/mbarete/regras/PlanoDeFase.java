package mbarete.regras;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/** O plano da fase: nível, taxa alvo e peso alvo, os três nulos na manutenção (RN-33, RN-41). */
public record PlanoDeFase(
    @Nullable Nivel nivel, @Nullable TaxaAlvo taxa, @Nullable BigDecimal pesoAlvo) {

  /**
   * O plano como a fase o grava. Na manutenção, o que vier é descartado (RN-33, RN-41). Em cutting
   * e bulking, nível, taxa alvo e peso alvo são obrigatórios (RN-28, RN-29, RN-41), e a taxa
   * diferente do padrão do nível o torna personalizado (RN-30).
   */
  public static PlanoDeFase montarPlano(
      TipoDeFase tipo,
      @Nullable Nivel nivel,
      @Nullable BigDecimal valor,
      @Nullable UnidadeDaTaxa unidade,
      @Nullable BigDecimal pesoAlvo) {
    if (tipo == TipoDeFase.MANUTENCAO) {
      return new PlanoDeFase(null, null, null);
    }
    if (nivel == null) {
      throw new RegraVioladaException(Recusa.NIVEL_AUSENTE, "nivel", "Escolha o nível da fase.");
    }
    if (valor == null || unidade == null) {
      throw new RegraVioladaException(
          Recusa.TAXA_ALVO_AUSENTE,
          valor == null ? "taxaAlvoValor" : "taxaAlvoUnidade",
          "Informe a taxa alvo, com o valor e a unidade.");
    }
    if (pesoAlvo == null) {
      throw new RegraVioladaException(Recusa.PESO_ALVO_AUSENTE, "pesoAlvo", "Informe o peso alvo.");
    }
    return comONivelDaTaxa(tipo, nivel, new TaxaAlvo(valor, unidade), pesoAlvo);
  }

  /**
   * O plano da fase editada (UC-06). Trocar entre cutting e bulking sem mexer na taxa aplica o
   * padrão do mesmo nível no tipo novo, e no personalizado, que não tem padrão, pede a taxa de
   * novo: a do outro tipo tem outro sinal e, no padrão, outra unidade (RN-28). No resto, vale o
   * plano como na criação, e a taxa editada torna o nível personalizado (RN-30).
   */
  public static PlanoDeFase editarPlano(
      TipoDeFase tipoAntes,
      @Nullable TaxaAlvo taxaAntes,
      TipoDeFase tipo,
      @Nullable Nivel nivel,
      @Nullable BigDecimal valor,
      @Nullable UnidadeDaTaxa unidade,
      @Nullable BigDecimal pesoAlvo) {
    boolean trocouEntreCuttingEBulking =
        tipoAntes != tipo && tipoAntes != TipoDeFase.MANUTENCAO && tipo != TipoDeFase.MANUTENCAO;
    if (trocouEntreCuttingEBulking
        && taxaAntes != null
        && nivel != null
        && pesoAlvo != null
        && valor != null
        && unidade == taxaAntes.unidade()
        && valor.compareTo(taxaAntes.valor()) == 0) {
      TaxaAlvo padrao =
          TaxaAlvo.padrao(tipo, nivel)
              .orElseThrow(
                  () ->
                      new RegraVioladaException(
                          Recusa.TAXA_ALVO_AUSENTE,
                          "taxaAlvoValor",
                          "O tipo mudou: informe a taxa alvo de novo."));
      return new PlanoDeFase(nivel, padrao, pesoAlvo);
    }
    return montarPlano(tipo, nivel, valor, unidade, pesoAlvo);
  }

  /** A taxa diferente do padrão do nível o torna personalizado (RN-30). */
  private static PlanoDeFase comONivelDaTaxa(
      TipoDeFase tipo, Nivel nivel, TaxaAlvo taxa, BigDecimal pesoAlvo) {
    boolean foraDoPadrao =
        TaxaAlvo.padrao(tipo, nivel)
            .filter(
                padrao ->
                    padrao.unidade() != taxa.unidade()
                        || padrao.valor().compareTo(taxa.valor()) != 0)
            .isPresent();
    return new PlanoDeFase(foraDoPadrao ? Nivel.PERSONALIZADO : nivel, taxa, pesoAlvo);
  }
}
