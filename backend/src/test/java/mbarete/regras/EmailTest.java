package mbarete.regras;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

  // Pega a exclusão barrada pela caixa ou pelo espaço que a pessoa digitou,
  // que o login não diferencia (RN-48, UC-12)
  @ParameterizedTest
  @ValueSource(strings = {"fulana@exemplo.com", "  Fulana@Exemplo.COM "})
  void oProprioEmailConfere(String digitado) {
    assertThatNoException()
        .isThrownBy(() -> Email.recusarEmailQueNaoConfere(digitado, "fulana@exemplo.com"));
  }

  // Pega a conta apagada com outro e-mail, e com texto sem forma de e-mail
  // contra uma conta que também não tivesse: dois vazios não conferem
  // (UC-12 2a)
  @ParameterizedTest
  @CsvSource({"outra@exemplo.com, fulana@exemplo.com", "'', fulana@exemplo.com", "'', ''"})
  void outroTextoNaoConfere(String digitado, String daConta) {
    assertThatThrownBy(() -> Email.recusarEmailQueNaoConfere(digitado, daConta))
        .isInstanceOfSatisfying(
            RegraVioladaException.class,
            excecao -> {
              assertThat(excecao.recusa()).isEqualTo(Recusa.EMAIL_NAO_CONFERE);
              assertThat(excecao.campo()).isEqualTo("email");
            });
  }
}
