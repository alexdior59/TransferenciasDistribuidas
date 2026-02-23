package com.universidad.transferencias.repository.internacional;

import com.universidad.transferencias.model.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoInternacionalRepository extends JpaRepository<Movimiento, Long> {
}