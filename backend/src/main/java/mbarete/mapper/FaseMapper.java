package mbarete.mapper;

import static mbarete.regras.Arredondamento.arredondarPeso;

import mbarete.dto.FaseDto;
import mbarete.dto.FaseListadaDto;
import mbarete.dto.HistoricoDto;
import mbarete.entity.Fase;
import mbarete.regras.FaseEncerrada;
import mbarete.regras.FaseEncerrada.VariacaoTotal;
import mbarete.regras.PeriodoDeFase;
import mbarete.regras.SituacaoDaFase;
import org.jspecify.annotations.Nullable;

public final class FaseMapper {

  private FaseMapper() {}

  public static FaseDto paraDto(Fase fase) {
    PeriodoDeFase periodo = fase.periodo();
    return new FaseDto(
        periodo.id(),
        fase.tipo(),
        periodo.inicio(),
        periodo.fim(),
        fase.nivel(),
        fase.taxaAlvoValor(),
        fase.taxaAlvoUnidade(),
        fase.pesoAlvo());
  }

  public static FaseListadaDto paraListada(
      Fase fase, SituacaoDaFase situacao, long registros, @Nullable FaseEncerrada encerrada) {
    PeriodoDeFase periodo = fase.periodo();
    return new FaseListadaDto(
        periodo.id(),
        fase.tipo(),
        periodo.inicio(),
        periodo.fim(),
        fase.nivel(),
        fase.taxaAlvoValor(),
        fase.taxaAlvoUnidade(),
        fase.pesoAlvo(),
        situacao,
        registros,
        encerrada == null ? null : paraHistorico(encerrada));
  }

  /** Só o resultado se arredonda, nunca o meio da conta (RN-37). */
  private static HistoricoDto paraHistorico(FaseEncerrada encerrada) {
    VariacaoTotal variacao = encerrada.variacaoTotal();
    return new HistoricoDto(
        encerrada.duracaoEmDias(),
        variacao == null
            ? null
            : new VariacaoTotal(
                arredondarPeso(variacao.valor()),
                variacao.de(),
                variacao.ate(),
                variacao.poucosPontos()),
        encerrada.alvo(),
        encerrada.alvoAtingidoEm());
  }
}
