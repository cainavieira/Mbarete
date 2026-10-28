package mbarete.regras;

import static mbarete.regras.Variacao.calcularVariacao;

import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** As três variações da tela, cada uma nula quando não há com o que comparar (RN-46, RN-47). */
public record Variacoes(
    @Nullable Variacao semana, @Nullable Variacao mes, @Nullable Variacao desdeOInicio) {

  private static final int DIAS_DA_SEMANA = 7;

  private static final int DIAS_DO_MES = 30;

  /**
   * As partidas são 7 dias atrás, 30 dias atrás e o primeiro registro, e cada uma avança até o
   * primeiro dia com peso semanal (RN-46, RN-47).
   */
  public static Variacoes calcularVariacoes(List<Pesagem> pesagens, LocalDate hoje) {
    return new Variacoes(
        calcularVariacao(pesagens, hoje.minusDays(DIAS_DA_SEMANA), hoje).orElse(null),
        calcularVariacao(pesagens, hoje.minusDays(DIAS_DO_MES), hoje).orElse(null),
        pesagens.stream()
            .map(Pesagem::data)
            .min(LocalDate::compareTo)
            .flatMap(primeiro -> calcularVariacao(pesagens, primeiro, hoje))
            .orElse(null));
  }
}
