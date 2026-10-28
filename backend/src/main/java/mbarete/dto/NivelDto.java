package mbarete.dto;

import java.math.BigDecimal;
import mbarete.regras.Nivel;
import mbarete.regras.TipoDeFase;
import mbarete.regras.UnidadeDaTaxa;

/** A taxa alvo padrão de um nível num tipo, sem sinal, para a tela sugerir (RF-16). */
public record NivelDto(TipoDeFase tipo, Nivel nivel, BigDecimal valor, UnidadeDaTaxa unidade) {}
