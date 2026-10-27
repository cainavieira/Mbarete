package mbarete;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import mbarete.regras.Email;
import mbarete.service.ConviteService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ApiApplication {

  private static final String[] MODO_MIGRAR = {
    "--spring.flyway.enabled=true", "--spring.main.web-application-type=none"
  };

  private static final String[] MODO_CONVIDAR = {
    "--spring.main.web-application-type=none", "--spring.main.banner-mode=off"
  };

  public static void main(String[] args) {
    if (args.length > 0 && args[0].equals("migrar")) {
      // Aplica as migrações e termina, sem subir a api (ADR-0040)
      SpringApplication.run(ApiApplication.class, doModo(MODO_MIGRAR, args, 1)).close();
      return;
    }
    if (args.length > 0 && args[0].equals("convidar")) {
      // Registra o convite e termina, sem subir a api (RF-44, ADR-0011)
      int saida = convidar(args);
      if (saida != 0) {
        System.exit(saida);
      }
      return;
    }
    SpringApplication.run(ApiApplication.class, args);
  }

  /**
   * O código de saída do modo: 0 com o convite registrado ou já existente (UC-13 2a), 1 sem um
   * e-mail válido, caso em que nem se conecta ao banco.
   */
  static int convidar(String[] args) {
    Optional<String> email = args.length > 1 ? Email.normalizar(args[1]) : Optional.empty();
    if (email.isEmpty()) {
      // Sem acento no que sai no terminal, como na mensagem de commit
      System.err.println("Informe um e-mail valido: convidar <e-mail>");
      return 1;
    }
    try (ConfigurableApplicationContext contexto =
        SpringApplication.run(ApiApplication.class, doModo(MODO_CONVIDAR, args, 2))) {
      boolean novo = contexto.getBean(ConviteService.class).convidar(email.get());
      System.out.println(
          novo
              ? "Convite registrado para " + email.get() + ". Avise a pessoa por fora do sistema."
              : email.get() + " ja estava na lista de convidados; nada mudou.");
    }
    return 0;
  }

  /** Os argumentos do modo, seguidos dos que vieram depois dos dele, para o Spring. */
  private static String[] doModo(String[] modo, String[] args, int consumidos) {
    return Stream.concat(Arrays.stream(modo), Arrays.stream(args).skip(consumidos))
        .toArray(String[]::new);
  }
}
