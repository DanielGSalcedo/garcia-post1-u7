package com.example.multas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "multas")
public class Multa {
    private static final BigDecimal VALOR_POR_DIA = new BigDecimal("500");
    private static final BigDecimal TOPE_MAXIMO = new BigDecimal("15000");

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Column(nullable = false)
    private String estudianteId;
    @NotBlank @Column(nullable = false)
    private String concepto;
    @Min(1)
    private int diasAtraso;
    @Column(nullable = false)
    private BigDecimal monto;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private EstadoMulta estado = EstadoMulta.PENDIENTE;
    @Column(nullable = false)
    private LocalDate fechaGeneracion = LocalDate.now();
    private LocalDate fechaPago;
    private String metodoPago;

    protected Multa() {}

    public static BigDecimal calcularMonto(int diasAtraso) {
        if (diasAtraso < 1) throw new IllegalArgumentException("Los días de atraso deben ser al menos 1");
        return VALOR_POR_DIA.multiply(BigDecimal.valueOf(diasAtraso)).min(TOPE_MAXIMO);
    }

    public void marcarComoPagada(String metodoPago) {
        if (estado == EstadoMulta.PAGADA) {
            throw new MultaYaPagadaException("La multa " + id + " ya fue pagada el " + fechaPago);
        }
        estado = EstadoMulta.PAGADA;
        fechaPago = LocalDate.now();
        this.metodoPago = metodoPago;
    }

    public Long getId() { return id; }
    public String getEstudianteId() { return estudianteId; }
    public void setEstudianteId(String estudianteId) { this.estudianteId = estudianteId; }
    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }
    public int getDiasAtraso() { return diasAtraso; }
    public void setDiasAtraso(int diasAtraso) { this.diasAtraso = diasAtraso; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public EstadoMulta getEstado() { return estado; }
    public LocalDate getFechaGeneracion() { return fechaGeneracion; }
    public LocalDate getFechaPago() { return fechaPago; }
    public String getMetodoPago() { return metodoPago; }
}
