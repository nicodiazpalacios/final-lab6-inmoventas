package com.example.inmobiliariaventa.domain.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Index;
import androidx.annotation.NonNull;

@Entity(
    tableName = "contratos",
    indices = {@Index("propiedadId"), @Index("clienteId")}
)
public class Contrato {
    @PrimaryKey
    @NonNull
    public String id;

    @NonNull
    public String propiedadId;

    public String propiedadTitulo;

    @NonNull
    public String clienteId;

    public String clienteNombre;
    public String clienteApellido;
    public String clienteDni;
    public String clienteTelefono;
    public String clienteEmail;

    public Double montoTotal;
    public Double seniaMonto;
    public String metodoPago;
    public Integer cuotas;
    public String fechaInicio;
    public String fechaVencimiento;
    public String fechaVenta;
    public String estado;
    public String descripcion;
    public String rutaLocalPdf;
    public String urlNubePdf;
    public long timestamp;

    @ColumnInfo(name = "estadoSincronizacion")
    public String estadoSincronizacion;

    public Contrato() {
        this.id = "";
        this.propiedadId = "";
        this.clienteId = "";
    }

    @Ignore
    public Contrato(@NonNull String id, String clienteNombre, String clienteApellido, String clienteDni,
                    String clienteTelefono, String clienteEmail, String propiedadTitulo, Double montoTotal,
                    Double seniaMonto, String metodoPago, Integer cuotas, String fechaInicio,
                    String fechaVencimiento, String estado, String descripcion, String rutaLocalPdf, long timestamp) {
        this.id = id;
        this.propiedadId = propiedadTitulo != null ? propiedadTitulo : "";
        this.propiedadTitulo = propiedadTitulo;
        this.clienteId = clienteDni != null && !clienteDni.isEmpty() ? clienteDni : id;
        this.clienteNombre = clienteNombre;
        this.clienteApellido = clienteApellido;
        this.clienteDni = clienteDni;
        this.clienteTelefono = clienteTelefono;
        this.clienteEmail = clienteEmail;
        this.montoTotal = montoTotal;
        this.seniaMonto = seniaMonto;
        this.metodoPago = metodoPago;
        this.cuotas = cuotas;
        this.fechaInicio = fechaInicio;
        this.fechaVencimiento = fechaVencimiento;
        this.fechaVenta = fechaInicio;
        this.estado = estado;
        this.descripcion = descripcion;
        this.rutaLocalPdf = rutaLocalPdf;
        this.timestamp = timestamp;
        this.estadoSincronizacion = "PENDIENTE_SUBIR";
    }

    public String getNombreCompletoCliente() {
        String n = clienteNombre != null ? clienteNombre.trim() : "";
        String a = clienteApellido != null ? clienteApellido.trim() : "";
        if (!n.isEmpty() && !a.isEmpty()) return n + " " + a;
        return !n.isEmpty() ? n : (!a.isEmpty() ? a : "Cliente");
    }
}
