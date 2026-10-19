package com.example.inmobiliariaventa.ui.propiedades;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.data.local.AppDatabase;
import com.example.inmobiliariaventa.data.repository.EmpresaRepository;
import com.example.inmobiliariaventa.domain.entity.MensajeContacto;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.domain.entity.TipoPropiedad;
import com.example.inmobiliariaventa.domain.entity.Disposicion;
import com.example.inmobiliariaventa.domain.entity.Usuario;
import com.example.inmobiliariaventa.domain.entity.Favorito;
import com.example.inmobiliariaventa.data.repository.FavoritoRepositoryImpl;
import com.example.inmobiliariaventa.data.repository.MensajeContactoRepositoryImpl;
import java.util.UUID;
import android.graphics.Typeface;
import com.example.inmobiliariaventa.utils.ImageLoader;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class PropiedadDetalleFragment extends Fragment {

    private EmpresaRepository empresaRepository;
    private PropiedadViewModel propiedadViewModel;
    private Propiedad currentPropiedad;

    private ViewPager2 vpDetalleBanner;
    private View cardDetalleFotosCount;
    private TextView tvDetalleTipo;
    private TextView tvDetalleFotosCount;
    private TextView tvDetallePrecio;
    private TextView tvDetalleTitulo;
    private TextView tvDetalleUbicacion;
    private TextView tvDetalleHabitaciones;
    private TextView tvDetalleBanos;
    private TextView tvDetalleM2;
    private TextView tvDetalleDescripcion;
    private TextView btnToggleDescripcion;
    private boolean isDescripcionExpandida = false;
    private TextView tvDetalleDireccionMapa;
    private View btnComoLlegar;
    private View cardMapaUbicacion;

    // Características especiales (Depto vs Casa)
    private View layoutSpecsDepto;
    private View layoutSpecsCasa;
    private TextView tvDetalleBalcon;
    private TextView tvDetalleAscensor;
    private TextView tvDetalleCochera;
    private TextView tvDetallePatio;

    // Disposición
    private View cardDetalleDisposicion;
    private TextView tvDetalleDisposicion;
    private TextView tvBadgeDisposicion;

    // Favoritos
    private View btnDetalleFavorito;
    private ImageView ivDetalleFavorito;
    private boolean isPropiedadFavorita = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_propiedad_detalle, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        empresaRepository = new EmpresaRepository(requireContext());
        empresaRepository.cargarConfiguracionAsync(null);

        // Bind Views
        vpDetalleBanner = view.findViewById(R.id.vp_detalle_banner);
        cardDetalleFotosCount = view.findViewById(R.id.card_detalle_fotos_count);
        tvDetalleTipo = view.findViewById(R.id.tv_detalle_tipo);
        tvDetalleFotosCount = view.findViewById(R.id.tv_detalle_fotos_count);
        tvDetallePrecio = view.findViewById(R.id.tv_detalle_precio);
        tvDetalleTitulo = view.findViewById(R.id.tv_detalle_titulo);
        tvDetalleUbicacion = view.findViewById(R.id.tv_detalle_ubicacion);
        tvDetalleHabitaciones = view.findViewById(R.id.tv_detalle_habitaciones);
        tvDetalleBanos = view.findViewById(R.id.tv_detalle_banos);
        tvDetalleM2 = view.findViewById(R.id.tv_detalle_m2);
        tvDetalleDescripcion = view.findViewById(R.id.tv_detalle_descripcion);
        btnToggleDescripcion = view.findViewById(R.id.btn_toggle_descripcion);
        tvDetalleDireccionMapa = view.findViewById(R.id.tv_detalle_direccion_mapa);
        btnComoLlegar = view.findViewById(R.id.btn_como_llegar);
        cardMapaUbicacion = view.findViewById(R.id.card_mapa_ubicacion);

        layoutSpecsDepto = view.findViewById(R.id.layout_specs_depto);
        layoutSpecsCasa = view.findViewById(R.id.layout_specs_casa);
        tvDetalleBalcon = view.findViewById(R.id.tv_detalle_balcon);
        tvDetalleAscensor = view.findViewById(R.id.tv_detalle_ascensor);
        tvDetalleCochera = view.findViewById(R.id.tv_detalle_cochera);
        tvDetallePatio = view.findViewById(R.id.tv_detalle_patio);

        cardDetalleDisposicion = view.findViewById(R.id.card_detalle_disposicion);
        tvDetalleDisposicion = view.findViewById(R.id.tv_detalle_disposicion);
        tvBadgeDisposicion = view.findViewById(R.id.tv_badge_disposicion);

        btnDetalleFavorito = view.findViewById(R.id.btn_detalle_favorito);
        ivDetalleFavorito = view.findViewById(R.id.iv_detalle_favorito);
        if (btnDetalleFavorito != null) {
            btnDetalleFavorito.setOnClickListener(v -> toggleFavorito());
        }

        // Toggle Expandir / Contraer descripción
        if (btnToggleDescripcion != null) {
            btnToggleDescripcion.setOnClickListener(v -> {
                if (tvDetalleDescripcion == null) return;
                isDescripcionExpandida = !isDescripcionExpandida;

                try {
                    if (getView() instanceof ViewGroup) {
                        android.transition.TransitionManager.beginDelayedTransition((ViewGroup) getView(), new android.transition.AutoTransition());
                    }
                } catch (Exception ignored) {}

                if (isDescripcionExpandida) {
                    tvDetalleDescripcion.setMaxLines(Integer.MAX_VALUE);
                    tvDetalleDescripcion.setEllipsize(null);
                    btnToggleDescripcion.setText("Leer menos ⌃");
                } else {
                    tvDetalleDescripcion.setMaxLines(3);
                    tvDetalleDescripcion.setEllipsize(TextUtils.TruncateAt.END);
                    btnToggleDescripcion.setText("Leer descripción completa ⌄");
                }
            });
        }

        // Click en la tarjeta del mapa para abrir Google Maps
        if (cardMapaUbicacion != null) {
            cardMapaUbicacion.setOnClickListener(v -> abrirMapaDireccion());
        }

        // Flecha de regreso
        View btnBack = view.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());
        }

        // Botón compartir
        View btnShare = view.findViewById(R.id.btn_share);
        if (btnShare != null) {
            btnShare.setOnClickListener(v -> compartirPropiedad());
        }

        // Botón WhatsApp con número oficial de la inmobiliaria
        MaterialButton btnWhatsapp = view.findViewById(R.id.btn_whatsapp_propiedad);
        if (btnWhatsapp != null) {
            btnWhatsapp.setOnClickListener(v -> abrirWhatsappEmpresa());
        }

        // Botón Llamar a la inmobiliaria
        MaterialButton btnLlamar = view.findViewById(R.id.btn_llamar_propiedad);
        if (btnLlamar != null) {
            btnLlamar.setOnClickListener(v -> llamarEmpresa());
        }

        // Botón Contactar agente / inmobiliaria (Guarda en la base de datos de mensajes)
        MaterialButton btnContactar = view.findViewById(R.id.btn_contactar_agente);
        if (btnContactar != null) {
            btnContactar.setOnClickListener(v -> contactarInmobiliaria());
        }

        // Botón Cómo llegar
        if (btnComoLlegar != null) {
            btnComoLlegar.setOnClickListener(v -> abrirMapaDireccion());
        }

        // Cargar datos reales de la propiedad seleccionada
        propiedadViewModel = new ViewModelProvider(requireActivity()).get(PropiedadViewModel.class);
        String propiedadId = getArguments() != null ? getArguments().getString("propiedad_id") : null;
        if (propiedadId != null && !propiedadId.isEmpty()) {
            propiedadViewModel.getPropiedadPorId(propiedadId).observe(getViewLifecycleOwner(), propiedad -> {
                if (propiedad != null) {
                    currentPropiedad = propiedad;
                    bindPropiedad(propiedad);
                }
            });
        }
    }

    private void bindPropiedad(Propiedad prop) {
        if (prop == null) return;

        // Formatear precio
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        format.setMaximumFractionDigits(0);
        if (tvDetallePrecio != null) {
            tvDetallePrecio.setText(format.format(prop.precio));
        }

        // Título
        String titulo = prop.titulo != null && !prop.titulo.trim().isEmpty() ? prop.titulo : prop.domicilio;
        if (tvDetalleTitulo != null) {
            tvDetalleTitulo.setText(titulo != null ? titulo : "Propiedad en Venta");
        }

        // Ubicación
        String ubicacion = (prop.domicilio != null ? prop.domicilio : "");
        if (prop.ciudad != null && !prop.ciudad.trim().isEmpty()) {
            ubicacion += (ubicacion.isEmpty() ? "" : ", ") + prop.ciudad;
        }
        if (prop.estadoProvincia != null && !prop.estadoProvincia.trim().isEmpty() && !ubicacion.contains(prop.estadoProvincia)) {
            ubicacion += (ubicacion.isEmpty() ? "" : ", ") + prop.estadoProvincia;
        }
        if (tvDetalleUbicacion != null) {
            tvDetalleUbicacion.setText(!ubicacion.isEmpty() ? ubicacion : "Ubicación disponible");
        }
        if (tvDetalleDireccionMapa != null) {
            tvDetalleDireccionMapa.setText(!ubicacion.isEmpty() ? ubicacion : "Dirección disponible");
        }

        // Habitaciones / Ambientes
        if (tvDetalleHabitaciones != null) {
            tvDetalleHabitaciones.setText(prop.ambientes > 0 ? String.valueOf(prop.ambientes) : "-");
        }

        // Baños
        if (tvDetalleBanos != null) {
            tvDetalleBanos.setText(prop.banos > 0 ? String.valueOf(prop.banos) : "-");
        }

        // Metros cuadrados
        if (tvDetalleM2 != null) {
            if (prop.superficieM2 != null && prop.superficieM2 > 0) {
                tvDetalleM2.setText(String.format(Locale.US, "%,d", prop.superficieM2.intValue()));
            } else {
                tvDetalleM2.setText("-");
            }
        }

        // Tipo / Estado tag
        if (tvDetalleTipo != null) {
            if (prop.tipoPropiedad != null) {
                tvDetalleTipo.setText(prop.tipoPropiedad.name());
            } else if (prop.estado != null) {
                tvDetalleTipo.setText(prop.estado);
            } else {
                tvDetalleTipo.setText("EN VENTA");
            }
        }

        // Descripción con control de expansión/contracción
        if (tvDetalleDescripcion != null) {
            String desc = (prop.descripcion != null && !prop.descripcion.trim().isEmpty())
                    ? prop.descripcion
                    : "Excelente oportunidad inmobiliaria con todas las comodidades y excelente ubicación.";
            tvDetalleDescripcion.setText(desc);

            isDescripcionExpandida = false;
            tvDetalleDescripcion.setMaxLines(3);
            tvDetalleDescripcion.setEllipsize(TextUtils.TruncateAt.END);

            if (btnToggleDescripcion != null) {
                btnToggleDescripcion.setText("Leer descripción completa ⌄");
                btnToggleDescripcion.setVisibility(View.GONE);

                // Determinar con precisión en el layout pass si el texto realmente supera las 3 líneas
                tvDetalleDescripcion.post(() -> {
                    if (tvDetalleDescripcion != null && btnToggleDescripcion != null && !isDescripcionExpandida) {
                        android.text.Layout layout = tvDetalleDescripcion.getLayout();
                        if (layout != null) {
                            int lineCount = layout.getLineCount();
                            boolean hasEllipsis = false;
                            if (lineCount > 0) {
                                hasEllipsis = layout.getEllipsisCount(lineCount - 1) > 0;
                            }
                            boolean superaTresLineas = hasEllipsis || lineCount > 3;
                            btnToggleDescripcion.setVisibility(superaTresLineas ? View.VISIBLE : View.GONE);
                        } else {
                            // Estimación segura: ~160 caracteres en pantalla móvil a 16sp
                            int saltos = desc.split("\n", -1).length;
                            boolean superaTresLineas = desc.length() > 160 || saltos > 3;
                            btnToggleDescripcion.setVisibility(superaTresLineas ? View.VISIBLE : View.GONE);
                        }
                    }
                });
            }
        }

        // Características especiales según la categoría (Departamento vs Casa)
        boolean esDepto = prop.tipoPropiedad == TipoPropiedad.DEPARTAMENTO;
        boolean esCasa = prop.tipoPropiedad == TipoPropiedad.CASA;

        if (esDepto) {
            if (layoutSpecsDepto != null) layoutSpecsDepto.setVisibility(View.VISIBLE);
            if (layoutSpecsCasa != null) layoutSpecsCasa.setVisibility(View.GONE);
            if (tvDetalleBalcon != null) tvDetalleBalcon.setText(prop.tieneBalcon ? "Sí" : "No");
            if (tvDetalleAscensor != null) tvDetalleAscensor.setText(prop.tieneAscensor ? "Sí" : "No");
        } else if (esCasa) {
            if (layoutSpecsCasa != null) layoutSpecsCasa.setVisibility(View.VISIBLE);
            if (layoutSpecsDepto != null) layoutSpecsDepto.setVisibility(View.GONE);
            if (tvDetalleCochera != null) tvDetalleCochera.setText(prop.tieneCochera ? "Sí" : "No");
            if (tvDetallePatio != null) tvDetallePatio.setText(prop.tienePatio ? "Sí" : "No");
        } else {
            // Si no está tipificado explícitamente, deducir por los atributos presentes
            if (prop.tieneBalcon || prop.tieneAscensor) {
                if (layoutSpecsDepto != null) layoutSpecsDepto.setVisibility(View.VISIBLE);
                if (layoutSpecsCasa != null) layoutSpecsCasa.setVisibility(View.GONE);
                if (tvDetalleBalcon != null) tvDetalleBalcon.setText(prop.tieneBalcon ? "Sí" : "No");
                if (tvDetalleAscensor != null) tvDetalleAscensor.setText(prop.tieneAscensor ? "Sí" : "No");
            } else {
                if (layoutSpecsCasa != null) layoutSpecsCasa.setVisibility(View.VISIBLE);
                if (layoutSpecsDepto != null) layoutSpecsDepto.setVisibility(View.GONE);
                if (tvDetalleCochera != null) tvDetalleCochera.setText(prop.tieneCochera ? "Sí" : "No");
                if (tvDetallePatio != null) tvDetallePatio.setText(prop.tienePatio ? "Sí" : "No");
            }
        }

        // Disposición debajo de la descripción (especialmente para Departamentos)
        if (cardDetalleDisposicion != null) {
            if (esDepto || (prop.disposicion != null && !esCasa)) {
                cardDetalleDisposicion.setVisibility(View.VISIBLE);
                String dispTexto = prop.disposicion != null ? formatearDisposicion(prop.disposicion) : "No especificada";
                if (tvDetalleDisposicion != null) {
                    tvDetalleDisposicion.setText(dispTexto);
                }
                if (tvBadgeDisposicion != null) {
                    if (prop.disposicion != null) {
                        switch (prop.disposicion) {
                            case FRENTE:
                                tvBadgeDisposicion.setText("Vista exterior");
                                break;
                            case CONTRAFRENTE:
                                tvBadgeDisposicion.setText("Vista al pulmón");
                                break;
                            case LATERAL:
                                tvBadgeDisposicion.setText("Vista lateral");
                                break;
                            case INTERNO:
                                tvBadgeDisposicion.setText("Vista interna");
                                break;
                            default:
                                tvBadgeDisposicion.setText("Edificio");
                                break;
                        }
                    } else {
                        tvBadgeDisposicion.setText("Edificio");
                    }
                }
            } else {
                cardDetalleDisposicion.setVisibility(View.GONE);
            }
        }

        // Galería y carrusel interactivo de fotos
        setupBannerGaleria(prop);

        // Verificar si la propiedad ya está en favoritos
        verificarEstadoFavorito(prop);
    }

    private String formatearDisposicion(Disposicion disposicion) {
        if (disposicion == null) return "No especificada";
        switch (disposicion) {
            case FRENTE:
                return "Frente";
            case CONTRAFRENTE:
                return "Contrafrente";
            case LATERAL:
                return "Lateral";
            case INTERNO:
                return "Interno";
            default:
                return disposicion.name();
        }
    }

    private void setupBannerGaleria(Propiedad prop) {
        if (vpDetalleBanner == null) return;

        List<String> listaFotos = new ArrayList<>();
        if (prop != null && prop.imagenes != null && !prop.imagenes.isEmpty()) {
            listaFotos.addAll(prop.imagenes);
        } else {
            listaFotos.add(""); // Placeholder si no tiene fotos
        }

        String titulo = prop != null && prop.titulo != null ? prop.titulo : "Propiedad";

        BannerAdapter bannerAdapter = new BannerAdapter(listaFotos, pos -> {
            abrirVisorFotos(listaFotos, pos, titulo);
        });
        vpDetalleBanner.setAdapter(bannerAdapter);

        int totalFotos = listaFotos.size();
        if (tvDetalleFotosCount != null) {
            tvDetalleFotosCount.setText("1 / " + totalFotos);
        }

        vpDetalleBanner.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (tvDetalleFotosCount != null) {
                    tvDetalleFotosCount.setText((position + 1) + " / " + totalFotos);
                }
            }
        });

        if (cardDetalleFotosCount != null) {
            cardDetalleFotosCount.setOnClickListener(v -> {
                abrirVisorFotos(listaFotos, vpDetalleBanner.getCurrentItem(), titulo);
            });
        }
    }

    private void abrirVisorFotos(List<String> fotos, int posicionInicial, String titulo) {
        VisorFotosDialog dialog = VisorFotosDialog.newInstance(fotos, posicionInicial, titulo);
        dialog.show(getParentFragmentManager(), "visor_fotos");
    }

    private static class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.ViewHolder> {
        private final List<String> fotos;
        private final OnBannerClickListener listener;

        interface OnBannerClickListener {
            void onBannerClick(int position);
        }

        BannerAdapter(List<String> fotos, OnBannerClickListener listener) {
            this.fotos = fotos;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banner_foto, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String url = fotos.get(position);
            if (url != null && !url.trim().isEmpty()) {
                ImageLoader.loadImage(holder.imageView, url);
            } else {
                holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBannerClick(position);
                }
            });
        }

        @Override
        public int getItemCount() {
            return fotos.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.iv_banner_imagen);
            }
        }
    }

    private void abrirWhatsappEmpresa() {
        try {
            String telefonoEmpresa = empresaRepository.getTelefonoWhatsappInmediato();
            if (telefonoEmpresa == null || telefonoEmpresa.trim().isEmpty()) {
                telefonoEmpresa = "+54 9 11 5842-1102";
            }
            String cleanPhone = telefonoEmpresa.replaceAll("[^0-9]", "");

            String titulo = currentPropiedad != null && currentPropiedad.titulo != null ? currentPropiedad.titulo : "la propiedad";
            String precio = "";
            if (currentPropiedad != null && currentPropiedad.precio > 0) {
                NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
                format.setMaximumFractionDigits(0);
                precio = " (" + format.format(currentPropiedad.precio) + ")";
            }

            String mensaje = "Hola, me interesa obtener información sobre " + titulo + precio + " que vi en la aplicación Home Finder.";
            String url = "https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + Uri.encode(mensaje);

            Intent whatsappIntent = new Intent(Intent.ACTION_VIEW);
            whatsappIntent.setData(Uri.parse(url));
            startActivity(whatsappIntent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No se pudo abrir WhatsApp. Verifique tener la aplicación instalada.", Toast.LENGTH_SHORT).show();
        }
    }

    private void llamarEmpresa() {
        try {
            String telefonoEmpresa = empresaRepository.getTelefonoWhatsappInmediato();
            if (telefonoEmpresa == null || telefonoEmpresa.trim().isEmpty()) {
                telefonoEmpresa = "+54 9 11 5842-1102";
            }
            String cleanPhone = telefonoEmpresa.replaceAll("[^0-9+]", "");
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.setData(Uri.parse("tel:" + cleanPhone));
            startActivity(dialIntent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No se pudo iniciar la llamada.", Toast.LENGTH_SHORT).show();
        }
    }

    private void contactarInmobiliaria() {
        if (getContext() == null) return;

        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("is_user_logged_in", false);
        String userId = prefs.getString("user_id", "");

        // Solo clientes registrados pueden enviar mensajes
        if (!isLoggedIn || userId.isEmpty()) {
            mostrarDialogoRequerirLogin(
                    "Iniciá sesión para contactar",
                    "Para enviar una consulta directa a la inmobiliaria y coordinar visitas, necesitás ingresar con tu cuenta de cliente en Home Finder.",
                    R.drawable.ic_chat
            );
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_contactar_propiedad, null);

        TextView tvPropiedadTitulo = dialogView.findViewById(R.id.dialog_tv_propiedad_titulo);
        TextView tvPropiedadUbicacion = dialogView.findViewById(R.id.dialog_tv_propiedad_ubicacion);
        TextInputEditText etNombre = dialogView.findViewById(R.id.dialog_et_nombre);
        TextInputEditText etTelefono = dialogView.findViewById(R.id.dialog_et_telefono);
        TextInputEditText etEmail = dialogView.findViewById(R.id.dialog_et_email);
        TextInputEditText etMensaje = dialogView.findViewById(R.id.dialog_et_mensaje);
        ImageButton btnCerrar = dialogView.findViewById(R.id.btn_cerrar_dialog_contacto);
        MaterialButton btnCancelar = dialogView.findViewById(R.id.dialog_btn_cancelar);
        MaterialButton btnEnviar = dialogView.findViewById(R.id.dialog_btn_enviar);

        Chip chipVisita = dialogView.findViewById(R.id.chip_visita);
        Chip chipFinanciacion = dialogView.findViewById(R.id.chip_financiacion);
        Chip chipPermuta = dialogView.findViewById(R.id.chip_permuta);
        Chip chipCredito = dialogView.findViewById(R.id.chip_credito);

        String titulo = currentPropiedad != null && currentPropiedad.titulo != null ? currentPropiedad.titulo : "Propiedad";
        String ubicacion = "";
        if (currentPropiedad != null) {
            ubicacion = currentPropiedad.domicilio != null ? currentPropiedad.domicilio : "";
            if (currentPropiedad.ciudad != null && !currentPropiedad.ciudad.isEmpty()) {
                ubicacion += (ubicacion.isEmpty() ? "" : ", ") + currentPropiedad.ciudad;
            }
        }
        if (ubicacion.isEmpty()) ubicacion = "Ubicación disponible";

        tvPropiedadTitulo.setText(titulo);
        tvPropiedadUbicacion.setText(ubicacion);

        // Pre-cargar datos del cliente registrado (nombre, teléfono, correo)
        String savedNombre = prefs.getString("user_nombre", "");
        String savedTelefono = prefs.getString("user_telefono", "");
        String savedEmail = prefs.getString("user_email", "");

        if (!savedNombre.isEmpty()) etNombre.setText(savedNombre);
        if (!savedTelefono.isEmpty()) etTelefono.setText(savedTelefono);
        if (!savedEmail.isEmpty()) etEmail.setText(savedEmail);

        // Si el teléfono no estaba en prefs, buscarlo en la base de datos local
        if (savedTelefono.isEmpty() && (!userId.isEmpty() || !savedEmail.isEmpty())) {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                Usuario u = !userId.isEmpty()
                        ? AppDatabase.getInstance(requireContext()).usuarioDao().obtenerUsuarioDirectoPorId(userId)
                        : AppDatabase.getInstance(requireContext()).usuarioDao().obtenerPorEmail(savedEmail);
                if (u != null && u.telefono != null && !u.telefono.isEmpty()) {
                    String telRecuperado = u.telefono;
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (etTelefono != null && (etTelefono.getText() == null || etTelefono.getText().toString().trim().isEmpty())) {
                                etTelefono.setText(telRecuperado);
                            }
                        });
                    }
                }
            });
        }

        etMensaje.setText("Hola, me interesa la propiedad '" + titulo + "' y quisiera coordinar una visita o recibir más información.");

        // Chips interactivos
        if (chipVisita != null) {
            chipVisita.setOnClickListener(v -> etMensaje.setText("Hola, quisiera coordinar una visita presencial para conocer el inmueble."));
        }
        if (chipFinanciacion != null) {
            chipFinanciacion.setOnClickListener(v -> etMensaje.setText("Hola, quisiera consultar las opciones de financiación y formas de pago disponibles para esta propiedad."));
        }
        if (chipPermuta != null) {
            chipPermuta.setOnClickListener(v -> etMensaje.setText("Hola, me gustaría saber si aceptan permuta o propiedad de menor valor como parte de pago."));
        }
        if (chipCredito != null) {
            chipCredito.setOnClickListener(v -> etMensaje.setText("Hola, ¿la propiedad es apta para crédito hipotecario bancario?"));
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (btnCerrar != null) btnCerrar.setOnClickListener(v -> dialog.dismiss());
        if (btnCancelar != null) btnCancelar.setOnClickListener(v -> dialog.dismiss());

        if (btnEnviar != null) {
            final String finalUbicacion = ubicacion;
            btnEnviar.setOnClickListener(v -> {
                String nombre = etNombre.getText() != null ? etNombre.getText().toString().trim() : "";
                String telefono = etTelefono.getText() != null ? etTelefono.getText().toString().trim() : "";
                String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
                String mensaje = etMensaje.getText() != null ? etMensaje.getText().toString().trim() : "";

                if (nombre.isEmpty()) {
                    etNombre.setError("Por favor ingresa tu nombre");
                    etNombre.requestFocus();
                    return;
                }
                if (telefono.isEmpty()) {
                    etTelefono.setError("Por favor ingresa un teléfono de contacto");
                    etTelefono.requestFocus();
                    return;
                }
                if (mensaje.isEmpty()) {
                    etMensaje.setError("Por favor escribe tu consulta");
                    etMensaje.requestFocus();
                    return;
                }

                // Opción 2: Los datos editados se aplican únicamente a este mensaje puntual,
                // manteniendo intacto el perfil y la cuenta original del cliente.

                // Formatear fecha y hora actual
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                Calendar now = Calendar.getInstance();
                String fecha = dateFormat.format(now.getTime());
                String hora = timeFormat.format(now.getTime());
                long timestamp = now.getTimeInMillis();

                MensajeContacto nuevoMensaje = new MensajeContacto(
                        nombre,
                        telefono,
                        email,
                        currentPropiedad != null ? currentPropiedad.id : "",
                        titulo,
                        finalUbicacion,
                        mensaje,
                        fecha,
                        hora,
                        timestamp,
                        false
                );
                nuevoMensaje.remoteId = UUID.randomUUID().toString();

                MensajeContactoRepositoryImpl.getInstance(requireContext()).enviarMensaje(nuevoMensaje);

                dialog.dismiss();
                Toast.makeText(requireContext(), "¡Mensaje enviado con éxito! Un asesor de la inmobiliaria revisará tu consulta.", Toast.LENGTH_LONG).show();
            });
        }

        dialog.show();
    }

    private void mostrarDialogoRequerirLogin(String titulo, String mensaje, int iconRes) {
        if (getContext() == null) return;

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_requerir_login, null);

        ImageView ivIcon = dialogView.findViewById(R.id.iv_dialog_login_icon);
        TextView tvTitulo = dialogView.findViewById(R.id.tv_dialog_login_titulo);
        TextView tvMensaje = dialogView.findViewById(R.id.tv_dialog_login_mensaje);
        MaterialButton btnLogin = dialogView.findViewById(R.id.btn_dialog_login);
        MaterialButton btnRegistro = dialogView.findViewById(R.id.btn_dialog_registro);
        MaterialButton btnCancelar = dialogView.findViewById(R.id.btn_dialog_cancelar);

        if (ivIcon != null && iconRes != 0) {
            ivIcon.setImageResource(iconRes);
        }
        if (tvTitulo != null && titulo != null) {
            tvTitulo.setText(titulo);
        }
        if (tvMensaje != null && mensaje != null) {
            tvMensaje.setText(mensaje);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> {
                dialog.dismiss();
                if (getView() != null) {
                    try {
                        Navigation.findNavController(getView()).navigate(R.id.nav_login);
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Por favor ingresa desde la sección Perfil", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (btnRegistro != null) {
            btnRegistro.setOnClickListener(v -> {
                dialog.dismiss();
                if (getView() != null) {
                    try {
                        Navigation.findNavController(getView()).navigate(R.id.nav_registro);
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Por favor registrate desde la sección Perfil", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (btnCancelar != null) {
            btnCancelar.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void toggleFavorito() {
        if (getContext() == null || currentPropiedad == null) return;

        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("is_user_logged_in", false);
        String userId = prefs.getString("user_id", "");

        if (!isLoggedIn || userId.isEmpty()) {
            mostrarDialogoRequerirLogin(
                    "Guardá tus favoritos",
                    "Para guardar esta propiedad en tu lista de favoritos y acceder a ella cuando quieras, necesitás ingresar con tu cuenta de cliente en Home Finder.",
                    R.drawable.ic_favorite
            );
            return;
        }

        final boolean nuevoEstado = !isPropiedadFavorita;
        isPropiedadFavorita = nuevoEstado;
        actualizarIconoFavorito(nuevoEstado);
        if (ivDetalleFavorito != null) {
            ivDetalleFavorito.animate().scaleX(1.3f).scaleY(1.3f).setDuration(120)
                    .withEndAction(() -> ivDetalleFavorito.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start())
                    .start();
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            // Asegurar que el usuario existe en Room para evitar fallos de Foreign Key
            Usuario u = db.usuarioDao().obtenerUsuarioDirectoPorId(userId);
            if (u == null) {
                Usuario nuevoU = new Usuario();
                nuevoU.id = userId;
                nuevoU.nombre = prefs.getString("user_nombre", "Usuario");
                nuevoU.email = prefs.getString("user_email", "");
                nuevoU.telefono = prefs.getString("user_telefono", "");
                nuevoU.rol = prefs.getString("user_rol", "CLIENTE");
                db.usuarioDao().insertar(nuevoU);
            }

            if (nuevoEstado) {
                Favorito fav = new Favorito();
                fav.usuarioId = userId;
                fav.propiedadId = currentPropiedad.id;
                fav.fechaGuardado = System.currentTimeMillis();
                FavoritoRepositoryImpl.getInstance(requireContext()).agregarFavorito(fav);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (getView() == null) return;
                        Snackbar snackbar = Snackbar.make(getView(), "¡Propiedad guardada en Favoritos!", Snackbar.LENGTH_SHORT)
                                .setTextColor(ContextCompat.getColor(requireContext(), R.color.white));

                        View snackbarView = snackbar.getView();
                        snackbarView.setBackgroundResource(R.drawable.bg_snackbar_favoritos);
                        snackbarView.setBackgroundTintList(null);

                        ViewGroup.LayoutParams lp = snackbarView.getLayoutParams();
                        if (lp instanceof ViewGroup.MarginLayoutParams) {
                            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) lp;
                            int marginH = (int) (16 * getResources().getDisplayMetrics().density);
                            int marginB = (int) (8 * getResources().getDisplayMetrics().density);
                            marginParams.setMargins(marginH, marginParams.topMargin, marginH, marginB);
                            snackbarView.setLayoutParams(marginParams);
                        }
                        snackbarView.setElevation(8 * getResources().getDisplayMetrics().density);

                        snackbar.show();
                    });
                }
            } else {
                Favorito existente = db.favoritoDao().obtenerFavorito(userId, currentPropiedad.id);
                final Favorito favRespaldo = new Favorito();
                favRespaldo.usuarioId = userId;
                favRespaldo.propiedadId = currentPropiedad.id;
                favRespaldo.fechaGuardado = (existente != null && existente.fechaGuardado > 0)
                        ? existente.fechaGuardado : System.currentTimeMillis();
                favRespaldo.estadoSincronizacion = "PENDIENTE_SUBIR";

                FavoritoRepositoryImpl.getInstance(requireContext()).eliminarFavorito(userId, currentPropiedad.id);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (getView() == null) return;
                        Snackbar snackbar = Snackbar.make(getView(), "Propiedad eliminada de Favoritos", Snackbar.LENGTH_LONG)
                                .setAction("DESHACER", vUndo -> {
                                    actualizarIconoFavorito(true);
                                    FavoritoRepositoryImpl.getInstance(requireContext()).agregarFavorito(favRespaldo);
                                })
                                .setActionTextColor(ContextCompat.getColor(requireContext(), R.color.colorSnackbarAction))
                                .setTextColor(ContextCompat.getColor(requireContext(), R.color.white));

                        View snackbarView = snackbar.getView();
                        snackbarView.setBackgroundResource(R.drawable.bg_snackbar_favoritos);
                        snackbarView.setBackgroundTintList(null);

                        ViewGroup.LayoutParams lp = snackbarView.getLayoutParams();
                        if (lp instanceof ViewGroup.MarginLayoutParams) {
                            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) lp;
                            int marginH = (int) (16 * getResources().getDisplayMetrics().density);
                            int marginB = (int) (8 * getResources().getDisplayMetrics().density);
                            marginParams.setMargins(marginH, marginParams.topMargin, marginH, marginB);
                            snackbarView.setLayoutParams(marginParams);
                        }
                        snackbarView.setElevation(8 * getResources().getDisplayMetrics().density);

                        TextView actionTextView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_action);
                        if (actionTextView != null) {
                            actionTextView.setTypeface(Typeface.DEFAULT_BOLD);
                        }

                        snackbar.show();
                    });
                }
            }
        });
    }

    private void verificarEstadoFavorito(Propiedad prop) {
        if (prop == null || getContext() == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("is_user_logged_in", false);
        String userId = prefs.getString("user_id", "");

        if (!isLoggedIn || userId.isEmpty()) {
            actualizarIconoFavorito(false);
            return;
        }

        FavoritoRepositoryImpl.getInstance(requireContext()).sincronizarConNube(userId, null);

        AppDatabase.getInstance(requireContext())
                .favoritoDao()
                .esFavoritoLiveData(userId, prop.id)
                .observe(getViewLifecycleOwner(), fav -> {
                    isPropiedadFavorita = (fav != null && fav);
                    actualizarIconoFavorito(isPropiedadFavorita);
                });
    }

    private void actualizarIconoFavorito(boolean fav) {
        if (ivDetalleFavorito == null || getContext() == null) return;
        if (fav) {
            ivDetalleFavorito.setImageResource(R.drawable.ic_favorite);
            ivDetalleFavorito.setColorFilter(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
            if (btnDetalleFavorito instanceof androidx.cardview.widget.CardView) {
                ((androidx.cardview.widget.CardView) btnDetalleFavorito).setCardBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.colorPrimaryFixed)
                );
            }
        } else {
            ivDetalleFavorito.setImageResource(R.drawable.ic_favorite_border);
            ivDetalleFavorito.setColorFilter(ContextCompat.getColor(requireContext(), R.color.colorOnSurfaceVariant));
            if (btnDetalleFavorito instanceof androidx.cardview.widget.CardView) {
                ((androidx.cardview.widget.CardView) btnDetalleFavorito).setCardBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.colorSurfaceContainerHighest)
                );
            }
        }
    }

    private void compartirPropiedad() {
        String titulo = currentPropiedad != null && currentPropiedad.titulo != null ? currentPropiedad.titulo : "Propiedad en venta";
        String desc = currentPropiedad != null && currentPropiedad.domicilio != null ? currentPropiedad.domicilio : "";
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Mirá esta propiedad en Home Finder");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Mirá esta propiedad en Home Finder: " + titulo + (desc.isEmpty() ? "" : " - " + desc));
        startActivity(Intent.createChooser(shareIntent, "Compartir propiedad vía"));
    }

    private void abrirMapaDireccion() {
        if (currentPropiedad != null) {
            String query = (currentPropiedad.domicilio != null ? currentPropiedad.domicilio : "") +
                    (currentPropiedad.ciudad != null ? ", " + currentPropiedad.ciudad : "");
            if (!query.trim().isEmpty()) {
                Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(query.trim()));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                try {
                    startActivity(mapIntent);
                } catch (Exception e) {
                    Intent webMap = new Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=" + Uri.encode(query.trim())));
                    startActivity(webMap);
                }
            }
        }
    }
}
