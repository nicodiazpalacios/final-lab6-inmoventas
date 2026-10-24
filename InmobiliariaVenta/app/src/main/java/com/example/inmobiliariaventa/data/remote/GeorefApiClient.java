package com.example.inmobiliariaventa.data.remote;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GeorefApiClient {

    private static final String TAG = "GeorefApiClient";
    private static final String BASE_URL = "https://apis.datos.gob.ar/georef/api";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    private static final Gson gson = new Gson();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface GeorefCallback<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }

    public static class ProvinciaItem {
        public final String id;
        public final String nombre;

        public ProvinciaItem(String id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        @NonNull
        @Override
        public String toString() {
            return nombre != null ? nombre : "";
        }
    }

    // Listado de respaldo de las 24 provincias por si falla la conexión a la API
    public static final List<String> PROVINCIAS_FALLBACK = Arrays.asList(
            "Buenos Aires",
            "Catamarca",
            "Chaco",
            "Chubut",
            "Ciudad Autónoma de Buenos Aires",
            "Córdoba",
            "Corrientes",
            "Entre Ríos",
            "Formosa",
            "Jujuy",
            "La Pampa",
            "La Rioja",
            "Mendoza",
            "Misiones",
            "Neuquén",
            "Río Negro",
            "Salta",
            "San Juan",
            "San Luis",
            "Santa Cruz",
            "Santa Fe",
            "Santiago del Estero",
            "Tierra del Fuego, Antártida e Islas del Atlántico Sur",
            "Tucumán"
    );

    public static void obtenerProvincias(GeorefCallback<List<ProvinciaItem>> callback) {
        String url = BASE_URL + "/provincias?campos=id,nombre&orden=nombre";
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "InmobiliariaVentaApp/1.0")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.w(TAG, "Error consultando provincias en Georef: " + e.getMessage());
                mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    mainHandler.post(() -> callback.onError(new IOException("HTTP " + response.code())));
                    return;
                }
                try {
                    String body = response.body().string();
                    JsonObject json = gson.fromJson(body, JsonObject.class);
                    JsonArray arr = (json != null && json.has("provincias")) ? json.getAsJsonArray("provincias") : null;
                    List<ProvinciaItem> list = new ArrayList<>();
                    if (arr != null) {
                        for (JsonElement el : arr) {
                            if (el != null && el.isJsonObject()) {
                                JsonObject obj = el.getAsJsonObject();
                                String id = obj.has("id") ? obj.get("id").getAsString() : "";
                                String nombre = obj.has("nombre") ? obj.get("nombre").getAsString() : "";
                                if (!nombre.isEmpty()) {
                                    list.add(new ProvinciaItem(id, nombre));
                                }
                            }
                        }
                    }

                    // Ordenar alfabéticamente respetando tildes en español
                    Collator collator = Collator.getInstance(new Locale("es", "AR"));
                    collator.setStrength(Collator.PRIMARY);
                    list.sort((a, b) -> collator.compare(a.nombre, b.nombre));

                    mainHandler.post(() -> callback.onSuccess(list));
                } catch (Exception ex) {
                    mainHandler.post(() -> callback.onError(ex));
                }
            }
        });
    }

    public static void obtenerLocalidadesPorProvincia(String provinciaIdONombre, GeorefCallback<List<String>> callback) {
        if (provinciaIdONombre == null || provinciaIdONombre.trim().isEmpty()) {
            mainHandler.post(() -> callback.onSuccess(new ArrayList<>()));
            return;
        }

        final String trimmedInput = provinciaIdONombre.trim();
        final boolean esIdNumerico = trimmedInput.matches("\\d{2}");

        String encodedProv;
        try {
            encodedProv = URLEncoder.encode(trimmedInput, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            encodedProv = trimmedInput;
        }

        String url = BASE_URL + "/localidades?provincia=" + encodedProv
                + "&campos=id,nombre,provincia.id,provincia.nombre&max=1000&orden=nombre";
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "InmobiliariaVentaApp/1.0")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.w(TAG, "Error consultando localidades en Georef: " + e.getMessage());
                mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    mainHandler.post(() -> callback.onError(new IOException("HTTP " + response.code())));
                    return;
                }
                try {
                    String body = response.body().string();
                    JsonObject json = gson.fromJson(body, JsonObject.class);
                    JsonArray arr = (json != null && json.has("localidades")) ? json.getAsJsonArray("localidades") : null;

                    Collator collator = Collator.getInstance(new Locale("es", "AR"));
                    collator.setStrength(Collator.PRIMARY);

                    TreeSet<String> uniqueLocalidades = new TreeSet<>((a, b) -> {
                        int cmp = collator.compare(a, b);
                        return cmp != 0 ? cmp : a.compareTo(b);
                    });

                    if (arr != null) {
                        for (JsonElement el : arr) {
                            if (el != null && el.isJsonObject()) {
                                JsonObject obj = el.getAsJsonObject();

                                // Verificar que la localidad pertenezca efectivamente a la provincia solicitada
                                if (esIdNumerico && obj.has("provincia") && obj.get("provincia").isJsonObject()) {
                                    JsonObject provObj = obj.getAsJsonObject("provincia");
                                    String provIdItem = provObj.has("id") ? provObj.get("id").getAsString() : "";
                                    if (!provIdItem.isEmpty() && !trimmedInput.equals(provIdItem)) {
                                        continue;
                                    }
                                }

                                String nombre = obj.has("nombre") ? obj.get("nombre").getAsString() : "";
                                if (!nombre.trim().isEmpty()) {
                                    uniqueLocalidades.add(nombre.trim());
                                }
                            }
                        }
                    }

                    List<String> list = new ArrayList<>(uniqueLocalidades);
                    mainHandler.post(() -> callback.onSuccess(list));
                } catch (Exception ex) {
                    mainHandler.post(() -> callback.onError(ex));
                }
            }
        });
    }
}
