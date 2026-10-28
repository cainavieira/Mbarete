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
    TaxaAlvo taxa = new TaxaAlvo(valor, unidade);
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
