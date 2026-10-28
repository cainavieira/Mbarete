package mbarete.regras;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/** O histórico no formato que a importação aceita, para a pessoa levá-lo embora (RN-52, RNF-15). */
public final class EscritaDoCsv {

  private static final String CABECALHO = "data;peso;cintura";

  private EscritaDoCsv() {}

  /** O registro como sai no arquivo. */
  public record LinhaDoHistorico(LocalDate data, BigDecimal peso, @Nullable BigDecimal cintura) {}

  /**
   * O cabeçalho e uma linha por registro, pela data, com ponto e vírgula entre os campos, a data em
   * AAAA-MM-DD e ponto decimal (RN-53, RN-54, RN-55); a cintura não medida fica vazia. Sem
   * registro, só o cabeçalho, porque arquivo vazio é resposta válida para histórico vazio (UC-11
   * 2a).
   */
  public static String escreverCsv(List<LinhaDoHistorico> linhas) {
    return Stream.concat(
            Stream.of(CABECALHO),
            linhas.stream()
                .sorted(comparing(LinhaDoHistorico::data))
                .map(
                    linha ->
                        linha.data()
                            + ";"
                            + linha.peso().toPlainString()
                            + ";"
                            + Optional.ofNullable(linha.cintura())
                                .map(BigDecimal::toPlainString)
                                .orElse("")))
        .collect(joining("\n", "", "\n"));
  }
}
