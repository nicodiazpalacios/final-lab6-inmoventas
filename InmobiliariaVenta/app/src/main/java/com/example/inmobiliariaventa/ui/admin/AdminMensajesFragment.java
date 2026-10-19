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
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class AdminMensajesFragment extends Fragment {

    private int currentPage = 1;
    private int startPage = 1;
    private int maxPages = 1;
    private static final int PAGE_SIZE = 8;

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
    private List<MensajeContacto> listaMensajes = new ArrayList<>();
    private List<MensajeContacto> listaFiltrada = new ArrayList<>();

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
        if (layoutPage2Mensajes != null) {
            layoutPage2Mensajes.setVisibility(View.GONE);
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

        // Flecha de regreso
        View btnBack = view.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());
        }

        setupSearchAndFilters(view);
        setupDatePicker(view);
        setupPagination(view);

        // Inicializar base de datos y observar mensajes
        initDatabaseAndObserve(view);
    }

    private void initDatabaseAndObserve(View root) {
        if (getContext() == null) return;
        MensajeContactoRepositoryImpl repository = MensajeContactoRepositoryImpl.getInstance(requireContext());

        // Observar mensajes en tiempo real desde Room
        repository.getAllMensajesLiveData().observe(getViewLifecycleOwner(), mensajes -> {
            if (mensajes != null) {
                listaMensajes = new ArrayList<>(mensajes);
                applyFilters(root);
            }
        });

        // Sincronizar en segundo plano con Supabase para traer mensajes enviados por los clientes
        repository.sincronizarConNube(() -> {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                if (db.mensajeContactoDao().countMensajesSync() == 0) {
                    List<MensajeContacto> seed = createInitialDemoMessages();
                    db.mensajeContactoDao().insertAll(seed);
                }
            });
        });
    }

    private List<MensajeContacto> createInitialDemoMessages() {
        List<MensajeContacto> list = new ArrayList<>();
        list.add(new MensajeContacto("Lucas Martínez", "+54 9 11 4821-3390", "lucas.m@gmail.com",
                "1", "Villa Los Laureles (San Isidro)", "San Isidro, Buenos Aires",
                "Hola, me interesa conocer las condiciones de financiación y si aceptan propiedad menor valor en parte de pago. Quisiera coordinar una visita para este sábado por la mañana.",
                "14/10/2026", "11:45 AM", 1792000000000L + 16, false));

        list.add(new MensajeContacto("Sofía Benítez", "+54 9 11 6729-1144", "sofi.benitez@hotmail.com",
                "2", "Penthouse Lumina (Puerto Madero)", "Puerto Madero, Buenos Aires",
                "Buenos días, quería consultar las expensas mensuales y si cuenta con dos cocheras cubiertas. Tengo disponibilidad para visitarlo esta semana.",
                "14/10/2026", "10:20 AM", 1792000000000L + 15, false));

        list.add(new MensajeContacto("Gonzalo Fernández", "+54 9 11 3902-8812", "gonzalo.f@gmail.com",
                "3", "Torre Bellini Dpto 14B (Palermo)", "Palermo, Buenos Aires",
                "Hola! ¿El departamento es apto crédito hipotecario bancario? Estoy muy interesado y con pre-aprobación del Banco Nación lista.",
                "14/10/2026", "09:15 AM", 1792000000000L + 14, false));

        list.add(new MensajeContacto("Valentina Rossi", "+54 9 11 5519-7032", "valen.rossi@yahoo.com",
                "4", "Casa Moderna con Piscina (Pilar)", "Pilar, Buenos Aires",
                "Buenas tardes, quisiera saber el costo aproximado de mantenimiento del jardín y piscina, y si el barrio cerrado cuenta con amenities habilitados.",
                "13/10/2026", "06:40 PM", 1792000000000L + 13, false));

        list.add(new MensajeContacto("Camila Gómez", "+54 9 11 2341-9980", "camilag@gmail.com",
                "5", "Dúplex de Diseño (Belgrano R)", "Belgrano R, Buenos Aires",
                "Hola, me gustaría saber si la posesión es inmediata o si actualmente se encuentra alquilado. Saludos.",
                "13/10/2026", "04:10 PM", 1792000000000L + 12, true));

        list.add(new MensajeContacto("Mateo Rodríguez", "+54 9 11 7890-4421", "mateo.rod@gmail.com",
                "6", "Residencia Quinta Real (Tigre)", "Tigre, Buenos Aires",
                "Hola! ¿Tiene amarra propia para embarcación? Me gustaría agendar una visita presencial con mi familia.",
                "13/10/2026", "02:00 PM", 1792000000000L + 11, true));

        list.add(new MensajeContacto("Florencia Díaz", "+54 9 11 8912-3301", "flor.diaz@outlook.com",
                "7", "Loft Industrial Tech (Colegiales)", "Colegiales, Buenos Aires",
                "Hola, ¿es apto profesional? Busco un espacio para estudio de arquitectura y vivienda combinada.",
                "12/10/2026", "11:30 AM", 1792000000000L + 10, false));

        list.add(new MensajeContacto("Emiliano Morales", "+54 9 11 4110-8765", "emiliano.m@gmail.com",
                "8", "Semipiso con Terraza (Recoleta)", "Recoleta, Buenos Aires",
                "Buen día, quisiera recibir el plano detallado de la propiedad y saber la orientación solar de la terraza.",
                "12/10/2026", "09:45 AM", 1792000000000L + 9, true));

        list.add(new MensajeContacto("Lucía Navarro", "+54 9 11 5022-7711", "lucia.navarro@gmail.com",
                "9", "Casa Country Club (Nordelta)", "Nordelta, Buenos Aires",
                "Hola, ¿aceptan permuta por departamento en Vicente López? Quisiéramos mudarnos antes de fin de año.",
                "11/10/2026", "05:50 PM", 1792000000000L + 8, false));

        list.add(new MensajeContacto("Diego Castro", "+54 9 11 3198-4455", "diego.castro@gmail.com",
                "10", "Departamento 3 Ambientes (Caballito)", "Caballito, Buenos Aires",
                "Hola, ¿las expensas incluyen el agua de Aysa? Me interesa coordinar visita para este viernes por la tarde.",
                "11/10/2026", "03:20 PM", 1792000000000L + 7, true));

        list.add(new MensajeContacto("Martina López", "+54 9 11 6201-9933", "martu.lopez@gmail.com",
                "11", "Chalet Tradicional (Olivos)", "Olivos, Buenos Aires",
                "Buenas tardes, ¿la casa tiene calefacción central por radiadores? Estamos buscando mudarnos a zona norte.",
                "10/10/2026", "01:15 PM", 1792000000000L + 6, true));

        list.add(new MensajeContacto("Agustín Silva", "+54 9 11 7734-2210", "agustin.silva@gmail.com",
                "12", "Monoambiente Premium (Palermo Soho)", "Palermo Soho, Buenos Aires",
                "¿Está permitido el alquiler temporal tipo Airbnb en el reglamento de copropiedad? Busco para inversión.",
                "10/10/2026", "10:00 AM", 1792000000000L + 5, false));

        return list;
    }

    private void renderMensajesList(View root) {
        if (getContext() == null || layoutPage1Mensajes == null) return;

        layoutPage1Mensajes.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        if (listaFiltrada.isEmpty()) {
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

        for (int i = startIndex; i < endIndex; i++) {
            MensajeContacto item = listaFiltrada.get(i);
            View cardView = inflater.inflate(R.layout.item_admin_mensaje_card, layoutPage1Mensajes, false);

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
            layoutPage1Mensajes.addView(cardView);
        }

        if (layoutPagination != null) {
            layoutPagination.setVisibility(maxPages > 1 ? View.VISIBLE : View.GONE);
        }
        renderPagination(root, false);
    }

    private void abrirModalDetalleMensaje(MensajeContacto item, View root) {
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

        // Acción: Copiar teléfono
        if (btnCopiarTel != null) {
            btnCopiarTel.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Teléfono cliente", item.clienteTelefono != null ? item.clienteTelefono : "");
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
                    String phone = item.clienteTelefono != null ? item.clienteTelefono.replaceAll("[^0-9+]", "") : "";
                    if (!phone.isEmpty()) {
                        Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                        dialIntent.setData(Uri.parse("tel:" + phone));
                        startActivity(dialIntent);
                    } else {
                        Toast.makeText(requireContext(), "El cliente no proporcionó teléfono", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "No se pudo iniciar la llamada", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(requireContext(), "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Acción: Marcar como leído
        if (btnMarcarLeido != null) {
            btnMarcarLeido.setOnClickListener(v -> {
                item.leido = true;
                actualizarEstadoBadgeDialog(tvEstado, btnMarcarLeido, true);

                // Guardar en la base de datos Room y en Supabase
                MensajeContactoRepositoryImpl.getInstance(requireContext()).marcarComoLeido(item);

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
                    currentPage = 1;
                    applyFilters(root);
                }

                @Override
                public void afterTextChanged(Editable s) { }
            });
        }

        if (chipGroupMensajes != null) {
            chipGroupMensajes.setOnCheckedChangeListener((group, checkedId) -> {
                currentPage = 1;
                applyFilters(root);
            });
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
                    currentPage = 1;
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
        currentPage = 1;
        applyFilters(root);
        Toast.makeText(requireContext(), "Filtro de fecha quitado", Toast.LENGTH_SHORT).show();
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

        renderMensajesList(root);
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
