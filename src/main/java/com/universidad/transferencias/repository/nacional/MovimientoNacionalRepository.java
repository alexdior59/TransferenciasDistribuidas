package com.universidad.transferencias.repository.nacional;

import com.universidad.transferencias.model.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoNacionalRepository extends JpaRepository<Movimiento, Long> {
}