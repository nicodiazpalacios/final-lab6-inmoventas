package com.example.inmobiliariaventa.data.local;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.inmobiliariaventa.domain.entity.Usuario;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.domain.entity.Favorito;
import com.example.inmobiliariaventa.domain.entity.Notificacion;
import com.example.inmobiliariaventa.domain.entity.Contrato;
import com.example.inmobiliariaventa.domain.entity.ConfiguracionEmpresa;
import com.example.inmobiliariaventa.domain.entity.MensajeContacto;
import com.example.inmobiliariaventa.domain.entity.Pago;
import com.example.inmobiliariaventa.data.local.dao.UsuarioDao;
import com.example.inmobiliariaventa.data.local.dao.PropiedadDao;
import com.example.inmobiliariaventa.data.local.dao.FavoritoDao;
import com.example.inmobiliariaventa.data.local.dao.NotificacionDao;
import com.example.inmobiliariaventa.data.local.dao.ContratoDao;
import com.example.inmobiliariaventa.data.local.dao.ConfiguracionEmpresaDao;
import com.example.inmobiliariaventa.data.local.dao.MensajeContactoDao;
import com.example.inmobiliariaventa.data.local.dao.PagoDao;

@Database(entities = {Usuario.class, Propiedad.class, Favorito.class, Notificacion.class, Contrato.class, ConfiguracionEmpresa.class, MensajeContacto.class, Pago.class}, version = 14, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract UsuarioDao usuarioDao();
    public abstract PropiedadDao propiedadDao();
    public abstract FavoritoDao favoritoDao();
    public abstract NotificacionDao notificacionDao();
    public abstract ContratoDao contratoDao();
    public abstract ConfiguracionEmpresaDao configuracionEmpresaDao();
    public abstract MensajeContactoDao mensajeContactoDao();
    public abstract PagoDao pagoDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final java.util.concurrent.ExecutorService databaseWriteExecutor =
            java.util.concurrent.Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    private static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE IF EXISTS `notificaciones`");
            database.execSQL("CREATE TABLE IF NOT EXISTS `notificaciones` ("
                    + "`id` TEXT NOT NULL, "
                    + "`usuario_id` TEXT, "
                    + "`titulo` TEXT, "
                    + "`mensaje` TEXT, "
                    + "`fecha` INTEGER NOT NULL, "
                    + "`isLeida` INTEGER NOT NULL, "
                    + "`categoria` TEXT, "
                    + "`referencia_id` TEXT, "
                    + "`estadoSincronizacion` TEXT, "
                    + "PRIMARY KEY(`id`))");
        }
    };

    private static final Migration MIGRATION_12_13 = new Migration(12, 13) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `pagos` ("
                    + "`id` TEXT NOT NULL, "
                    + "`contratoId` TEXT NOT NULL, "
                    + "`nroCuota` INTEGER NOT NULL, "
                    + "`monto` REAL NOT NULL, "
                    + "`fecha` TEXT, "
                    + "`metodoPago` TEXT, "
                    + "`concepto` TEXT, "
                    + "`estadoSincronizacion` TEXT, "
                    + "PRIMARY KEY(`id`))");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_pagos_contratoId` ON `pagos` (`contratoId`)");
        }
    };

    private static final Migration MIGRATION_13_14 = new Migration(13, 14) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `pagos_new` ("
                    + "`id` TEXT NOT NULL, "
                    + "`contratoId` TEXT NOT NULL, "
                    + "`nroCuota` INTEGER NOT NULL, "
                    + "`monto` REAL NOT NULL, "
                    + "`fecha` TEXT, "
                    + "`metodoPago` TEXT, "
                    + "`concepto` TEXT, "
                    + "`estadoSincronizacion` TEXT, "
                    + "PRIMARY KEY(`id`))");
            database.execSQL("INSERT OR IGNORE INTO `pagos_new` SELECT * FROM `pagos`");
            database.execSQL("DROP TABLE `pagos`");
            database.execSQL("ALTER TABLE `pagos_new` RENAME TO `pagos`");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_pagos_contratoId` ON `pagos` (`contratoId`)");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "homefinder_database")
                            .addMigrations(MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
