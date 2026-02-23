package com.universidad.transferencias.repository.nacional;

import com.universidad.transferencias.model.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface CuentaNacionalRepository extends JpaRepository<Cuenta, Long> {

    // El lock pesimista bloquea el registro en PostgreSQL
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cuenta c WHERE c.numeroCuenta = :numeroCuenta")
    Optional<Cuenta> findByNumeroCuentaWithLock(String numeroCuenta);
}