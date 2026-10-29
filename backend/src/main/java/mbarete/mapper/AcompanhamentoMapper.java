package mbarete.mapper;

import static mbarete.regras.Arredondamento.arredondarPercentual;
import static mbarete.regras.Arredondamento.arredondarPeso;
import static mbarete.regras.Arredondamento.arredondarTaxa;

import java.math.BigDecimal;
import java.util.List;
import mbarete.dto.AcompanhamentoDto;
import mbarete.dto.ProgressoDto;
import mbarete.dto.VereditoDto;
import mbarete.entity.Fase;
import mbarete.regras.AcompanhamentoDaFase;
import mbarete.regras.PontoDaSerie;
import mbarete.regras.Progresso;
import mbarete.regras.ResumoCintura;
import mbarete.regras.Ritmo;
import mbarete.regras.TaxaReal;
import mbarete.regras.Variacao;
import mbarete.regras.Variacoes;
import org.jspecify.annotations.Nullable;

public final class AcompanhamentoMapper {

  private AcompanhamentoMapper() {}

  /**
   * Só o resultado se arredonda, como a tela mostra: peso com uma casa, taxa com duas e a barra
   * inteira (RN-37). A cintura já chega com uma casa (RN-10).
   */
  public static AcompanhamentoDto paraDto(
      @Nullable BigDecimal pesoSemanal,
      int registrosNaJanela,
      Variacoes variacoes,
      @Nullable Fase corrente,
      @Nullable AcompanhamentoDaFase daFase,
      @Nullable ResumoCintura cintura,
      List<PontoDaSerie> serie) {
    return new AcompanhamentoDto(
        pesoSemanal == null ? null : arredondarPeso(pesoSemanal),
        registrosNaJanela,
        new Variacoes(
            arredondada(variacoes.semana()),
            arredondada(variacoes.mes()),
            arredondada(variacoes.desdeOInicio())),
        corrente == null ? null : FaseMapper.paraDto(corrente),
        daFase == null ? null : veredito(daFase),
        daFase == null ? null : taxaAlvo(daFase),
        daFase == null ? null : daFase.faixa(),
        daFase == null ? null : progresso(daFase),
        cintura,
        serie.stream().map(AcompanhamentoMapper::arredondado).toList());
  }

  private static @Nullable Variacao arredondada(@Nullable Variacao variacao) {
    return variacao == null
        ? null
        : new Variacao(arredondarPeso(variacao.valor()), variacao.de(), variacao.ate());
  }

  private static VereditoDto veredito(AcompanhamentoDaFase daFase) {
    Ritmo ritmo = daFase.ritmo();
    TaxaReal taxaReal = daFase.taxaReal();
    VereditoDto.Estado estado =
        ritmo == null
            ? VereditoDto.Estado.DADOS_INSUFICIENTES
            : switch (ritmo) {
              case ABAIXO -> VereditoDto.Estado.ABAIXO;
              case NO_RITMO -> VereditoDto.Estado.NO_RITMO;
              case ACIMA -> VereditoDto.Estado.ACIMA;
            };
    return new VereditoDto(
        estado,
        taxaReal == null
            ? null
            : new TaxaReal(arredondarTaxa(taxaReal.valor()), taxaReal.de(), taxaReal.ate()),
        daFase.vereditoAPartirDe());
  }

  private static @Nullable BigDecimal taxaAlvo(AcompanhamentoDaFase daFase) {
    BigDecimal taxa = daFase.taxaAlvoKg();
    return taxa == null ? null : arredondarTaxa(taxa);
  }

  private static @Nullable ProgressoDto progresso(AcompanhamentoDaFase daFase) {
    Progresso progresso = daFase.progresso();
    if (progresso == null) {
      return null;
    }
    return switch (progresso) {
      case Progresso.Barra(BigDecimal percentual, boolean atingido) ->
          new ProgressoDto(ProgressoDto.Estado.BARRA, arredondarPercentual(percentual), atingido);
      case Progresso.DadosInsuficientes() ->
          new ProgressoDto(ProgressoDto.Estado.DADOS_INSUFICIENTES, null, false);
      case Progresso.NaoExibido() -> new ProgressoDto(ProgressoDto.Estado.NAO_EXIBIDO, null, false);
    };
  }

  private static PontoDaSerie arredondado(PontoDaSerie ponto) {
    BigDecimal semanal = ponto.pesoSemanal();
    return new PontoDaSerie(
        ponto.data(),
        ponto.peso(),
        semanal == null ? null : arredondarPeso(semanal),
        ponto.faseId());
  }
}
