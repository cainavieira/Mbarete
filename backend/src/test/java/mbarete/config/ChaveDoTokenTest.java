package mbarete.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ChaveDoTokenTest {

  // Pega a chave mal colada no .env ou no Secret derrubando a subida com um
  // erro que não diz qual variável olhar
  @Test
  void chaveQueNaoERsaDerrubaComONomeDaVariavel() {
    assertThatThrownBy(() -> ChaveDoToken.publica("isso não é uma chave"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("MBARETE_JWT_CHAVE");
  }
}
