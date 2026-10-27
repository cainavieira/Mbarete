package mbarete.regras;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailTest {

  // Pega o e-mail guardado com a caixa ou o espaço que a pessoa digitou, que o
  // login, comparando com o do Google, não acharia (RN-48)
  @Test
  void ficaSemEspacoEmVoltaEEmMinusculas() {
    assertThat(Email.normalizar("  Fulano.Silva@Gmail.COM ")).contains("fulano.silva@gmail.com");
  }

  // Pega o convite gravado para um texto que nenhum login vai trazer
  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "   ",
        "fulano",
        "fulano@",
        "@gmail.com",
        "fulano@@gmail.com",
        "a@b@gmail.com",
        "ful ano@gmail.com"
      })
  void textoSemFormaDeEmailERecusado(String texto) {
    assertThat(Email.normalizar(texto)).isEmpty();
  }
}
