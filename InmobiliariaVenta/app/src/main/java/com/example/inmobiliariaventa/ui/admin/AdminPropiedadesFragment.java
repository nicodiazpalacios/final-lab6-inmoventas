package com.example.inmobiliariaventa.ui.admin;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.inmobiliariaventa.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminPropiedadesFragment extends Fragment {

    // Modelo de datos de las propiedades en catálogo
    private static class AdminPropiedadCard {
        final int cardId;
        final int badgeCardId;
        final int badgeTvId;
        final String titulo;
        final String ubicacion;
        final double precio;
        String estado; // "Publicado", "Pausado", "Finalizado"
        final int switchId;
        final int originalOrder;

        AdminPropiedadCard(int cardId, int badgeCardId, int badgeTvId, String titulo,
                           String ubicacion, double precio, String estado, int switchId, int originalOrder) {
            this.cardId = cardId;
            this.badgeCardId = badgeCardId;
            this.badgeTvId = badgeTvId;
            this.titulo = titulo;
            this.ubicacion = ubicacion;
            this.precio = precio;
            this.estado = estado;
            this.switchId = switchId;
            this.originalOrder = originalOrder;
        }
    }

    private final List<AdminPropiedadCard> allProperties = new ArrayList<>();

    // Elementos de la interfaz
    private MaterialButton btnFiltros;
    private MaterialButton btnOrden;
    private TextInputEditText etBuscarPropiedades;
    private LinearLayout containerCards;
    private LinearLayout layoutEmpty;
    private LinearLayout layoutPagination;
    private MaterialButton btnEmptyReset;

    // Estado actual de filtro y orden
    private String selectedFilter = "Todos"; // "Todos", "Publicados", "Pausados", "Finalizados"
    private String selectedSort = "Recientes"; // "Recientes", "Mayor Monto", "Menor Monto"
    private String searchQuery = "";

    // Paginación
    private int currentPage = 1;
    private int startPage = 1;
    private final int maxPages = 143; // 856 propiedades / 6 por página

    private MaterialCardView btnPagePrev, btnPageNext;
    private MaterialCardView cardPage1, cardPage2, cardPage3, cardPage4;
    private TextView tvPagePrev, tvPageNext;
    private TextView tvPage1, tvPage2, tvPage3, tvPage4;

    public AdminPropiedadesFragment() {
        // Constructor público vacío requerido
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_propiedades, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initPropertiesData();

        // Botón regresar
        View btnBack = view.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());
        }

        // Floating Action Button - Agregar Propiedad
        FloatingActionButton fabAddProperty = view.findViewById(R.id.fab_add_property);
        if (fabAddProperty != null) {
            fabAddProperty.setOnClickListener(v -> {
                Navigation.findNavController(view).navigate(R.id.nav_admin_agregar_propiedad);
            });
        }

        // Elementos de Búsqueda, Filtros y Orden
        btnFiltros = view.findViewById(R.id.btn_filtros_propiedades);
        btnOrden = view.findViewById(R.id.btn_orden_propiedades);
        etBuscarPropiedades = view.findViewById(R.id.et_buscar_propiedades);
        containerCards = view.findViewById(R.id.container_propiedades_cards);
        layoutEmpty = view.findViewById(R.id.layout_empty_propiedades);
        layoutPagination = view.findViewById(R.id.layout_pagination);
        btnEmptyReset = view.findViewById(R.id.btn_empty_reset_filtros);

        // Configuración de Switches con actualización interactiva de badge y estado
        setupSwitches(view);

        // Botón Filtros: Todos, Publicados, Pausados, Finalizados (como en Contratos)
        if (btnFiltros != null) {
            btnFiltros.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(requireContext(), btnFiltros);
                popup.getMenu().add("Todos");
                popup.getMenu().add("Publicados");
                popup.getMenu().add("Pausados");
                popup.getMenu().add("Finalizados");

                popup.setOnMenuItemClickListener(item -> {
                    selectedFilter = item.getTitle().toString();
                    actualizarEstiloBotonFiltros();
                    aplicarFiltrosYOrden();
                    Toast.makeText(requireContext(), "Filtro: " + selectedFilter, Toast.LENGTH_SHORT).show();
                    return true;
                });
                popup.show();
            });
        }

        // Botón Orden: Recientes, Mayor Monto, Menor Monto (como en Contratos)
        if (btnOrden != null) {
            btnOrden.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(requireContext(), btnOrden);
                popup.getMenu().add("Recientes");
                popup.getMenu().add("Mayor Monto");
                popup.getMenu().add("Menor Monto");

                popup.setOnMenuItemClickListener(item -> {
                    selectedSort = item.getTitle().toString();
                    actualizarEstiloBotonOrden();
                    aplicarFiltrosYOrden();
                    Toast.makeText(requireContext(), "Orden: " + selectedSort, Toast.LENGTH_SHORT).show();
                    return true;
                });
                popup.show();
            });
        }

        // Botón de restablecer filtros cuando no hay resultados
        if (btnEmptyReset != null) {
            btnEmptyReset.setOnClickListener(v -> restablecerFiltros());
        }

        // Búsqueda en tiempo real
        if (etBuscarPropiedades != null) {
            etBuscarPropiedades.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s != null ? s.toString().trim() : "";
                    aplicarFiltrosYOrden();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        setupPagination(view);
    }

    private void initPropertiesData() {
        allProperties.clear();
        allProperties.add(new AdminPropiedadCard(
                R.id.card_propiedad_1,
                R.id.card_badge_propiedad_1,
                R.id.tv_badge_propiedad_1,
                "Residencia Horizon Vista",
                "Colonia Escalón, San Salvador",
                1250000.0,
                "Publicado",
                R.id.switch_1,
                1
        ));
        allProperties.add(new AdminPropiedadCard(
                R.id.card_propiedad_2,
                R.id.card_badge_propiedad_2,
                R.id.tv_badge_propiedad_2,
                "Apartamento Loft Skywalk",
                "Antiguo Cuscatlán, La Libertad",
                485000.0,
                "Publicado",
                R.id.switch_2,
                2
        ));
        allProperties.add(new AdminPropiedadCard(
                R.id.card_propiedad_3,
                R.id.card_badge_propiedad_3,
                R.id.tv_badge_propiedad_3,
                "Villa Los Laureles",
                "Santa Elena, Antiguo Cuscatlán",
                890000.0,
                "Publicado",
                R.id.switch_3,
                3
        ));
        allProperties.add(new AdminPropiedadCard(
                R.id.card_propiedad_4,
                R.id.card_badge_propiedad_4,
                R.id.tv_badge_propiedad_4,
                "Penthouse Torre Altagracia",
                "San Benito, San Salvador",
                1750000.0,
                "Pausado",
                R.id.switch_4,
                4
        ));
        allProperties.add(new AdminPropiedadCard(
                R.id.card_propiedad_5,
                R.id.card_badge_propiedad_5,
                R.id.tv_badge_propiedad_5,
                "Casa Moderna Santa Elena",
                "Residencial Arcos, La Libertad",
                620000.0,
                "Publicado",
                R.id.switch_5,
                5
        ));
        allProperties.add(new AdminPropiedadCard(
                R.id.card_propiedad_6,
                R.id.card_badge_propiedad_6,
                R.id.tv_badge_propiedad_6,
                "Terreno Campestre San José",
                "Planes de Renderos, San Salvador",
                310000.0,
                "Finalizado",
                R.id.switch_6,
                6
        ));
    }

    private void setupSwitches(View root) {
        for (AdminPropiedadCard prop : allProperties) {
            SwitchCompat sw = root.findViewById(prop.switchId);
            if (sw != null) {
                sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    prop.estado = isChecked ? "Publicado" : "Pausado";

                    // Actualizar dinámicamente el badge de la tarjeta
                    MaterialCardView badgeCard = root.findViewById(prop.badgeCardId);
                    TextView badgeTv = root.findViewById(prop.badgeTvId);
                    if (badgeCard != null && badgeTv != null) {
                        if (isChecked) {
                            badgeCard.setCardBackgroundColor(Color.parseColor("#006C49"));
                            badgeTv.setText("Publicado");
                        } else {
                            badgeCard.setCardBackgroundColor(Color.parseColor("#BA1A1A"));
                            badgeTv.setText("Pausado");
                        }
                    }

                    String status = isChecked ? "activada" : "pausada";
                    Snackbar.make(root, "Publicación " + status + " en el catálogo", Snackbar.LENGTH_SHORT).show();

                    // Re-filtrar si hay un filtro de estado activo
                    if (!"Todos".equals(selectedFilter)) {
                        aplicarFiltrosYOrden();
                    }
                });
            }
        }
    }

    private void aplicarFiltrosYOrden() {
        if (containerCards == null || getContext() == null) return;

        List<AdminPropiedadCard> visibles = new ArrayList<>();

        for (AdminPropiedadCard prop : allProperties) {
            // Filtro por texto de búsqueda
            boolean matchesSearch = true;
            if (!searchQuery.isEmpty()) {
                String q = searchQuery.toLowerCase();
                matchesSearch = prop.titulo.toLowerCase().contains(q) || prop.ubicacion.toLowerCase().contains(q);
            }

            // Filtro por Estado: Todos, Publicados, Pausados, Finalizados
            boolean matchesFilter = true;
            if ("Publicados".equalsIgnoreCase(selectedFilter)) {
                matchesFilter = "Publicado".equalsIgnoreCase(prop.estado);
            } else if ("Pausados".equalsIgnoreCase(selectedFilter)) {
                matchesFilter = "Pausado".equalsIgnoreCase(prop.estado);
            } else if ("Finalizados".equalsIgnoreCase(selectedFilter)) {
                matchesFilter = "Finalizado".equalsIgnoreCase(prop.estado);
            }

            View cardView = containerCards.findViewById(prop.cardId);
            if (matchesSearch && matchesFilter) {
                visibles.add(prop);
                if (cardView != null) cardView.setVisibility(View.VISIBLE);
            } else {
                if (cardView != null) cardView.setVisibility(View.GONE);
            }
        }

        // Ordenamiento: Recientes, Mayor Monto, Menor Monto
        Collections.sort(visibles, (a, b) -> {
            switch (selectedSort) {
                case "Mayor Monto":
                    return Double.compare(b.precio, a.precio);
                case "Menor Monto":
                    return Double.compare(a.precio, b.precio);
                case "Recientes":
                default:
                    return Integer.compare(a.originalOrder, b.originalOrder);
            }
        });

        // Reordenar físicamente en el contenedor LinearLayout
        for (AdminPropiedadCard prop : visibles) {
            View cardView = containerCards.findViewById(prop.cardId);
            if (cardView != null) {
                containerCards.removeView(cardView);
                containerCards.addView(cardView);
            }
        }

        // Manejar estado vacío
        boolean isEmpty = visibles.isEmpty();
        if (layoutEmpty != null) {
            layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
        if (layoutPagination != null) {
            layoutPagination.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }

    private void actualizarEstiloBotonFiltros() {
        if (btnFiltros == null || getContext() == null) return;
        if (!"Todos".equalsIgnoreCase(selectedFilter)) {
            btnFiltros.setText("FILTROS: " + selectedFilter.toUpperCase());
            btnFiltros.setStrokeColorResource(R.color.colorPrimary);
            btnFiltros.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
            btnFiltros.setIconTintResource(R.color.colorPrimary);
        } else {
            btnFiltros.setText("FILTROS");
            btnFiltros.setStrokeColorResource(R.color.colorOutlineVariant);
            btnFiltros.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOnSurface));
            btnFiltros.setIconTintResource(R.color.colorOnSurface);
        }
    }

    private void actualizarEstiloBotonOrden() {
        if (btnOrden == null || getContext() == null) return;
        if (!"Recientes".equalsIgnoreCase(selectedSort)) {
            btnOrden.setText("ORDEN: " + selectedSort.toUpperCase());
            btnOrden.setStrokeColorResource(R.color.colorPrimary);
            btnOrden.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
            btnOrden.setIconTintResource(R.color.colorPrimary);
        } else {
            btnOrden.setText("ORDEN");
            btnOrden.setStrokeColorResource(R.color.colorOutlineVariant);
            btnOrden.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOnSurface));
            btnOrden.setIconTintResource(R.color.colorOnSurface);
        }
    }

    private void restablecerFiltros() {
        selectedFilter = "Todos";
        selectedSort = "Recientes";
        searchQuery = "";

        if (etBuscarPropiedades != null) {
            etBuscarPropiedades.setText("");
        }

        actualizarEstiloBotonFiltros();
        actualizarEstiloBotonOrden();
        aplicarFiltrosYOrden();

        if (getView() != null) {
            Snackbar.make(getView(), "Filtros y orden restablecidos", Snackbar.LENGTH_SHORT).show();
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
        startPage = currentPage;
        renderPagination(root, true);
    }

    private void renderPagination(View root, boolean showSnackbar) {
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

        if (showSnackbar) {
            Snackbar.make(root, "Página " + currentPage + " de " + maxPages, Snackbar.LENGTH_SHORT).show();
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
