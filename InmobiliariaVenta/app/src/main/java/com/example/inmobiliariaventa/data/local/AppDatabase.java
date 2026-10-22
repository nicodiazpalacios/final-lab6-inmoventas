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
import com.example.inmobiliariaventa.data.local.dao.UsuarioDao;
import com.example.inmobiliariaventa.data.local.dao.PropiedadDao;
import com.example.inmobiliariaventa.data.local.dao.FavoritoDao;
import com.example.inmobiliariaventa.data.local.dao.NotificacionDao;
import com.example.inmobiliariaventa.data.local.dao.ContratoDao;
import com.example.inmobiliariaventa.data.local.dao.ConfiguracionEmpresaDao;
import com.example.inmobiliariaventa.data.local.dao.MensajeContactoDao;

@Database(entities = {Usuario.class, Propiedad.class, Favorito.class, Notificacion.class, Contrato.class, ConfiguracionEmpresa.class, MensajeContacto.class}, version = 12, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract UsuarioDao usuarioDao();
    public abstract PropiedadDao propiedadDao();
    public abstract FavoritoDao favoritoDao();
    public abstract NotificacionDao notificacionDao();
    public abstract ContratoDao contratoDao();
    public abstract ConfiguracionEmpresaDao configuracionEmpresaDao();
    public abstract MensajeContactoDao mensajeContactoDao();

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

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "homefinder_database")
                            .addMigrations(MIGRATION_11_12)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
