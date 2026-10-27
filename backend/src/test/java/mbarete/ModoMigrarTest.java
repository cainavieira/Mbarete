package mbarete;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

class ModoMigrarTest {

  @Test
  void aplicaTodasAsMigracoes() {
    try (var postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:18"))) {
      postgres.start();

      ApiApplication.main(
          new String[] {
            "migrar",
            "--MBARETE_DB_URL=" + postgres.getJdbcUrl(),
            "--MBARETE_DB_USUARIO=" + postgres.getUsername(),
            "--MBARETE_DB_SENHA=" + postgres.getPassword()
          });

      var pendentes =
          Flyway.configure()
              .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
              .load()
              .info()
              .pending();
      assertThat(pendentes).isEmpty();
    }
  }
}
