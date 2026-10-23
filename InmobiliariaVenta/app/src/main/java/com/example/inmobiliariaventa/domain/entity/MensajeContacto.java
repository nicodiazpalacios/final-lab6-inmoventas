package com.example.inmobiliariaventa.domain.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.Date;
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

    @androidx.room.Ignore
    @SerializedName(value = "created_at", alternate = {"createdAt"})
    public String createdAt;

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

    public boolean isDemoMessage() {
        if (this.remoteId == null || this.remoteId.trim().isEmpty()) {
            return true;
        }
        if ("lucas.m@gmail.com".equalsIgnoreCase(this.clienteEmail)
                || "sofi.benitez@hotmail.com".equalsIgnoreCase(this.clienteEmail)
                || "gonzalo.f@gmail.com".equalsIgnoreCase(this.clienteEmail)
                || "valen.rossi@yahoo.com".equalsIgnoreCase(this.clienteEmail)
                || "camilag@gmail.com".equalsIgnoreCase(this.clienteEmail)
                || "mateo.rod@gmail.com".equalsIgnoreCase(this.clienteEmail)
                || "flor.diaz@outlook.com".equalsIgnoreCase(this.clienteEmail)
                || "emiliano.m@gmail.com".equalsIgnoreCase(this.clienteEmail)
                || "lucia.navarro@gmail.com".equalsIgnoreCase(this.clienteEmail)
                || "diego.castro@gmail.com".equalsIgnoreCase(this.clienteEmail)
                || "martu.lopez@gmail.com".equalsIgnoreCase(this.clienteEmail)
                || "agustin.silva@gmail.com".equalsIgnoreCase(this.clienteEmail)) {
            return true;
        }
        return false;
    }

    private static final long DIA_MS = 86400000L;

    /** Mantiene los mensajes demo siempre en el pasado (3+ días atrás) para que nunca tapen consultas reales. */
    private void ajustarTimestampDemo() {
        long now = System.currentTimeMillis();
        if (this.timestamp > now - (3L * DIA_MS) || this.timestamp <= 0) {
            long offset = (3L + (Math.abs(this.id) % 15)) * DIA_MS + (Math.abs(this.id) * 3600000L);
            this.timestamp = now - offset;
        }
    }

    public void calcularTimestampSiEsNecesario() {
        if (isDemoMessage()) {
            ajustarTimestampDemo();
            return;
        }
        if (this.timestamp > 0) return;
        recalcularTimestampDesdeOrigen();
    }

    /**
     * Recalcula SIEMPRE el timestamp a partir de los datos de origen (created_at de Supabase
     * o fecha/hora del mensaje), ignorando el valor actual. Devuelve true si se pudo calcular.
     */
    public boolean recalcularTimestampDesdeOrigen() {
        // 1. created_at de Supabase (hora exacta en que el mensaje llegó al servidor)
        long ts = parseIsoUtc(this.createdAt);
        if (ts > 0) {
            this.timestamp = ts;
            return true;
        }
        // 2. fecha + hora guardadas en el mensaje
        long previo = this.timestamp;
        long desdeFecha = parseFechaHora(this.fecha, this.hora);
        if (desdeFecha > 0) {
            this.timestamp = desdeFecha;
            return true;
        }
        this.timestamp = previo > 0 ? previo : System.currentTimeMillis();
        return false;
    }

    /** Parser ISO-8601 tolerante (T o espacio, fracciones de cualquier largo, Z / +00 / +00:00 / +0000). Devuelve 0 si falla. */
    public static long parseIsoUtc(String raw) {
        if (raw == null) return 0L;
        String s = raw.trim();
        if (s.isEmpty()) return 0L;
        try {
            if (s.matches("^\\d{11,}$")) return Long.parseLong(s);
            java.util.regex.Matcher mt = java.util.regex.Pattern.compile(
                    "^(\\d{4})-(\\d{2})-(\\d{2})[T ](\\d{2}):(\\d{2})(?::(\\d{2}))?(?:\\.(\\d+))?\\s*(Z|[+-]\\d{2}(?::?\\d{2})?)?$")
                    .matcher(s);
            if (!mt.matches()) return 0L;
            java.util.Calendar c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
            c.clear();
            c.set(Integer.parseInt(mt.group(1)), Integer.parseInt(mt.group(2)) - 1, Integer.parseInt(mt.group(3)),
                    Integer.parseInt(mt.group(4)), Integer.parseInt(mt.group(5)),
                    mt.group(6) != null ? Integer.parseInt(mt.group(6)) : 0);
            long millis = c.getTimeInMillis();
            if (mt.group(7) != null) {
                String frac = (mt.group(7) + "000").substring(0, 3);
                millis += Integer.parseInt(frac);
            }
            String tz = mt.group(8);
            if (tz != null && !tz.equalsIgnoreCase("Z")) {
                int sign = tz.charAt(0) == '-' ? -1 : 1;
                String digits = tz.substring(1).replace(":", "");
                int h = Integer.parseInt(digits.substring(0, 2));
                int m = digits.length() >= 4 ? Integer.parseInt(digits.substring(2, 4)) : 0;
                millis -= sign * (h * 3600000L + m * 60000L);
            }
            return millis;
        } catch (Exception e) {
            return 0L;
        }
    }

    /** Parsea fecha "dd/MM/yyyy" + hora ("09:30 PM", "09:30 p. m.", "21:30"). Devuelve 0 si falla. */
    public static long parseFechaHora(String fecha, String hora) {
        if (fecha == null || fecha.trim().isEmpty()) return 0L;
        String fechaLimpia = fecha.trim();
        String horaLimpia = hora != null ? hora.trim() : "";

        // Reemplazar espacios no separables de Unicode por espacio normal
        horaLimpia = horaLimpia.replaceAll("[\\s\\u00A0\\u202F]+", " ").trim();
        // Normalizar variaciones de a. m. / p. m. a AM / PM
        horaLimpia = horaLimpia.replaceAll("(?i)a\\.?\\s*m\\.?", "AM")
                               .replaceAll("(?i)p\\.?\\s*m\\.?", "PM");

        String combined = (fechaLimpia + (horaLimpia.isEmpty() ? "" : " " + horaLimpia)).trim();
        boolean tieneAmPm = combined.endsWith("AM") || combined.endsWith("PM");

        String[] patterns = tieneAmPm
                ? new String[] {"dd/MM/yyyy hh:mm a", "dd/MM/yyyy h:mm a", "dd-MM-yyyy hh:mm a"}
                : new String[] {"dd/MM/yyyy HH:mm", "dd/MM/yyyy H:mm", "dd-MM-yyyy HH:mm",
                                "yyyy-MM-dd HH:mm", "dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd"};

        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
                sdf.setLenient(false);
                java.text.ParsePosition pos = new java.text.ParsePosition(0);
                Date d = sdf.parse(combined, pos);
                if (d != null && pos.getIndex() == combined.length()) {
                    return d.getTime();
                }
            } catch (Exception ignored) {}
        }
        return 0L;
    }

    public long getEfectiveTimestamp() {
        if (isDemoMessage()) {
            ajustarTimestampDemo();
            return this.timestamp;
        }
        if (this.timestamp > 0) return this.timestamp;
        calcularTimestampSiEsNecesario();
        return this.timestamp > 0 ? this.timestamp : System.currentTimeMillis();
    }
}
