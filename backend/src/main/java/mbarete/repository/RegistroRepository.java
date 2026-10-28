package mbarete.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import mbarete.entity.Registro;
import org.springframework.data.jpa.repository.JpaRepository;

/** Toda consulta recebe o usuário (ADR-0034). */
public interface RegistroRepository extends JpaRepository<Registro, Integer> {

  Optional<Registro> findByUsuarioIdAndData(int usuarioId, LocalDate data);

  List<Registro> findByUsuarioId(int usuarioId);
}
