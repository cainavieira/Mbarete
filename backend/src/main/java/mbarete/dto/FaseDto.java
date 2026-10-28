package mbarete.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import mbarete.regras.Nivel;
import mbarete.regras.TipoDeFase;
import mbarete.regras.UnidadeDaTaxa;
import org.jspecify.annotations.Nullable;

/**
 * A fase, endereçada pelo id (endpoints.md). A taxa alvo volta sem sinal, como a pessoa a digita;
 * na manutenção, nível, taxa e peso alvo são nulos (RN-33, RN-41).
 */
public record FaseDto(
    int id,
    TipoDeFase tipo,
    LocalDate dataInicio,
    @Nullable LocalDate dataFim,
    @Nullable Nivel nivel,
    @Nullable BigDecimal taxaAlvoValor,
    @Nullable UnidadeDaTaxa taxaAlvoUnidade,
    @Nullable BigDecimal pesoAlvo) {}
