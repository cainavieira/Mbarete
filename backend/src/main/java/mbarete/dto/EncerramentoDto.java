package mbarete.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** A data de encerramento da fase corrente, que a tela propõe como hoje (UC-14). */
public record EncerramentoDto(@NotNull LocalDate dataFim) {}
