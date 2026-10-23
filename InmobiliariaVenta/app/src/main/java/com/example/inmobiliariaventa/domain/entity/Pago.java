package com.example.inmobiliariaventa.domain.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

@Entity(
    tableName = "pagos",
    indices = {@Index("contratoId")}
)
public class Pago {

    @PrimaryKey
    @NonNull
    @SerializedName("id")
    public String id;

    @NonNull
    @ColumnInfo(name = "contratoId")
    @SerializedName(value = "contrato_id", alternate = {"contratoid", "contratoId"})
    public String contratoId;

    @ColumnInfo(name = "nroCuota")
    @SerializedName(value = "nro_cuota", alternate = {"nrocuota", "nroCuota"})
    public int nroCuota;

    @SerializedName("monto")
    public double monto;

    @SerializedName("fecha")
    public String fecha;

    @ColumnInfo(name = "metodoPago")
    @SerializedName(value = "metodo_pago", alternate = {"metodopago", "metodoPago"})
    public String metodoPago;

    @SerializedName("concepto")
    public String concepto;

    @ColumnInfo(name = "estadoSincronizacion")
    public String estadoSincronizacion;

    public Pago() {
        this.id = "";
        this.contratoId = "";
        this.metodoPago = "Efectivo";
        this.estadoSincronizacion = "SINCRONIZADO";
    }

    @Ignore
    public Pago(@NonNull String id, @NonNull String contratoId, int nroCuota, double monto,
                String fecha, String metodoPago, String concepto) {
        this.id = id;
        this.contratoId = contratoId;
        this.nroCuota = nroCuota;
        this.monto = monto;
        this.fecha = fecha != null ? fecha : "";
        this.metodoPago = metodoPago != null ? metodoPago : "Efectivo";
        this.concepto = concepto != null ? concepto : "";
        this.estadoSincronizacion = "PENDIENTE_SUBIR";
    }
}
