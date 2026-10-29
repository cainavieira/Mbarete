package mbarete.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import mbarete.regras.Nivel;
import mbarete.regras.SituacaoDaFase;
import mbarete.regras.TipoDeFase;
import mbarete.regras.UnidadeDaTaxa;
import org.jspecify.annotations.Nullable;

/**
 * A fase na lista, com a situação em relação a hoje, quantos registros tem e o que apagá-la faz com
 * a anterior, o que a confirmação de apagar informa (UC-07). O histórico só vem na encerrada
 * (RN-40).
 */
public record FaseListadaDto(
    int id,
    TipoDeFase tipo,
    LocalDate dataInicio,
    @Nullable LocalDate dataFim,
    @Nullable Nivel nivel,
    @Nullable BigDecimal taxaAlvoValor,
    @Nullable UnidadeDaTaxa taxaAlvoUnidade,
    @Nullable BigDecimal pesoAlvo,
    SituacaoDaFase situacao,
    long registros,
    @Nullable AoApagarDto aoApagar,
    @Nullable HistoricoDto historico) {}
