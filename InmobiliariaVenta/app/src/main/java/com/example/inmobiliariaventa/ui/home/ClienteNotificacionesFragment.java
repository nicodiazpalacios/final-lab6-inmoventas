package com.example.inmobiliariaventa.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.data.remote.SupabaseClient;
import com.example.inmobiliariaventa.data.repository.ActividadRepository;
import com.example.inmobiliariaventa.domain.entity.MensajeContacto;
import com.example.inmobiliariaventa.domain.entity.Notificacion;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.utils.CartelUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ClienteNotificacionesFragment extends Fragment {

    private LinearLayout layoutNotificacionesContainer;
    private LinearLayout layoutEmptyNotificaciones;
    private TextView tvSubtituloNotificaciones;
    private TextInputEditText etBuscarNotificacion;
    private ChipGroup chipGroupNotif;
    private TextView btnMarcarTodasLeidas;

    private final List<Notificacion> listaNotificaciones = new ArrayList<>();

    public ClienteNotificacionesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cliente_notificaciones, container, false);
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
            btnConfig.setOnClickListener(v -> mostrarDialogoPreferenciasCliente());
        }

        setupSearchAndFilter();

        if (btnMarcarTodasLeidas != null) {
            btnMarcarTodasLeidas.setOnClickListener(v -> marcarTodasComoLeidas());
        }

        cargarNotificacionesCliente();
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

    private void cargarNotificacionesCliente() {
        if (getContext() == null) return;
        Context ctx = requireContext().getApplicationContext();
        SharedPreferences appPrefs = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String userId = appPrefs.getString("user_id", "");
        String userEmail = appPrefs.getString("user_email", "");
        String userNombre = appPrefs.getString("user_nombre", "");

        SharedPreferences notifPrefs = ctx.getSharedPreferences(ActividadRepository.PREF_NOTIF_SETTINGS, Context.MODE_PRIVATE);
        boolean recibirPreciosFav = notifPrefs.getBoolean(ActividadRepository.KEY_CLI_PRECIOS_FAV, true);
        boolean recibirEstadoFav = notifPrefs.getBoolean(ActividadRepository.KEY_CLI_ESTADO_FAV, true);
        boolean recibirRecomendaciones = notifPrefs.getBoolean(ActividadRepository.KEY_CLI_RECOMENDACIONES, true);
        boolean recibirConsultas = notifPrefs.getBoolean(ActividadRepository.KEY_CLI_CONSULTAS, true);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(ctx);

            List<Propiedad> misFavoritos = (!userId.isEmpty()) ? db.favoritoDao().obtenerFavoritosDeUsuario(userId) : new ArrayList<>();
            List<MensajeContacto> todosMensajes = db.mensajeContactoDao().getAllMensajesSync();
            List<Propiedad> todasPropiedades = db.propiedadDao().obtenerTodasSync();

            // Las notificaciones siempre se registran en la bandeja/base de datos local
            // para que estén disponibles en el historial de la app. Los switches de la tuerca
            // configuran si el celular alerta activamente o de forma silenciosa.
            List<Notificacion> existentes = db.notificacionDao().obtenerTodasSync();

            // 1. Favoritos
            if (misFavoritos != null) {
                for (Propiedad fav : misFavoritos) {
                    if (fav == null) continue;
                    String idNotifFav = "cli_fav_" + fav.id;
                    boolean yaExiste = false;
                    for (Notificacion n : existentes) {
                        if (idNotifFav.equals(n.id)) {
                            yaExiste = true;
                            break;
                        }
                    }
                    if (!yaExiste) {
                        String estadoFav = fav.estado != null ? fav.estado : "Disponible";
                        double precio = fav.precio;
                        String tit;
                        String desc;
                        String cat = "FAVORITOS";

                        if ("Reservada".equalsIgnoreCase(estadoFav)) {
                            tit = "Tu favorito fue reservado: " + fav.titulo;
                            desc = "Se registró una reserva sobre '" + fav.titulo + "'. Tocá para ver la ficha completa.";
                        } else if ("Pausada".equalsIgnoreCase(estadoFav) || "Vendida".equalsIgnoreCase(estadoFav)) {
                            tit = "Disponibilidad actualizada: " + fav.titulo;
                            desc = "La propiedad '" + fav.titulo + "' ahora figura como " + estadoFav + ".";
                        } else {
                            tit = "Favorito guardado: " + fav.titulo;
                            desc = String.format(Locale.US, "Ubicada en %s • USD %,.0f. Recibirás avisos si cambia su valor o estado.",
                                    fav.ciudad != null ? fav.ciudad : "Santiago del Estero", precio);
                        }

                        Notificacion n = new Notificacion(idNotifFav, userId, tit, desc, System.currentTimeMillis() - 3600000L, false, cat, fav.id);
                        db.notificacionDao().insertar(n);
                    }
                }
            }

            // 2. Consultas
            if (todosMensajes != null) {
                for (MensajeContacto m : todosMensajes) {
                    boolean esDelUsuario = (userEmail != null && !userEmail.isEmpty() && userEmail.equalsIgnoreCase(m.clienteEmail))
                            || (userNombre != null && !userNombre.isEmpty() && m.clienteNombre != null && m.clienteNombre.toLowerCase().contains(userNombre.toLowerCase()));
                    if (esDelUsuario) {
                        String idNotifMsg = "cli_msg_" + m.id;
                        boolean yaExiste = false;
                        for (Notificacion n : existentes) {
                            if (idNotifMsg.equals(n.id)) {
                                yaExiste = true;
                                break;
                            }
                        }
                        if (!yaExiste) {
                            String propTit = m.propiedadTitulo != null ? m.propiedadTitulo : "Propiedad";
                            String tit = m.leido ? "Tu consulta fue revisada" : "Consulta enviada con éxito";
                            String desc = m.leido
                                    ? "El asesor inmobiliario revisó tu mensaje sobre '" + propTit + "'. Pronto se comunicarán con vos."
                                    : "Tu mensaje sobre '" + propTit + "' fue recibido en el sistema y está en revisión.";
                            Notificacion n = new Notificacion(idNotifMsg, userId, tit, desc, System.currentTimeMillis() - 7200000L, m.leido, "CONSULTAS", m.propiedadId);
                            db.notificacionDao().insertar(n);
                        }
                    }
                }
            }

            // 3. Recomendaciones cerca de tu ubicación
            if (todasPropiedades != null) {
                int countRec = 0;
                for (Propiedad p : todasPropiedades) {
                    if (p == null || !"Disponible".equalsIgnoreCase(p.estado)) continue;
                    boolean yaFav = false;
                    if (misFavoritos != null) {
                        for (Propiedad f : misFavoritos) {
                            if (f != null && f.id != null && f.id.equals(p.id)) {
                                yaFav = true;
                                break;
                            }
                        }
                    }
                    if (yaFav) continue;

                    String idNotifRec = "cli_rec_" + p.id;
                    boolean yaExiste = false;
                    for (Notificacion n : existentes) {
                        if (idNotifRec.equals(n.id)) {
                            yaExiste = true;
                            break;
                        }
                    }
                    if (!yaExiste) {
                        String ciudad = p.ciudad != null && !p.ciudad.isEmpty() ? p.ciudad : "Santiago del Estero";
                        String tit = "Recomendada cerca tuyo: " + p.titulo;
                        String desc = String.format(Locale.US, "📍 Ubicada en %s (%s) • USD %,.0f. Tocá para ver fotos y detalles.",
                                ciudad, p.domicilio != null ? p.domicilio : "Zona céntrica", p.precio);
                        Notificacion n = new Notificacion(idNotifRec, userId, tit, desc, System.currentTimeMillis() - 10800000L, false, "UBICACION", p.id);
                        db.notificacionDao().insertar(n);
                        countRec++;
                        if (countRec >= 3) break;
                    }
                }
            }

            // Consultar la lista final de notificaciones de cliente
            List<Notificacion> actualizadas = db.notificacionDao().obtenerTodasSync();
            List<Notificacion> paraCliente = new ArrayList<>();
            if (actualizadas != null) {
                for (Notificacion n : actualizadas) {
                    String cat = n.categoria != null ? n.categoria.toUpperCase(Locale.ROOT) : "";
                    if (cat.contains("FAVORITO") || cat.contains("CONSULTA") || cat.contains("UBICACION")
                            || cat.contains("RECOMEND") || (n.usuario_id != null && n.usuario_id.equals(userId))) {
                        paraCliente.add(n);
                    }
                }
            }

            if (!isAdded() || getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                listaNotificaciones.clear();
                listaNotificaciones.addAll(paraCliente);
                renderizarListaFiltrada();
            });
        });
    }

    private void marcarTodasComoLeidas() {
        if (getContext() == null) return;
        Context appCtx = requireContext().getApplicationContext();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(appCtx);
            for (Notificacion n : listaNotificaciones) {
                if (!n.isLeida) {
                    db.notificacionDao().marcarComoLeida(n.id);
                    n.isLeida = true;
                }
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(this::renderizarListaFiltrada);
            }
        });
        CartelUtils.mostrar(layoutNotificacionesContainer, "Todas las notificaciones marcadas como leídas");
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

        for (Notificacion n : listaNotificaciones) {
            if (!n.isLeida) noLeidasTotal++;

            String cat = (n.categoria != null && !n.categoria.trim().isEmpty())
                    ? n.categoria.trim().toUpperCase(Locale.ROOT)
                    : "NOTIFICACIÓN";

            boolean matchesCategory = true;
            if (checkedId == R.id.chip_no_leidas) {
                matchesCategory = !n.isLeida;
            } else if (checkedId == R.id.chip_favoritos) {
                matchesCategory = cat.contains("FAVORITO");
            } else if (checkedId == R.id.chip_consultas) {
                matchesCategory = cat.contains("CONSULTA") || cat.contains("LEAD") || cat.contains("MENSAJE");
            } else if (checkedId == R.id.chip_recomendaciones) {
                matchesCategory = cat.contains("UBICACION") || cat.contains("RECOMEND");
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
                tvSubtituloNotificaciones.setText("Tienes " + noLeidasTotal + " alerta(s) sin leer • Total: " + listaNotificaciones.size());
            } else {
                tvSubtituloNotificaciones.setText("Al día • " + listaNotificaciones.size() + " aviso(s) en tu cuenta");
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

    private void bindNotificacionCard(View cardView, Notificacion notif) {
        MaterialCardView cardRoot = cardView.findViewById(R.id.card_notificacion_root);
        ImageView ivIcono = cardView.findViewById(R.id.iv_icono_notif);
        TextView tvCategoria = cardView.findViewById(R.id.tv_notif_categoria);
        TextView tvEstado = cardView.findViewById(R.id.tv_notif_estado);
        TextView tvFecha = cardView.findViewById(R.id.tv_notif_fecha);
        TextView tvTitulo = cardView.findViewById(R.id.tv_notif_titulo);
        TextView tvMensaje = cardView.findViewById(R.id.tv_notif_mensaje);
        View btnDismiss = cardView.findViewById(R.id.btn_notif_dismiss);

        String cat = notif.categoria != null ? notif.categoria.toUpperCase(Locale.ROOT) : "GENERAL";
        if (cat.contains("FAVORITO")) {
            ivIcono.setImageResource(R.drawable.ic_propiedades);
            tvCategoria.setText("FAVORITOS");
        } else if (cat.contains("CONSULTA") || cat.contains("MENSAJE")) {
            ivIcono.setImageResource(R.drawable.ic_notifications);
            tvCategoria.setText("CONSULTAS");
        } else {
            ivIcono.setImageResource(R.drawable.ic_home);
            tvCategoria.setText("CERCA TUYO");
        }

        tvTitulo.setText(notif.titulo != null ? notif.titulo : "Notificación");
        tvMensaje.setText(notif.mensaje != null ? notif.mensaje : "");
        tvFecha.setText(formatearTiempoRelativo(notif.fecha));

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
            Context appCtx = requireContext().getApplicationContext();
            AppDatabase.databaseWriteExecutor.execute(() -> {
                AppDatabase.getInstance(appCtx).notificacionDao().eliminarPorId(notif.id);
            });
            listaNotificaciones.remove(notif);
            renderizarListaFiltrada();
            SupabaseClient.eliminarNotificacion(notif.id, null);
            CartelUtils.mostrar(this, "Notificación eliminada");
        });

        cardRoot.setOnClickListener(v -> {
            if (!notif.isLeida) {
                notif.isLeida = true;
                Context appCtx = requireContext().getApplicationContext();
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    AppDatabase.getInstance(appCtx).notificacionDao().marcarComoLeida(notif.id);
                });
                renderizarListaFiltrada();
            }
            if (notif.referencia_id != null && !notif.referencia_id.isEmpty()) {
                try {
                    Bundle bundle = new Bundle();
                    bundle.putString("propiedad_id", notif.referencia_id);
                    Navigation.findNavController(v).navigate(R.id.nav_detalle_propiedad, bundle);
                } catch (Exception ignored) {}
            }
        });
    }

    private void mostrarDialogoPreferenciasCliente() {
        if (!isAdded() || getContext() == null) return;
        Context context = requireContext();
        SharedPreferences prefs = context.getSharedPreferences(ActividadRepository.PREF_NOTIF_SETTINGS, Context.MODE_PRIVATE);

        float density = context.getResources().getDisplayMetrics().density;
        int pad22 = Math.round(22 * density);
        int pad12 = Math.round(12 * density);
        int margin16 = Math.round(16 * density);

        FrameLayout wrapper = new FrameLayout(context);
        MaterialCardView cardRoot = new MaterialCardView(context);
        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(
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
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
        root.addView(tvTitle);

        TextView tvSub = new TextView(context);
        tvSub.setText("Elegí qué alertas querés que te lleguen a tu cuenta (podés activar o pausar la recepción de cada tipo):");
        tvSub.setTextSize(13f);
        tvSub.setTextColor(ContextCompat.getColor(context, R.color.colorOnSurfaceVariant));
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = Math.round(4 * density);
        subLp.bottomMargin = pad12;
        tvSub.setLayoutParams(subLp);
        root.addView(tvSub);

        List<SwitchMaterial> switchList = new ArrayList<>();
        List<String> keyList = new ArrayList<>();

        MaterialButton btnToggleTodas = new MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        LinearLayout.LayoutParams toggleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.round(42 * density));
        toggleLp.bottomMargin = pad12;
        btnToggleTodas.setLayoutParams(toggleLp);
        btnToggleTodas.setCornerRadius(Math.round(12 * density));
        btnToggleTodas.setStrokeWidth(Math.round(1.2f * density));
        btnToggleTodas.setInsetTop(0);
        btnToggleTodas.setInsetBottom(0);
        btnToggleTodas.setTextSize(12.5f);
        btnToggleTodas.setTypeface(null, Typeface.BOLD);
        btnToggleTodas.setIconPadding(Math.round(8 * density));
        btnToggleTodas.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);

        Runnable actualizarBotonToggle = () -> {
            boolean hayAlgunaActiva = false;
            for (SwitchMaterial sw : switchList) {
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
            for (SwitchMaterial sw : switchList) {
                if (sw.isChecked()) {
                    hayAlgunaActiva = true;
                    break;
                }
            }
            boolean nuevoEstado = !hayAlgunaActiva;
            SharedPreferences.Editor editor = prefs.edit();
            for (int i = 0; i < switchList.size(); i++) {
                SwitchMaterial sw = switchList.get(i);
                sw.setChecked(nuevoEstado);
                if (i < keyList.size()) {
                    editor.putBoolean(keyList.get(i), nuevoEstado);
                }
            }
            editor.apply();
            actualizarBotonToggle.run();
        });

        root.addView(btnToggleTodas);

        ScrollView scrollView = new ScrollView(context);
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(list);

        keyList.add(ActividadRepository.KEY_CLI_PRECIOS_FAV);
        switchList.add(agregarFilaSwitch(context, list, prefs,
                ActividadRepository.KEY_CLI_PRECIOS_FAV,
                "Aumentos y bajas de precio",
                "Recibir alertas cuando cambia el precio en USD de tus favoritos.",
                actualizarBotonToggle));

        keyList.add(ActividadRepository.KEY_CLI_ESTADO_FAV);
        switchList.add(agregarFilaSwitch(context, list, prefs,
                ActividadRepository.KEY_CLI_ESTADO_FAV,
                "Estado de mis favoritos",
                "Recibir alertas cuando un favorito se reserva, pausa o libera.",
                actualizarBotonToggle));

        keyList.add(ActividadRepository.KEY_CLI_REACTIVACION_FAV);
        switchList.add(agregarFilaSwitch(context, list, prefs,
                ActividadRepository.KEY_CLI_REACTIVACION_FAV,
                "Favoritos disponibles nuevamente",
                "Avisos cuando una propiedad que guardaste vuelve a publicarse activa en el catálogo.",
                actualizarBotonToggle));

        keyList.add(ActividadRepository.KEY_CLI_RECOMENDACIONES);
        switchList.add(agregarFilaSwitch(context, list, prefs,
                ActividadRepository.KEY_CLI_RECOMENDACIONES,
                "Recomendaciones cerca tuyo",
                "Recibir avisos de nuevas propiedades disponibles en tu zona.",
                actualizarBotonToggle));

        keyList.add(ActividadRepository.KEY_CLI_CONSULTAS);
        switchList.add(agregarFilaSwitch(context, list, prefs,
                ActividadRepository.KEY_CLI_CONSULTAS,
                "Estado de mis consultas",
                "Recibir confirmación cuando el asesor inmobiliario lee tus mensajes.",
                actualizarBotonToggle));

        actualizarBotonToggle.run();

        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.round(350 * density));
        root.addView(scrollView, scrollLp);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(wrapper)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        MaterialButton btnListo = new MaterialButton(context);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.round(46 * density));
        btnLp.topMargin = Math.round(14 * density);
        btnListo.setLayoutParams(btnLp);
        btnListo.setText("Listo");
        btnListo.setTextSize(13f);
        btnListo.setTypeface(null, Typeface.BOLD);
        btnListo.setCornerRadius(Math.round(12 * density));
        btnListo.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.colorPrimary));
        btnListo.setTextColor(ContextCompat.getColor(context, R.color.colorOnPrimary));
        btnListo.setOnClickListener(v -> dialog.dismiss());
        root.addView(btnListo);

        cardRoot.addView(root);
        wrapper.addView(cardRoot);
        dialog.show();
    }

    private SwitchMaterial agregarFilaSwitch(
            Context context,
            LinearLayout parent,
            SharedPreferences prefs,
            String key,
            String titulo,
            String subtitulo,
            Runnable onStateChanged) {

        float density = context.getResources().getDisplayMetrics().density;
        int padV = Math.round(10 * density);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, padV, 0, padV);

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        colLp.setMarginEnd(Math.round(12 * density));
        textCol.setLayoutParams(colLp);

        TextView tvTit = new TextView(context);
        tvTit.setText(titulo);
        tvTit.setTextSize(14f);
        tvTit.setTypeface(null, Typeface.BOLD);
        tvTit.setTextColor(ContextCompat.getColor(context, R.color.colorOnSurface));
        textCol.addView(tvTit);

        TextView tvSub = new TextView(context);
        tvSub.setText(subtitulo);
        tvSub.setTextSize(12f);
        tvSub.setTextColor(ContextCompat.getColor(context, R.color.colorOnSurfaceVariant));
        textCol.addView(tvSub);

        SwitchMaterial sw = new SwitchMaterial(context);
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
}
