package mbarete.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

/**
 * O contrato em OpenAPI de onde o frontend gera os tipos (ADR-0027), dizendo o que o springdoc não
 * lê sozinho: quem pode vir nulo. Sem isso, todo campo de resposta sairia opcional, e o tipo gerado
 * não distinguiria o campo ausente do nulo, que são coisas diferentes para quem consome
 * (endpoints.md).
 */
@Configuration
class ContratoOpenApi {

  static {
    // O dia de hoje chega no cabeçalho Data-Local, que o cliente HTTP põe em toda chamada
    SpringDocUtils.getConfig().addAnnotationsToIgnore(Hoje.class);
  }

  private static final String PREFIXO_DOS_ESQUEMAS = "#/components/schemas/";

  /**
   * Na resposta, todo campo vem, e o anotado com {@code @Nullable} pode vir nulo. No corpo enviado,
   * o {@code @Nullable} é o campo que pode faltar, e o obrigatório já sai do Bean Validation.
   */
  // O modelo do swagger-core devolve os esquemas com o tipo cru
  @SuppressWarnings({"rawtypes", "unchecked"})
  @Bean
  OpenApiCustomizer nulosDoContrato() {
    Map<String, Class<?>> registros = registrosDoProjeto();
    return api -> {
      Map<String, Schema> esquemas = api.getComponents().getSchemas();
      if (esquemas == null) {
        return;
      }
      Set<String> enviados = enviados(api);
      esquemas.forEach(
          (nome, esquema) -> {
            Class<?> registro = registros.get(nome);
            Map<String, Schema> propriedades = esquema.getProperties();
            if (registro == null || propriedades == null || enviados.contains(nome)) {
              return;
            }
            List<String> obrigatorios = new ArrayList<>();
            for (RecordComponent componente : registro.getRecordComponents()) {
              Schema<?> propriedade = propriedades.get(componente.getName());
              if (propriedade == null) {
                continue;
              }
              obrigatorios.add(componente.getName());
              if (componente.getAnnotatedType().isAnnotationPresent(Nullable.class)) {
                propriedades.put(componente.getName(), anulavel(propriedade));
              }
            }
            esquema.setRequired(obrigatorios);
          });
    };
  }

  /** O esquema que aceita também o nulo, no jeito do OpenAPI 3.1. */
  private static Schema<?> anulavel(Schema<?> propriedade) {
    if (propriedade.get$ref() != null) {
      Schema<?> nulo = new Schema<>();
      // No 3.1 o tipo é uma lista, e o campo type do modelo não chega ao documento
      nulo.addType("null");
      return new Schema<>().oneOf(List.of(new Schema<>().$ref(propriedade.get$ref()), nulo));
    }
    propriedade.addType("null");
    return propriedade;
  }

  /** Os esquemas que são corpo de requisição, e não de resposta. */
  private static Set<String> enviados(OpenAPI api) {
    return api.getPaths().values().stream()
        .flatMap(caminho -> caminho.readOperations().stream())
        .map(operacao -> operacao.getRequestBody())
        .filter(Objects::nonNull)
        .flatMap(corpo -> corpo.getContent().values().stream())
        .map(conteudo -> conteudo.getSchema() == null ? null : conteudo.getSchema().get$ref())
        .filter(Objects::nonNull)
        .map(ref -> ref.substring(PREFIXO_DOS_ESQUEMAS.length()))
        .collect(Collectors.toSet());
  }

  /** Os records do projeto pelo nome simples, que é o nome do esquema no springdoc. */
  private static Map<String, Class<?>> registrosDoProjeto() {
    var busca = new ClassPathScanningCandidateComponentProvider(false);
    busca.addIncludeFilter(
        (leitor, fabrica) ->
            Record.class.getName().equals(leitor.getClassMetadata().getSuperClassName()));
    Map<String, Class<?>> registros = new HashMap<>();
    for (BeanDefinition definicao : busca.findCandidateComponents("mbarete")) {
      String nome = Objects.requireNonNull(definicao.getBeanClassName());
      Class<?> registro = ClassUtils.resolveClassName(nome, ContratoOpenApi.class.getClassLoader());
      registros.put(registro.getSimpleName(), registro);
    }
    return registros;
  }
}
