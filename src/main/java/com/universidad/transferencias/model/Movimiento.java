package com.universidad.transferencias.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "movimiento")
public class Movimiento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuenta_id", nullable = false)
    private Long cuentaId;

    private String tipo;

    private BigDecimal monto;

    @Column(name = "saldo_anterior")
    private BigDecimal saldoAnterior;

    @Column(name = "saldo_nuevo")
    private BigDecimal saldoNuevo;

    private String descripcion;

    @Column(name = "referencia_transferencia")
    private String referenciaTransferencia;

    @Column(insertable = false, updatable = false)
    private LocalDateTime fecha;
}