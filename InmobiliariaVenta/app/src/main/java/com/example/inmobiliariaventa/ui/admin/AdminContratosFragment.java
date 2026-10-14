package com.example.inmobiliariaventa.ui.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

public class AdminContratosFragment extends Fragment {

    private TextInputEditText etBuscarContrato;
    private MaterialCardView cardContrato1;
    private MaterialCardView cardContrato2;
    private MaterialCardView cardContrato3;
    private MaterialCardView cardContrato4;

    private TextView tvCliente1, tvPropiedad1;
    private TextView tvCliente2, tvPropiedad2;
    private TextView tvCliente3, tvPropiedad3;
    private TextView tvCliente4, tvPropiedad4;

    private FloatingActionButton fabNuevoContrato;

    public AdminContratosFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_contratos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind search input
        etBuscarContrato = view.findViewById(R.id.et_buscar_contrato);

        // Bind cards
        cardContrato1 = view.findViewById(R.id.card_contrato_1);
        cardContrato2 = view.findViewById(R.id.card_contrato_2);
        cardContrato3 = view.findViewById(R.id.card_contrato_3);
        cardContrato4 = view.findViewById(R.id.card_contrato_4);

        // Bind card texts for filtering
        tvCliente1 = view.findViewById(R.id.tv_cliente_1);
        tvPropiedad1 = view.findViewById(R.id.tv_propiedad_1);

        tvCliente2 = view.findViewById(R.id.tv_cliente_2);
        tvPropiedad2 = view.findViewById(R.id.tv_propiedad_2);

        tvCliente3 = view.findViewById(R.id.tv_cliente_3);
        tvPropiedad3 = view.findViewById(R.id.tv_propiedad_3);

        tvCliente4 = view.findViewById(R.id.tv_cliente_4);
        tvPropiedad4 = view.findViewById(R.id.tv_propiedad_4);

        // Bind action buttons
        setupCardActions(view, R.id.btn_modificar_1, R.id.btn_baja_1, "Juan Pérez");
        setupCardActions(view, R.id.btn_modificar_2, R.id.btn_baja_2, "Elena Rodríguez");
        setupCardActions(view, R.id.btn_modificar_3, R.id.btn_baja_3, "Roberto Gómez");
        setupCardActions(view, R.id.btn_modificar_4, R.id.btn_baja_4, "Ana Martínez");

        // Bind FAB
        fabNuevoContrato = view.findViewById(R.id.fab_nuevo_contrato);
        if (fabNuevoContrato != null) {
            fabNuevoContrato.setOnClickListener(v -> 
                Navigation.findNavController(v).navigate(R.id.action_nav_contratos_to_nav_admin_agregar_contrato)
            );
        }

        // Setup real-time search filter
        setupSearchFilter();
    }

    private void setupCardActions(View root, int btnModificarId, int btnBajaId, String clienteName) {
        MaterialButton btnModificar = root.findViewById(btnModificarId);
        MaterialButton btnBaja = root.findViewById(btnBajaId);

        if (btnModificar != null) {
            btnModificar.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Modificar contrato de " + clienteName, Toast.LENGTH_SHORT).show()
            );
        }

        if (btnBaja != null) {
            btnBaja.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Dar de baja contrato de " + clienteName, Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void setupSearchFilter() {
        if (etBuscarContrato == null) return;

        etBuscarContrato.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterContracts(s != null ? s.toString().trim().toLowerCase() : "");
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void filterContracts(String query) {
        filterSingleCard(cardContrato1, tvCliente1, tvPropiedad1, query);
        filterSingleCard(cardContrato2, tvCliente2, tvPropiedad2, query);
        filterSingleCard(cardContrato3, tvCliente3, tvPropiedad3, query);
        filterSingleCard(cardContrato4, tvCliente4, tvPropiedad4, query);
    }

    private void filterSingleCard(MaterialCardView card, TextView tvCliente, TextView tvPropiedad, String query) {
        if (card == null) return;
        if (query.isEmpty()) {
            card.setVisibility(View.VISIBLE);
            return;
        }

        String cliente = tvCliente != null ? tvCliente.getText().toString().toLowerCase() : "";
        String propiedad = tvPropiedad != null ? tvPropiedad.getText().toString().toLowerCase() : "";

        if (cliente.contains(query) || propiedad.contains(query)) {
            card.setVisibility(View.VISIBLE);
        } else {
            card.setVisibility(View.GONE);
        }
    }
}
