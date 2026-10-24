package com.example.inmobiliariaventa.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.data.local.dao.CiudadGeorefDao;
import com.example.inmobiliariaventa.data.remote.GeorefApiClient;
import com.example.inmobiliariaventa.utils.NetworkUtils;

import java.io.IOException;
import java.text.Collator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repositorio para Provincias y Ciudades/Localidades (Georef API) con caché local
 * permanente en Room indexado por provincia.
 *
 * Estrategia Cache-First (100% Offline luego de la primera consulta):
 * 1. Al consultar las ciudades de una provincia, primero busca en Room por `provinciaKey`.
 * 2. Si existen registros locales en Room para esa provincia, los devuelve inmediatamente
 *    sin realizar ninguna llamada de red (100% offline).
 * 3. Si no existen en Room, valida la red, consulta la API de Georef usando el ID oficial INDEC,
 *    persiste la lista de forma permanente en Room y retorna el resultado.
 */
public class GeorefRepository {

    private static final String TAG = "GeorefRepository";
    private static volatile GeorefRepository INSTANCE;

    private final Context appContext;
    private final CiudadGeorefDao ciudadGeorefDao;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Caché en memoria L1 complementaria a la persistencia permanente L2 en Room
    private final Map<String, List<String>> memoryCachePorProvincia = new ConcurrentHashMap<>();

    // Mapa oficial INDEC de ID de provincia -> Nombre canónico para unificar claves online/offline
    private static final Map<String, String> INDEC_ID_A_PROVINCIA = new HashMap<>();
    private static final Map<String, String> PROVINCIA_KEY_A_INDEC_ID = new HashMap<>();

    static {
        INDEC_ID_A_PROVINCIA.put("02", "Ciudad Autónoma de Buenos Aires");
        INDEC_ID_A_PROVINCIA.put("06", "Buenos Aires");
        INDEC_ID_A_PROVINCIA.put("10", "Catamarca");
        INDEC_ID_A_PROVINCIA.put("14", "Córdoba");
        INDEC_ID_A_PROVINCIA.put("18", "Corrientes");
        INDEC_ID_A_PROVINCIA.put("22", "Chaco");
        INDEC_ID_A_PROVINCIA.put("26", "Chubut");
        INDEC_ID_A_PROVINCIA.put("30", "Entre Ríos");
        INDEC_ID_A_PROVINCIA.put("34", "Formosa");
        INDEC_ID_A_PROVINCIA.put("38", "Jujuy");
        INDEC_ID_A_PROVINCIA.put("42", "La Pampa");
        INDEC_ID_A_PROVINCIA.put("46", "La Rioja");
        INDEC_ID_A_PROVINCIA.put("50", "Mendoza");
        INDEC_ID_A_PROVINCIA.put("54", "Misiones");
        INDEC_ID_A_PROVINCIA.put("58", "Neuquén");
        INDEC_ID_A_PROVINCIA.put("62", "Río Negro");
        INDEC_ID_A_PROVINCIA.put("66", "Salta");
        INDEC_ID_A_PROVINCIA.put("70", "San Juan");
        INDEC_ID_A_PROVINCIA.put("74", "San Luis");
        INDEC_ID_A_PROVINCIA.put("78", "Santa Cruz");
        INDEC_ID_A_PROVINCIA.put("82", "Santa Fe");
        INDEC_ID_A_PROVINCIA.put("86", "Santiago del Estero");
        INDEC_ID_A_PROVINCIA.put("90", "Tucumán");
        INDEC_ID_A_PROVINCIA.put("94", "Tierra del Fuego, Antártida e Islas del Atlántico Sur");

        for (Map.Entry<String, String> entry : INDEC_ID_A_PROVINCIA.entrySet()) {
            String key = quitarTildesYMinusculas(entry.getValue());
            PROVINCIA_KEY_A_INDEC_ID.put(key, entry.getKey());
        }
    }

