package mbarete.dto;

import java.time.LocalDate;
import mbarete.regras.FaseEncerrada.Alvo;
import mbarete.regras.FaseEncerrada.VariacaoTotal;
import org.jspecify.annotations.Nullable;

/**
 * O que a fase encerrada apresenta no lugar do veredito (RN-40). A variação vem com uma casa, como
 * a tela a mostra, e nula com menos de dois registros; o alvo é nulo na manutenção (RN-41).
 */
public record HistoricoDto(
    long duracaoEmDias,
    @Nullable VariacaoTotal variacaoTotal,
    @Nullable Alvo alvo,
    @Nullable LocalDate alvoAtingidoEm) {}
