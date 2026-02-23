package com.universidad.transferencias.controller;

import com.universidad.transferencias.dto.TransferenciaRequest;
import com.universidad.transferencias.service.TransferenciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transferencias")
@RequiredArgsConstructor
public class TransferenciaController {

    private final TransferenciaService transferenciaService;

    // POST /api/transferencias
    @PostMapping
    public ResponseEntity<String> transferir(@Valid @RequestBody TransferenciaRequest request) {

        // Llamamos al orquestador SAGA
        String resultado = transferenciaService.ejecutarTransferenciaSaga(
                request.getCuentaOrigen(),
                request.getCuentaDestino(),
                request.getMonto()
        );

        // Manejo básico de la respuesta HTTP según el resultado del SAGA
        if (resultado.contains("FALLIDA") || resultado.contains("REVERTIDA")) {
            return ResponseEntity.badRequest().body(resultado);
        }

        return ResponseEntity.ok(resultado);
    }
}