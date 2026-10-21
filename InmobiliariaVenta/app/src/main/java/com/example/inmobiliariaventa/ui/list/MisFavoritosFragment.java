package com.example.inmobiliariaventa.ui.list;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.data.repository.FavoritoRepositoryImpl;
import com.example.inmobiliariaventa.domain.entity.Favorito;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MisFavoritosFragment extends Fragment {

    private ImageView btnBack;
    private TextView tvContador;
    private LinearLayout btnOrdenar;
    private TextView tvOrdenarLabel;
    private ProgressBar pbFavoritos;
    private LinearLayout layoutVacio;
    private MaterialButton btnExplorar;
    private LinearLayout layoutNoLogin;
    private MaterialButton btnLogin;
    private RecyclerView rvFavoritos;
    private View coordinatorLayout;
    private com.google.android.material.card.MaterialCardView cardModoOfflineFavoritos;

    private FavoritosAdapter adapter;
    private final List<Propiedad> listaOriginal = new ArrayList<>();
    private int criterioOrdenActual = 0; // 0: Reciente, 1: Menor precio, 2: Mayor precio, 3: Más ambientes, 4: Mayor m²

    private final String[] opcionesOrden = {
            "Añadido reciente",
            "Menor precio",
            "Mayor precio",
            "Más ambientes",
            "Mayor superficie (m²)"
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mis_favoritos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        coordinatorLayout = view.findViewById(R.id.coordinator_favoritos);
        btnBack = view.findViewById(R.id.btn_back_favoritos);
        tvContador = view.findViewById(R.id.tv_favoritos_contador);
        btnOrdenar = view.findViewById(R.id.btn_ordenar_favoritos);
        tvOrdenarLabel = view.findViewById(R.id.tv_ordenar_label);
        pbFavoritos = view.findViewById(R.id.pb_favoritos);
        layoutVacio = view.findViewById(R.id.layout_favoritos_vacio);
        btnExplorar = view.findViewById(R.id.btn_explorar_propiedades);
        layoutNoLogin = view.findViewById(R.id.layout_favoritos_no_login);
        btnLogin = view.findViewById(R.id.btn_login_favoritos);
        rvFavoritos = view.findViewById(R.id.rv_mis_favoritos);

        NavController navController = Navigation.findNavController(view);

        // Flecha de retroceso si se llegó desde perfil u otra pantalla
        if (btnBack != null) {
            if (navController.getPreviousBackStackEntry() != null) {
                btnBack.setVisibility(View.VISIBLE);
                btnBack.setOnClickListener(v -> navController.popBackStack());
            } else {
                btnBack.setVisibility(View.GONE);
            }
        }

        // Configurar RecyclerView
        rvFavoritos.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FavoritosAdapter(new ArrayList<>(),
                this::eliminarFavoritoConDeshacer,
                this::abrirDetallePropiedad);
        rvFavoritos.setAdapter(adapter);

        // Botón Explorar si está vacío
        if (btnExplorar != null) {
            btnExplorar.setOnClickListener(v -> {
                if (!navController.popBackStack(R.id.nav_inicio, false)) {
                    navController.navigate(R.id.nav_inicio);
                }
            });
        }

        // Botón Iniciar Sesión si no está autenticado
        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> navController.navigate(R.id.nav_login));
        }

        // Selector de ordenamiento
        if (btnOrdenar != null) {
            btnOrdenar.setOnClickListener(v -> mostrarDialogoOrdenamiento());
        }

        // Modo offline
        cardModoOfflineFavoritos = view.findViewById(R.id.cardModoOfflineFavoritos);
        com.example.inmobiliariaventa.utils.NetworkUtils.getNetworkLiveData(requireContext())
                .observe(getViewLifecycleOwner(), isOnline -> {
                    if (cardModoOfflineFavoritos != null) {
                        cardModoOfflineFavoritos.setVisibility(Boolean.TRUE.equals(isOnline) ? View.GONE : View.VISIBLE);
                    }
                });

        // Cargar favoritos del usuario conectado
        cargarFavoritosUsuario();
    }

    private void cargarFavoritosUsuario() {
        if (getContext() == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("is_user_logged_in", false);
        String userId = prefs.getString("user_id", "");

        if (!isLoggedIn || userId.isEmpty()) {
            if (pbFavoritos != null) pbFavoritos.setVisibility(View.GONE);
            if (rvFavoritos != null) rvFavoritos.setVisibility(View.GONE);
            if (layoutVacio != null) layoutVacio.setVisibility(View.GONE);
            if (btnOrdenar != null) btnOrdenar.setVisibility(View.GONE);
            if (layoutNoLogin != null) layoutNoLogin.setVisibility(View.VISIBLE);
            if (tvContador != null) tvContador.setText("Inicia sesión para ver y gestionar tus favoritos.");
            return;
        }

        if (layoutNoLogin != null) layoutNoLogin.setVisibility(View.GONE);
        if (pbFavoritos != null) pbFavoritos.setVisibility(View.VISIBLE);

        // Observar en tiempo real los favoritos del usuario desde Room
        AppDatabase.getInstance(requireContext())
                .favoritoDao()
                .obtenerFavoritosDeUsuarioLiveData(userId)
                .observe(getViewLifecycleOwner(), propiedades -> {
                    if (pbFavoritos != null) pbFavoritos.setVisibility(View.GONE);

                    listaOriginal.clear();
                    if (propiedades != null) {
                        for (Propiedad p : propiedades) {
                            if (p != null && p.isVisibleParaCliente()) {
                                listaOriginal.add(p);
                            }
                        }
                    }

                    actualizarVistaFavoritos();
                });

        // Sincronizar en tiempo real con Supabase
        FavoritoRepositoryImpl.getInstance(requireContext()).sincronizarConNube(userId, () -> {
            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (pbFavoritos != null) pbFavoritos.setVisibility(View.GONE);
                });
            }
        });
    }

    private void actualizarVistaFavoritos() {
        if (listaOriginal.isEmpty()) {
            if (rvFavoritos != null) rvFavoritos.setVisibility(View.GONE);
            if (btnOrdenar != null) btnOrdenar.setVisibility(View.GONE);
            if (layoutVacio != null) layoutVacio.setVisibility(View.VISIBLE);
            if (tvContador != null) tvContador.setText("No tienes propiedades guardadas todavía.");
            adapter.setPropiedades(new ArrayList<>());
        } else {
            if (layoutVacio != null) layoutVacio.setVisibility(View.GONE);
            if (rvFavoritos != null) rvFavoritos.setVisibility(View.VISIBLE);
            if (btnOrdenar != null) btnOrdenar.setVisibility(View.VISIBLE);

            int total = listaOriginal.size();
            String texto = total == 1
                    ? "Tienes 1 propiedad guardada en tu lista."
                    : "Tienes " + total + " propiedades guardadas en tu lista.";
            if (tvContador != null) tvContador.setText(texto);

            // Aplicar el orden seleccionado
            List<Propiedad> listaOrdenada = new ArrayList<>(listaOriginal);
            ordenarLista(listaOrdenada, criterioOrdenActual);
            adapter.setPropiedades(listaOrdenada);
        }
    }

    private void eliminarFavoritoConDeshacer(Propiedad prop, int position) {
        if (getContext() == null || prop == null || prop.id == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");
        if (userId.isEmpty()) return;

        // Capturamos los IDs de los vecinos anterior y siguiente en la lista actual
        List<Propiedad> listaActual = adapter != null ? adapter.getPropiedades() : listaOriginal;
        String idPrev = (position > 0 && position - 1 < listaActual.size()) ? listaActual.get(position - 1).id : null;
        String idNext = (position + 1 < listaActual.size()) ? listaActual.get(position + 1).id : null;

        final Favorito[] favoritoRespaldo = new Favorito[1];

        // 1. Guardamos el respaldo y calculamos el timestamp exacto para que quede en su misma fila
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Favorito favExistente = AppDatabase.getInstance(requireContext())
                    .favoritoDao().obtenerFavorito(userId, prop.id);

            Favorito respaldo = new Favorito();
            respaldo.usuarioId = userId;
            respaldo.propiedadId = prop.id;
            respaldo.estadoSincronizacion = (favExistente != null && favExistente.estadoSincronizacion != null)
                    ? favExistente.estadoSincronizacion : "SINCRONIZADO";

            long timestampOriginal = favExistente != null ? favExistente.fechaGuardado : 0;

            Long timePrev = idPrev != null
                    ? AppDatabase.getInstance(requireContext()).favoritoDao().obtenerFechaGuardado(userId, idPrev)
                    : null;
            Long timeNext = idNext != null
                    ? AppDatabase.getInstance(requireContext()).favoritoDao().obtenerFechaGuardado(userId, idNext)
                    : null;

            if (timePrev != null && timeNext != null) {
                if (timestampOriginal <= timeNext || timestampOriginal >= timePrev) {
                    respaldo.fechaGuardado = timeNext + (timePrev - timeNext) / 2;
                    if (respaldo.fechaGuardado == timePrev || respaldo.fechaGuardado == timeNext) {
                        respaldo.fechaGuardado = timePrev - 1;
                    }
                } else {
                    respaldo.fechaGuardado = timestampOriginal;
                }
            } else if (timePrev != null) {
                if (timestampOriginal >= timePrev || timestampOriginal <= 0) {
                    respaldo.fechaGuardado = timePrev - 1000;
                } else {
                    respaldo.fechaGuardado = timestampOriginal;
                }
            } else if (timeNext != null) {
                if (timestampOriginal <= timeNext || timestampOriginal <= 0) {
                    respaldo.fechaGuardado = timeNext + 1000;
                } else {
                    respaldo.fechaGuardado = timestampOriginal;
                }
            } else {
                respaldo.fechaGuardado = timestampOriginal > 0 ? timestampOriginal : System.currentTimeMillis();
            }

            favoritoRespaldo[0] = respaldo;

            // Eliminamos de la base de datos local y de Supabase
            FavoritoRepositoryImpl.getInstance(requireContext()).eliminarFavorito(userId, prop.id);
        });

        // 2. Snackbar interactivo integrado con la arquitectura visual de la app
        View targetView = coordinatorLayout != null ? coordinatorLayout : requireView();
        Snackbar snackbar = Snackbar.make(targetView, "Propiedad eliminada de Favoritos", Snackbar.LENGTH_LONG)
                .setAction("DESHACER", v -> {
                    Favorito fav = favoritoRespaldo[0];
                    if (fav != null) {
                        FavoritoRepositoryImpl.getInstance(requireContext()).agregarFavorito(fav);
                    }
                })
                .setActionTextColor(ContextCompat.getColor(requireContext(), R.color.colorSnackbarAction))
                .setTextColor(ContextCompat.getColor(requireContext(), R.color.white));

        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundResource(R.drawable.bg_snackbar_favoritos);
        snackbarView.setBackgroundTintList(null);

        // Flotar estéticamente con márgenes para no pegarse a la barra de navegación
        ViewGroup.LayoutParams lp = snackbarView.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) lp;
            int marginH = (int) (16 * getResources().getDisplayMetrics().density);
            int marginB = (int) (8 * getResources().getDisplayMetrics().density);
            marginParams.setMargins(marginH, marginParams.topMargin, marginH, marginB);
            snackbarView.setLayoutParams(marginParams);
        }
        snackbarView.setElevation(8 * getResources().getDisplayMetrics().density);

        // Estilo negrita para el botón de acción
        TextView actionTextView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_action);
        if (actionTextView != null) {
            actionTextView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        }

        snackbar.show();
    }

    private void abrirDetallePropiedad(Propiedad prop) {
        if (prop == null || prop.id == null) return;
        Bundle bundle = new Bundle();
        bundle.putString("propiedad_id", prop.id);
        try {
            Navigation.findNavController(requireView()).navigate(R.id.nav_detalle_propiedad, bundle);
        } catch (Exception ignored) {}
    }

    private void mostrarDialogoOrdenamiento() {
        if (getContext() == null) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Ordenar propiedades por")
                .setSingleChoiceItems(opcionesOrden, criterioOrdenActual, (dialog, which) -> {
                    criterioOrdenActual = which;
                    if (tvOrdenarLabel != null) {
                        tvOrdenarLabel.setText("Ordenar por: " + opcionesOrden[which]);
                    }
                    actualizarVistaFavoritos();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void ordenarLista(List<Propiedad> lista, int criterio) {
        if (lista == null || lista.isEmpty()) return;
        switch (criterio) {
            case 1: // Menor precio
                Collections.sort(lista, (p1, p2) -> Double.compare(p1.precio, p2.precio));
                break;
            case 2: // Mayor precio
                Collections.sort(lista, (p1, p2) -> Double.compare(p2.precio, p1.precio));
                break;
            case 3: // Más ambientes
                Collections.sort(lista, (p1, p2) -> Integer.compare(p2.ambientes, p1.ambientes));
                break;
            case 4: // Mayor superficie
                Collections.sort(lista, (p1, p2) -> {
                    double s1 = p1.superficieM2 != null ? p1.superficieM2 : 0;
                    double s2 = p2.superficieM2 != null ? p2.superficieM2 : 0;
                    return Double.compare(s2, s1);
                });
                break;
            case 0:
            default:
                // Conserva el orden original devuelto por la consulta Room (ORDER BY f.fechaGuardado DESC)
                break;
        }
    }
}
