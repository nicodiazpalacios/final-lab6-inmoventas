package com.example.inmobiliariaventa.ui.admin;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.inmobiliariaventa.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class AdminMensajesFragment extends Fragment {

    private int currentPage = 1;
    private int startPage = 1;
    private final int maxPages = 4; // Paginación dinámica de hasta 4 páginas

    private LinearLayout layoutPage1Mensajes;
    private LinearLayout layoutPage2Mensajes;
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

    // Clase interna para representar los datos de cada mensaje
    private static class MensajeItem {
        final int id;
        final int cardId;
        final int tvClienteId;
        final int tvFechaId;
        final int tvBadgeEstadoId;
        final int tvPropiedadId;
        final int tvSnippetId;
        final int tvTelefonoId;
        final String cliente;
        final String propiedad;
        final String telefono;
        final String fecha; // "dd/MM/yyyy"
        final String hora;  // "11:45 AM"
        final String mensajeCompleto;
        boolean isRead;

        MensajeItem(int id, int cardId, int tvClienteId, int tvFechaId, int tvBadgeEstadoId,
                    int tvPropiedadId, int tvSnippetId, int tvTelefonoId,
                    String cliente, String propiedad, String telefono,
                    String fecha, String hora, String mensajeCompleto, boolean isRead) {
            this.id = id;
            this.cardId = cardId;
            this.tvClienteId = tvClienteId;
            this.tvFechaId = tvFechaId;
            this.tvBadgeEstadoId = tvBadgeEstadoId;
            this.tvPropiedadId = tvPropiedadId;
            this.tvSnippetId = tvSnippetId;
            this.tvTelefonoId = tvTelefonoId;
            this.cliente = cliente;
            this.propiedad = propiedad;
            this.telefono = telefono;
            this.fecha = fecha;
            this.hora = hora;
            this.mensajeCompleto = mensajeCompleto;
            this.isRead = isRead;
        }
    }

    private final MensajeItem[] mensajesList = new MensajeItem[] {
        new MensajeItem(1, R.id.card_mensaje_1, R.id.tv_cliente_1, R.id.tv_fecha_1, R.id.tv_badge_estado_1,
                R.id.tv_propiedad_1, R.id.tv_snippet_1, R.id.tv_telefono_1,
                "Lucas Martínez", "Villa Los Laureles (San Isidro)", "+54 9 11 4821-3390",
                "14/10/2026", "11:45 AM",
                "Hola, me interesa conocer las condiciones de financiación y si aceptan propiedad menor valor en parte de pago. Quisiera coordinar una visita para este sábado por la mañana.", false),

        new MensajeItem(2, R.id.card_mensaje_2, R.id.tv_cliente_2, R.id.tv_fecha_2, R.id.tv_badge_estado_2,
                R.id.tv_propiedad_2, R.id.tv_snippet_2, R.id.tv_telefono_2,
                "Sofía Benítez", "Penthouse Lumina (Puerto Madero)", "+54 9 11 6729-1144",
                "14/10/2026", "10:20 AM",
                "Buenos días, quería consultar las expensas mensuales y si cuenta con dos cocheras cubiertas. Tengo disponibilidad para visitarlo esta semana.", false),

        new MensajeItem(3, R.id.card_mensaje_3, R.id.tv_cliente_3, R.id.tv_fecha_3, R.id.tv_badge_estado_3,
                R.id.tv_propiedad_3, R.id.tv_snippet_3, R.id.tv_telefono_3,
                "Gonzalo Fernández", "Torre Bellini Dpto 14B (Palermo)", "+54 9 11 3902-8812",
                "14/10/2026", "09:15 AM",
                "Hola! ¿El departamento es apto crédito hipotecario bancario? Estoy muy interesado y con pre-aprobación del Banco Nación lista.", false),

        new MensajeItem(4, R.id.card_mensaje_4, R.id.tv_cliente_4, R.id.tv_fecha_4, R.id.tv_badge_estado_4,
                R.id.tv_propiedad_4, R.id.tv_snippet_4, R.id.tv_telefono_4,
                "Valentina Rossi", "Casa Moderna con Piscina (Pilar)", "+54 9 11 5519-7032",
                "13/10/2026", "06:40 PM",
                "Buenas tardes, quisiera saber el costo aproximado de mantenimiento del jardín y piscina, y si el barrio cerrado cuenta con amenities habilitados.", false),

        new MensajeItem(5, R.id.card_mensaje_5, R.id.tv_cliente_5, R.id.tv_fecha_5, R.id.tv_badge_estado_5,
                R.id.tv_propiedad_5, R.id.tv_snippet_5, R.id.tv_telefono_5,
                "Camila Gómez", "Dúplex de Diseño (Belgrano R)", "+54 9 11 2341-9980",
                "13/10/2026", "04:10 PM",
                "Hola, me gustaría saber si la posesión es inmediata o si actualmente se encuentra alquilado. Saludos.", true),

        new MensajeItem(6, R.id.card_mensaje_6, R.id.tv_cliente_6, R.id.tv_fecha_6, R.id.tv_badge_estado_6,
                R.id.tv_propiedad_6, R.id.tv_snippet_6, R.id.tv_telefono_6,
                "Mateo Rodríguez", "Residencia Quinta Real (Tigre)", "+54 9 11 7890-4421",
                "13/10/2026", "02:00 PM",
                "Hola! ¿Tiene amarra propia para embarcación? Me gustaría agendar una visita presencial con mi familia.", true),

        new MensajeItem(7, R.id.card_mensaje_7, R.id.tv_cliente_7, R.id.tv_fecha_7, R.id.tv_badge_estado_7,
                R.id.tv_propiedad_7, R.id.tv_snippet_7, R.id.tv_telefono_7,
                "Florencia Díaz", "Loft Industrial Tech (Colegiales)", "+54 9 11 8912-3301",
                "12/10/2026", "11:30 AM",
                "Hola, ¿es apto profesional? Busco un espacio para estudio de arquitectura y vivienda combinada.", false),

        new MensajeItem(8, R.id.card_mensaje_8, R.id.tv_cliente_8, R.id.tv_fecha_8, R.id.tv_badge_estado_8,
                R.id.tv_propiedad_8, R.id.tv_snippet_8, R.id.tv_telefono_8,
                "Emiliano Morales", "Semipiso con Terraza (Recoleta)", "+54 9 11 4110-8765",
                "12/10/2026", "09:45 AM",
                "Buen día, quisiera recibir el plano detallado de la propiedad y saber la orientación solar de la terraza.", true),

        new MensajeItem(9, R.id.card_mensaje_9, R.id.tv_cliente_9, R.id.tv_fecha_9, R.id.tv_badge_estado_9,
                R.id.tv_propiedad_9, R.id.tv_snippet_9, R.id.tv_telefono_9,
                "Lucía Navarro", "Casa Country Club (Nordelta)", "+54 9 11 5022-7711",
                "11/10/2026", "05:50 PM",
                "Hola, ¿aceptan permuta por departamento en Vicente López? Quisiéramos mudarnos antes de fin de año.", false),

        new MensajeItem(10, R.id.card_mensaje_10, R.id.tv_cliente_10, R.id.tv_fecha_10, R.id.tv_badge_estado_10,
                R.id.tv_propiedad_10, R.id.tv_snippet_10, R.id.tv_telefono_10,
                "Diego Castro", "Departamento 3 Ambientes (Caballito)", "+54 9 11 3198-4455",
                "11/10/2026", "03:20 PM",
                "Hola, ¿las expensas incluyen el agua de Aysa? Me interesa coordinar visita para este viernes por la tarde.", true),

        new MensajeItem(11, R.id.card_mensaje_11, R.id.tv_cliente_11, R.id.tv_fecha_11, R.id.tv_badge_estado_11,
                R.id.tv_propiedad_11, R.id.tv_snippet_11, R.id.tv_telefono_11,
                "Martina López", "Chalet Tradicional (Olivos)", "+54 9 11 6201-9933",
                "10/10/2026", "01:15 PM",
                "Buenas tardes, ¿la casa tiene calefacción central por radiadores? Estamos buscando mudarnos a zona norte.", true),

        new MensajeItem(12, R.id.card_mensaje_12, R.id.tv_cliente_12, R.id.tv_fecha_12, R.id.tv_badge_estado_12,
                R.id.tv_propiedad_12, R.id.tv_snippet_12, R.id.tv_telefono_12,
                "Agustín Silva", "Monoambiente Premium (Palermo Soho)", "+54 9 11 7734-2210",
                "10/10/2026", "10:00 AM",
                "¿Está permitido el alquiler temporal tipo Airbnb en el reglamento de copropiedad? Busco para inversión.", false),

        new MensajeItem(13, R.id.card_mensaje_13, R.id.tv_cliente_13, R.id.tv_fecha_13, R.id.tv_badge_estado_13,
                R.id.tv_propiedad_13, R.id.tv_snippet_13, R.id.tv_telefono_13,
                "Paula Pereyra", "Piso Exclusivo sobre Libertador (Núñez)", "+54 9 11 8845-6677",
                "09/10/2026", "04:30 PM",
                "Hola, quisiera consultar si incluye seguridad las 24 hs y grupo electrógeno para todo el edificio.", true),

        new MensajeItem(14, R.id.card_mensaje_14, R.id.tv_cliente_14, R.id.tv_fecha_14, R.id.tv_badge_estado_14,
                R.id.tv_propiedad_14, R.id.tv_snippet_14, R.id.tv_telefono_14,
                "Joaquín Herrera", "Casa en Barrio Cerrado (Escobar)", "+54 9 11 4567-1123",
                "09/10/2026", "11:20 AM",
                "Buenos días, ¿cuál es el valor de las expensas ordinarias del barrio? Queremos coordinar una visita.", false),

        new MensajeItem(15, R.id.card_mensaje_15, R.id.tv_cliente_15, R.id.tv_fecha_15, R.id.tv_badge_estado_15,
                R.id.tv_propiedad_15, R.id.tv_snippet_15, R.id.tv_telefono_15,
                "Julieta Romero", "Dpto Luminoso con Balcón (Villa Urquiza)", "+54 9 11 9012-3456",
                "08/10/2026", "06:10 PM",
                "Hola, me interesa saber si aceptan mascotas de tamaño mediano y si el edificio tiene ascensor principal y de servicio.", true),

        new MensajeItem(16, R.id.card_mensaje_16, R.id.tv_cliente_16, R.id.tv_fecha_16, R.id.tv_badge_estado_16,
                R.id.tv_propiedad_16, R.id.tv_snippet_16, R.id.tv_telefono_16,
                "Ignacio Torres", "Oficina Corporativa (Catalinas)", "+54 9 11 3456-7890",
                "08/10/2026", "02:40 PM",
                "Buenas tardes, ¿la planta libre cuenta con piso técnico y cableado estructurado ya instalado? Gracias.", true)
    };

    public AdminMensajesFragment() {
        // Constructor público vacío requerido
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
        layoutPage2Mensajes = view.findViewById(R.id.layout_page_2_mensajes);
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

        // Flecha de regreso
        View btnBack = view.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());
        }

        setupMessageCardClicks(view);
        setupSearchAndFilters(view);
        setupDatePicker(view);
        setupPagination(view);
    }

    private void setupMessageCardClicks(View root) {
        for (MensajeItem item : mensajesList) {
            MaterialCardView card = root.findViewById(item.cardId);
            if (card != null) {
                card.setOnClickListener(v -> abrirModalDetalleMensaje(item, root));
            }
        }
    }

    private void abrirModalDetalleMensaje(MensajeItem item, View root) {
        if (getContext() == null) return;

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
        tvNombre.setText(item.cliente);
        tvFecha.setText(item.fecha + " • " + item.hora);
        tvPropiedad.setText(item.propiedad);
        tvTelefono.setText(item.telefono);
        tvMensaje.setText(item.mensajeCompleto);

        // Actualizar UI del estado (NUEVO o LEÍDO)
        actualizarEstadoBadgeDialog(tvEstado, btnMarcarLeido, item.isRead);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Acción: Copiar teléfono
        if (btnCopiarTel != null) {
            btnCopiarTel.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Teléfono cliente", item.telefono);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(requireContext(), "Teléfono copiado al portapapeles", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Acción: Llamar
        if (btnLlamar != null) {
            btnLlamar.setOnClickListener(v -> {
                try {
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                    dialIntent.setData(Uri.parse("tel:" + item.telefono.replaceAll("[^0-9+]", "")));
                    startActivity(dialIntent);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "No se pudo iniciar la llamada", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Acción: Enviar WhatsApp
        if (btnWhatsapp != null) {
            btnWhatsapp.setOnClickListener(v -> {
                try {
                    String cleanPhone = item.telefono.replaceAll("[^0-9]", "");
                    String textoMensaje = "Hola " + item.cliente + ", te contacto desde Inmobiliaria por tu consulta sobre " + item.propiedad + ".";
                    String url = "https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + Uri.encode(textoMensaje);
                    Intent whatsappIntent = new Intent(Intent.ACTION_VIEW);
                    whatsappIntent.setData(Uri.parse(url));
                    startActivity(whatsappIntent);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Acción: Marcar como leído
        if (btnMarcarLeido != null) {
            btnMarcarLeido.setOnClickListener(v -> {
                item.isRead = true;
                actualizarEstadoBadgeDialog(tvEstado, btnMarcarLeido, true);

                // Actualizar la tarjeta en el fragment
                TextView tvCardBadge = root.findViewById(item.tvBadgeEstadoId);
                if (tvCardBadge != null) {
                    tvCardBadge.setText("LEÍDO");
                    tvCardBadge.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.colorSurfaceVariant));
                    tvCardBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOutline));
                }

                Toast.makeText(requireContext(), "Mensaje marcado como leído", Toast.LENGTH_SHORT).show();
                applyFilters(root);
            });
        }

        // Botón cerrar
        if (btnCerrarTop != null) {
            btnCerrarTop.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
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
                    applyFilters(root);
                }

                @Override
                public void afterTextChanged(Editable s) { }
            });
        }

        if (chipGroupMensajes != null) {
            chipGroupMensajes.setOnCheckedChangeListener((group, checkedId) -> applyFilters(root));
        }
    }

    private void setupDatePicker(View root) {
        if (chipFecha != null) {
            chipFecha.setOnClickListener(v -> {
                MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Filtrar mensajes por fecha")
                        .setTheme(R.style.CustomDatePickerTheme)
                        .build();

                datePicker.addOnPositiveButtonClickListener(selection -> {
                    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    calendar.setTimeInMillis(selection);
                    SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    selectedDateFilter = format.format(calendar.getTime());

                    chipFecha.setText(selectedDateFilter);
                    chipFecha.setCloseIconVisible(true);
                    applyFilters(root);
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
        applyFilters(root);
        Toast.makeText(requireContext(), "Filtro de fecha quitado", Toast.LENGTH_SHORT).show();
    }

    private void applyFilters(View root) {
        String query = "";
        if (etBuscarMensaje != null && etBuscarMensaje.getText() != null) {
            query = etBuscarMensaje.getText().toString().trim().toLowerCase();
        }

        int checkedId = chipGroupMensajes != null ? chipGroupMensajes.getCheckedChipId() : R.id.chip_todos;

        int visibleCount = 0;
        for (MensajeItem item : mensajesList) {
            MaterialCardView card = root.findViewById(item.cardId);
            if (card == null) continue;

            // Filtro por Estado (Todos, No Leídos, Leídos)
            boolean matchesStatus = true;
            if (checkedId == R.id.chip_no_leidos) {
                matchesStatus = !item.isRead;
            } else if (checkedId == R.id.chip_leidos) {
                matchesStatus = item.isRead;
            }

            // Filtro por Fecha específica
            boolean matchesDate = true;
            if (selectedDateFilter != null && !selectedDateFilter.isEmpty()) {
                matchesDate = item.fecha.equalsIgnoreCase(selectedDateFilter);
            }

            // Filtro por Búsqueda (nombre, propiedad, teléfono, mensaje)
            boolean matchesQuery = query.isEmpty() ||
                    item.cliente.toLowerCase().contains(query) ||
                    item.propiedad.toLowerCase().contains(query) ||
                    item.telefono.contains(query) ||
                    item.mensajeCompleto.toLowerCase().contains(query);

            boolean isVisible = matchesStatus && matchesDate && matchesQuery;

            // Si estamos navegando por páginas sin búsqueda activa y sin filtros específicos
            if (selectedDateFilter == null && query.isEmpty() && checkedId == R.id.chip_todos) {
                // Modo página normal: 8 mensajes por página
                if (currentPage == 1) {
                    card.setVisibility(item.id <= 8 ? View.VISIBLE : View.GONE);
                } else {
                    card.setVisibility(item.id > 8 ? View.VISIBLE : View.GONE);
                }
                if (card.getVisibility() == View.VISIBLE) {
                    visibleCount++;
                }
            } else {
                card.setVisibility(isVisible ? View.VISIBLE : View.GONE);
                if (isVisible) {
                    visibleCount++;
                }
            }
        }

        // Si se están aplicando filtros o búsquedas, asegurar que ambos contenedores sean visibles para mostrar las tarjetas filtradas
        if (selectedDateFilter != null || !query.isEmpty() || checkedId != R.id.chip_todos) {
            if (layoutPage1Mensajes != null) layoutPage1Mensajes.setVisibility(View.VISIBLE);
            if (layoutPage2Mensajes != null) layoutPage2Mensajes.setVisibility(View.VISIBLE);
            if (layoutPagination != null) layoutPagination.setVisibility(View.GONE);
        } else {
            if (layoutPagination != null) layoutPagination.setVisibility(View.VISIBLE);
            if (currentPage == 1) {
                if (layoutPage1Mensajes != null) layoutPage1Mensajes.setVisibility(View.VISIBLE);
                if (layoutPage2Mensajes != null) layoutPage2Mensajes.setVisibility(View.GONE);
            } else {
                if (layoutPage1Mensajes != null) layoutPage1Mensajes.setVisibility(View.GONE);
                if (layoutPage2Mensajes != null) layoutPage2Mensajes.setVisibility(View.VISIBLE);
            }
        }

        // Mostrar u ocultar estado sin resultados
        if (layoutEmptyMensajes != null) {
            layoutEmptyMensajes.setVisibility(visibleCount == 0 ? View.VISIBLE : View.GONE);
        }
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

        applyFilters(root);
        renderPagination(root, true);
    }

    private void renderPagination(View root, boolean showToast) {
        if (cardPage1 == null) return;

        int slot1 = startPage;
        int slot2 = startPage + 1;
        int slot3 = startPage + 2;
        int slot4 = startPage + 3;

        tvPage1.setText(String.valueOf(slot1));
        tvPage2.setText(String.valueOf(slot2));
        tvPage3.setText(String.valueOf(slot3));
        tvPage4.setText(String.valueOf(slot4));

        cardPage2.setVisibility(slot2 <= maxPages ? View.VISIBLE : View.GONE);
        cardPage3.setVisibility(slot3 <= maxPages ? View.VISIBLE : View.GONE);
        cardPage4.setVisibility(slot4 <= maxPages ? View.VISIBLE : View.GONE);

        // Slot 1 siempre representa la página seleccionada al desplazarse
        stylePageCard(cardPage1, tvPage1, true);
        stylePageCard(cardPage2, tvPage2, false);
        stylePageCard(cardPage3, tvPage3, false);
        stylePageCard(cardPage4, tvPage4, false);

        boolean canGoPrev = currentPage > 1;
        btnPagePrev.setEnabled(canGoPrev);
        btnPagePrev.setAlpha(canGoPrev ? 1.0f : 0.35f);

        boolean canGoNext = currentPage < maxPages;
        btnPageNext.setEnabled(canGoNext);
        btnPageNext.setAlpha(canGoNext ? 1.0f : 0.35f);

        if (showToast) {
            Toast.makeText(requireContext(), "Página " + currentPage + " de " + maxPages, Toast.LENGTH_SHORT).show();
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
}
