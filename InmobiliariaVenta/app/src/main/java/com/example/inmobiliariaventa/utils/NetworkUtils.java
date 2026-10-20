package com.example.inmobiliariaventa.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.inmobiliariaventa.data.local.AppDatabase;

/**
 * Utilidad centralizada para detección y observación reactiva del estado de red.
 * Permite alternar automáticamente entre modo online y offline (solo favoritos).
 */
public class NetworkUtils {

    private static NetworkLiveData networkLiveData;

    public static synchronized LiveData<Boolean> getNetworkLiveData(Context context) {
        if (networkLiveData == null) {
            networkLiveData = new NetworkLiveData(context.getApplicationContext());
        }
        return networkLiveData;
    }

    /**
     * Verifica de forma síncrona si hay conexión activa a internet.
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) return false;
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network activeNetwork = cm.getActiveNetwork();
                if (activeNetwork == null) return false;
                NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
                if (caps == null) return false;
                return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            } else {
                android.net.NetworkInfo netInfo = cm.getActiveNetworkInfo();
                return netInfo != null && netInfo.isConnected();
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Comprueba si una propiedad está disponible para visualizarse sin conexión.
     * En modo offline, solo se permite si el usuario la tiene en su lista de favoritos.
     */
    public static boolean isPropertyAvailableOffline(Context context, String propiedadId) {
        if (context == null || propiedadId == null || propiedadId.isEmpty()) return false;
        if (isNetworkAvailable(context)) return true;

        try {
            SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            String userId = prefs.getString("user_id", "");
            if (userId == null || userId.isEmpty()) return false;
            return AppDatabase.getInstance(context).favoritoDao().esFavorito(userId, propiedadId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * LiveData reactivo que notifica de inmediato cambios en la conectividad
     * mediante ConnectivityManager.NetworkCallback.
     */
    public static class NetworkLiveData extends LiveData<Boolean> {
        private final Context context;
        private final ConnectivityManager connectivityManager;
        private final Handler mainHandler = new Handler(Looper.getMainLooper());
        private ConnectivityManager.NetworkCallback networkCallback;

        public NetworkLiveData(Context context) {
            this.context = context.getApplicationContext();
            this.connectivityManager = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
            setValue(NetworkUtils.isNetworkAvailable(this.context));
        }

        private void postNetworkStatus() {
            mainHandler.post(() -> {
                boolean isConnected = NetworkUtils.isNetworkAvailable(context);
                if (getValue() == null || !getValue().equals(isConnected)) {
                    setValue(isConnected);
                }
            });
        }

        @Override
        protected void onActive() {
            super.onActive();
            // Actualizar estado de inmediato al activarse
            setValue(NetworkUtils.isNetworkAvailable(context));

            try {
                if (networkCallback == null) {
                    networkCallback = new ConnectivityManager.NetworkCallback() {
                        @Override
                        public void onAvailable(@NonNull Network network) {
                            postNetworkStatus();
                        }

                        @Override
                        public void onLost(@NonNull Network network) {
                            postNetworkStatus();
                        }

                        @Override
                        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                            postNetworkStatus();
                        }
                    };
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    connectivityManager.registerDefaultNetworkCallback(networkCallback);
                } else {
                    NetworkRequest request = new NetworkRequest.Builder()
                            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            .build();
                    connectivityManager.registerNetworkCallback(request, networkCallback);
                }
            } catch (Exception e) {
                setValue(NetworkUtils.isNetworkAvailable(context));
            }
        }

        @Override
        protected void onInactive() {
            super.onInactive();
            if (connectivityManager != null && networkCallback != null) {
                try {
                    connectivityManager.unregisterNetworkCallback(networkCallback);
                } catch (Exception ignored) {}
            }
        }
    }
}
