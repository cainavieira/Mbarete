package mbarete.regras;

import static mbarete.regras.LeituraDoCsv.lerCsv;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import mbarete.regras.LeituraDoCsv.LinhaRecusada;
import mbarete.regras.LeituraDoCsv.LinhaValida;
import mbarete.regras.LeituraDoCsv.RegistroGravado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class LeituraDoCsvTest {

  private static final LocalDate HOJE = LocalDate.of(2026, 10, 20);

  private static final LocalDate PRIMEIRO_DE_SETEMBRO = LocalDate.of(2026, 9, 1);
  private static final RegistroGravado GRAVADO =
      new RegistroGravado(new BigDecimal("86.0"), new BigDecimal("91.0"));

  // Pega a coluna lida pela posição, o nome com maiúscula ou espaço, a marca
  // de ordem dos bytes que a planilha grava no começo e a quebra de linha do
  // Windows (RN-53)
  @Test
  void achaAsColunasPeloNomeEmQualquerOrdem() {
    var leitura = ler("\uFEFFPeso ; Cintura;DATA\r\n85.7;90.0;2026-09-01\r\n");

    assertThat(leitura.validas())
        .containsExactly(
            new LinhaValida(
                2, PRIMEIRO_DE_SETEMBRO, new BigDecimal("85.7"), new BigDecimal("90.0"), null));
    assertThat(leitura.recusadas()).isEmpty();
  }

  // Pega o separador fixo em ponto e vírgula (RN-54)
  @Test
  void separaPorVirgulaTambem() {
    assertThat(ler("data,peso\n2026-09-01,85.7").validas())
        .containsExactly(
            new LinhaValida(2, PRIMEIRO_DE_SETEMBRO, new BigDecimal("85.7"), null, null));
  }

  // Pega o arquivo sem separador conhecido lido como uma coluna só (UC-10 1b)
  @Test
  void separadorDesconhecidoRecusaOArquivo() {
    assertThatThrownBy(() -> ler("data\tpeso\n2026-09-01\t85.7"))
        .isInstanceOfSatisfying(
            RegraVioladaException.class,
            excecao -> {
              assertThat(excecao.recusa()).isEqualTo(Recusa.CSV_SEPARADOR_DESCONHECIDO);
              assertThat(excecao.campo()).isEqualTo("arquivo");
            });
  }

  // Pega o arquivo sem data ou sem peso aceito, e o vazio estourando (UC-10 1a)
  @ParameterizedTest
  @ValueSource(strings = {"data;cintura\n2026-09-01;90.0", "peso,cintura\n85.7,90.0", "", "\n"})
  void semAsColunasObrigatoriasRecusaOArquivo(String conteudo) {
    assertThatThrownBy(() -> ler(conteudo))
        .isInstanceOfSatisfying(
            RegraVioladaException.class,
            excecao -> assertThat(excecao.recusa()).isEqualTo(Recusa.CSV_COLUNA_AUSENTE));
  }

  // Pega um dos dois formatos de data recusado (RN-55)
  @Test
  void aDataAceitaOsDoisFormatos() {
    assertThat(ler("data;peso\n07/09/2026;85.7\n2026-09-08;85.6").validas())
        .extracting(LinhaValida::data)
        .containsExactly(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 8));
  }

  // Pega cada regra do registro que a linha importada pularia (RN-60), a
  // vírgula decimal aceita (RN-55), o dia do mês trocado pelo mês e a linha com
  // campos a menos estourando
  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "2026-9-8;85.7         | A data deve estar em AAAA-MM-DD ou DD/MM/AAAA.",
        "31/02/2026;85.7       | A data deve estar em AAAA-MM-DD ou DD/MM/AAAA.",
        ";85.7                 | Falta a data.",
        "2026-10-21;85.7       | A data do registro não pode estar no futuro.",
        "2026-09-01;           | Falta o peso.",
        "2026-09-01            | Falta o peso.",
        "2026-09-01;85,7       | O peso deve ser um número com ponto decimal, como 85.7.",
        "2026-09-01;85.75      | O peso tem só uma casa decimal, como 85.7.",
        "2026-09-01;19.9       | O peso deve estar entre 20,0 e 400,0 kg.",
        "2026-09-01;400.1      | O peso deve estar entre 20,0 e 400,0 kg.",
        "2026-09-01;85.7;90,5  | A cintura deve ser um número com ponto decimal, como 90.5.",
        "2026-09-01;85.7;90.55 | A cintura tem só uma casa decimal, como 90.5.",
        "2026-09-01;85.7;39.9  | A cintura deve estar entre 40,0 e 200,0 cm.",
        "2026-09-01;85.7;200.1 | A cintura deve estar entre 40,0 e 200,0 cm.",
      })
  void aLinhaQueQuebraUmaRegraERecusadaComOMotivo(String linha, String motivo) {
    var leitura = ler("data;peso;cintura\n" + linha);

    assertThat(leitura.recusadas()).containsExactly(new LinhaRecusada(2, motivo));
    assertThat(leitura.validas()).isEmpty();
  }

  // Pega as bordas da faixa e o dia de hoje recusados, e o zero do fim contado
  // como segunda casa: 85.70 é 85,7, e 85 é 85,0 (RN-02)
  @ParameterizedTest
  @CsvSource({
    "2026-10-20;20.0;40.0, 20.0, 40.0",
    "2026-09-01;400.0;200.0, 400.0, 200.0",
    "2026-09-01;85.70;, 85.7, ",
    "2026-09-01;85;90, 85.0, 90.0",
  })
  void asBordasEOsZerosDoFimValem(String linha, String peso, String cintura) {
    assertThat(ler("data;peso;cintura\n" + linha).validas())
        .singleElement()
        .satisfies(
            valida -> {
              assertThat(valida.peso()).isEqualTo(new BigDecimal(peso));
              assertThat(valida.cintura())
                  .isEqualTo(cintura == null ? null : new BigDecimal(cintura));
            });
  }

  // Pega a data repetida aceita uma vez, só a segunda recusada, a repetição
  // escrita em outro formato não reconhecida e a frase que não cita as outras
  // linhas (RN-60)
  @Test
  void aDataRepetidaRecusaTodasAsLinhasDela() {
    var leitura =
        ler(
            """
            data;peso
            2026-09-01;85.7
            2026-09-02;85.6
            01/09/2026;85.5
            2026-09-01;85.4
            2026-09-03;85.0
            2026-09-03;85.1
            """);

    assertThat(leitura.recusadas())
        .containsExactly(
            new LinhaRecusada(2, "A mesma data está também nas linhas 4 e 5."),
            new LinhaRecusada(4, "A mesma data está também nas linhas 2 e 5."),
            new LinhaRecusada(5, "A mesma data está também nas linhas 2 e 4."),
            new LinhaRecusada(6, "A mesma data está também na linha 7."),
            new LinhaRecusada(7, "A mesma data está também na linha 6."));
    assertThat(leitura.validas()).extracting(LinhaValida::linha).containsExactly(3);
  }

  // Pega a linha em branco recusada, ou pulada sem contar na numeração
  @Test
  void aLinhaEmBrancoNaoERecusadaMasContaNaNumeracao() {
    var leitura = ler("data;peso\n\n2026-09-01;19.0\n   \n");

    assertThat(leitura.recusadas()).extracting(LinhaRecusada::linha).containsExactly(3);
  }

  // Pega a substituição marcada pela data errada ou não marcada, e sem o que
  // está gravado, que a substituição apaga sem mostrar (RN-58, UC-10)
  @Test
  void marcaALinhaQueSubstituiUmRegistro() {
    var leitura =
        lerCsv(
            "data;peso\n2026-09-01;85.7\n2026-09-02;85.6",
            Map.of(PRIMEIRO_DE_SETEMBRO, GRAVADO),
            HOJE);

    assertThat(leitura.validas()).extracting(LinhaValida::substitui).containsExactly(GRAVADO, null);
  }

  // Pega a confirmação que substitui uma data que a pessoa viu como nova, e a
  // conferência que só olha um dos lados: a data listada que perdeu o registro
  // também recusa (RN-01, UC-10 3b)
  @ParameterizedTest
  @ValueSource(strings = {"", "2026-09-01;2026-09-02"})
  void aConfirmacaoComOutrasDatasDoQueAsSubstituidasERecusada(String confirmada) {
    var leitura =
        lerCsv(
            "data;peso\n2026-09-01;85.7\n2026-09-02;85.6",
            Map.of(PRIMEIRO_DE_SETEMBRO, GRAVADO),
            HOJE);
    var confirmadas =
        Arrays.stream(confirmada.split(";"))
            .filter(data -> !data.isEmpty())
            .map(LocalDate::parse)
            .collect(Collectors.toSet());

    assertThatThrownBy(() -> leitura.conferirSubstituicoes(confirmadas))
        .isInstanceOfSatisfying(
            RegraVioladaException.class,
            recusa -> assertThat(recusa.recusa()).isEqualTo(Recusa.PREVISUALIZACAO_DESATUALIZADA));
  }

  // Pega a conferência que recusa a confirmação certa
  @Test
  void aConfirmacaoComAsMesmasDatasPassa() {
    var leitura =
        lerCsv(
            "data;peso\n2026-09-01;85.7\n2026-09-02;85.6",
            Map.of(PRIMEIRO_DE_SETEMBRO, GRAVADO),
            HOJE);

    leitura.conferirSubstituicoes(Set.of(PRIMEIRO_DE_SETEMBRO));
  }

  private static LeituraDoCsv ler(String conteudo) {
    return lerCsv(conteudo, Map.of(), HOJE);
  }
}
