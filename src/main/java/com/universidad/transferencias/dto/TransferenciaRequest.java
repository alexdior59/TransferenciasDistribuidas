package com.universidad.transferencias.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferenciaRequest {

    @NotBlank(message = "La cuenta de origen es obligatoria")
    private String cuentaOrigen;

    @NotBlank(message = "La cuenta de destino es obligatoria")
    private String cuentaDestino;

    @NotNull(message = "El monto es obligatorio")
    @Min(value = 1, message = "El monto a transferir debe ser mayor a 0")
    private BigDecimal monto;
}