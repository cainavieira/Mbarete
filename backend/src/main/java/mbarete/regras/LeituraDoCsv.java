package mbarete.regras;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;

/** O arquivo importado, lido linha a linha, sem gravar nada (RN-58). */
public record LeituraDoCsv(List<LinhaValida> validas, List<LinhaRecusada> recusadas) {

  /** A linha lida, numerada como no arquivo, com o cabeçalho na linha 1. */
  public sealed interface LinhaLida permits LinhaValida, LinhaRecusada {}

  /** A linha que pode ser gravada; substitui quando a data já tem registro (RN-58). */
  public record LinhaValida(
      int linha, LocalDate data, BigDecimal peso, @Nullable BigDecimal cintura, boolean substitui)
      implements LinhaLida {}

  public record LinhaRecusada(int linha, String motivo) implements LinhaLida {}

  private static final String CAMPO = "arquivo";

  /** Só ponto decimal: a vírgula pode ser o separador das colunas (RN-54, RN-55). */
  private static final Pattern NUMERO = Pattern.compile("\\d+(\\.\\d+)?");

  private static final DateTimeFormatter BRASILEIRA =
      DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

  private static final Medida PESO =
      new Medida(
          "O peso deve ser um número com ponto decimal, como 85.7.",
          "O peso tem só uma casa decimal, como 85.7.",
          FaixaDoRegistro.PESO_MINIMO,
          FaixaDoRegistro.PESO_MAXIMO,
          FaixaDoRegistro.PESO_FORA_DE_FAIXA);

  private static final Medida CINTURA =
      new Medida(
          "A cintura deve ser um número com ponto decimal, como 90.5.",
          "A cintura tem só uma casa decimal, como 90.5.",
          FaixaDoRegistro.CINTURA_MINIMA,
          FaixaDoRegistro.CINTURA_MAXIMA,
          FaixaDoRegistro.CINTURA_FORA_DE_FAIXA);

  /**
   * Confere as datas que a pessoa viu substituídas na pré-visualização contra as que esta leitura
   * substitui. Se o histórico mudou entre as duas chamadas, em qualquer direção, nada é gravado, e
   * a tela refaz a pré-visualização: a data mostrada como nova não é substituída sem a confirmação
   * da RN-01 (UC-10 3b).
   */
  public void conferirSubstituicoes(Set<LocalDate> confirmadas) {
    Set<LocalDate> substituidas =
        validas.stream().filter(LinhaValida::substitui).map(LinhaValida::data).collect(toSet());
    if (!substituidas.equals(confirmadas)) {
      throw new RegraVioladaException(
          Recusa.PREVISUALIZACAO_DESATUALIZADA,
          "O histórico mudou desde a pré-visualização: nada foi gravado.");
    }
  }

  /**
   * O separador sai do cabeçalho, e as colunas são achadas pelo nome, em qualquer ordem (RN-53,
   * RN-54). Sem as duas obrigatórias ou sem separador conhecido, o arquivo inteiro é recusado,
   * porque nem dá para começar a ler (UC-10 1a, 1b). Cada linha passa pelas regras do registro, e a
   * data repetida no arquivo recusa todas as linhas dela (RN-60). Linha em branco não conta como
   * recusada, mas conta na numeração.
   */
  public static LeituraDoCsv lerCsv(String conteudo, Set<LocalDate> existentes, LocalDate hoje) {
    List<String> linhas = conteudo.lines().toList();
    String cabecalho = linhas.isEmpty() ? "" : semMarcaDeOrdem(linhas.getFirst());
    if (cabecalho.isBlank()) {
      throw colunasAusentes();
    }
    String separador = separador(cabecalho);
    List<String> colunas =
        campos(cabecalho, separador).stream().map(nome -> nome.toLowerCase(Locale.ROOT)).toList();
    int data = colunas.indexOf("data");
    int peso = colunas.indexOf("peso");
    int cintura = colunas.indexOf("cintura");
    if (data < 0 || peso < 0) {
      throw colunasAusentes();
    }
    List<Bruta> brutas =
        IntStream.range(1, linhas.size())
            .filter(indice -> !linhas.get(indice).isBlank())
            .mapToObj(
                indice -> {
                  List<String> campos = campos(linhas.get(indice), separador);
                  return new Bruta(
                      indice + 1, campo(campos, data), campo(campos, peso), campo(campos, cintura));
                })
            .toList();
    Map<LocalDate, List<Integer>> linhasPorData =
        brutas.stream()
            .flatMap(
                bruta -> lerData(bruta.data()).map(dia -> Map.entry(dia, bruta.linha())).stream())
            .collect(groupingBy(Map.Entry::getKey, mapping(Map.Entry::getValue, toList())));
    List<LinhaLida> lidas =
        brutas.stream().map(bruta -> ler(bruta, linhasPorData, existentes, hoje)).toList();
    return new LeituraDoCsv(
        lidas.stream().filter(LinhaValida.class::isInstance).map(LinhaValida.class::cast).toList(),
        lidas.stream()
            .filter(LinhaRecusada.class::isInstance)
            .map(LinhaRecusada.class::cast)
            .toList());
  }

