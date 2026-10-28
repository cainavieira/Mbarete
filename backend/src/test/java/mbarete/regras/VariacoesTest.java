package mbarete.regras;

import static mbarete.regras.Variacoes.calcularVariacoes;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class VariacoesTest {

  private static final LocalDate HOJE = LocalDate.of(2026, 10, 20);

  // Pega as partidas de 6 ou 8 dias, de 29 ou 31, e o primeiro registro
  // tomado como o mais recente: a lista chega do mais novo para o mais antigo,
  // com um registro por dia desde 40 dias atrás, 0,1 kg mais leve a cada dia.
  // O primeiro peso semanal é o de 37 dias atrás, quando a janela chega a 4
  // registros (RN-46, RN-47)
  @Test
  void partemDeSeteDiasDeTrintaDiasEDoPrimeiroRegistro() {
    var pesagens =
        IntStream.rangeClosed(0, 40)
            .mapToObj(
                dias ->
                    new Pesagem(
                        HOJE.minusDays(dias),
                        new BigDecimal("80.0").add(BigDecimal.valueOf(dias, 1))))
            .toList();

    var variacoes = calcularVariacoes(pesagens, HOJE);

    assertThat(variacoes.semana())
        .isNotNull()
        .satisfies(
            semana -> {
              assertThat(semana.de()).isEqualTo(HOJE.minusDays(7));
              assertThat(semana.valor()).isEqualByComparingTo("-0.7");
            });
    assertThat(variacoes.mes()).isNotNull().extracting(Variacao::de).isEqualTo(HOJE.minusDays(30));
    assertThat(variacoes.desdeOInicio())
        .isNotNull()
        .extracting(Variacao::de)
        .isEqualTo(HOJE.minusDays(37));
  }
}
