package com.example.inmobiliariaventa.ui.admin;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.data.remote.SupabaseClient;
import com.example.inmobiliariaventa.data.repository.ActividadRepository;
import com.example.inmobiliariaventa.domain.entity.Notificacion;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminNotificacionesFragment extends Fragment {

    private LinearLayout layoutNotificacionesContainer;
    private LinearLayout layoutEmptyNotificaciones;
    private TextView tvSubtituloNotificaciones;
    private TextInputEditText etBuscarNotificacion;
    private ChipGroup chipGroupNotif;
    private TextView btnMarcarTodasLeidas;

    private final List<Notificacion> listaCompleta = new ArrayList<>();

    public AdminNotificacionesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_notificaciones, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        layoutNotificacionesContainer = view.findViewById(R.id.layout_notificaciones_container);
        layoutEmptyNotificaciones = view.findViewById(R.id.layout_empty_notificaciones);
        tvSubtituloNotificaciones = view.findViewById(R.id.tv_subtitulo_notificaciones);
        etBuscarNotificacion = view.findViewById(R.id.et_buscar_notificacion);
        chipGroupNotif = view.findViewById(R.id.chip_group_notif);
        btnMarcarTodasLeidas = view.findViewById(R.id.btn_marcar_todas_leidas);

        View btnBack = view.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());
        }

        View btnConfig = view.findViewById(R.id.btn_config_notificaciones);
        if (btnConfig != null) {
            btnConfig.setOnClickListener(v -> mostrarDialogoPreferenciasAdmin());
        }

        setupSearchAndFilter();

        if (btnMarcarTodasLeidas != null) {
            btnMarcarTodasLeidas.setOnClickListener(v -> marcarTodasComoLeidas());
        }

        AppDatabase db = AppDatabase.getInstance(requireContext());
        db.notificacionDao().obtenerTodasLiveData().observe(getViewLifecycleOwner(), notificaciones -> {
            listaCompleta.clear();
            if (notificaciones != null) {
                listaCompleta.addAll(notificaciones);
            }
            renderizarListaFiltrada();
        });

        ActividadRepository.getInstance(requireContext()).sincronizarNotificacionesYActividades(null);
    }

    private void setupSearchAndFilter() {
        if (etBuscarNotificacion != null) {
            etBuscarNotificacion.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    renderizarListaFiltrada();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (chipGroupNotif != null) {
            chipGroupNotif.setOnCheckedChangeListener((group, checkedId) -> renderizarListaFiltrada());
        }
    }

    private void marcarTodasComoLeidas() {
        if (getContext() == null) return;
        android.content.Context appCtx = requireContext().getApplicationContext();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase.getInstance(appCtx).notificacionDao().marcarTodasComoLeidas();
        });
        SupabaseClient.marcarTodasNotificacionesLeidas(null);
        com.example.inmobiliariaventa.utils.CartelUtils.mostrar(layoutNotificacionesContainer, "Todas las notificaciones marcadas como leídas");
    }

    private void renderizarListaFiltrada() {
        if (!isAdded() || getContext() == null || layoutNotificacionesContainer == null) return;

        String query = "";
        if (etBuscarNotificacion != null && etBuscarNotificacion.getText() != null) {
            query = etBuscarNotificacion.getText().toString().trim().toLowerCase(Locale.ROOT);
        }

        int checkedId = chipGroupNotif != null ? chipGroupNotif.getCheckedChipId() : R.id.chip_todas;

        List<Notificacion> filtradas = new ArrayList<>();
        int noLeidasTotal = 0;

        for (Notificacion n : listaCompleta) {
            if (!n.isLeida) noLeidasTotal++;

            String cat = (n.categoria != null && !n.categoria.trim().isEmpty())
                    ? n.categoria.trim().toUpperCase(Locale.ROOT)
                    : inferirCategoria(n);

            boolean matchesCategory = true;
            if (checkedId == R.id.chip_no_leidas) {
                matchesCategory = !n.isLeida;
            } else if (checkedId == R.id.chip_contratos) {
                matchesCategory = cat.contains("CONTRATO");
            } else if (checkedId == R.id.chip_propiedades) {
                matchesCategory = cat.contains("PROPIEDAD") || cat.contains("FAVORITO") || cat.contains("UBICACION");
            } else if (checkedId == R.id.chip_leads) {
                matchesCategory = cat.contains("LEAD") || cat.contains("MENSAJE") || cat.contains("CONSULTA");
            }

            if (!matchesCategory) continue;

            if (!query.isEmpty()) {
                String t = n.titulo != null ? n.titulo.toLowerCase(Locale.ROOT) : "";
                String m = n.mensaje != null ? n.mensaje.toLowerCase(Locale.ROOT) : "";
                String c = cat.toLowerCase(Locale.ROOT);
                if (!t.contains(query) && !m.contains(query) && !c.contains(query)) {
                    continue;
                }
            }

            filtradas.add(n);
        }

        if (tvSubtituloNotificaciones != null) {
            if (noLeidasTotal > 0) {
                tvSubtituloNotificaciones.setText("Tienes " + noLeidasTotal + " alerta(s) sin leer • Total: " + listaCompleta.size());
            } else {
                tvSubtituloNotificaciones.setText("Al día • " + listaCompleta.size() + " notificación(es) registradas en el sistema");
            }
        }

        layoutNotificacionesContainer.removeAllViews();

        if (filtradas.isEmpty()) {
            layoutNotificacionesContainer.setVisibility(View.GONE);
            if (layoutEmptyNotificaciones != null) layoutEmptyNotificaciones.setVisibility(View.VISIBLE);
            return;
        }

        layoutNotificacionesContainer.setVisibility(View.VISIBLE);
        if (layoutEmptyNotificaciones != null) layoutEmptyNotificaciones.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (Notificacion notif : filtradas) {
            View itemView = inflater.inflate(R.layout.item_notificacion_card, layoutNotificacionesContainer, false);
            bindNotificacionCard(itemView, notif);
            layoutNotificacionesContainer.addView(itemView);
        }
    }

    private String inferirCategoria(Notificacion n) {
        String txt = ((n.titulo != null ? n.titulo : "") + " " + (n.mensaje != null ? n.mensaje : "")).toUpperCase(Locale.ROOT);
        if (txt.contains("CONTRATO")) return "CONTRATOS";
        if (txt.contains("CONSULTA") || txt.contains("MENSAJE") || txt.contains("LEAD")) return "LEADS";
        return "PROPIEDADES";
    }

    private void bindNotificacionCard(View cardView, Notificacion notif) {
        MaterialCardView cardRoot = cardView.findViewById(R.id.card_notificacion_root);
        ImageView ivIcono = cardView.findViewById(R.id.iv_icono_notif);
        TextView tvCategoria = cardView.findViewById(R.id.tv_notif_categoria);
        TextView tvEstado = cardView.findViewById(R.id.tv_notif_estado);
        TextView tvFecha = cardView.findViewById(R.id.tv_notif_fecha);
        TextView tvTitulo = cardView.findViewById(R.id.tv_notif_titulo);
        TextView tvMensaje = cardView.findViewById(R.id.tv_notif_mensaje);
        View btnDismiss = cardView.findViewById(R.id.btn_notif_dismiss);

        String cat = (notif.categoria != null && !notif.categoria.trim().isEmpty())
                ? notif.categoria.trim().toUpperCase(Locale.ROOT)
                : inferirCategoria(notif);

        tvCategoria.setText(cat);
        tvTitulo.setText(notif.titulo != null ? notif.titulo : "Notificación");
        tvMensaje.setText(notif.mensaje != null ? notif.mensaje : "");
        tvFecha.setText(formatearTiempoRelativo(notif.fecha));

        if (cat.contains("CONTRATO")) {
            ivIcono.setImageResource(R.drawable.ic_contratos);
        } else if (cat.contains("LEAD") || cat.contains("CONSULTA") || cat.contains("MENSAJE")) {
            ivIcono.setImageResource(R.drawable.ic_notifications);
        } else {
            ivIcono.setImageResource(R.drawable.ic_propiedades);
        }

        if (notif.isLeida) {
            tvEstado.setText("LEÍDA");
            tvEstado.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.colorSurfaceVariant));
            tvEstado.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOutline));
        } else {
            tvEstado.setText("NUEVA");
            tvEstado.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#d8f3e5")));
            tvEstado.setTextColor(Color.parseColor("#005236"));
        }

        btnDismiss.setOnClickListener(v -> {
            android.content.Context appCtx = requireContext().getApplicationContext();
            AppDatabase.databaseWriteExecutor.execute(() -> {
                AppDatabase.getInstance(appCtx).notificacionDao().eliminarPorId(notif.id);
            });
            SupabaseClient.eliminarNotificacion(notif.id, null);
            com.example.inmobiliariaventa.utils.CartelUtils.mostrar(cardRoot, "Notificación eliminada");
        });

        cardRoot.setOnClickListener(v -> {
            if (!notif.isLeida) {
                android.content.Context appCtx = requireContext().getApplicationContext();
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    AppDatabase.getInstance(appCtx).notificacionDao().marcarComoLeida(notif.id);
                });
                SupabaseClient.marcarNotificacionLeida(notif.id, null);
            }
            try {
                if (cat.contains("CONTRATO")) {
                    Navigation.findNavController(v).navigate(R.id.nav_contratos);
                } else if (cat.contains("LEAD") || cat.contains("CONSULTA")) {
                    Navigation.findNavController(v).navigate(R.id.nav_admin_mensajes);
                } else {
                    Navigation.findNavController(v).navigate(R.id.nav_admin_propiedades);
                }
            } catch (Exception ignored) {}
        });
    }

    private String formatearTiempoRelativo(Long timestamp) {
        if (timestamp == null || timestamp <= 0) return "Reciente";
        long diffMs = System.currentTimeMillis() - timestamp;
        long min = diffMs / 60000L;
        if (min < 1) return "Hace un momento";
        if (min < 60) return "Hace " + min + " min";
        long horas = min / 60L;
        if (horas < 24) return "Hace " + horas + " h";
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    private void mostrarDialogoPreferenciasAdmin() {
        if (!isAdded() || getContext() == null) return;
        android.content.Context context = requireContext();
        android.content.SharedPreferences prefs = context.getSharedPreferences(
                ActividadRepository.PREF_NOTIF_SETTINGS, android.content.Context.MODE_PRIVATE);

        float density = context.getResources().getDisplayMetrics().density;
        int pad22 = Math.round(22 * density);
        int pad12 = Math.round(12 * density);
        int margin16 = Math.round(16 * density);

        android.widget.FrameLayout wrapper = new android.widget.FrameLayout(context);
        MaterialCardView cardRoot = new MaterialCardView(context);
        android.widget.FrameLayout.LayoutParams cardLp = new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(margin16, margin16, margin16, margin16);
        cardRoot.setLayoutParams(cardLp);
        cardRoot.setRadius(24 * density);
        cardRoot.setCardElevation(8 * density);
        cardRoot.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorSurfaceContainerLowest));

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad22, pad22, pad22, pad22);

        TextView tvTitle = new TextView(context);
        tvTitle.setText("Recepción de Alertas");
        tvTitle.setTextSize(20f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
        root.addView(tvTitle);

        TextView tvSub = new TextView(context);
        tvSub.setText("Elegí qué alertas querés que te lleguen al panel (podés activar o pausar la recepción de cada tipo):");
        tvSub.setTextSize(13f);
        tvSub.setTextColor(ContextCompat.getColor(context, R.color.colorOnSurfaceVariant));
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = Math.round(4 * density);
        subLp.bottomMargin = pad12;
        tvSub.setLayoutParams(subLp);
        root.addView(tvSub);

        List<com.google.android.material.switchmaterial.SwitchMaterial> switchList = new ArrayList<>();
        List<String> keyList = new ArrayList<>();

        com.google.android.material.button.MaterialButton btnToggleTodas =
                new com.google.android.material.button.MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        LinearLayout.LayoutParams toggleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.round(42 * density));
        toggleLp.bottomMargin = pad12;
        btnToggleTodas.setLayoutParams(toggleLp);
        btnToggleTodas.setCornerRadius(Math.round(12 * density));
        btnToggleTodas.setStrokeWidth(Math.round(1.2f * density));
        btnToggleTodas.setInsetTop(0);
        btnToggleTodas.setInsetBottom(0);
        btnToggleTodas.setTextSize(12.5f);
        btnToggleTodas.setTypeface(null, android.graphics.Typeface.BOLD);
        btnToggleTodas.setIconPadding(Math.round(8 * density));
        btnToggleTodas.setIconGravity(com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START);

        Runnable actualizarBotonToggle = () -> {
            boolean hayAlgunaActiva = false;
            for (com.google.android.material.switchmaterial.SwitchMaterial sw : switchList) {
                if (sw.isChecked()) {
                    hayAlgunaActiva = true;
                    break;
                }
            }
            if (hayAlgunaActiva) {
                btnToggleTodas.setText("Cancelar todas las notificaciones");
                btnToggleTodas.setIconResource(R.drawable.ic_visibility_off);
                btnToggleTodas.setTextColor(ContextCompat.getColor(context, R.color.colorError));
                btnToggleTodas.setStrokeColor(ContextCompat.getColorStateList(context, R.color.colorError));
                btnToggleTodas.setIconTint(ContextCompat.getColorStateList(context, R.color.colorError));
            } else {
                btnToggleTodas.setText("Activar todas las notificaciones");
                btnToggleTodas.setIconResource(R.drawable.ic_notifications);
                btnToggleTodas.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
                btnToggleTodas.setStrokeColor(ContextCompat.getColorStateList(context, R.color.colorPrimary));
                btnToggleTodas.setIconTint(ContextCompat.getColorStateList(context, R.color.colorPrimary));
            }
        };

        btnToggleTodas.setOnClickListener(v -> {
            boolean hayAlgunaActiva = false;
            for (com.google.android.material.switchmaterial.SwitchMaterial sw : switchList) {
                if (sw.isChecked()) {
                    hayAlgunaActiva = true;
                    break;
                }
            }
            boolean nuevoEstado = !hayAlgunaActiva;
            android.content.SharedPreferences.Editor editor = prefs.edit();
            for (int i = 0; i < switchList.size(); i++) {
                com.google.android.material.switchmaterial.SwitchMaterial sw = switchList.get(i);
                sw.setChecked(nuevoEstado);
                if (i < keyList.size()) {
                    editor.putBoolean(keyList.get(i), nuevoEstado);
                }
            }
            editor.apply();
            actualizarBotonToggle.run();
        });

        root.addView(btnToggleTodas);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(context);
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(list);

        keyList.add(ActividadRepository.KEY_ADMIN_NUEVO_CONTRATO);
        switchList.add(agregarFilaSwitch(context, list, prefs,
                ActividadRepository.KEY_ADMIN_NUEVO_CONTRATO,
                "Nuevos contratos firmados",
                "Alertas cuando se registra un contrato de compraventa.",
                actualizarBotonToggle));

        keyList.add(ActividadRepository.KEY_ADMIN_MODIF_CONTRATO);
        switchList.add(agregarFilaSwitch(context, list, prefs,
                ActividadRepository.KEY_ADMIN_MODIF_CONTRATO,
                "Cambios de situación en contratos",
                "Avisos cuando un contrato pasa a Activo, Pendiente, Finalizado o Cancelado.",
                actualizarBotonToggle));

        keyList.add(ActividadRepository.KEY_ADMIN_NUEVO_LEAD);
        switchList.add(agregarFilaSwitch(context, list, prefs,
                ActividadRepository.KEY_ADMIN_NUEVO_LEAD,
                "Consultas y leads de clientes",
                "Notificaciones cuando un interesado envía una consulta.",
                actualizarBotonToggle));

        keyList.add(ActividadRepository.KEY_ADMIN_MENSAJE_VISTO);
        switchList.add(agregarFilaSwitch(context, list, prefs,
                ActividadRepository.KEY_ADMIN_MENSAJE_VISTO,
                "Confirmación de consultas leídas",
                "Registro cuando un asesor marca como vista una consulta.",
                actualizarBotonToggle));

        keyList.add(ActividadRepository.KEY_ADMIN_NUEVA_PROPIEDAD);
        switchList.add(agregarFilaSwitch(context, list, prefs,
                ActividadRepository.KEY_ADMIN_NUEVA_PROPIEDAD,
                "Nuevas propiedades publicadas",
                "Avisos de ingreso de nuevos inmuebles al catálogo.",
                actualizarBotonToggle));

        keyList.add(ActividadRepository.KEY_ADMIN_REACTIVAR_PROPIEDAD);
        switchList.add(agregarFilaSwitch(context, list, prefs,
                ActividadRepository.KEY_ADMIN_REACTIVAR_PROPIEDAD,
                "Reactivación de inmuebles",
                "Avisos cuando una propiedad pausada vuelve a activarse en el catálogo.",
                actualizarBotonToggle));

        keyList.add(ActividadRepository.KEY_ADMIN_CAMBIO_PRECIO);
        switchList.add(agregarFilaSwitch(context, list, prefs,
                ActividadRepository.KEY_ADMIN_CAMBIO_PRECIO,
                "Aumentos y bajas de precio",
                "Alertas cuando se actualiza el valor en USD de una propiedad.",
                actualizarBotonToggle));

        keyList.add(ActividadRepository.KEY_ADMIN_BAJA_PROPIEDAD);
        switchList.add(agregarFilaSwitch(context, list, prefs,
                ActividadRepository.KEY_ADMIN_BAJA_PROPIEDAD,
                "Bajas o pausas de propiedades",
                "Avisos cuando un inmueble se pausa, reserva o retira del catálogo.",
                actualizarBotonToggle));

        actualizarBotonToggle.run();

        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.round(350 * density));
        root.addView(scrollView, scrollLp);

        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                .setView(wrapper)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }

        com.google.android.material.button.MaterialButton btnListo =
                new com.google.android.material.button.MaterialButton(context);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.round(46 * density));
        btnLp.topMargin = Math.round(14 * density);
        btnListo.setLayoutParams(btnLp);
        btnListo.setText("Listo");
        btnListo.setTextSize(13f);
        btnListo.setTypeface(null, android.graphics.Typeface.BOLD);
        btnListo.setCornerRadius(Math.round(12 * density));
        btnListo.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.colorPrimary));
        btnListo.setTextColor(ContextCompat.getColor(context, R.color.colorOnPrimary));
        btnListo.setOnClickListener(v -> dialog.dismiss());
        root.addView(btnListo);

        cardRoot.addView(root);
        wrapper.addView(cardRoot);
        dialog.show();
    }

    private com.google.android.material.switchmaterial.SwitchMaterial agregarFilaSwitch(
            android.content.Context context,
            LinearLayout parent,
            android.content.SharedPreferences prefs,
            String key,
            String titulo,
            String subtitulo,
            Runnable onStateChanged) {

        float density = context.getResources().getDisplayMetrics().density;
        int padV = Math.round(10 * density);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, padV, 0, padV);

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        colLp.setMarginEnd(Math.round(12 * density));
        textCol.setLayoutParams(colLp);

        TextView tvTit = new TextView(context);
        tvTit.setText(titulo);
        tvTit.setTextSize(14f);
        tvTit.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTit.setTextColor(ContextCompat.getColor(context, R.color.colorOnSurface));
        textCol.addView(tvTit);

        TextView tvSub = new TextView(context);
        tvSub.setText(subtitulo);
        tvSub.setTextSize(12f);
        tvSub.setTextColor(ContextCompat.getColor(context, R.color.colorOnSurfaceVariant));
        textCol.addView(tvSub);

        com.google.android.material.switchmaterial.SwitchMaterial sw =
                new com.google.android.material.switchmaterial.SwitchMaterial(context);
        sw.setChecked(prefs.getBoolean(key, true));
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(key, isChecked).apply();
            if (onStateChanged != null) {
                onStateChanged.run();
            }
        });

        row.addView(textCol);
        row.addView(sw);
        parent.addView(row);

        View divider = new View(context);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Math.round(density))));
        divider.setBackgroundColor(ContextCompat.getColor(context, R.color.colorOutlineVariant));
        parent.addView(divider);
        return sw;
    }
}
