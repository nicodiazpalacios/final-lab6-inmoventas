package com.example.inmobiliariaventa.ui.admin;

import android.graphics.Typeface;
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
import androidx.core.content.ContextCompat;
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

    private int currentPage = 1;
    private int startPage = 1;
    private final int maxPages = 7; // 42 contratos / 6 por página

    private MaterialCardView btnPagePrev, btnPageNext;
    private MaterialCardView cardPage1, cardPage2, cardPage3, cardPage4;
    private TextView tvPagePrev, tvPageNext;
    private TextView tvPage1, tvPage2, tvPage3, tvPage4;

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

        // Bind Filter and Sort buttons
        View btnFilterStatus = view.findViewById(R.id.btn_filter_status);
        if (btnFilterStatus != null) {
            btnFilterStatus.setOnClickListener(v -> {
                android.widget.PopupMenu popup = new android.widget.PopupMenu(requireContext(), btnFilterStatus);
                popup.getMenu().add("Todos");
                popup.getMenu().add("Activo");
                popup.getMenu().add("Pendiente");
                popup.getMenu().add("Finalizado");
                popup.setOnMenuItemClickListener(item -> {
                    String status = item.getTitle().toString();
                    filterByStatus(status);
                    return true;
                });
                popup.show();
            });
        }

        View btnSortContracts = view.findViewById(R.id.btn_sort_contracts);
        if (btnSortContracts != null) {
            btnSortContracts.setOnClickListener(v -> {
                android.widget.PopupMenu popup = new android.widget.PopupMenu(requireContext(), btnSortContracts);
                popup.getMenu().add("Recientes");
                popup.getMenu().add("Mayor Monto");
                popup.getMenu().add("Menor Monto");
                popup.setOnMenuItemClickListener(item -> {
                    Toast.makeText(requireContext(), "Orden: " + item.getTitle(), Toast.LENGTH_SHORT).show();
                    return true;
                });
                popup.show();
            });
        }

        // Setup real-time search filter
        setupSearchFilter();

        setupPagination(view);
    }

    private void filterByStatus(String status) {
        if ("Todos".equals(status)) {
            if (cardContrato1 != null) cardContrato1.setVisibility(View.VISIBLE);
            if (cardContrato2 != null) cardContrato2.setVisibility(View.VISIBLE);
            if (cardContrato3 != null) cardContrato3.setVisibility(View.VISIBLE);
            if (cardContrato4 != null) cardContrato4.setVisibility(View.VISIBLE);
        } else if ("Activo".equals(status)) {
            if (cardContrato1 != null) cardContrato1.setVisibility(View.VISIBLE);
            if (cardContrato2 != null) cardContrato2.setVisibility(View.VISIBLE);
            if (cardContrato3 != null) cardContrato3.setVisibility(View.GONE);
            if (cardContrato4 != null) cardContrato4.setVisibility(View.GONE);
        } else if ("Pendiente".equals(status)) {
            if (cardContrato1 != null) cardContrato1.setVisibility(View.GONE);
            if (cardContrato2 != null) cardContrato2.setVisibility(View.GONE);
            if (cardContrato3 != null) cardContrato3.setVisibility(View.VISIBLE);
            if (cardContrato4 != null) cardContrato4.setVisibility(View.GONE);
        } else if ("Finalizado".equals(status)) {
            if (cardContrato1 != null) cardContrato1.setVisibility(View.GONE);
            if (cardContrato2 != null) cardContrato2.setVisibility(View.GONE);
            if (cardContrato3 != null) cardContrato3.setVisibility(View.GONE);
            if (cardContrato4 != null) cardContrato4.setVisibility(View.VISIBLE);
        }
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

        if (cardPage1 != null) {
            cardPage1.setOnClickListener(v -> goToPage(startPage, root));
        }
        if (cardPage2 != null) {
            cardPage2.setOnClickListener(v -> goToPage(startPage + 1, root));
        }
        if (cardPage3 != null) {
            cardPage3.setOnClickListener(v -> goToPage(startPage + 2, root));
        }
        if (cardPage4 != null) {
            cardPage4.setOnClickListener(v -> goToPage(startPage + 3, root));
        }

        renderPagination(root, false);
    }

    private void goToPage(int page, View root) {
        if (page < 1) page = 1;
        if (page > maxPages) page = maxPages;
        if (page == currentPage && startPage == page) return;

        currentPage = page;
        // As requested: "si presionas por ejemplo la pagina 2 que muestre del 2 que esta seleccionada hasta 5 y asi"
        startPage = currentPage;
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

        // Slot 1 is always the selected page since startPage == currentPage
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
