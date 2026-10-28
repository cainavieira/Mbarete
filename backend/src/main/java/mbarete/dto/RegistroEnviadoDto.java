package mbarete.dto;

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
        @DecimalMin(value = "20.0", message = "PESO_FORA_DE_FAIXA")
        @DecimalMax(value = "400.0", message = "PESO_FORA_DE_FAIXA")
        @Digits(integer = 3, fraction = 1)
        BigDecimal peso,
    @DecimalMin(value = "40.0", message = "CINTURA_FORA_DE_FAIXA")
        @DecimalMax(value = "200.0", message = "CINTURA_FORA_DE_FAIXA")
        @Digits(integer = 3, fraction = 1)
        @Nullable BigDecimal cintura,
    @Nullable Boolean substituir) {}