    public static GeorefRepository getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (GeorefRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new GeorefRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public GeorefRepository(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.ciudadGeorefDao = AppDatabase.getInstance(this.appContext).ciudadGeorefDao();
    }

    private static String quitarTildesYMinusculas(String input) {
        if (input == null) return "";
        String sinTildes = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinTildes.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Normaliza el identificador o nombre de provincia (ej. "86", "Santiago del Estero", "Córdoba")
     * a una clave canónica sin tildes y en minúsculas ("santiago del estero", "cordoba")
     * para garantizar que las consultas online y offline coincidan siempre en el índice de Room.
     */
    @NonNull
    public static String normalizarProvinciaKey(String provinciaIdONombre) {
        if (provinciaIdONombre == null) return "";
        String raw = provinciaIdONombre.trim();
        if (raw.isEmpty()) return "";

        if (INDEC_ID_A_PROVINCIA.containsKey(raw)) {
            raw = INDEC_ID_A_PROVINCIA.get(raw);
        }

        return quitarTildesYMinusculas(raw);
    }

    @NonNull
    public static String obtenerNombreProvinciaLegible(String provinciaIdONombre) {
        if (provinciaIdONombre == null) return "";
        String raw = provinciaIdONombre.trim();
        if (INDEC_ID_A_PROVINCIA.containsKey(raw)) {
            return INDEC_ID_A_PROVINCIA.get(raw);
        }
        String key = quitarTildesYMinusculas(raw);
        String indecId = PROVINCIA_KEY_A_INDEC_ID.get(key);
        if (indecId != null && INDEC_ID_A_PROVINCIA.containsKey(indecId)) {
            return INDEC_ID_A_PROVINCIA.get(indecId);
        }
        return raw;
    }

    @NonNull
    public static String obtenerIndecIdOValor(String provinciaIdONombre) {
        String key = normalizarProvinciaKey(provinciaIdONombre);
        String indecId = PROVINCIA_KEY_A_INDEC_ID.get(key);
        if (indecId != null && !indecId.isEmpty()) {
            return indecId;
        }
        return provinciaIdONombre != null ? provinciaIdONombre.trim() : "";
    }

    /**
     * Obtiene la lista de provincias argentinas. Si no hay conexión de red o la API falla,
     * resuelve 100% offline usando el listado oficial local de las 24 jurisdicciones.
     */
    public void obtenerProvincias(@NonNull GeorefApiClient.GeorefCallback<List<GeorefApiClient.ProvinciaItem>> callback) {
        if (!NetworkUtils.isNetworkAvailable(appContext)) {
            List<GeorefApiClient.ProvinciaItem> fallback = construirProvinciasFallback();
            mainHandler.post(() -> callback.onSuccess(fallback));
            return;
        }

        GeorefApiClient.obtenerProvincias(new GeorefApiClient.GeorefCallback<List<GeorefApiClient.ProvinciaItem>>() {
            @Override
            public void onSuccess(List<GeorefApiClient.ProvinciaItem> result) {
                if (result != null && !result.isEmpty()) {
                    callback.onSuccess(result);
                } else {
                    callback.onSuccess(construirProvinciasFallback());
                }
            }

            @Override
            public void onError(Exception error) {
                Log.w(TAG, "Usando provincias locales de respaldo por error de red: " + error.getMessage());
                callback.onSuccess(construirProvinciasFallback());
            }
        });
    }

    private List<GeorefApiClient.ProvinciaItem> construirProvinciasFallback() {
        List<GeorefApiClient.ProvinciaItem> lista = new ArrayList<>();
        for (String nombre : GeorefApiClient.PROVINCIAS_FALLBACK) {
            String id = obtenerIndecIdOValor(nombre);
            lista.add(new GeorefApiClient.ProvinciaItem(id, nombre));
        }
        return lista;
    }

    /**
     * Obtiene las ciudades/localidades de una provincia aplicando el patrón de caché permanente:
     * 1. Si ya están en memoria o en Room para esa provincia, se devuelven 100% offline sin llamar a la API.
     * 2. Si aún no están en Room, consulta la API de Georef usando el ID oficial INDEC de la provincia,
     *    las guarda permanentemente en Room y las devuelve.
     */
    public void obtenerCiudadesPorProvincia(
            String provinciaIdONombre,
            @NonNull GeorefApiClient.GeorefCallback<List<String>> callback
    ) {
        final String provinciaKey = normalizarProvinciaKey(provinciaIdONombre);
        final String nombreLegible = obtenerNombreProvinciaLegible(provinciaIdONombre);
        final String paramConsultaApi = obtenerIndecIdOValor(provinciaIdONombre);

        if (provinciaKey.isEmpty()) {
            mainHandler.post(() -> callback.onSuccess(new ArrayList<>()));
            return;
        }

        // 1. Revisar caché rápida L1 en memoria
        List<String> enMemoria = memoryCachePorProvincia.get(provinciaKey);
        if (enMemoria != null && !enMemoria.isEmpty()) {
            Log.d(TAG, "Ciudades de [" + nombreLegible + "] resueltas desde caché en memoria (" + enMemoria.size() + ").");
            mainHandler.post(() -> callback.onSuccess(new ArrayList<>(enMemoria)));
            return;
        }

        // 2. Revisar caché persistente L2 en Room en hilo secundario
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<String> cacheadasEnRoom = ciudadGeorefDao.obtenerNombresPorProvinciaSync(provinciaKey);
                if (cacheadasEnRoom != null && !cacheadasEnRoom.isEmpty()) {
                    ordenarAlfabeticamenteEspanol(cacheadasEnRoom);
                    memoryCachePorProvincia.put(provinciaKey, Collections.unmodifiableList(new ArrayList<>(cacheadasEnRoom)));
                    Log.d(TAG, "Ciudades de [" + nombreLegible + "] resueltas 100% OFFLINE desde Room (" + cacheadasEnRoom.size() + " localidades).");
                    final List<String> resultadoLocal = new ArrayList<>(cacheadasEnRoom);
                    mainHandler.post(() -> callback.onSuccess(resultadoLocal));
                    return;
                }
            } catch (Exception e) {
                Log.w(TAG, "Error leyendo caché de ciudades en Room: " + e.getMessage());
            }

            // 3. Cache Miss en Room: validar red antes de consultar API online
            if (!NetworkUtils.isNetworkAvailable(appContext)) {
                Log.w(TAG, "Sin red y sin caché previo en Room para la provincia: " + nombreLegible);
                mainHandler.post(() -> callback.onError(
                        new IOException("Sin conexión a internet y la provincia " + nombreLegible + " aún no está en caché local.")
                ));
                return;
            }

            // 4. Consultar API Georef online usando el ID oficial INDEC y persistir permanentemente en Room
            Log.d(TAG, "Consultando API Georef online por primera vez para provincia: " + nombreLegible + " (id=" + paramConsultaApi + ")");
            GeorefApiClient.obtenerLocalidadesPorProvincia(paramConsultaApi, new GeorefApiClient.GeorefCallback<List<String>>() {
                @Override
                public void onSuccess(List<String> localidadesRemotas) {
                    if (localidadesRemotas != null && !localidadesRemotas.isEmpty()) {
                        final List<String> copiaParaGuardar = new ArrayList<>(localidadesRemotas);
                        memoryCachePorProvincia.put(provinciaKey, Collections.unmodifiableList(copiaParaGuardar));

                        // Guardar de forma permanente en Room en segundo plano
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            try {
                                ciudadGeorefDao.guardarCacheProvincia(provinciaKey, nombreLegible, copiaParaGuardar);
                                Log.d(TAG, "Guardadas permanentemente en Room " + copiaParaGuardar.size()
                                        + " ciudades para la provincia [" + nombreLegible + "] (key=" + provinciaKey + ").");
                            } catch (Exception e) {
                                Log.e(TAG, "Error guardando ciudades en Room: " + e.getMessage());
                            }
                        });
                    }
                    callback.onSuccess(localidadesRemotas != null ? localidadesRemotas : new ArrayList<>());
                }

                @Override
                public void onError(Exception error) {
                    callback.onError(error);
                }
            });
        });
    }

    private void ordenarAlfabeticamenteEspanol(List<String> lista) {
        if (lista == null || lista.size() <= 1) return;
        Collator collator = Collator.getInstance(new Locale("es", "AR"));
        collator.setStrength(Collator.PRIMARY);
        lista.sort(collator::compare);
    }
}
