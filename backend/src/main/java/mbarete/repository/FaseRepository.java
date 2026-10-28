package mbarete.repository;

import java.util.List;
import java.util.Optional;
import mbarete.entity.Fase;
import org.springframework.data.jpa.repository.JpaRepository;

/** Toda consulta recebe o usuário (ADR-0034). */
public interface FaseRepository extends JpaRepository<Fase, Integer> {

  List<Fase> findByUsuarioIdAndBaseDataIsNull(int usuarioId);

  List<Fase> findByUsuarioId(int usuarioId);

  Optional<Fase> findByIdAndUsuarioId(int id, int usuarioId);
}