  /** A primeira regra que a linha quebra dá o motivo, na ordem dos campos. */
  private static LinhaLida ler(
      Bruta bruta,
      Map<LocalDate, List<Integer>> linhasPorData,
      Set<LocalDate> existentes,
      LocalDate hoje) {
    if (bruta.data().isEmpty()) {
      return new LinhaRecusada(bruta.linha(), "Falta a data.");
    }
    Optional<LocalDate> lida = lerData(bruta.data());
    if (lida.isEmpty()) {
      return new LinhaRecusada(bruta.linha(), "A data deve estar em AAAA-MM-DD ou DD/MM/AAAA.");
    }
    LocalDate data = lida.get();
    if (data.isAfter(hoje)) {
      return new LinhaRecusada(bruta.linha(), DataDoRegistro.MENSAGEM);
    }
    List<Integer> outras =
        linhasPorData.getOrDefault(data, List.of()).stream()
            .filter(numero -> numero != bruta.linha())
            .toList();
    if (!outras.isEmpty()) {
      return new LinhaRecusada(
          bruta.linha(),
          "A mesma data está também "
              + (outras.size() == 1 ? "na linha " : "nas linhas ")
              + emFrase(outras)
              + ".");
    }
    if (bruta.peso().isEmpty()) {
      return new LinhaRecusada(bruta.linha(), "Falta o peso.");
    }
    Optional<String> doPeso = PESO.recusar(bruta.peso());
    if (doPeso.isPresent()) {
      return new LinhaRecusada(bruta.linha(), doPeso.get());
    }
    Optional<String> daCintura =
        bruta.cintura().isEmpty() ? Optional.empty() : CINTURA.recusar(bruta.cintura());
    if (daCintura.isPresent()) {
      return new LinhaRecusada(bruta.linha(), daCintura.get());
    }
    return new LinhaValida(
        bruta.linha(),
        data,
        comUmaCasa(bruta.peso()),
        bruta.cintura().isEmpty() ? null : comUmaCasa(bruta.cintura()),
        existentes.contains(data));
  }

  private static String separador(String cabecalho) {
    if (cabecalho.contains(";")) {
      return ";";
    }
    if (cabecalho.contains(",")) {
      return ",";
    }
    throw new RegraVioladaException(
        Recusa.CSV_SEPARADOR_DESCONHECIDO,
        CAMPO,
        "As colunas do arquivo devem ser separadas por ponto e vírgula ou por vírgula.");
  }

  private static RegraVioladaException colunasAusentes() {
    return new RegraVioladaException(
        Recusa.CSV_COLUNA_AUSENTE, CAMPO, "O arquivo precisa das colunas data e peso.");
  }

  /** A planilha costuma gravar a marca de ordem dos bytes no começo do arquivo. */
  private static String semMarcaDeOrdem(String linha) {
    return linha.startsWith("\uFEFF") ? linha.substring(1) : linha;
  }

  private static List<String> campos(String linha, String separador) {
    return Arrays.stream(linha.split(Pattern.quote(separador), -1)).map(String::strip).toList();
  }

  /** A linha com campos a menos tem os que faltam vazios. */
  private static String campo(List<String> campos, int indice) {
    return indice >= 0 && indice < campos.size() ? campos.get(indice) : "";
  }

  private static Optional<LocalDate> lerData(String texto) {
    try {
      return Optional.of(
          texto.contains("/") ? LocalDate.parse(texto, BRASILEIRA) : LocalDate.parse(texto));
    } catch (DateTimeParseException naoEData) {
      return Optional.empty();
    }
  }

  /** O número já conferido, com uma casa, como o banco guarda: 85 vira 85.0 (RN-02). */
  private static BigDecimal comUmaCasa(String texto) {
    return new BigDecimal(texto).setScale(1);
  }

  /** "3", "3 e 9", "3, 5 e 9". */
  private static String emFrase(List<Integer> numeros) {
    List<String> textos = numeros.stream().map(String::valueOf).toList();
    if (textos.size() == 1) {
      return textos.getFirst();
    }
    return String.join(", ", textos.subList(0, textos.size() - 1)) + " e " + textos.getLast();
  }

  /** Os campos da linha como vieram, antes de qualquer conferência. */
  private record Bruta(int linha, String data, String peso, String cintura) {}

  /**
   * Ponto decimal, uma casa e a faixa (RN-02, RN-03, RN-10, RN-11, RN-55). A casa é contada sem os
   * zeros do fim, como na conferência do corpo das rotas: 85.70 vale.
   */
  private record Medida(
      String formato, String casas, String minimo, String maximo, String foraDeFaixa) {

    Optional<String> recusar(String texto) {
      if (!NUMERO.matcher(texto).matches()) {
        return Optional.of(formato);
      }
      BigDecimal numero = new BigDecimal(texto);
      if (numero.stripTrailingZeros().scale() > 1) {
        return Optional.of(casas);
      }
      if (numero.compareTo(new BigDecimal(minimo)) < 0
          || numero.compareTo(new BigDecimal(maximo)) > 0) {
        return Optional.of(foraDeFaixa);
      }
      return Optional.empty();
    }
  }
}
