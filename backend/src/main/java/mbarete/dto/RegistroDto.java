package mbarete.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

/** O registro endereçado pela data, sem o identificador interno (endpoints.md). */
public record RegistroDto(LocalDate data, BigDecimal peso, @Nullable BigDecimal cintura) {}
