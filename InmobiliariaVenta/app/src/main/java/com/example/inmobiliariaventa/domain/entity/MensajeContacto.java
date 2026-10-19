package com.example.inmobiliariaventa.domain.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.Locale;

@Entity(tableName = "mensajes_contacto")
public class MensajeContacto {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    @SerializedName("local_id")
    public long id;

    @ColumnInfo(name = "remote_id")
    @SerializedName(value = "id", alternate = {"remote_id"})
    public String remoteId;

    @ColumnInfo(name = "cliente_nombre")
    @SerializedName(value = "clientenombre", alternate = {"cliente_nombre", "clienteNombre"})
    public String clienteNombre;

    @ColumnInfo(name = "cliente_telefono")
    @SerializedName(value = "clientetelefono", alternate = {"cliente_telefono", "clienteTelefono"})
    public String clienteTelefono;

    @ColumnInfo(name = "cliente_email")
    @SerializedName(value = "clienteemail", alternate = {"cliente_email", "clienteEmail"})
    public String clienteEmail;

    @ColumnInfo(name = "propiedad_id")
    @SerializedName(value = "propiedadid", alternate = {"propiedad_id", "propiedadId"})
    public String propiedadId;

    @ColumnInfo(name = "propiedad_titulo")
    @SerializedName(value = "propiedadtitulo", alternate = {"propiedad_titulo", "propiedadTitulo"})
    public String propiedadTitulo;

    @ColumnInfo(name = "propiedad_direccion")
    public String propiedadDireccion;

    @ColumnInfo(name = "mensaje")
    @SerializedName("mensaje")
    public String mensaje;

    @ColumnInfo(name = "fecha")
    @SerializedName("fecha")
    public String fecha; // "dd/MM/yyyy"

    @ColumnInfo(name = "hora")
    @SerializedName("hora")
    public String hora;  // "11:45 AM"

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    @ColumnInfo(name = "leido")
    @SerializedName(value = "isleido", alternate = {"leido"})
    public boolean leido;

    @ColumnInfo(name = "origen")
    public String origen; // "App Home Finder"

    @ColumnInfo(name = "estadoSincronizacion")
    public String estadoSincronizacion;

    public MensajeContacto() {
    }

    @androidx.room.Ignore
    public MensajeContacto(String clienteNombre, String clienteTelefono, String clienteEmail,
                           String propiedadId, String propiedadTitulo, String propiedadDireccion,
                           String mensaje, String fecha, String hora, long timestamp, boolean leido) {
        this.clienteNombre = clienteNombre;
        this.clienteTelefono = clienteTelefono;
        this.clienteEmail = clienteEmail;
        this.propiedadId = propiedadId;
        this.propiedadTitulo = propiedadTitulo;
        this.propiedadDireccion = propiedadDireccion;
        this.mensaje = mensaje;
        this.fecha = fecha;
        this.hora = hora;
        this.timestamp = timestamp;
        this.leido = leido;
        this.origen = "App Home Finder";
        this.estadoSincronizacion = "PENDIENTE_SUBIR";
    }

    public void calcularTimestampSiEsNecesario() {
        if (this.timestamp > 0) return;
        try {
            if (this.fecha != null && this.hora != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH);
                java.util.Date d = sdf.parse(this.fecha + " " + this.hora);
                if (d != null) {
                    this.timestamp = d.getTime();
                }
            }
        } catch (Exception ignored) {}
        if (this.timestamp <= 0) {
            this.timestamp = System.currentTimeMillis();
        }
    }
}
