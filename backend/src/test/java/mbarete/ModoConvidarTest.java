package mbarete;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** O modo `convidar` do `.jar`, rodado como o operador o roda, contra um banco migrado. */
@ExtendWith(OutputCaptureExtension.class)
class ModoConvidarTest {

  private static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(DockerImageName.parse("postgres:18"));

  @BeforeAll
  static void migra() {
    POSTGRES.start();
    ApiApplication.main(comOBanco("migrar"));
  }

  @AfterAll
  static void para() {
    POSTGRES.stop();
  }

  // Pega o modo que não chega ao banco, e o e-mail gravado como foi digitado
  @Test
  void registraOConviteComOEmailNormalizado(CapturedOutput saida) throws SQLException {
    ApiApplication.main(comOBanco("convidar", "  Beltrano@Exemplo.com "));

    assertThat(dataDoConvite("beltrano@exemplo.com")).isPresent();
    assertThat(saida.getOut()).contains("Convite registrado para beltrano@exemplo.com.");
  }

  // Pega o segundo convite que regrava a linha com a data nova, em vez de
  // informar e manter a do primeiro (UC-13 2a)
  @Test
  void convidarDeNovoInformaENaoMudaNada(CapturedOutput saida) throws SQLException {
    ApiApplication.main(comOBanco("convidar", "ciclano@exemplo.com"));
    Instant primeiro = dataDoConvite("ciclano@exemplo.com").orElseThrow();

    ApiApplication.main(comOBanco("convidar", "Ciclano@Exemplo.com"));

    assertThat(dataDoConvite("ciclano@exemplo.com")).contains(primeiro);
    assertThat(saida.getOut()).contains("ciclano@exemplo.com ja estava na lista de convidados");
  }

  // Pega o texto que não é e-mail chegando ao banco, e o e-mail ausente
  // quebrando o comando em vez de dizer o uso
  @Test
  void semEmailValidoSaiComErroSemGravar(CapturedOutput saida) throws SQLException {
    assertThat(ApiApplication.convidar(comOBanco("convidar", "fulano"))).isEqualTo(1);
    assertThat(ApiApplication.convidar(new String[] {"convidar"})).isEqualTo(1);

    assertThat(dataDoConvite("fulano")).isEmpty();
    assertThat(saida.getErr()).contains("Informe um e-mail valido: convidar <e-mail>");
  }

  private static String[] comOBanco(String... args) {
    return Stream.concat(
            Arrays.stream(args),
            Stream.of(
                "--MBARETE_DB_URL=" + POSTGRES.getJdbcUrl(),
                "--MBARETE_DB_USUARIO=" + POSTGRES.getUsername(),
                "--MBARETE_DB_SENHA=" + POSTGRES.getPassword()))
        .toArray(String[]::new);
  }

  private static Optional<Instant> dataDoConvite(String email) throws SQLException {
    try (Connection conexao =
            DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        PreparedStatement consulta =
            conexao.prepareStatement("select convidado_em from convidado where email = ?")) {
      consulta.setString(1, email);
      try (ResultSet linha = consulta.executeQuery()) {
        return linha.next()
            ? Optional.of(linha.getObject(1, OffsetDateTime.class).toInstant())
            : Optional.empty();
      }
    }
  }
}
