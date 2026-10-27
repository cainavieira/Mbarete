package mbarete.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import mbarete.entity.Usuario;
import mbarete.repository.ConvidadoRepository;
import mbarete.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContaService {

  private final ConvidadoRepository convidados;
  private final UsuarioRepository usuarios;
  private final Clock relogio;

  public ContaService(ConvidadoRepository convidados, UsuarioRepository usuarios, Clock relogio) {
    this.convidados = convidados;
    this.usuarios = usuarios;
    this.relogio = relogio;
  }

  /**
   * A conta de quem voltou do Google, pelo e-mail já normalizado (UC-01). Vazio para quem não está
   * na lista de convidados (RN-48). A conta nova nasce sem fase (RN-20, RN-66); a existente recebe
   * o nome e a foto de agora.
   */
  @Transactional
  public Optional<Usuario> entrar(String email, String nome, String fotoUrl) {
    if (!convidados.existsById(email)) {
      return Optional.empty();
    }
    Usuario usuario =
        usuarios
            .findByEmail(email)
            .orElseGet(
                () -> usuarios.save(new Usuario(email, nome, fotoUrl, Instant.now(relogio))));
    usuario.atualizarPerfil(nome, fotoUrl);
    return Optional.of(usuario);
  }
}
