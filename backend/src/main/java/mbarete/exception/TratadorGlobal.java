package mbarete.exception;

import java.util.Arrays;
import java.util.StringJoiner;
import mbarete.regras.Recusa;
import mbarete.regras.RegraVioladaException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.core.JacksonException;

/** Converte toda exceção no formato único de erro (convencoes-backend.md). */
@RestControllerAdvice
public class TratadorGlobal {

  private static final Logger LOG = LoggerFactory.getLogger(TratadorGlobal.class);

  private static final String NAO_ENCONTRADO = "Não encontrado.";
  private static final String CORPO_MALFORMADO =
      "A requisição veio num formato que o sistema não entende.";
  private static final String ERRO_INTERNO = "Erro inesperado. Tente de novo em instantes.";
  private static final String NAO_AUTENTICADO = "Entre de novo para continuar.";

  @ExceptionHandler
  ResponseEntity<RespostaDeErro> recusaDeRegra(RegraVioladaException excecao) {
    return resposta(excecao.recusa(), excecao.campo(), excecao.getMessage());
  }

  /**
   * Sem token de acesso válido, ou, na renovação, sem token de renovação válido (endpoints.md). O
   * frontend renova uma vez e, se voltar isto de novo, leva à tela de entrada.
   */
  @ExceptionHandler
  ResponseEntity<RespostaDeErro> naoAutenticado(AuthenticationException excecao) {
    return resposta(Recusa.NAO_AUTENTICADO, null, NAO_AUTENTICADO);
  }

  /** O corpo não vira o DTO: JSON quebrado, ou valor que não tem o tipo do campo. */
  @ExceptionHandler
  ResponseEntity<RespostaDeErro> corpoMalformado(HttpMessageNotReadableException excecao) {
    String campo = null;
    if (excecao.getCause() instanceof JacksonException jackson && !jackson.getPath().isEmpty()) {
      campo = jackson.getPath().getLast().getPropertyName();
    }
    return resposta(Recusa.CORPO_MALFORMADO, campo, CORPO_MALFORMADO);
  }

  /**
   * Campo do corpo que não passa no Bean Validation: a anotação traz o código do catálogo, e a
   * frase que a pessoa lê vem do catálogo. Anotação sem código sai como corpo malformado, com o
   * campo.
   */
  @ExceptionHandler
  ResponseEntity<RespostaDeErro> campoRecusado(MethodArgumentNotValidException excecao) {
    FieldError erro = excecao.getBindingResult().getFieldError();
    String campo = erro == null ? null : erro.getField();
    String codigo = erro == null ? null : erro.getDefaultMessage();
    return Arrays.stream(Recusa.values())
        .filter(recusa -> recusa.name().equals(codigo))
        .findFirst()
        .map(recusa -> resposta(recusa, campo, CatalogoDeRecusas.mensagemDoCampo(recusa)))
        .orElseGet(() -> resposta(Recusa.CORPO_MALFORMADO, campo, CORPO_MALFORMADO));
  }

  /**
   * Parâmetro com tipo errado. No caminho, o recurso não pode existir, como a data que não é data
   * em `/registros/{data}`; na consulta, é pedido malformado.
   */
  @ExceptionHandler
  ResponseEntity<RespostaDeErro> tipoErrado(MethodArgumentTypeMismatchException excecao) {
    if (excecao.getParameter().hasParameterAnnotation(PathVariable.class)) {
      return resposta(Recusa.NAO_ENCONTRADO, null, NAO_ENCONTRADO);
    }
    return resposta(Recusa.CORPO_MALFORMADO, excecao.getName(), CORPO_MALFORMADO);
  }

  /** O que não tem tratador próprio: erro que o Spring já classifica, ou o inesperado. */
  @ExceptionHandler
  ResponseEntity<RespostaDeErro> outroErro(Exception excecao) {
    if (excecao instanceof ErrorResponse doSpring) {
      int status = doSpring.getStatusCode().value();
      if (status == 404 || status == 405) {
        return resposta(Recusa.NAO_ENCONTRADO, null, NAO_ENCONTRADO);
      }
      if (doSpring.getStatusCode().is4xxClientError()) {
        return resposta(Recusa.CORPO_MALFORMADO, null, CORPO_MALFORMADO);
      }
    }
    // Só a classe e o lugar de cada causa: a mensagem pode trazer peso ou cintura (RNF-13)
    LOG.error("Erro inesperado: {}", rastro(excecao));
    return resposta(Recusa.ERRO_INTERNO, null, ERRO_INTERNO);
  }

  static String rastro(Throwable erro) {
    StringJoiner causas = new StringJoiner(" <- ");
    for (Throwable causa = erro; causa != null; causa = causa.getCause()) {
      StackTraceElement[] pilha = causa.getStackTrace();
      causas.add(causa.getClass().getName() + (pilha.length > 0 ? " em " + pilha[0] : ""));
    }
    return causas.toString();
  }

  private static ResponseEntity<RespostaDeErro> resposta(
      Recusa recusa, @Nullable String campo, String mensagem) {
    CatalogoDeRecusas.Descricao descricao = CatalogoDeRecusas.descricao(recusa);
    RespostaDeErro.Erro erro =
        new RespostaDeErro.Erro(recusa.name(), campo, mensagem, descricao.regra());
    return ResponseEntity.status(descricao.status()).body(new RespostaDeErro(erro));
  }
}
