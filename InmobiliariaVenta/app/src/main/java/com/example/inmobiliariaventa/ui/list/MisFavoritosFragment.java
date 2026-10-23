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

        // Configurar RecyclerView con reciclaje fluido de vistas
        rvFavoritos.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFavoritos.setHasFixedSize(true);
        rvFavoritos.setItemViewCacheSize(6);
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
        Context context = requireContext();
        float density = context.getResources().getDisplayMetrics().density;
        int pad24 = Math.round(24 * density);
        int pad14 = Math.round(14 * density);
        int margin20 = Math.round(20 * density);

        android.widget.FrameLayout wrapper = new android.widget.FrameLayout(context);
        com.google.android.material.card.MaterialCardView cardRoot =
                new com.google.android.material.card.MaterialCardView(context);
        android.widget.FrameLayout.LayoutParams cardLp = new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(margin20, margin20, margin20, margin20);
        cardRoot.setLayoutParams(cardLp);
        cardRoot.setRadius(24 * density);
        cardRoot.setCardElevation(8 * density);
        cardRoot.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorSurfaceContainerLowest));

        LinearLayout inner = new LinearLayout(context);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(pad24, pad24, pad24, pad24);

        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerLp.bottomMargin = Math.round(16 * density);
        headerRow.setLayoutParams(headerLp);

        com.google.android.material.card.MaterialCardView iconCircle =
                new com.google.android.material.card.MaterialCardView(context);
        int s44 = Math.round(44 * density);
        iconCircle.setLayoutParams(new LinearLayout.LayoutParams(s44, s44));
        iconCircle.setRadius(s44 / 2f);
        iconCircle.setCardElevation(0f);
        iconCircle.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimaryFixed));

        ImageView ivIcon = new ImageView(context);
        int s22 = Math.round(22 * density);
        android.widget.FrameLayout.LayoutParams ivLp = new android.widget.FrameLayout.LayoutParams(
                s22, s22, android.view.Gravity.CENTER);
        ivIcon.setLayoutParams(ivLp);
        ivIcon.setImageResource(R.drawable.ic_tune);
        ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary));
        iconCircle.addView(ivIcon);
        headerRow.addView(iconCircle);

        LinearLayout titleCol = new LinearLayout(context);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleColLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleColLp.setMarginStart(pad14);
        titleCol.setLayoutParams(titleColLp);

        TextView tvTitle = new TextView(context);
        tvTitle.setText("Ordenar favoritos");
        tvTitle.setTextSize(19f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(ContextCompat.getColor(context, R.color.colorOnSurface));
        titleCol.addView(tvTitle);

        TextView tvSub = new TextView(context);
        tvSub.setText("Elegí cómo organizar tus propiedades guardadas");
        tvSub.setTextSize(12f);
        tvSub.setTextColor(ContextCompat.getColor(context, R.color.colorOnSurfaceVariant));
        titleCol.addView(tvSub);
        headerRow.addView(titleCol);
        inner.addView(headerRow);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(wrapper)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        for (int i = 0; i < opcionesOrden.length; i++) {
            final int index = i;
            boolean isSelected = (i == criterioOrdenActual);

            com.google.android.material.card.MaterialCardView optCard =
                    new com.google.android.material.card.MaterialCardView(context);
            LinearLayout.LayoutParams optLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            optLp.bottomMargin = Math.round(8 * density);
            optCard.setLayoutParams(optLp);
            optCard.setRadius(12 * density);
            optCard.setCardElevation(0f);
            optCard.setStrokeWidth(Math.max(1, Math.round((isSelected ? 1.5f : 1f) * density)));
            optCard.setStrokeColor(ContextCompat.getColor(context,
                    isSelected ? R.color.colorPrimary : R.color.colorOutlineVariant));
            optCard.setCardBackgroundColor(ContextCompat.getColor(context,
                    isSelected ? R.color.colorPrimaryFixed : R.color.colorSurface));

            TextView tvOpt = new TextView(context);
            tvOpt.setPadding(pad14, Math.round(12 * density), pad14, Math.round(12 * density));
            tvOpt.setText((isSelected ? "✓  " : "") + opcionesOrden[i]);
            tvOpt.setTextSize(14f);
            tvOpt.setTypeface(null, isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            tvOpt.setTextColor(ContextCompat.getColor(context,
                    isSelected ? R.color.colorOnPrimaryFixed : R.color.colorOnSurface));
            optCard.addView(tvOpt);

            optCard.setOnClickListener(v -> {
                criterioOrdenActual = index;
                if (tvOrdenarLabel != null) {
                    tvOrdenarLabel.setText("Ordenar por: " + opcionesOrden[index]);
                }
                actualizarVistaFavoritos();
                dialog.dismiss();
            });
            inner.addView(optCard);
        }

        MaterialButton btnCancelar = new MaterialButton(context, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.round(46 * density));
        btnLp.topMargin = Math.round(8 * density);
        btnCancelar.setLayoutParams(btnLp);
        btnCancelar.setText("Cancelar");
        btnCancelar.setTextSize(13f);
        btnCancelar.setTypeface(null, android.graphics.Typeface.BOLD);
        btnCancelar.setCornerRadius(Math.round(12 * density));
        btnCancelar.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.colorSurfaceContainerHigh));
        btnCancelar.setTextColor(ContextCompat.getColor(context, R.color.colorOnSurface));
        btnCancelar.setStrokeWidth(0);
        btnCancelar.setOnClickListener(v -> dialog.dismiss());
        inner.addView(btnCancelar);

        cardRoot.addView(inner);
        wrapper.addView(cardRoot);
        dialog.show();
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
