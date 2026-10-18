package com.example.inmobiliariaventa.ui.propiedades;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.utils.ImageLoader;
import com.example.inmobiliariaventa.utils.ZoomableImageView;

import java.util.ArrayList;
import java.util.List;

public class VisorFotosDialog extends DialogFragment {

    private List<String> fotos = new ArrayList<>();
    private int posicionInicial = 0;
    private String tituloPropiedad = "Propiedad";

    public static VisorFotosDialog newInstance(List<String> fotos, int posicionInicial, String titulo) {
        VisorFotosDialog dialog = new VisorFotosDialog();
        dialog.fotos = (fotos != null && !fotos.isEmpty()) ? new ArrayList<>(fotos) : new ArrayList<>();
        dialog.posicionInicial = Math.max(0, posicionInicial);
        dialog.tituloPropiedad = titulo != null ? titulo : "Propiedad";
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_visor_fotos, container, false);

        ViewPager2 viewPager = view.findViewById(R.id.vp_visor_fotos);
        TextView tvContador = view.findViewById(R.id.tv_visor_contador);
        TextView tvTitulo = view.findViewById(R.id.tv_visor_titulo);
        ImageView btnCerrar = view.findViewById(R.id.btn_cerrar_visor);
        View btnAnterior = view.findViewById(R.id.btn_foto_anterior);
        View btnSiguiente = view.findViewById(R.id.btn_foto_siguiente);

        tvTitulo.setText(tituloPropiedad);

        if (fotos.isEmpty()) {
            fotos.add(""); // Imagen de muestra si viene vacía
        }

        VisorAdapter adapter = new VisorAdapter(fotos);
        viewPager.setAdapter(adapter);

        if (posicionInicial < fotos.size()) {
            viewPager.setCurrentItem(posicionInicial, false);
        }

        int total = fotos.size();
        tvContador.setText((posicionInicial + 1) + " de " + total);

        if (total <= 1) {
            if (btnAnterior != null) btnAnterior.setVisibility(View.GONE);
            if (btnSiguiente != null) btnSiguiente.setVisibility(View.GONE);
            viewPager.setUserInputEnabled(false);
        } else {
            if (btnAnterior != null) btnAnterior.setVisibility(posicionInicial > 0 ? View.VISIBLE : View.GONE);
            if (btnSiguiente != null) btnSiguiente.setVisibility(posicionInicial < total - 1 ? View.VISIBLE : View.GONE);
            viewPager.setUserInputEnabled(true);
        }

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                tvContador.setText((position + 1) + " de " + total);
                if (total > 1) {
                    if (btnAnterior != null) btnAnterior.setVisibility(position > 0 ? View.VISIBLE : View.GONE);
                    if (btnSiguiente != null) btnSiguiente.setVisibility(position < total - 1 ? View.VISIBLE : View.GONE);
                }
            }
        });

        if (btnAnterior != null) {
            btnAnterior.setOnClickListener(v -> {
                int cur = viewPager.getCurrentItem();
                if (cur > 0) viewPager.setCurrentItem(cur - 1, true);
            });
        }

        if (btnSiguiente != null) {
            btnSiguiente.setOnClickListener(v -> {
                int cur = viewPager.getCurrentItem();
                if (cur < total - 1) viewPager.setCurrentItem(cur + 1, true);
            });
        }

        if (btnCerrar != null) {
            btnCerrar.setOnClickListener(v -> dismiss());
        }

        return view;
    }

    private static class VisorAdapter extends RecyclerView.Adapter<VisorAdapter.ViewHolder> {
        private final List<String> list;

        VisorAdapter(List<String> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_visor_foto, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String url = list.get(position);
            holder.zoomImageView.resetZoom();
            if (url != null && !url.trim().isEmpty()) {
                ImageLoader.loadImageFit(holder.zoomImageView, url);
            } else {
                holder.zoomImageView.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ZoomableImageView zoomImageView;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                zoomImageView = itemView.findViewById(R.id.zoomImageView);
            }
        }
    }
}
