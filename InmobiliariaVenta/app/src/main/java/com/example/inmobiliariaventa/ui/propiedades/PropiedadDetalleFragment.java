package com.example.inmobiliariaventa.ui.propiedades;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.data.repository.EmpresaRepository;
import com.google.android.material.button.MaterialButton;

public class PropiedadDetalleFragment extends Fragment {

    private EmpresaRepository empresaRepository;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_propiedad_detalle, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        empresaRepository = new EmpresaRepository(requireContext());
        // Carga y asegura que exista la configuración en la base de datos Room
        empresaRepository.cargarConfiguracionAsync(null);

        // Flecha de regreso
        View btnBack = view.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());
        }

        // Botón compartir
        View btnShare = view.findViewById(R.id.btn_share);
        if (btnShare != null) {
            btnShare.setOnClickListener(v -> {
                TextView tvTitulo = view.findViewById(R.id.tv_detalle_titulo);
                String titulo = tvTitulo != null ? tvTitulo.getText().toString() : "Propiedad";
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Mirá esta propiedad en InmoVentas");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Mirá esta propiedad en InmoVentas: " + titulo);
                startActivity(Intent.createChooser(shareIntent, "Compartir propiedad vía"));
            });
        }

        // Botón WhatsApp con número de la empresa guardado en base de datos
        MaterialButton btnWhatsapp = view.findViewById(R.id.btn_whatsapp_propiedad);
        if (btnWhatsapp != null) {
            btnWhatsapp.setOnClickListener(v -> abrirWhatsappEmpresa(view));
        }

        // Botón Llamar a la empresa
        MaterialButton btnLlamar = view.findViewById(R.id.btn_llamar_propiedad);
        if (btnLlamar != null) {
            btnLlamar.setOnClickListener(v -> llamarEmpresa());
        }

        // Botón Contactar agente
        MaterialButton btnContactar = view.findViewById(R.id.btn_contactar_agente);
        if (btnContactar != null) {
            btnContactar.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "Solicitud de contacto enviada. Un asesor se comunicará a la brevedad.", Toast.LENGTH_LONG).show();
            });
        }
    }

    private void abrirWhatsappEmpresa(View root) {
        try {
            String telefonoEmpresa = empresaRepository.getTelefonoWhatsappInmediato();
            if (telefonoEmpresa == null || telefonoEmpresa.trim().isEmpty()) {
                telefonoEmpresa = "+54 9 11 5842-1102";
            }

            // Sanitizar número a sólo dígitos
            String cleanPhone = telefonoEmpresa.replaceAll("[^0-9]", "");

            // Obtener datos de la propiedad para el mensaje predeterminado
            TextView tvTitulo = root.findViewById(R.id.tv_detalle_titulo);
            TextView tvPrecio = root.findViewById(R.id.tv_detalle_precio);

            String titulo = (tvTitulo != null && tvTitulo.getText() != null) ? tvTitulo.getText().toString() : "esta propiedad";
            String precio = (tvPrecio != null && tvPrecio.getText() != null) ? tvPrecio.getText().toString() : "";

            String mensaje;
            if (!precio.isEmpty()) {
                mensaje = "Hola, me interesa obtener información sobre la propiedad: " + titulo + " (" + precio + ") que vi en la aplicación.";
            } else {
                mensaje = "Hola, me interesa obtener información sobre la propiedad: " + titulo + " que vi en la aplicación.";
            }

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
}
