package mbarete.regras;

import static mbarete.regras.EscritaDoCsv.escreverCsv;
import static mbarete.regras.LeituraDoCsv.lerCsv;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import mbarete.regras.EscritaDoCsv.LinhaDoHistorico;
import mbarete.regras.LeituraDoCsv.LinhaValida;
import org.junit.jupiter.api.Test;

class EscritaDoCsvTest {

  private static final List<LinhaDoHistorico> FORA_DE_ORDEM =
      List.of(
          new LinhaDoHistorico(
              LocalDate.of(2026, 9, 2), new BigDecimal("85.6"), new BigDecimal("90.0")),
          new LinhaDoHistorico(LocalDate.of(2026, 9, 1), new BigDecimal("85.0"), null));

  // Pega a ordem em que os registros chegaram e a cintura ausente escrita como
  // "null" (RN-55)
  @Test
  void escrevePelaDataComACinturaVazia() {
    assertThat(escreverCsv(FORA_DE_ORDEM))
        .isEqualTo("data;peso;cintura\n2026-09-01;85.0;\n2026-09-02;85.6;90.0\n");
  }

  // Pega o histórico vazio sem cabeçalho, que a importação recusaria (UC-11 2a)
  @Test
  void semRegistroSaiSoOCabecalho() {
    assertThat(escreverCsv(List.of())).isEqualTo("data;peso;cintura\n");
  }

  // Pega a exportação num formato que a própria importação não aceita
  // (UC-11)
  @Test
  void aImportacaoLeOQueAExportacaoEscreve() {
    var lidas = lerCsv(escreverCsv(FORA_DE_ORDEM), Map.of(), LocalDate.of(2026, 10, 20));

    assertThat(lidas.recusadas()).isEmpty();
    assertThat(lidas.validas())
        .extracting(LinhaValida::data, LinhaValida::peso, LinhaValida::cintura)
        .containsExactly(
            tuple(LocalDate.of(2026, 9, 1), new BigDecimal("85.0"), null),
            tuple(LocalDate.of(2026, 9, 2), new BigDecimal("85.6"), new BigDecimal("90.0")));
  }
}
