package br.com.fiap.pluvia_api.repository;

import br.com.fiap.pluvia_api.model.LeituraSensor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LeituraSensorRepository extends JpaRepository<LeituraSensor, Long> {
    Optional<LeituraSensor> findFirstBySensor_IdOrderByDataHoraDesc(Long sensorId);
}
