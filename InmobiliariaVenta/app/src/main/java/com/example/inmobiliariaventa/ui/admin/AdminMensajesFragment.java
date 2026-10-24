package com.example.inmobiliariaventa.ui.admin;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.data.repository.MensajeContactoRepositoryImpl;
import com.example.inmobiliariaventa.domain.entity.MensajeContacto;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class AdminMensajesFragment extends Fragment {

    private int currentPage = 1;
    private int startPage = 1;
    private int maxPages = 1;
    private static final int PAGE_SIZE = 8;

    private static volatile List<MensajeContacto> cachedMensajesGlobal = null;
    private LinearLayout layoutPage1Mensajes;
    private LinearLayout layoutEmptyMensajes;
    private LinearLayout layoutPagination;
    private NestedScrollView scrollViewMensajes;

    private MaterialCardView btnPagePrev, btnPageNext;
    private MaterialCardView cardPage1, cardPage2, cardPage3, cardPage4;
    private TextView tvPagePrev, tvPageNext;
    private TextView tvPage1, tvPage2, tvPage3, tvPage4;

    private TextInputEditText etBuscarMensaje;
    private ChipGroup chipGroupMensajes;
    private Chip chipTodos, chipNoLeidos, chipLeidos, chipFecha;
    private HorizontalScrollView scrollChipsMensajes;

    private String selectedDateFilter = null; // null o formato "dd/MM/yyyy"
    private List<MensajeContacto> listaMensajes = new ArrayList<>();
    private List<MensajeContacto> listaFiltrada = new ArrayList<>();
    private String ultimoTopKey = null;
    private int ultimoTotal = 0;
    private final android.os.Handler renderDebounceHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable renderDebounceRunnable;

    private void scheduleApplyFilters(View root) {
        if (!isAdded() || getContext() == null) return;
        if (renderDebounceRunnable != null) {
            renderDebounceHandler.removeCallbacks(renderDebounceRunnable);
        }
        renderDebounceRunnable = () -> {
            if (isAdded() && getContext() != null) {
                applyFilters(root);
            }
        };
        renderDebounceHandler.postDelayed(renderDebounceRunnable, 80);
    }

    private static String claveMensaje(MensajeContacto m) {
        if (m == null) return null;
        return (m.remoteId != null && !m.remoteId.isEmpty()) ? m.remoteId : ("local_" + m.id);
    }

    public AdminMensajesFragment() {
        // Constructor público vacío requerido
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (renderDebounceHandler != null && renderDebounceRunnable != null) {
            renderDebounceHandler.removeCallbacks(renderDebounceRunnable);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_mensajes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        layoutPage1Mensajes = view.findViewById(R.id.layout_page_1_mensajes);
        if (layoutPage1Mensajes != null) {
            layoutPage1Mensajes.removeAllViews();
        }

        layoutEmptyMensajes = view.findViewById(R.id.layout_empty_mensajes);
        layoutPagination = view.findViewById(R.id.layout_pagination);
        scrollViewMensajes = view.findViewById(R.id.scroll_view_mensajes);

        etBuscarMensaje = view.findViewById(R.id.et_buscar_mensaje);
        chipGroupMensajes = view.findViewById(R.id.chip_group_mensajes);
        chipTodos = view.findViewById(R.id.chip_todos);
        chipNoLeidos = view.findViewById(R.id.chip_no_leidos);
        chipLeidos = view.findViewById(R.id.chip_leidos);
        chipFecha = view.findViewById(R.id.chip_fecha);
        scrollChipsMensajes = view.findViewById(R.id.scroll_chips_mensajes);

        // Flecha de regreso segura
        View btnBack = view.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (isAdded()) {
                    try {
                        Navigation.findNavController(v).popBackStack();
                    } catch (Exception ignored) { }
                }
            });
        }

        setupSearchAndFilters(view);
        setupDatePicker(view);
        setupPagination(view);

        // Renderizado instantáneo desde memoria si ya hay mensajes cacheados
        if (cachedMensajesGlobal != null && !cachedMensajesGlobal.isEmpty()) {
            listaMensajes = new ArrayList<>(cachedMensajesGlobal);
            applyFilters(view);
        }

        // Inicializar base de datos y observar mensajes
        initDatabaseAndObserve(view);
    }

    private void initDatabaseAndObserve(View root) {
        if (getContext() == null) return;
        final Context appContext = requireContext().getApplicationContext();
        MensajeContactoRepositoryImpl repository = MensajeContactoRepositoryImpl.getInstance(appContext);

        // Observar mensajes en tiempo real desde Room
        repository.getAllMensajesLiveData().observe(getViewLifecycleOwner(), mensajes -> {
            if (mensajes != null) {
                listaMensajes = new ArrayList<>(mensajes);
                ordenarMensajesPorMasRecientes(listaMensajes);
                cachedMensajesGlobal = new ArrayList<>(listaMensajes);

                // Si llegó un mensaje nuevo (cambió el primero de la lista), volver a la página 1 y al tope
                String nuevoTopKey = !listaMensajes.isEmpty() ? claveMensaje(listaMensajes.get(0)) : null;
                boolean llegoNuevo = ultimoTopKey != null && nuevoTopKey != null
                        && !nuevoTopKey.equals(ultimoTopKey)
                        && listaMensajes.size() > ultimoTotal;
                ultimoTopKey = nuevoTopKey;
                ultimoTotal = listaMensajes.size();
                if (llegoNuevo) {
                    currentPage = 1;
                    startPage = 1;
                }

                // Renderizar inmediatamente sin debounce para eliminar parpadeo o demoras
                applyFilters(root);

                if (llegoNuevo && scrollViewMensajes != null) {
                    scrollViewMensajes.post(() -> scrollViewMensajes.smoothScrollTo(0, 0));
                }
            }
        });

        // Sincronizar en segundo plano con Supabase para traer mensajes enviados por los clientes
        repository.sincronizarConNube(() -> {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                AppDatabase db = AppDatabase.getInstance(appContext);
                if (db.mensajeContactoDao().countMensajesSync() == 0) {
                    List<MensajeContacto> seed = createInitialDemoMessages();
                    db.mensajeContactoDao().insertAll(seed);
                }
            });
        });
    }

    private List<MensajeContacto> createInitialDemoMessages() {
        List<MensajeContacto> list = new ArrayList<>();
        long now = System.currentTimeMillis();
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        SimpleDateFormat tf = new SimpleDateFormat("hh:mm a", Locale.US);

        long[] offsets = new long[] {
                86400000L * 3 + 3600000L * 2,
                86400000L * 3 + 3600000L * 4,
                86400000L * 3 + 3600000L * 6,
                86400000L * 4 + 3600000L * 1,
                86400000L * 4 + 3600000L * 3,
                86400000L * 4 + 3600000L * 5,
                86400000L * 5 + 3600000L * 2,
                86400000L * 5 + 3600000L * 4,
                86400000L * 6 + 3600000L * 2,
                86400000L * 6 + 3600000L * 5,
                86400000L * 7 + 3600000L * 3,
                86400000L * 7 + 3600000L * 6
        };

        String[] nombres = new String[] {
                "Lucas Martínez", "Sofía Benítez", "Gonzalo Fernández", "Valentina Rossi",
                "Camila Gómez", "Mateo Rodríguez", "Florencia Díaz", "Emiliano Morales",
                "Lucía Navarro", "Diego Castro", "Martina López", "Agustín Silva"
        };
        String[] tels = new String[] {
                "+54 9 11 4821-3390", "+54 9 11 6729-1144", "+54 9 11 3902-8812", "+54 9 11 5519-7032",
                "+54 9 11 2341-9980", "+54 9 11 7890-4421", "+54 9 11 8912-3301", "+54 9 11 4110-8765",
                "+54 9 11 5022-7711", "+54 9 11 3198-4455", "+54 9 11 6201-9933", "+54 9 11 7734-2210"
        };
        String[] emails = new String[] {
                "lucas.m@gmail.com", "sofi.benitez@hotmail.com", "gonzalo.f@gmail.com", "valen.rossi@yahoo.com",
                "camilag@gmail.com", "mateo.rod@gmail.com", "flor.diaz@outlook.com", "emiliano.m@gmail.com",
                "lucia.navarro@gmail.com", "diego.castro@gmail.com", "martu.lopez@gmail.com", "agustin.silva@gmail.com"
        };
        String[] titulos = new String[] {
                "Villa Los Laureles (San Isidro)", "Penthouse Lumina (Puerto Madero)", "Torre Bellini Dpto 14B (Palermo)",
                "Casa Moderna con Piscina (Pilar)", "Dúplex de Diseño (Belgrano R)", "Residencia Quinta Real (Tigre)",
                "Loft Industrial Tech (Colegiales)", "Semipiso con Terraza (Recoleta)", "Casa Country Club (Nordelta)",
                "Departamento 3 Ambientes (Caballito)", "Chalet Tradicional (Olivos)", "Monoambiente Premium (Palermo Soho)"
        };
        String[] ubicaciones = new String[] {
                "San Isidro, Buenos Aires", "Puerto Madero, Buenos Aires", "Palermo, Buenos Aires",
                "Pilar, Buenos Aires", "Belgrano R, Buenos Aires", "Tigre, Buenos Aires",
                "Colegiales, Buenos Aires", "Recoleta, Buenos Aires", "Nordelta, Buenos Aires",
                "Caballito, Buenos Aires", "Olivos, Buenos Aires", "Palermo Soho, Buenos Aires"
        };
        String[] textos = new String[] {
                "Hola, me interesa conocer las condiciones de financiación y si aceptan propiedad menor valor en parte de pago. Quisiera coordinar una visita para este sábado por la mañana.",
                "Buenos días, quería consultar las expensas mensuales y si cuenta con dos cocheras cubiertas. Tengo disponibilidad para visitarlo esta semana.",
                "Hola! ¿El departamento es apto crédito hipotecario bancario? Estoy muy interesado y con pre-aprobación del Banco Nación lista.",
                "Buenas tardes, quisiera saber el costo aproximado de mantenimiento del jardín y piscina, y si el barrio cerrado cuenta con amenities habilitados.",
                "Hola, me gustaría saber si la posesión es inmediata o si actualmente se encuentra alquilado. Saludos.",
                "Hola! ¿Tiene amarra propia para embarcación? Me gustaría agendar una visita presencial con mi familia.",
                "Hola, ¿es apto profesional? Busco un espacio para estudio de arquitectura y vivienda combinada.",
                "Buen día, quisiera recibir el plano detallado de la propiedad y saber la orientación solar de la terraza.",
                "Hola, ¿aceptan permuta por departamento en Vicente López? Quisiéramos mudarnos antes de fin de año.",
                "Hola, ¿las expensas incluyen el agua de Aysa? Me interesa coordinar visita para este viernes por la tarde.",
                "Buenas tardes, ¿la casa tiene calefacción central por radiadores? Estamos buscando mudarnos a zona norte.",
                "¿Está permitido el alquiler temporal tipo Airbnb en el reglamento de copropiedad? Busco para inversión."
        };
        boolean[] leidos = new boolean[] {
                false, false, false, false, true, true, false, true, false, true, true, false
        };

        for (int i = 0; i < 12; i++) {
            long itemTs = now - offsets[i];
            Date itemDate = new Date(itemTs);
            MensajeContacto item = new MensajeContacto(
                    nombres[i], tels[i], emails[i],
                    String.valueOf(i + 1), titulos[i], ubicaciones[i],
                    textos[i], df.format(itemDate), tf.format(itemDate), itemTs, leidos[i]
            );
            item.estadoSincronizacion = "DEMO";
            list.add(item);
        }

        return list;
    }

    private void renderMensajesList(View root) {
        if (!isAdded() || getContext() == null || layoutPage1Mensajes == null) return;

        LayoutInflater inflater = LayoutInflater.from(requireContext());

        if (listaFiltrada.isEmpty()) {
            layoutPage1Mensajes.removeAllViews();
            if (layoutEmptyMensajes != null) layoutEmptyMensajes.setVisibility(View.VISIBLE);
            if (layoutPagination != null) layoutPagination.setVisibility(View.GONE);
            return;
        }

        if (layoutEmptyMensajes != null) layoutEmptyMensajes.setVisibility(View.GONE);

        // Calcular paginación
        maxPages = (int) Math.ceil((double) listaFiltrada.size() / PAGE_SIZE);
        if (maxPages < 1) maxPages = 1;
        if (currentPage > maxPages) currentPage = maxPages;

        int startIndex = (currentPage - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, listaFiltrada.size());

        int targetCount = endIndex - startIndex;
        int currentCount = layoutPage1Mensajes.getChildCount();
        if (currentCount > targetCount) {
            layoutPage1Mensajes.removeViews(targetCount, currentCount - targetCount);
        }

        for (int k = 0; k < targetCount; k++) {
            int i = startIndex + k;
            MensajeContacto item = listaFiltrada.get(i);
            String itemTag = item.remoteId != null && !item.remoteId.isEmpty() ? item.remoteId : ("local_" + item.id);
            View cardView;
            if (k < layoutPage1Mensajes.getChildCount()) {
                View existing = layoutPage1Mensajes.getChildAt(k);
                if (itemTag.equals(existing.getTag())) {
                    cardView = existing;
                } else {
                    cardView = inflater.inflate(R.layout.item_admin_mensaje_card, layoutPage1Mensajes, false);
                    cardView.setTag(itemTag);
                    layoutPage1Mensajes.removeViewAt(k);
                    layoutPage1Mensajes.addView(cardView, k);
                }
            } else {
                cardView = inflater.inflate(R.layout.item_admin_mensaje_card, layoutPage1Mensajes, false);
                cardView.setTag(itemTag);
                layoutPage1Mensajes.addView(cardView);
            }

            TextView tvCliente = cardView.findViewById(R.id.tv_item_cliente);
            TextView tvFecha = cardView.findViewById(R.id.tv_item_fecha);
            TextView tvBadgeEstado = cardView.findViewById(R.id.tv_item_badge_estado);
            TextView tvPropiedad = cardView.findViewById(R.id.tv_item_propiedad);
            TextView tvSnippet = cardView.findViewById(R.id.tv_item_snippet);
            TextView tvTelefono = cardView.findViewById(R.id.tv_item_telefono);

            if (tvCliente != null) tvCliente.setText(item.clienteNombre != null ? item.clienteNombre : "Cliente");
            if (tvFecha != null) {
                String horaStr = item.hora != null ? " • " + item.hora : "";
                tvFecha.setText((item.fecha != null ? item.fecha : "") + horaStr);
            }

            if (tvBadgeEstado != null) {
                if (item.leido) {
                    tvBadgeEstado.setText("LEÍDO");
                    tvBadgeEstado.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.colorSurfaceVariant));
                    tvBadgeEstado.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOutline));
                } else {
                    tvBadgeEstado.setText("NUEVO");
                    tvBadgeEstado.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.colorPrimaryFixed));
                    tvBadgeEstado.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
                }
            }

            if (tvPropiedad != null) {
                tvPropiedad.setText(item.propiedadTitulo != null ? item.propiedadTitulo : "Propiedad");
            }
            if (tvSnippet != null) {
                tvSnippet.setText(item.mensaje != null ? item.mensaje : "");
            }
            if (tvTelefono != null) {
                tvTelefono.setText(item.clienteTelefono != null && !item.clienteTelefono.isEmpty() ? item.clienteTelefono : "Sin teléfono");
            }

            cardView.setOnClickListener(v -> abrirModalDetalleMensaje(item, root));
        }

        if (layoutPagination != null) {
            layoutPagination.setVisibility(maxPages > 1 ? View.VISIBLE : View.GONE);
        }
        renderPagination(root, false);
    }

    private void abrirModalDetalleMensaje(MensajeContacto item, View root) {
        if (!isAdded() || getContext() == null) return;

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_detalle_mensaje, null);

        TextView tvNombre = dialogView.findViewById(R.id.dialog_tv_nombre);
        TextView tvEstado = dialogView.findViewById(R.id.dialog_tv_estado);
        TextView tvFecha = dialogView.findViewById(R.id.dialog_tv_fecha);
        TextView tvPropiedad = dialogView.findViewById(R.id.dialog_tv_propiedad);
        TextView tvTelefono = dialogView.findViewById(R.id.dialog_tv_telefono);
        TextView btnCopiarTel = dialogView.findViewById(R.id.dialog_btn_copiar_tel);
        TextView tvMensaje = dialogView.findViewById(R.id.dialog_tv_mensaje);
        MaterialButton btnMarcarLeido = dialogView.findViewById(R.id.dialog_btn_marcar_leido);
        MaterialButton btnLlamar = dialogView.findViewById(R.id.dialog_btn_llamar);
        MaterialButton btnWhatsapp = dialogView.findViewById(R.id.dialog_btn_whatsapp);
        ImageButton btnCerrarTop = dialogView.findViewById(R.id.dialog_btn_cerrar_top);

        // Vincular información del mensaje
        tvNombre.setText(item.clienteNombre);
        String horaStr = item.hora != null ? " • " + item.hora : "";
        tvFecha.setText((item.fecha != null ? item.fecha : "") + horaStr);
        tvPropiedad.setText(item.propiedadTitulo);
        tvTelefono.setText(item.clienteTelefono != null && !item.clienteTelefono.isEmpty() ? item.clienteTelefono : "Sin teléfono");
        tvMensaje.setText(item.mensaje);

        // Actualizar UI del estado (NUEVO o LEÍDO)
        actualizarEstadoBadgeDialog(tvEstado, btnMarcarLeido, item.leido);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View snackbarAnchor = btnMarcarLeido != null ? btnMarcarLeido : dialogView;

        // Acción: Copiar teléfono
        if (btnCopiarTel != null) {
            btnCopiarTel.setOnClickListener(v -> {
                if (!isAdded() || getContext() == null) return;
                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Teléfono cliente", item.clienteTelefono != null ? item.clienteTelefono : "");
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    mostrarSnackbarElegante("Teléfono copiado al portapapeles", snackbarAnchor);
                }
            });
        }

        // Acción: Llamar
        if (btnLlamar != null) {
            btnLlamar.setOnClickListener(v -> {
                try {
                    String phone = item.clienteTelefono != null ? item.clienteTelefono.replaceAll("[^0-9+]", "") : "";
                    if (!phone.isEmpty()) {
                        Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                        dialIntent.setData(Uri.parse("tel:" + phone));
                        startActivity(dialIntent);
                    } else {
                        mostrarSnackbarElegante("El cliente no proporcionó teléfono", snackbarAnchor);
                    }
                } catch (Exception e) {
                    mostrarSnackbarElegante("No se pudo iniciar la llamada", snackbarAnchor);
                }
            });
        }

        // Acción: Enviar WhatsApp
        if (btnWhatsapp != null) {
            btnWhatsapp.setOnClickListener(v -> {
                try {
                    String cleanPhone = item.clienteTelefono != null ? item.clienteTelefono.replaceAll("[^0-9]", "") : "";
                    String textoMensaje = "Hola " + (item.clienteNombre != null ? item.clienteNombre : "") +
                            ", te contacto desde Home Finder por tu consulta sobre " + (item.propiedadTitulo != null ? item.propiedadTitulo : "la propiedad") + ".";
                    String url = "https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + Uri.encode(textoMensaje);
                    Intent whatsappIntent = new Intent(Intent.ACTION_VIEW);
                    whatsappIntent.setData(Uri.parse(url));
                    startActivity(whatsappIntent);
                } catch (Exception e) {
                    mostrarSnackbarElegante("No se pudo abrir WhatsApp", snackbarAnchor);
                }
            });
        }

        // Acción: Marcar como leído
        if (btnMarcarLeido != null) {
            btnMarcarLeido.setOnClickListener(v -> {
                dialog.dismiss();
                if (!isAdded() || getContext() == null) return;

                item.leido = true;

                // Guardar en la base de datos Room y en Supabase
                MensajeContactoRepositoryImpl.getInstance(requireContext()).marcarComoLeido(item);

                scheduleApplyFilters(root);
                mostrarSnackbarEleganteConDeshacer(item, root);
            });
        }

        // Botón cerrar
        if (btnCerrarTop != null) {
            btnCerrarTop.setOnClickListener(v -> dialog.dismiss());
        }

        dialogView.setClickable(true);
        dialogView.setFocusable(true);

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            View parentPanel = dialog.findViewById(androidx.appcompat.R.id.parentPanel);
            if (parentPanel != null) {
                ViewGroup.LayoutParams lp = parentPanel.getLayoutParams();
                if (lp instanceof FrameLayout.LayoutParams) {
                    ((FrameLayout.LayoutParams) lp).gravity = Gravity.CENTER;
                }
                if (lp instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                    int marginH = (int) (20 * getResources().getDisplayMetrics().density);
                    mlp.setMargins(marginH, 0, marginH, 0);
                    parentPanel.setLayoutParams(mlp);
                }
            }

            View decorView = dialog.getWindow().getDecorView();
            decorView.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    Rect rect = new Rect();
                    dialogView.getGlobalVisibleRect(rect);
                    if (!rect.contains((int) event.getX(), (int) event.getY())) {
                        dialog.dismiss();
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private void actualizarEstadoBadgeDialog(TextView tvEstado, MaterialButton btnMarcarLeido, boolean isRead) {
        if (getContext() == null || tvEstado == null) return;

        if (isRead) {
            tvEstado.setText("LEÍDO");
            tvEstado.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.colorSurfaceVariant));
            tvEstado.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOutline));

            if (btnMarcarLeido != null) {
                btnMarcarLeido.setText("Leído");
                btnMarcarLeido.setEnabled(false);
                btnMarcarLeido.setAlpha(0.6f);
            }
        } else {
            tvEstado.setText("NUEVO");
            tvEstado.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.colorPrimaryFixed));
            tvEstado.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));

            if (btnMarcarLeido != null) {
                btnMarcarLeido.setText("Marcar como leído");
                btnMarcarLeido.setEnabled(true);
                btnMarcarLeido.setAlpha(1.0f);
            }
        }
    }

    private void setupSearchAndFilters(View root) {
        if (etBuscarMensaje != null) {
            etBuscarMensaje.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentPage = 1;
                    scheduleApplyFilters(root);
                }

                @Override
                public void afterTextChanged(Editable s) { }
            });
        }

        if (chipGroupMensajes != null) {
            chipGroupMensajes.setOnCheckedChangeListener((group, checkedId) -> {
                currentPage = 1;
                scheduleApplyFilters(root);
            });
        }
    }

    private void setupDatePicker(View root) {
        if (chipFecha != null) {
            chipFecha.setOnClickListener(v -> {
                if (!isAdded() || isStateSaved()) return;
                MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Filtrar mensajes por fecha")
                        .setTheme(R.style.CustomDatePickerTheme)
                        .build();

                datePicker.addOnPositiveButtonClickListener(selection -> {
                    if (selection != null) {
                        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        format.setTimeZone(TimeZone.getTimeZone("UTC"));
                        selectedDateFilter = format.format(new Date(selection));

                        chipFecha.setText(selectedDateFilter);
                        chipFecha.setCloseIconVisible(true);
                        currentPage = 1;
                        scheduleApplyFilters(root);
                    }
                });

                datePicker.show(getParentFragmentManager(), "DATE_PICKER_MENSAJES");
            });

            chipFecha.setOnCloseIconClickListener(v -> limpiarFiltroFecha(root));
        }
    }

    private void limpiarFiltroFecha(View root) {
        selectedDateFilter = null;
        if (chipFecha != null) {
            chipFecha.setText("Por fecha");
            chipFecha.setCloseIconVisible(false);
        }
        if (scrollChipsMensajes != null) {
            scrollChipsMensajes.smoothScrollTo(0, 0);
        }
        currentPage = 1;
        applyFilters(root);
        mostrarSnackbarElegante("Filtro de fecha quitado");
    }

    private void applyFilters(View root) {
        String query = "";
        if (etBuscarMensaje != null && etBuscarMensaje.getText() != null) {
            query = etBuscarMensaje.getText().toString().trim().toLowerCase();
        }

        int checkedId = chipGroupMensajes != null ? chipGroupMensajes.getCheckedChipId() : R.id.chip_todos;

        listaFiltrada.clear();
        for (MensajeContacto item : listaMensajes) {
            // Filtro por Estado (Todos, No Leídos, Leídos)
            boolean matchesStatus = true;
            if (checkedId == R.id.chip_no_leidos) {
                matchesStatus = !item.leido;
            } else if (checkedId == R.id.chip_leidos) {
                matchesStatus = item.leido;
            }

            // Filtro por Fecha específica
            boolean matchesDate = true;
            if (selectedDateFilter != null && !selectedDateFilter.isEmpty()) {
                matchesDate = item.fecha != null && item.fecha.equalsIgnoreCase(selectedDateFilter);
            }

            // Filtro por Búsqueda (nombre, propiedad, teléfono, mensaje)
            boolean matchesQuery = query.isEmpty() ||
                    (item.clienteNombre != null && item.clienteNombre.toLowerCase().contains(query)) ||
                    (item.propiedadTitulo != null && item.propiedadTitulo.toLowerCase().contains(query)) ||
                    (item.clienteTelefono != null && item.clienteTelefono.contains(query)) ||
                    (item.mensaje != null && item.mensaje.toLowerCase().contains(query));

            if (matchesStatus && matchesDate && matchesQuery) {
                listaFiltrada.add(item);
            }
        }

        ordenarMensajesPorMasRecientes(listaFiltrada);
        renderMensajesList(root);
    }

    private void ordenarMensajesPorMasRecientes(List<MensajeContacto> list) {
        if (list == null || list.size() <= 1) return;
        java.util.Collections.sort(list, (m1, m2) -> {
            if (m1 == null && m2 == null) return 0;
            if (m1 == null) return 1;
            if (m2 == null) return -1;

            boolean isDemo1 = m1.isDemoMessage();
            boolean isDemo2 = m2.isDemoMessage();

            // 1. Mensajes reales de clientes siempre van primero antes que cualquier mensaje demo
            if (isDemo1 != isDemo2) {
                return isDemo1 ? 1 : -1;
            }

            // 2. Ordenar por timestamp más reciente primero (DESC)
            long t1 = m1.getEfectiveTimestamp();
            long t2 = m2.getEfectiveTimestamp();
            if (t1 != t2) {
                return Long.compare(t2, t1); // DESC: Más reciente primero
            }

            // 3. Fallback: ID autoincremental más alto primero
            return Long.compare(m2.id, m1.id);
        });
    }

    private void setupPagination(View root) {
        btnPagePrev = root.findViewById(R.id.btn_page_prev);
        btnPageNext = root.findViewById(R.id.btn_page_next);
        tvPagePrev = root.findViewById(R.id.tv_page_prev);
        tvPageNext = root.findViewById(R.id.tv_page_next);

        cardPage1 = root.findViewById(R.id.card_page_1);
        cardPage2 = root.findViewById(R.id.card_page_2);
        cardPage3 = root.findViewById(R.id.card_page_3);
        cardPage4 = root.findViewById(R.id.card_page_4);

        tvPage1 = root.findViewById(R.id.tv_page_1);
        tvPage2 = root.findViewById(R.id.tv_page_2);
        tvPage3 = root.findViewById(R.id.tv_page_3);
        tvPage4 = root.findViewById(R.id.tv_page_4);

        if (btnPagePrev != null) {
            btnPagePrev.setOnClickListener(v -> {
                if (currentPage > 1) {
                    goToPage(currentPage - 1, root);
                }
            });
        }

        if (btnPageNext != null) {
            btnPageNext.setOnClickListener(v -> {
                if (currentPage < maxPages) {
                    goToPage(currentPage + 1, root);
                }
            });
        }

        if (cardPage1 != null) cardPage1.setOnClickListener(v -> goToPage(startPage, root));
        if (cardPage2 != null) cardPage2.setOnClickListener(v -> goToPage(startPage + 1, root));
        if (cardPage3 != null) cardPage3.setOnClickListener(v -> goToPage(startPage + 2, root));
        if (cardPage4 != null) cardPage4.setOnClickListener(v -> goToPage(startPage + 3, root));

        renderPagination(root, false);
    }

    private void goToPage(int page, View root) {
        if (page < 1) page = 1;
        if (page > maxPages) page = maxPages;
        if (page == currentPage && startPage == page) return;

        currentPage = page;
        startPage = currentPage;

        if (scrollViewMensajes != null) {
            scrollViewMensajes.smoothScrollTo(0, 0);
        }

        renderMensajesList(root);
        renderPagination(root, true);
    }

    private void renderPagination(View root, boolean showToast) {
        if (cardPage1 == null) return;

        int slot1 = startPage;
        int slot2 = startPage + 1;
        int slot3 = startPage + 2;
        int slot4 = startPage + 3;

        if (tvPage1 != null) tvPage1.setText(String.valueOf(slot1));
        if (tvPage2 != null) tvPage2.setText(String.valueOf(slot2));
        if (tvPage3 != null) tvPage3.setText(String.valueOf(slot3));
        if (tvPage4 != null) tvPage4.setText(String.valueOf(slot4));

        if (cardPage2 != null) cardPage2.setVisibility(slot2 <= maxPages ? View.VISIBLE : View.GONE);
        if (cardPage3 != null) cardPage3.setVisibility(slot3 <= maxPages ? View.VISIBLE : View.GONE);
        if (cardPage4 != null) cardPage4.setVisibility(slot4 <= maxPages ? View.VISIBLE : View.GONE);

        stylePageCard(cardPage1, tvPage1, true);
        stylePageCard(cardPage2, tvPage2, false);
        stylePageCard(cardPage3, tvPage3, false);
        stylePageCard(cardPage4, tvPage4, false);

        boolean canGoPrev = currentPage > 1;
        if (btnPagePrev != null) {
            btnPagePrev.setEnabled(canGoPrev);
            btnPagePrev.setAlpha(canGoPrev ? 1.0f : 0.35f);
        }

        boolean canGoNext = currentPage < maxPages;
        if (btnPageNext != null) {
            btnPageNext.setEnabled(canGoNext);
            btnPageNext.setAlpha(canGoNext ? 1.0f : 0.35f);
        }

        if (showToast && getContext() != null) {
            mostrarSnackbarElegante("Página " + currentPage + " de " + maxPages);
        }
    }

    private void stylePageCard(MaterialCardView card, TextView tv, boolean isSelected) {
        if (card == null || tv == null || getContext() == null) return;
        int dp1 = Math.round(1 * getResources().getDisplayMetrics().density);
        int dp2 = Math.round(2 * getResources().getDisplayMetrics().density);

        if (isSelected) {
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
            card.setStrokeWidth(0);
            card.setCardElevation(dp2);
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOnPrimary));
            tv.setTypeface(null, Typeface.BOLD);
        } else {
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorSurfaceContainerLowest));
            card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.colorOutlineVariant));
            card.setStrokeWidth(dp1);
            card.setCardElevation(dp1);
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOnSurfaceVariant));
            tv.setTypeface(null, Typeface.NORMAL);
        }
    }

    private void mostrarSnackbarEleganteConDeshacer(MensajeContacto item, View root) {
        if (!isAdded() || getActivity() == null || getContext() == null || item == null) return;
        final Context appContext = requireContext().getApplicationContext();

        View rootView = requireActivity().findViewById(R.id.main);
        if (rootView == null) {
            rootView = requireActivity().getWindow().getDecorView().findViewById(android.R.id.content);
        }
        if (rootView == null) {
            rootView = root;
        }
        if (rootView == null) return;

        Snackbar snackbar = Snackbar.make(rootView, "Mensaje marcado como visto", Snackbar.LENGTH_LONG)
                .setAction("DESHACER", v -> {
                    item.leido = false;
                    MensajeContactoRepositoryImpl.getInstance(appContext).marcarComoNoLeido(item);
                    if (isAdded() && getContext() != null) {
                        scheduleApplyFilters(root);
                        mostrarSnackbarElegante("Se restableció a no leído");
                    }
                })
                .setActionTextColor(ContextCompat.getColor(requireContext(), R.color.colorSnackbarAction))
                .setTextColor(ContextCompat.getColor(requireContext(), R.color.white));

        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundResource(R.drawable.bg_snackbar_favoritos);
        snackbarView.setBackgroundTintList(null);

        TextView tv = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (tv != null) {
            tv.setMaxLines(3);
        }

        TextView actionTextView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_action);
        if (actionTextView != null) {
            actionTextView.setTypeface(Typeface.DEFAULT_BOLD);
        }

        ViewGroup.LayoutParams lp = snackbarView.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) lp;
            int marginH = (int) (16 * getResources().getDisplayMetrics().density);
            int marginB = (int) (8 * getResources().getDisplayMetrics().density);
            marginParams.setMargins(marginH, 0, marginH, marginB);
            snackbarView.setLayoutParams(marginParams);
        }
        if (lp instanceof FrameLayout.LayoutParams) {
            ((FrameLayout.LayoutParams) lp).gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        }
        snackbarView.setElevation(8 * getResources().getDisplayMetrics().density);

        View bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
        if (bottomNav != null && bottomNav.getVisibility() == View.VISIBLE) {
            snackbar.setAnchorView(bottomNav);
        }

        snackbar.show();
    }

    private void mostrarSnackbarElegante(String mensaje) {
        mostrarSnackbarElegante(mensaje, null);
    }

    private void mostrarSnackbarElegante(String mensaje, View anchorOrRoot) {
        if (!isAdded() || getActivity() == null || getContext() == null || mensaje == null) return;

        View rootView = anchorOrRoot;
        if (rootView == null) {
            rootView = requireActivity().findViewById(R.id.main);
        }
        if (rootView == null) {
            rootView = requireActivity().getWindow().getDecorView().findViewById(android.R.id.content);
        }
        if (rootView == null) {
            rootView = getView();
        }
        if (rootView == null) return;

        Snackbar snackbar = Snackbar.make(rootView, mensaje, Snackbar.LENGTH_SHORT)
                .setTextColor(ContextCompat.getColor(requireContext(), R.color.white));

        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundResource(R.drawable.bg_snackbar_favoritos);
        snackbarView.setBackgroundTintList(null);

        TextView tv = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (tv != null) {
            tv.setMaxLines(3);
        }

        ViewGroup.LayoutParams lp = snackbarView.getLayoutParams();
        float density = getResources().getDisplayMetrics().density;
        int marginLeft = (int) (16 * density);
        int marginRight = (int) (16 * density);
        int marginB = (int) (8 * density);

        boolean anchorToButton = anchorOrRoot != null && anchorOrRoot.getWidth() > 0 && anchorOrRoot.getHeight() > 0;
        if (anchorToButton) {
            View contentView = anchorOrRoot.getRootView().findViewById(android.R.id.content);
            if (contentView == null) {
                contentView = anchorOrRoot.getRootView();
            }
            int[] anchorLoc = new int[2];
            int[] parentLoc = new int[2];
            anchorOrRoot.getLocationOnScreen(anchorLoc);
            contentView.getLocationOnScreen(parentLoc);

            marginLeft = Math.max(0, anchorLoc[0] - parentLoc[0]);
            marginRight = Math.max(0, (parentLoc[0] + contentView.getWidth()) - (anchorLoc[0] + anchorOrRoot.getWidth()));
            marginB = -anchorOrRoot.getHeight();
        } else if (anchorOrRoot != null) {
            marginLeft = (int) (44 * density);
            marginRight = (int) (44 * density);
            marginB = (int) (24 * density);
        }

        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) lp;
            marginParams.setMargins(marginLeft, 0, marginRight, marginB);
            snackbarView.setLayoutParams(marginParams);
        }
        if (lp instanceof FrameLayout.LayoutParams) {
            ((FrameLayout.LayoutParams) lp).gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        } else if (lp instanceof androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) {
            ((androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) lp).gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        }
        snackbarView.setElevation(8 * density);

        if (anchorToButton) {
            if (tv != null) {
                tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                tv.setGravity(Gravity.CENTER);
            }
            snackbar.setAnchorView(anchorOrRoot);
        } else {
            View bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null && bottomNav.getVisibility() == View.VISIBLE && anchorOrRoot == null) {
                snackbar.setAnchorView(bottomNav);
            }
        }

        snackbar.show();
    }
}
