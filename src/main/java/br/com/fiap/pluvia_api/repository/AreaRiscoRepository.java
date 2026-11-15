package br.com.fiap.pluvia_api.repository;

import br.com.fiap.pluvia_api.model.AreaRisco;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AreaRiscoRepository extends JpaRepository<AreaRisco, Long> {

    List<AreaRisco> findByAtivaTrue();
}
