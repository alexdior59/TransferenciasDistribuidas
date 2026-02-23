package com.universidad.transferencias.service;

import com.universidad.transferencias.model.Cuenta;
import com.universidad.transferencias.model.Movimiento;
import com.universidad.transferencias.repository.nacional.CuentaNacionalRepository;
import com.universidad.transferencias.repository.nacional.MovimientoNacionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BancoNacionalService {

    private final CuentaNacionalRepository cuentaRepository;
    private final MovimientoNacionalRepository movimientoRepository;

    @Transactional(transactionManager = "nacionalTransactionManager")
    public void debitar(String numeroCuenta, BigDecimal monto, String referencia) {
        // Obtenemos la cuenta con un lock pesimista
        Cuenta cuenta = cuentaRepository.findByNumeroCuentaWithLock(numeroCuenta)
                .orElseThrow(() -> new RuntimeException("Cuenta de origen no encontrada: " + numeroCuenta));

        if (cuenta.getSaldo().compareTo(monto) < 0) {
            throw new RuntimeException("Saldo insuficiente en la cuenta origen");
        }

        BigDecimal saldoAnterior = cuenta.getSaldo();
        cuenta.setSaldo(saldoAnterior.subtract(monto));
        cuentaRepository.save(cuenta);

        registrarMovimiento(cuenta.getId(), "DEBITO", monto, saldoAnterior, cuenta.getSaldo(),
                "Débito por transferencia SAGA", referencia);
    }

    @Transactional(transactionManager = "nacionalTransactionManager")
    public void revertirDebito(String numeroCuenta, BigDecimal monto, String referencia) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuentaWithLock(numeroCuenta)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada para compensación"));

        BigDecimal saldoAnterior = cuenta.getSaldo();
        cuenta.setSaldo(saldoAnterior.add(monto));
        cuentaRepository.save(cuenta);

        registrarMovimiento(cuenta.getId(), "CREDITO", monto, saldoAnterior, cuenta.getSaldo(),
                "COMPENSACIÓN: Reversión de débito fallido", referencia);
    }

    private void registrarMovimiento(Long cuentaId, String tipo, BigDecimal monto, BigDecimal saldoAnterior,
                                     BigDecimal saldoNuevo, String descripcion, String referencia) {
        Movimiento mov = new Movimiento();
        mov.setCuentaId(cuentaId);
        mov.setTipo(tipo);
        mov.setMonto(monto);
        mov.setSaldoAnterior(saldoAnterior);
        mov.setSaldoNuevo(saldoNuevo);
        mov.setDescripcion(descripcion);
        mov.setReferenciaTransferencia(referencia);
        mov.setFecha(LocalDateTime.now());
        movimientoRepository.save(mov);
    }
}