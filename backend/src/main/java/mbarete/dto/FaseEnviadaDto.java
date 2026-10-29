package mbarete.dto;

import static mbarete.regras.FaixaDoRegistro.PESO_MAXIMO;
import static mbarete.regras.FaixaDoRegistro.PESO_MINIMO;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import mbarete.regras.Nivel;
import mbarete.regras.TipoDeFase;
import mbarete.regras.UnidadeDaTaxa;
import org.jspecify.annotations.Nullable;

/**
 * A fase como a pessoa a envia, ao criar e ao editar: na edição, é a fase inteira como ela fica. A
 * data de fim só vem na fase que já terminou (RF-15), ou com o fim no futuro que ela já tinha. A
 * taxa alvo vem sem sinal, com até duas casas (RN-29), e o peso alvo com uma (RN-02). O que o tipo
 * exige ou dispensa é conferido na regra do plano. As confirmações só vêm no segundo envio, depois
 * de a pessoa ler o efeito na outra fase: {@code encerrarEmAberto} ao criar (RN-22) e {@code
 * moverFimDaAnterior} ao editar (RN-67); ausentes, o Jackson recusaria um {@code boolean}.
 */
public record FaseEnviadaDto(
    @NotNull TipoDeFase tipo,
    @NotNull LocalDate dataInicio,
    @Nullable LocalDate dataFim,
    @Nullable Nivel nivel,
    @DecimalMin(value = "0", inclusive = false, message = "TAXA_ALVO_INVALIDA")
        @Digits(integer = 2, fraction = 2)
        @Nullable BigDecimal taxaAlvoValor,
    @Nullable UnidadeDaTaxa taxaAlvoUnidade,
    @DecimalMin(value = PESO_MINIMO, message = "PESO_FORA_DE_FAIXA")
        @DecimalMax(value = PESO_MAXIMO, message = "PESO_FORA_DE_FAIXA")
        @Digits(integer = 3, fraction = 1)
        @Nullable BigDecimal pesoAlvo,
    @Nullable Boolean encerrarEmAberto,
    @Nullable Boolean moverFimDaAnterior) {}
