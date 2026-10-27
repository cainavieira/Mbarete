package mbarete.repository;

import java.util.Optional;
import mbarete.entity.TokenRenovacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenRenovacaoRepository extends JpaRepository<TokenRenovacao, Integer> {

  Optional<TokenRenovacao> findByHash(String hash);
}
