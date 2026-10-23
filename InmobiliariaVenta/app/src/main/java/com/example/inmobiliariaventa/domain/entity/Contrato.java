package com.example.inmobiliariaventa.domain.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Index;
import androidx.annotation.NonNull;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

@Entity(
    tableName = "contratos",
    indices = {@Index("propiedadId"), @Index("clienteId")}
)
public class Contrato {

    public static final String ESTADO_PENDIENTE = "Pendiente";
    public static final String ESTADO_COMPLETADO = "Completado";
    public static final String ESTADO_RECHAZADO = "Rechazado";


    @Ignore
    public List<Pago> listaPagos = new ArrayList<>();
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

    @Ignore
    @SerializedName(value = "created_at", alternate = {"createdAt", "created_at_str"})
    public String createdAt;

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
        if (this.createdAt != null && !this.createdAt.trim().isEmpty()) {
            try {
                long parsed = com.example.inmobiliariaventa.data.remote.SupabaseClient.parseTimestampFlexible(
                        new com.google.gson.JsonPrimitive(this.createdAt.trim()));
                if (parsed > 0) {
                    this.timestamp = parsed;
                }
            } catch (Exception ignored) { }
        }
        if (this.timestamp <= 0 && this.fechaInicio != null && !this.fechaInicio.trim().isEmpty()) {
            try {
                long parsed = com.example.inmobiliariaventa.data.remote.SupabaseClient.parseTimestampFlexible(
                        new com.google.gson.JsonPrimitive(this.fechaInicio.trim()));
                if (parsed > 0) {
                    this.timestamp = parsed;
                }
            } catch (Exception ignored) { }
        }
        if (this.timestamp <= 0) {
            this.timestamp = System.currentTimeMillis();
        }
        this.estado = getEstadoNormalizado();
        this.estadoSincronizacion = "SINCRONIZADO";
    }

    public void setListaPagos(List<Pago> pagos) {
        this.listaPagos = pagos != null ? pagos : new ArrayList<>();
        actualizarEstadoSegunSaldo();
    }

    public List<Pago> getListaPagos() {
        return this.listaPagos != null ? this.listaPagos : new ArrayList<>();
    }

    public boolean esPagoDeSenia(Pago p) {
        if (p == null) return false;
        String concepto = p.concepto != null ? p.concepto.toLowerCase(Locale.ROOT) : "";
        if (concepto.contains("seña") || concepto.contains("senia") || concepto.contains("anticipo")) {
            return true;
        }
        if (concepto.contains("saldo") || concepto.contains("liquidac") || concepto.contains("cuota")) {
            return false;
        }
        if (p.nroCuota == 0 && this.seniaMonto != null && Math.abs(p.monto - this.seniaMonto) < 0.01) {
            return true;
        }
        return false;
    }

    public double calcularTotalAbonado() {
        if (listaPagos != null && !listaPagos.isEmpty()) {
            double total = 0.0;
            boolean incluyeSenia = false;
            for (Pago p : listaPagos) {
                if (p == null) continue;
                total += p.monto;
                if (esPagoDeSenia(p)) {
                    incluyeSenia = true;
                }
            }
            if (!incluyeSenia && this.seniaMonto != null && this.seniaMonto > 0) {
                total += this.seniaMonto;
            }
            return total;
        }
        return this.seniaMonto != null ? this.seniaMonto : 0.0;
    }

    public double calcularSaldoAdeudado() {
        double monto = this.montoTotal != null ? this.montoTotal : 0.0;
        double abonado = calcularTotalAbonado();
        return Math.max(0.0, monto - abonado);
    }

    public int getCuotasPagadasCount() {
        if (listaPagos != null && !listaPagos.isEmpty()) {
            int count = 0;
            for (Pago p : listaPagos) {
                if (p.nroCuota > 0) {
                    count++;
                }
            }
            return count;
        }
        return 0;
    }

    public boolean esCuotaPagada(int nroCuota) {
        if (nroCuota <= 0 || listaPagos == null) return false;
        for (Pago p : listaPagos) {
            if (p.nroCuota == nroCuota) return true;
        }
        return false;
    }

    public Pago obtenerPagoDeCuota(int nroCuota) {
        if (nroCuota <= 0 || listaPagos == null) return null;
        for (Pago p : listaPagos) {
            if (p.nroCuota == nroCuota) return p;
        }
        return null;
    }

    public String getEstadoNormalizado() {
        if (this.estado == null || this.estado.trim().isEmpty()) {
            return (calcularSaldoAdeudado() <= 0.001) ? ESTADO_COMPLETADO : ESTADO_PENDIENTE;
        }
        String est = this.estado.trim();
        if ("Activo".equalsIgnoreCase(est)) {
            return (calcularSaldoAdeudado() <= 0.001) ? ESTADO_COMPLETADO : ESTADO_PENDIENTE;
        }
        if ("Cancelado".equalsIgnoreCase(est) || "Rechazado".equalsIgnoreCase(est)) {
            return ESTADO_RECHAZADO;
        }
        if ("Finalizado".equalsIgnoreCase(est) || "Completado".equalsIgnoreCase(est)) {
            return ESTADO_COMPLETADO;
        }
        return ESTADO_PENDIENTE;
    }

    public void actualizarEstadoSegunSaldo() {
        if (ESTADO_RECHAZADO.equalsIgnoreCase(this.estado) || "Cancelado".equalsIgnoreCase(this.estado)) {
            this.estado = ESTADO_RECHAZADO;
            return;
        }
        if (calcularSaldoAdeudado() <= 0.001) {
            this.estado = ESTADO_COMPLETADO;
        } else {
            this.estado = ESTADO_PENDIENTE;
        }
    }
}
