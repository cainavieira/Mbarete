package mbarete.regras;

import static mbarete.regras.DataDoDispositivo.lerDataLocal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class DataDoDispositivoTest {

  private static final LocalDate HOJE_EM_UTC = LocalDate.of(2026, 10, 20);

  // Pega a borda de um dia fora: de 12 horas antes a 14 depois do UTC, o
  // dispositivo pode estar na véspera ou no dia seguinte
  @ParameterizedTest
  @ValueSource(strings = {"2026-10-19", "2026-10-20", "2026-10-21"})
  void dataAteUmDiaDoServidorEAceita(String cabecalho) {
    assertThat(lerDataLocal(cabecalho, HOJE_EM_UTC)).isEqualTo(LocalDate.parse(cabecalho));
  }

  // Pega a recusa que só olha um lado, e o cabeçalho ausente que cai para a
  // data do servidor
  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"2026-10-18", "2026-10-22", "2026-1-20", "20/10/2026", "2026-02-30", ""})
  void ausenteMalformadaOuLongeDoServidorERecusada(String cabecalho) {
    assertThatThrownBy(() -> lerDataLocal(cabecalho, HOJE_EM_UTC))
        .isInstanceOfSatisfying(
            RegraVioladaException.class,
            recusa -> assertThat(recusa.recusa()).isEqualTo(Recusa.DATA_LOCAL_INVALIDA));
  }

  // Pega a distância contada pelo dia do ano ou do mês, que na virada dá 364
  @Test
  void naViradaDoAnoContaOsDiasDoCalendario() {
    assertThat(lerDataLocal("2026-12-31", LocalDate.of(2027, 1, 1)))
        .isEqualTo(LocalDate.of(2026, 12, 31));
  }
}
