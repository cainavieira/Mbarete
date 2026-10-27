package mbarete.regras;

import static mbarete.regras.Pesagens.ate;
import static mbarete.regras.Progresso.calcularProgresso;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ProgressoTest {

  private static final LocalDate HOJE = LocalDate.of(2026, 10, 20);
  private static final LocalDate BASE = HOJE.minusDays(20);

  // Pega a barra sem trava e o alvo exato contado como não atingido: com base
  // de 80,0 e alvo de 76,0, o peso semanal de hoje anda 1, 4, 5 ou -1 (RN-44)
  @ParameterizedTest
  @CsvSource({"79.0, 25, false", "76.0, 100, true", "75.0, 100, true", "81.0, 0, false"})
  void cuttingTravaEntreZeroECem(String pesoDeHoje, String percentual, boolean atingido) {
    var pesagens = baseEHoje("80.0", pesoDeHoje, pesoDeHoje, pesoDeHoje, pesoDeHoje);

    assertThat(calcularProgresso(TipoDeFase.CUTTING, new BigDecimal("76.0"), pesagens, BASE, HOJE))
        .isInstanceOfSatisfying(
            Progresso.Barra.class,
            barra -> {
              assertThat(barra.percentual()).isEqualByComparingTo(percentual);
              assertThat(barra.atingido()).isEqualTo(atingido);
            });
  }

  // Pega a direção do cutting aplicada ao bulking (RN-42)
  @Test
  void bulkingAndaParaCima() {
    var pesagens = baseEHoje("70.0", "71.0", "71.0", "71.0", "71.0");

    assertThat(calcularProgresso(TipoDeFase.BULKING, new BigDecimal("74.0"), pesagens, BASE, HOJE))
        .isInstanceOfSatisfying(
            Progresso.Barra.class,
            barra -> assertThat(barra.percentual()).isEqualByComparingTo("25"));
  }

  // Pega o alvo acima da base em cutting e o alvo igual a ela mostrados como
  // barra cheia com aviso (RN-44)
  @ParameterizedTest
  @ValueSource(strings = {"82.0", "80.0"})
  void alvoDoLadoErradoOuIgualABaseNaoTemBarra(String alvo) {
    var pesagens = baseEHoje("80.0", "79.0", "79.0", "79.0", "79.0");

    assertThat(calcularProgresso(TipoDeFase.CUTTING, new BigDecimal(alvo), pesagens, BASE, HOJE))
        .isInstanceOf(Progresso.NaoExibido.class);
  }

  // Pega o peso semanal arredondado como a tela mostra: 79,04 aparece 79,0, igual
  // ao alvo, mas ainda não o atingiu (RN-44)
  @Test
  void comparaOPesoSemanalExato() {
    var pesagens = baseEHoje("80.0", "79.0", "79.0", "79.0", "79.0", "79.2");

    assertThat(calcularProgresso(TipoDeFase.CUTTING, new BigDecimal("79.0"), pesagens, BASE, HOJE))
        .isInstanceOfSatisfying(
            Progresso.Barra.class,
            barra -> {
              assertThat(barra.percentual()).isEqualByComparingTo("96");
              assertThat(barra.atingido()).isFalse();
            });
  }

  // Pega a base que repete o último valor conhecido quando a janela dela cai
  // para três registros (RN-68)
  @Test
  void baseSemQuatroRegistrosNaJanelaEDadosInsuficientes() {
    var pesagens = new ArrayList<Pesagem>(ate(BASE, "80.0", "80.0", "80.0"));
    pesagens.addAll(ate(HOJE, "79.0", "79.0", "79.0", "79.0"));

    assertThat(calcularProgresso(TipoDeFase.CUTTING, new BigDecimal("76.0"), pesagens, BASE, HOJE))
        .isInstanceOf(Progresso.DadosInsuficientes.class);
  }

  @Test
  void semPesoSemanalHojeEDadosInsuficientes() {
    var pesagens = baseEHoje("80.0", "79.0", "79.0", "79.0");

    assertThat(calcularProgresso(TipoDeFase.CUTTING, new BigDecimal("76.0"), pesagens, BASE, HOJE))
        .isInstanceOf(Progresso.DadosInsuficientes.class);
  }

  /** Quatro pesagens iguais terminando no dia da base, e as de hoje para trás. */
  private static List<Pesagem> baseEHoje(String pesoDaBase, String... pesosDeHoje) {
    var pesagens =
        new ArrayList<Pesagem>(ate(BASE, pesoDaBase, pesoDaBase, pesoDaBase, pesoDaBase));
    pesagens.addAll(ate(HOJE, pesosDeHoje));
    return pesagens;
  }
}
