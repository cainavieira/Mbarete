package mbarete.regras;

import static mbarete.regras.PesoSemanal.calcularPesoSemanal;
import static mbarete.regras.Progresso.calcularProgresso;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * O que a fase encerrada apresenta no lugar do veredito (RN-40): a duração, com o início e o fim
 * contados; a variação total, nula com menos de dois registros; e o alvo, nulo na manutenção, que
 * não tem (RN-41), com o primeiro dia em que foi atingido.
 */
public record FaseEncerrada(
    long duracaoEmDias,
    @Nullable VariacaoTotal variacaoTotal,
    @Nullable Alvo alvo,
    @Nullable LocalDate alvoAtingidoEm) {

  /** Com as duas datas, como as da RN-47, e marcada quando não veio do peso semanal. */
  public record VariacaoTotal(
      BigDecimal valor, LocalDate de, LocalDate ate, boolean poucosPontos) {}

  public enum Alvo {
    ATINGIDO,
    NAO_ATINGIDO,
    /** Do lado errado da base, ou igual a ela (RN-44). */
    LADO_ERRADO,
    /** Sem a base não há com que comparar (RN-68). */
    SEM_AVALIACAO
  }

  /**
   * A variação vai da base ao peso semanal do último dia da fase que o tiver, e o alvo conta como
   * atingido quando o peso semanal o alcançou em algum dia dela: a variação diz onde a fase
   * terminou, e o alvo, o que ela alcançou no caminho. Sem a base, a variação vai do primeiro ao
   * último registro, e o alvo fica sem avaliação (RN-40, RN-68).
   */
  public static FaseEncerrada resumirFaseEncerrada(
      TipoDeFase tipo,
      @Nullable BigDecimal pesoAlvo,
      LocalDate inicio,
      LocalDate fim,
      @Nullable LocalDate diaDaBase,
      List<Pesagem> pesagens) {
    long duracao = ChronoUnit.DAYS.between(inicio, fim) + 1;
    Optional<BigDecimal> base =
        diaDaBase == null ? Optional.empty() : calcularPesoSemanal(pesagens, diaDaBase);
    if (diaDaBase == null || base.isEmpty()) {
      return new FaseEncerrada(
          duracao,
          doPrimeiroAoUltimo(pesagens, inicio, fim),
          pesoAlvo == null ? null : Alvo.SEM_AVALIACAO,
          null);
    }
    VariacaoTotal variacao = daBase(pesagens, diaDaBase, base.get(), fim);
    if (pesoAlvo == null) {
      return new FaseEncerrada(duracao, variacao, null, null);
    }
    if (calcularProgresso(tipo, pesoAlvo, pesagens, diaDaBase, diaDaBase)
        instanceof Progresso.NaoExibido) {
      return new FaseEncerrada(duracao, variacao, Alvo.LADO_ERRADO, null);
    }
    Optional<LocalDate> atingidoEm = primeiroDiaNoAlvo(tipo, pesoAlvo, pesagens, diaDaBase, fim);
    return new FaseEncerrada(
        duracao,
        variacao,
        atingidoEm.isPresent() ? Alvo.ATINGIDO : Alvo.NAO_ATINGIDO,
        atingidoEm.orElse(null));
  }

  /** Recua do fim até achar peso semanal; no pior caso, para na própria base. */
  private static VariacaoTotal daBase(
      List<Pesagem> pesagens, LocalDate diaDaBase, BigDecimal base, LocalDate fim) {
    return Stream.iterate(fim, dia -> !dia.isBefore(diaDaBase), dia -> dia.minusDays(1))
        .flatMap(
            dia ->
                calcularPesoSemanal(pesagens, dia)
                    .map(
                        semanal -> new VariacaoTotal(semanal.subtract(base), diaDaBase, dia, false))
                    .stream())
        .findFirst()
        .orElseThrow();
  }

  private static @Nullable VariacaoTotal doPrimeiroAoUltimo(
      List<Pesagem> pesagens, LocalDate inicio, LocalDate fim) {
    List<Pesagem> naFase =
        pesagens.stream()
            .filter(pesagem -> !pesagem.data().isBefore(inicio) && !pesagem.data().isAfter(fim))
            .sorted(Comparator.comparing(Pesagem::data))
            .toList();
    if (naFase.size() < 2) {
      return null;
    }
    Pesagem primeira = naFase.getFirst();
    Pesagem ultima = naFase.getLast();
    return new VariacaoTotal(
        ultima.peso().subtract(primeira.peso()), primeira.data(), ultima.data(), true);
  }

  /** Medido como a barra mede, pelo peso semanal exato (RN-44, RN-45). */
  private static Optional<LocalDate> primeiroDiaNoAlvo(
      TipoDeFase tipo,
      BigDecimal pesoAlvo,
      List<Pesagem> pesagens,
      LocalDate diaDaBase,
      LocalDate fim) {
    return diaDaBase
        .datesUntil(fim.plusDays(1))
        .filter(
            dia ->
                calcularProgresso(tipo, pesoAlvo, pesagens, diaDaBase, dia)
                        instanceof Progresso.Barra barra
                    && barra.atingido())
        .findFirst();
  }
}
