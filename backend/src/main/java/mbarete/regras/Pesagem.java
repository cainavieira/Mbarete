package mbarete.regras;

import java.math.BigDecimal;
import java.time.LocalDate;

/** O peso registrado numa data (RN-01, RN-02). */
public record Pesagem(LocalDate data, BigDecimal peso) {}
