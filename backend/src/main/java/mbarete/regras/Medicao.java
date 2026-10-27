package mbarete.regras;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A cintura medida numa data, em centímetros (RN-10). */
public record Medicao(LocalDate data, BigDecimal cintura) {}
