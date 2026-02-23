package com.universidad.transferencias.service;

import com.universidad.transferencias.model.EstadoTransferencia;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferenciaService {

    private final BancoNacionalService nacionalService;
    private final BancoInternacionalService internacionalService;

    public String ejecutarTransferenciaSaga(String cuentaOrigen, String cuentaDestino, BigDecimal monto) {
        // Generación de referencia única pedida en la rúbrica
        String referencia = UUID.randomUUID().toString();
        log.info("--- INICIANDO TRANSACCIÓN SAGA ---");
        log.info("Paso 0: Crear registro INICIADA. Ref: {}", referencia);

        try {
            // PASO 1: Transacción Local 1 (Débito)
            log.info("Paso 1: Intentando debitar Banco Nacional...");
            nacionalService.debitar(cuentaOrigen, monto, referencia);
            log.info("Paso 2: Estado -> {}", EstadoTransferencia.DEBITO_COMPLETADO);

            try {
                // PASO 3: Transacción Local 2 (Crédito)
                log.info("Paso 3: Intentando acreditar Banco Internacional...");
                // Aquí podrías simular un fallo lanzando una excepción para probar el Caso 3
                internacionalService.acreditar(cuentaDestino, monto, referencia);
                log.info("Paso 4: Estado -> {}", EstadoTransferencia.CREDITO_COMPLETADO);

            } catch (Exception e) {
                // FALLO EN EL DESTINO -> EJECUTAR COMPENSACIÓN SAGA
                log.error("Error al acreditar en destino: {}. INICIANDO COMPENSACIÓN...", e.getMessage());
                nacionalService.revertirDebito(cuentaOrigen, monto, referencia);
                log.warn("Estado -> {}", EstadoTransferencia.REVERTIDA);
                return "TRANSFERENCIA REVERTIDA - Fallo en el banco de destino";
            }

            // ÉXITO TOTAL
            log.info("Paso 5: Estado -> {}", EstadoTransferencia.COMPLETADA);
            return "TRANSFERENCIA COMPLETADA EXITOSAMENTE";

        } catch (Exception e) {
            // FALLO EN EL ORIGEN -> SE ABORTA ANTES DE TOCAR EL DESTINO
            log.error("Error al debitar del origen. Estado -> {}", EstadoTransferencia.FALLIDA);
            return "TRANSFERENCIA FALLIDA - " + e.getMessage();
        }
    }
}