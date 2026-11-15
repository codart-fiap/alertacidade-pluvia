package br.com.fiap.pluvia_api.repository;

import br.com.fiap.pluvia_api.model.Alerta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    List<Alerta> findTop50ByOrderByLeituraDataHoraDesc();
}
