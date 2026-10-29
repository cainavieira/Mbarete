package mbarete.regras;

import static mbarete.regras.FaixaDeTolerancia.calcularFaixa;
import static mbarete.regras.Pesagens.ate;
import static mbarete.regras.Ritmo.avaliarRitmo;
import static mbarete.regras.Ritmo.dataDoVeredito;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RitmoTest {

  private static final LocalDate HOJE = LocalDate.of(2026, 10, 20);
  private static final LocalDate MES_ATRAS = HOJE.minusDays(28);
  private static final LocalDate BASE_ANTIGA = HOJE.minusDays(60);
  private static final TaxaAlvo QUATROCENTOS_GRAMAS =
      new TaxaAlvo(new BigDecimal("0.40"), UnidadeDaTaxa.KG_SEMANA);

  // Pega a borda fora da tolerância e a taxa real comparada sem arredondar:
  // contra 80,0 há 28 dias, os pesos de hoje dão taxas de -0,20, -0,19, -0,60,
  // -0,61 e -0,195, que a tela mostra como -0,20 (RN-37, RN-38)
  @ParameterizedTest
  @CsvSource({
    "'79.2 79.2 79.2 79.2', NO_RITMO",
    "'79.2 79.2 79.2 79.2 79.4', ABAIXO",
    "'77.6 77.6 77.6 77.6', NO_RITMO",
    "'77.6 77.6 77.6 77.6 77.4', ACIMA",
    "'79.2 79.2 79.2 79.2 79.3', NO_RITMO"
  })
  void cuttingNoLimiteDaTolerancia(String pesosDeHoje, Ritmo esperado) {
    var pesagens = mesAtrasEHoje("80.0", pesosDeHoje);

    assertThat(avaliarRitmo(TipoDeFase.CUTTING, QUATROCENTOS_GRAMAS, pesagens, BASE_ANTIGA, HOJE))
        .contains(esperado);
  }

  // Pega a distância sem direção: ganhar 0,60 em cutting não é estar no ritmo
  // de perder 0,40 (RN-37)
  @Test
  void cuttingNaDirecaoContrariaEstaAbaixoDoRitmo() {
    var pesagens = mesAtrasEHoje("80.0", "82.4 82.4 82.4 82.4");

    assertThat(avaliarRitmo(TipoDeFase.CUTTING, QUATROCENTOS_GRAMAS, pesagens, BASE_ANTIGA, HOJE))
        .contains(Ritmo.ABAIXO);
  }

  // Pega a direção do cutting aplicada ao bulking: ganhar 0,80 com alvo de
  // 0,40 é acima do ritmo, e ganhar 0,10 é abaixo (RN-37)
  @ParameterizedTest
  @CsvSource({"83.2, ACIMA", "80.4, ABAIXO"})
  void bulkingTemADirecaoDoGanho(String pesoDeHoje, Ritmo esperado) {
    var pesagens =
        mesAtrasEHoje("80.0", String.join(" ", pesoDeHoje, pesoDeHoje, pesoDeHoje, pesoDeHoje));

    assertThat(avaliarRitmo(TipoDeFase.BULKING, QUATROCENTOS_GRAMAS, pesagens, BASE_ANTIGA, HOJE))
        .contains(esperado);
  }

  // Pega a falta de um dos pesos semanais comparados tratada como taxa zero, e
  // não como dados insuficientes (RN-39)
  @Test
  void semTaxaRealNaoHaVeredito() {
    var pesagens = ate(HOJE, "79.2", "79.2", "79.2", "79.2");

    assertThat(avaliarRitmo(TipoDeFase.CUTTING, QUATROCENTOS_GRAMAS, pesagens, BASE_ANTIGA, HOJE))
        .isEmpty();
  }

  // Pega a espera contada com a borda trocada (RN-39)
  @ParameterizedTest
  @CsvSource({"13, false", "14, true"})
  void cuttingTemVereditoCom14DiasDesdeABase(int dias, boolean temVeredito) {
    var base = HOJE.minusDays(dias);
    var pesagens = new ArrayList<Pesagem>(ate(base, "80.0", "80.0", "80.0", "80.0"));
    pesagens.addAll(ate(HOJE, "79.6", "79.6", "79.6", "79.6"));

    var veredito = avaliarRitmo(TipoDeFase.CUTTING, QUATROCENTOS_GRAMAS, pesagens, base, HOJE);

    assertThat(veredito.isPresent()).isEqualTo(temVeredito);
  }

  // Pega a data contada do início da fase, ou com a borda da espera trocada, e
  // a data dada quando os 14 dias já se cumpriram (RF-31, RN-39)
  @ParameterizedTest
  @CsvSource({"10, 2026-10-24", "13, 2026-10-21", "14,"})
  void aDataDoVereditoEODiaDaBaseMais14(int dias, @Nullable LocalDate esperada) {
    var base = HOJE.minusDays(dias);
    var pesagens = new ArrayList<Pesagem>(ate(base, "80.0", "80.0", "80.0", "80.0"));
    pesagens.addAll(ate(HOJE, "79.6", "79.6", "79.6", "79.6"));

    assertThat(dataDoVeredito(TipoDeFase.CUTTING, pesagens, base, null, HOJE))
        .isEqualTo(Optional.ofNullable(esperada));
  }

  // Pega a promessa feita sem um dos pesos semanais comparados, que não há
  // como prever quando vai existir (RF-31, UC-08 4a)
  @ParameterizedTest
  @CsvSource({"'79.6 79.6 79.6 79.6', '80.0 80.0 80.0'", "'79.6 79.6 79.6', '80.0 80.0 80.0 80.0'"})
  void semUmDosPesosSemanaisComparadosNaoHaData(String pesosDeHoje, String pesosDaBase) {
    var base = HOJE.minusDays(10);
    var pesagens = new ArrayList<Pesagem>(ate(base, pesosDaBase.split(" ")));
    pesagens.addAll(ate(HOJE, pesosDeHoje.split(" ")));

    assertThat(dataDoVeredito(TipoDeFase.BULKING, pesagens, base, null, HOJE)).isEmpty();
  }

  // Pega a data prometida depois do último dia da fase, quando o veredito dela
  // não vai aparecer, com a borda do último dia dentro (RN-40, UC-08 4a)
  @ParameterizedTest
  @CsvSource({"2026-10-24, true", "2026-10-23, false"})
  void aDataSoVemSeCouberNaFase(LocalDate fim, boolean temData) {
    var base = HOJE.minusDays(10);
    var pesagens = new ArrayList<Pesagem>(ate(base, "80.0", "80.0", "80.0", "80.0"));
    pesagens.addAll(ate(HOJE, "79.6", "79.6", "79.6", "79.6"));

    assertThat(dataDoVeredito(TipoDeFase.CUTTING, pesagens, base, fim, HOJE).isPresent())
        .isEqualTo(temData);
  }

  // Pega a data dada à manutenção, que não espera os 14 dias (RN-39)
  @Test
  void aManutencaoNaoTemDataDoVeredito() {
    var base = HOJE.minusDays(3);
    var pesagens = ate(HOJE, "80.0", "80.0", "80.0", "80.0", "80.0", "80.0", "80.0");

    assertThat(dataDoVeredito(TipoDeFase.MANUTENCAO, pesagens, base, null, HOJE)).isEmpty();
  }

  // Pega a espera de 14 dias aplicada também à manutenção (RN-39)
  @Test
  void manutencaoTemVereditoJaNoDiaDaBase() {
    var pesagens = ate(HOJE, "80.0", "80.0", "80.0", "80.0");

    assertThat(avaliarRitmo(TipoDeFase.MANUTENCAO, null, pesagens, HOJE, HOJE))
        .contains(Ritmo.NO_RITMO);
  }

  // Pega o peso semanal comparado sem arredondar e o meio levado para o par:
  // com base de 80,0 a faixa vai de 79,2 a 80,8, e os pesos semanais de hoje
  // são 80,84, 80,85, 79,15 e 79,14 (RN-33, RN-37)
  @ParameterizedTest
  @CsvSource({
    "'80.8 80.8 80.8 80.8 81.0', NO_RITMO",
    "'80.8 80.8 80.9 80.9', ACIMA",
    "'79.1 79.1 79.2 79.2', NO_RITMO",
    "'79.1 79.1 79.1 79.1 79.3', ABAIXO"
  })
  void manutencaoComparaAPosicaoComAFaixa(String pesosDeHoje, Ritmo esperado) {
    var base = HOJE.minusDays(20);
    var pesagens = new ArrayList<Pesagem>(ate(base, "80.0", "80.0", "80.0", "80.0"));
    pesagens.addAll(ate(HOJE, pesosDeHoje.split(" ")));

    assertThat(avaliarRitmo(TipoDeFase.MANUTENCAO, null, pesagens, base, HOJE)).contains(esperado);
  }

  // Pega os limites sem arredondar, que a tela escreveria com mais casas que o
  // peso: a base de 80,05 dá 79,2495 e 80,8505 (RN-37)
  @Test
  void faixaVemComOsLimitesComoATelaMostra() {
    var pesagens = ate(HOJE, "80.0", "80.0", "80.1", "80.1");

    assertThat(calcularFaixa(pesagens, HOJE))
        .hasValueSatisfying(
            faixa -> {
              assertThat(faixa.minimo()).isEqualByComparingTo("79.2");
              assertThat(faixa.maximo()).isEqualByComparingTo("80.9");
            });
  }

  /** Quatro pesagens iguais terminando 28 dias atrás, e as de hoje para trás. */
  private static List<Pesagem> mesAtrasEHoje(String pesoMesAtras, String pesosDeHoje) {
    var pesagens =
        new ArrayList<Pesagem>(
            ate(MES_ATRAS, pesoMesAtras, pesoMesAtras, pesoMesAtras, pesoMesAtras));
    pesagens.addAll(ate(HOJE, pesosDeHoje.split(" ")));
    return pesagens;
  }
}
