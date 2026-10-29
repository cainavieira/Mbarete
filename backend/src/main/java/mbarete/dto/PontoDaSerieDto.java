package mbarete.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import mbarete.regras.TipoDeFase;
import org.jspecify.annotations.Nullable;

/**
 * Um dia do gráfico: o peso do dia, o peso semanal e a fase que cobre o dia, com o tipo dela, que o
 * gráfico escreve no bloco da fase, cada um nulo onde não há (RN-09, RN-16, RF-47).
 */
public record PontoDaSerieDto(
    LocalDate data,
    @Nullable BigDecimal peso,
    @Nullable BigDecimal pesoSemanal,
    @Nullable Integer faseId,
    @Nullable TipoDeFase faseTipo) {}
