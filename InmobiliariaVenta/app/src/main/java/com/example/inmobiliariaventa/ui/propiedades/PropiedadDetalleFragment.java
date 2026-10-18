package com.example.inmobiliariaventa.ui.propiedades;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.data.repository.EmpresaRepository;
import com.example.inmobiliariaventa.domain.entity.Propiedad;
import com.example.inmobiliariaventa.utils.ImageLoader;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.ArrayList;
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
    private TextView tvDetalleDireccionMapa;
    private View btnComoLlegar;

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
        tvDetalleDireccionMapa = view.findViewById(R.id.tv_detalle_direccion_mapa);
        btnComoLlegar = view.findViewById(R.id.btn_como_llegar);

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

        // Botón Contactar agente / inmobiliaria
        MaterialButton btnContactar = view.findViewById(R.id.btn_contactar_agente);
        if (btnContactar != null) {
            btnContactar.setOnClickListener(v -> contactarInmobiliaria());
        }

        // Botón Cómo llegar
        if (btnComoLlegar != null) {
            btnComoLlegar.setOnClickListener(v -> abrirMapaDireccion());
        }

        // Cargar datos reales de la propiedad seleccionada
        propiedadViewModel = new ViewModelProvider(this).get(PropiedadViewModel.class);
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

        // Descripción
        if (tvDetalleDescripcion != null) {
            if (prop.descripcion != null && !prop.descripcion.trim().isEmpty()) {
                tvDetalleDescripcion.setText(prop.descripcion);
            } else {
                tvDetalleDescripcion.setText("Excelente oportunidad inmobiliaria con todas las comodidades y excelente ubicación.");
            }
        }

        // Galería y carrusel interactivo de fotos
        setupBannerGaleria(prop);
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
        try {
            String emailEmpresa = empresaRepository.getEmailInmediato();
            if (emailEmpresa == null || emailEmpresa.trim().isEmpty()) {
                emailEmpresa = "contacto@homefinder.com";
            }
            String titulo = currentPropiedad != null && currentPropiedad.titulo != null ? currentPropiedad.titulo : "Propiedad";

            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:" + emailEmpresa));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Consulta por: " + titulo);
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Hola, quisiera consultar por la propiedad '" + titulo + "' publicada en Home Finder.");

            startActivity(Intent.createChooser(emailIntent, "Contactar a la inmobiliaria"));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Solicitud de contacto enviada. Un asesor se comunicará a la brevedad.", Toast.LENGTH_LONG).show();
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
