package com.example.inmobiliariaventa.domain.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Index;
import androidx.annotation.NonNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

@Entity(
    tableName = "contratos",
    indices = {@Index("propiedadId"), @Index("clienteId")}
)
public class Contrato {
    @PrimaryKey
    @NonNull
    @SerializedName("id")
    public String id;

    @NonNull
    @SerializedName(value = "propiedadid", alternate = {"propiedadId", "propiedad_id"})
    public String propiedadId;

    @SerializedName(value = "propiedadtitulo", alternate = {"propiedadTitulo", "propiedad_titulo"})
    public String propiedadTitulo;

    @NonNull
    @SerializedName(value = "clienteid", alternate = {"clienteId", "cliente_id"})
    public String clienteId;

    @SerializedName(value = "clientenombre", alternate = {"clienteNombre", "cliente_nombre"})
    public String clienteNombre;

    @SerializedName(value = "clienteapellido", alternate = {"clienteApellido", "cliente_apellido"})
    public String clienteApellido;

    @SerializedName(value = "clientedni", alternate = {"clienteDni", "cliente_dni"})
    public String clienteDni;

    @SerializedName(value = "clientetelefono", alternate = {"clienteTelefono", "cliente_telefono"})
    public String clienteTelefono;

    @SerializedName(value = "clienteemail", alternate = {"clienteEmail", "cliente_email"})
    public String clienteEmail;

    @SerializedName(value = "montototal", alternate = {"montoTotal", "monto_total"})
    public Double montoTotal;

    @SerializedName(value = "seniamonto", alternate = {"seniaMonto", "senia_monto"})
    public Double seniaMonto;

    @SerializedName(value = "metodopago", alternate = {"metodoPago", "metodo_pago"})
    public String metodoPago;

    @SerializedName("cuotas")
    public Integer cuotas;

    @SerializedName(value = "fechainicio", alternate = {"fechaInicio", "fecha_inicio"})
    public String fechaInicio;

    @SerializedName(value = "fechavencimiento", alternate = {"fechaVencimiento", "fecha_vencimiento"})
    public String fechaVencimiento;

    @SerializedName(value = "fechaventa", alternate = {"fechaVenta", "fecha_venta"})
    public String fechaVenta;

    @SerializedName("estado")
    public String estado;

    @SerializedName("descripcion")
    public String descripcion;

    @SerializedName(value = "rutalocalpdf", alternate = {"rutaLocalPdf", "ruta_local_pdf"})
    public String rutaLocalPdf;

    @SerializedName(value = "urlnubepdf", alternate = {"urlNubePdf", "url_nube_pdf"})
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

    public void normalizarDesdeSupabase() {
        if (this.id == null) {
            this.id = "";
        }
        // Desempaquetar metadatos guardados en urlNubePdf si existen
        if (this.urlNubePdf != null && this.urlNubePdf.trim().startsWith("{")) {
            try {
                JsonObject meta = JsonParser.parseString(this.urlNubePdf).getAsJsonObject();
                if ((this.clienteTelefono == null || this.clienteTelefono.isEmpty()) && meta.has("telefono") && !meta.get("telefono").isJsonNull()) {
                    this.clienteTelefono = meta.get("telefono").getAsString();
                }
                if (this.seniaMonto == null && meta.has("seniaMonto") && !meta.get("seniaMonto").isJsonNull()) {
                    this.seniaMonto = meta.get("seniaMonto").getAsDouble();
                }
                if ((this.propiedadTitulo == null || this.propiedadTitulo.isEmpty()) && meta.has("propiedadTitulo") && !meta.get("propiedadTitulo").isJsonNull()) {
                    this.propiedadTitulo = meta.get("propiedadTitulo").getAsString();
                }
                if ((this.fechaVencimiento == null || this.fechaVencimiento.isEmpty()) && meta.has("fechaVencimiento") && !meta.get("fechaVencimiento").isJsonNull()) {
                    this.fechaVencimiento = meta.get("fechaVencimiento").getAsString();
                }
                if (this.timestamp <= 0 && meta.has("timestamp") && !meta.get("timestamp").isJsonNull()) {
                    this.timestamp = meta.get("timestamp").getAsLong();
                }
            } catch (Exception ignored) { }
        }

        if ((this.fechaVencimiento == null || this.fechaVencimiento.isEmpty()) && this.fechaVenta != null && !this.fechaVenta.isEmpty()) {
            this.fechaVencimiento = this.fechaVenta;
        }
        if (this.propiedadId == null || this.propiedadId.isEmpty()) {
            this.propiedadId = this.propiedadTitulo != null ? this.propiedadTitulo : "";
        }
        if (this.clienteId == null || this.clienteId.isEmpty()) {
            this.clienteId = (this.clienteDni != null && !this.clienteDni.isEmpty()) ? this.clienteDni : this.id;
        }
        if (this.timestamp <= 0) {
            this.timestamp = System.currentTimeMillis();
        }
        this.estadoSincronizacion = "SINCRONIZADO";
    }
}
