package mbarete.regras;

import static mbarete.regras.FaixaDeTolerancia.calcularFaixa;
import static mbarete.regras.Progresso.calcularProgresso;
import static mbarete.regras.Ritmo.avaliarRitmo;
import static mbarete.regras.Ritmo.dataDoVeredito;
import static mbarete.regras.TaxaAlvo.converterTaxaAlvo;
import static mbarete.regras.TaxaReal.calcularTaxaReal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * O que a fase corrente mostra no acompanhamento, cada parte nula onde o tipo não a tem ou faltam
 * dados: a taxa alvo em quilogramas da semana (RN-35); o veredito, nulo enquanto os dados forem
 * insuficientes (RN-39), com a taxa real ao lado em cutting e bulking (RN-36) ou, enquanto falta só
 * a espera, o dia em que ele aparece (RF-31); a faixa da manutenção (RN-33); e a barra, que a
 * manutenção não tem (RN-41).
 */
public record AcompanhamentoDaFase(
    @Nullable BigDecimal taxaAlvoKg,
    @Nullable Ritmo ritmo,
    @Nullable TaxaReal taxaReal,
    @Nullable LocalDate vereditoAPartirDe,
    @Nullable FaixaDeTolerancia faixa,
    @Nullable Progresso progresso) {

  /**
   * Sem o dia da base, não há veredito, faixa nem barra: só a taxa alvo, e a barra informa dados
   * insuficientes (RN-68). A taxa real só aparece junto do veredito, porque antes dos 14 dias ela
   * ainda é mais água do que ritmo (RN-39, UC-08 4a).
   */
  public static AcompanhamentoDaFase acompanharFase(
      TipoDeFase tipo,
      @Nullable TaxaAlvo taxa,
      @Nullable BigDecimal pesoAlvo,
      @Nullable LocalDate diaDaBase,
      @Nullable LocalDate fimDaFase,
      List<Pesagem> pesagens,
      LocalDate hoje) {
    @Nullable BigDecimal taxaAlvoKg = converterTaxaAlvo(tipo, taxa, pesagens, hoje).orElse(null);
    if (diaDaBase == null) {
      return new AcompanhamentoDaFase(
          taxaAlvoKg,
          null,
          null,
          null,
          null,
          tipo == TipoDeFase.MANUTENCAO ? null : new Progresso.DadosInsuficientes());
    }
    @Nullable Ritmo ritmo = avaliarRitmo(tipo, taxa, pesagens, diaDaBase, hoje).orElse(null);
    if (tipo == TipoDeFase.MANUTENCAO) {
      return new AcompanhamentoDaFase(
          taxaAlvoKg, ritmo, null, null, calcularFaixa(pesagens, diaDaBase).orElse(null), null);
    }
    BigDecimal alvo = Objects.requireNonNull(pesoAlvo, "Cutting e bulking têm peso alvo (RN-41)");
    return new AcompanhamentoDaFase(
        taxaAlvoKg,
        ritmo,
        ritmo == null ? null : calcularTaxaReal(pesagens, diaDaBase, hoje).orElse(null),
        dataDoVeredito(tipo, pesagens, diaDaBase, fimDaFase, hoje).orElse(null),
        null,
        calcularProgresso(tipo, alvo, pesagens, diaDaBase, hoje));
  }
}
