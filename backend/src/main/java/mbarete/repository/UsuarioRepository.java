package mbarete.repository;

import java.util.Optional;
import mbarete.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

  Optional<Usuario> findByEmail(String email);
}
