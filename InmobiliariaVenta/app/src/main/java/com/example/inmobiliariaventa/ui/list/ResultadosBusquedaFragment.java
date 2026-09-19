package com.example.inmobiliariaventa.ui.list;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.PopupMenu;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.inmobiliariaventa.R;

public class ResultadosBusquedaFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_resultados_busqueda, container, false);
        
        // Volver a los filtros al tocar el icono de configuración (tune)
        ImageView btnFiltrosTune = view.findViewById(R.id.btnFiltrosTune);
        btnFiltrosTune.setOnClickListener(v -> {
            Navigation.findNavController(v).navigateUp();
        });
        
        // Menú desplegable para tipo de propiedad
        View chipTipoPropiedad = view.findViewById(R.id.chipTipoPropiedad);
        TextView txtTipoPropiedad = view.findViewById(R.id.txtTipoPropiedad);
        
        chipTipoPropiedad.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(requireContext(), chipTipoPropiedad);
            popupMenu.getMenu().add("Casa");
            popupMenu.getMenu().add("Departamento");
            
            popupMenu.setOnMenuItemClickListener(item -> {
                txtTipoPropiedad.setText(item.getTitle());
                return true;
            });
            
            popupMenu.show();
        });
        
        // Menú desplegable para ordenar
        View btnOrdenar = view.findViewById(R.id.btnOrdenar);
        TextView txtSortOption = view.findViewById(R.id.txtSortOption);
        
        btnOrdenar.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(requireContext(), btnOrdenar);
            popupMenu.getMenu().add("Recomendados");
            popupMenu.getMenu().add("Menor Precio");
            popupMenu.getMenu().add("Mayor Precio");
            
            popupMenu.setOnMenuItemClickListener(item -> {
                txtSortOption.setText(item.getTitle());
                return true;
            });
            
            popupMenu.show();
        });

        return view;
    }
}
