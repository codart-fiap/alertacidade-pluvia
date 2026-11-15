package br.com.fiap.pluvia_api.repository;

import br.com.fiap.pluvia_api.model.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorRepository extends JpaRepository<Sensor, Long> {
}
