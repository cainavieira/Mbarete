package mbarete.dto;

import static mbarete.regras.FaixaDoRegistro.CINTURA_MAXIMA;
import static mbarete.regras.FaixaDoRegistro.CINTURA_MINIMA;
import static mbarete.regras.FaixaDoRegistro.PESO_MAXIMO;
import static mbarete.regras.FaixaDoRegistro.PESO_MINIMO;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

/**
 * O registro como a pessoa o envia, ao criar e ao editar: na edição, é o registro inteiro como ele
 * fica, e a data diferente da do caminho o move. Peso e cintura com uma casa (RN-02, RN-10), a
 * cintura opcional. O {@code substituir} só vem no segundo envio, depois de a pessoa confirmar
 * (UC-02 4a); ausente, o Jackson recusaria um {@code boolean}, e por isso ele é {@code Boolean}.
 */
public record RegistroEnviadoDto(
    @NotNull LocalDate data,
    @NotNull
        @DecimalMin(value = PESO_MINIMO, message = "PESO_FORA_DE_FAIXA")
        @DecimalMax(value = PESO_MAXIMO, message = "PESO_FORA_DE_FAIXA")
        @Digits(integer = 3, fraction = 1)
        BigDecimal peso,
    @DecimalMin(value = CINTURA_MINIMA, message = "CINTURA_FORA_DE_FAIXA")
        @DecimalMax(value = CINTURA_MAXIMA, message = "CINTURA_FORA_DE_FAIXA")
        @Digits(integer = 3, fraction = 1)
        @Nullable BigDecimal cintura,
    @Nullable Boolean substituir) {}
