package com.universidad.transferencias.service;

import com.universidad.transferencias.model.Cuenta;
import com.universidad.transferencias.model.Movimiento;
import com.universidad.transferencias.repository.internacional.CuentaInternacionalRepository;
import com.universidad.transferencias.repository.internacional.MovimientoInternacionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BancoInternacionalService {

    private final CuentaInternacionalRepository cuentaRepository;
    private final MovimientoInternacionalRepository movimientoRepository;

    @Transactional(transactionManager = "internacionalTransactionManager")
    public void acreditar(String numeroCuenta, BigDecimal monto, String referencia) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuentaWithLock(numeroCuenta)
                .orElseThrow(() -> new RuntimeException("Cuenta de destino no encontrada: " + numeroCuenta));

        BigDecimal saldoAnterior = cuenta.getSaldo();
        cuenta.setSaldo(saldoAnterior.add(monto));
        cuentaRepository.save(cuenta);

        Movimiento mov = new Movimiento();
        mov.setCuentaId(cuenta.getId());
        mov.setTipo("CREDITO");
        mov.setMonto(monto);
        mov.setSaldoAnterior(saldoAnterior);
        mov.setSaldoNuevo(cuenta.getSaldo());
        mov.setDescripcion("Crédito por transferencia SAGA");
        mov.setReferenciaTransferencia(referencia);
        mov.setFecha(LocalDateTime.now());

        movimientoRepository.save(mov);
    }
}