package mbarete.dto;

import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

/**
 * O que apagar a fase faz com a anterior, cujo fim no futuro estava ali por causa dela: a data de
 * fim que ela passa a ter, nula quando ela volta a ser a fase em aberto (RN-67, UC-07 3b).
 */
public record AoApagarDto(int fase, @Nullable LocalDate dataFim) {}
