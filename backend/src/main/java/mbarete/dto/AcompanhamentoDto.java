package mbarete.dto;

import java.math.BigDecimal;
import java.util.List;
import mbarete.regras.FaixaDeTolerancia;
import mbarete.regras.PontoDaSerie;
import mbarete.regras.ResumoCintura;
import mbarete.regras.Variacoes;
import org.jspecify.annotations.Nullable;

/**
 * A tela principal numa resposta só, porque tudo nela deriva do mesmo peso semanal (endpoints.md).
 * Sem fase corrente, vêm nulos a fase, o veredito, a taxa alvo, a faixa e a barra, e o resto
 * continua (RN-66). Os números vêm como a tela os mostra (RN-37).
 */
public record AcompanhamentoDto(
    @Nullable BigDecimal pesoSemanal,
    int registrosNaJanela,
    Variacoes variacoes,
    @Nullable FaseDto faseCorrente,
    @Nullable VereditoDto veredito,
    @Nullable BigDecimal taxaAlvoKgSemana,
    @Nullable FaixaDeTolerancia faixa,
    @Nullable ProgressoDto progresso,
    @Nullable ResumoCintura cintura,
    List<PontoDaSerie> serie) {}
