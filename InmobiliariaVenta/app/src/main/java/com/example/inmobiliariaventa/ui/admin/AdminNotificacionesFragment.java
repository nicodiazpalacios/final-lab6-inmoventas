package com.example.inmobiliariaventa.ui.admin;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.example.inmobiliariaventa.R;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

public class AdminNotificacionesFragment extends Fragment {

    private int currentPage = 1;
    private int startPage = 1;
    private final int maxPages = 6; // 48 notificaciones / 8 por página

    private LinearLayout layoutPage1Notificaciones;
    private LinearLayout layoutPage2Notificaciones;
    private NestedScrollView scrollViewNotificaciones;

    private MaterialCardView btnPagePrev, btnPageNext;
    private MaterialCardView cardPage1, cardPage2, cardPage3, cardPage4;
    private TextView tvPagePrev, tvPageNext;
    private TextView tvPage1, tvPage2, tvPage3, tvPage4;

    private TextInputEditText etBuscarNotificacion;
    private ChipGroup chipGroupNotif;
    private TextView btnMarcarTodasLeidas;

    private static class NotifItem {
        final int cardId;
        final int titleId;
        final int descId;
        final int categoryId;
        final int statusId;
        final int dismissId;

        NotifItem(int cardId, int titleId, int descId, int categoryId, int statusId, int dismissId) {
            this.cardId = cardId;
            this.titleId = titleId;
            this.descId = descId;
            this.categoryId = categoryId;
            this.statusId = statusId;
            this.dismissId = dismissId;
        }
    }

    private final NotifItem[] notifItems = new NotifItem[] {
        new NotifItem(R.id.card_notif_1, R.id.tv_titulo_1, R.id.tv_desc_1, R.id.tv_categoria_1, R.id.tv_badge_estado_1, R.id.btn_dismiss_1),
        new NotifItem(R.id.card_notif_2, R.id.tv_titulo_2, R.id.tv_desc_2, R.id.tv_categoria_2, R.id.tv_badge_estado_2, R.id.btn_dismiss_2),
        new NotifItem(R.id.card_notif_3, R.id.tv_titulo_3, R.id.tv_desc_3, R.id.tv_categoria_3, R.id.tv_badge_estado_3, R.id.btn_dismiss_3),
        new NotifItem(R.id.card_notif_4, R.id.tv_titulo_4, R.id.tv_desc_4, R.id.tv_categoria_4, R.id.tv_badge_estado_4, R.id.btn_dismiss_4),
        new NotifItem(R.id.card_notif_5, R.id.tv_titulo_5, R.id.tv_desc_5, R.id.tv_categoria_5, R.id.tv_badge_estado_5, R.id.btn_dismiss_5),
        new NotifItem(R.id.card_notif_6, R.id.tv_titulo_6, R.id.tv_desc_6, R.id.tv_categoria_6, R.id.tv_badge_estado_6, R.id.btn_dismiss_6),
        new NotifItem(R.id.card_notif_7, R.id.tv_titulo_7, R.id.tv_desc_7, R.id.tv_categoria_7, R.id.tv_badge_estado_7, R.id.btn_dismiss_7),
        new NotifItem(R.id.card_notif_8, R.id.tv_titulo_8, R.id.tv_desc_8, R.id.tv_categoria_8, R.id.tv_badge_estado_8, R.id.btn_dismiss_8),
        new NotifItem(R.id.card_notif_9, R.id.tv_titulo_9, R.id.tv_desc_9, R.id.tv_categoria_9, R.id.tv_badge_estado_9, R.id.btn_dismiss_9),
        new NotifItem(R.id.card_notif_10, R.id.tv_titulo_10, R.id.tv_desc_10, R.id.tv_categoria_10, R.id.tv_badge_estado_10, R.id.btn_dismiss_10),
        new NotifItem(R.id.card_notif_11, R.id.tv_titulo_11, R.id.tv_desc_11, R.id.tv_categoria_11, R.id.tv_badge_estado_11, R.id.btn_dismiss_11),
        new NotifItem(R.id.card_notif_12, R.id.tv_titulo_12, R.id.tv_desc_12, R.id.tv_categoria_12, R.id.tv_badge_estado_12, R.id.btn_dismiss_12),
        new NotifItem(R.id.card_notif_13, R.id.tv_titulo_13, R.id.tv_desc_13, R.id.tv_categoria_13, R.id.tv_badge_estado_13, R.id.btn_dismiss_13),
        new NotifItem(R.id.card_notif_14, R.id.tv_titulo_14, R.id.tv_desc_14, R.id.tv_categoria_14, R.id.tv_badge_estado_14, R.id.btn_dismiss_14),
        new NotifItem(R.id.card_notif_15, R.id.tv_titulo_15, R.id.tv_desc_15, R.id.tv_categoria_15, R.id.tv_badge_estado_15, R.id.btn_dismiss_15),
        new NotifItem(R.id.card_notif_16, R.id.tv_titulo_16, R.id.tv_desc_16, R.id.tv_categoria_16, R.id.tv_badge_estado_16, R.id.btn_dismiss_16)
    };

    public AdminNotificacionesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_notificaciones, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        layoutPage1Notificaciones = view.findViewById(R.id.layout_page_1_notificaciones);
        layoutPage2Notificaciones = view.findViewById(R.id.layout_page_2_notificaciones);
        scrollViewNotificaciones = view.findViewById(R.id.scroll_view_notificaciones);

        etBuscarNotificacion = view.findViewById(R.id.et_buscar_notificacion);
        chipGroupNotif = view.findViewById(R.id.chip_group_notif);
        btnMarcarTodasLeidas = view.findViewById(R.id.btn_marcar_todas_leidas);

        View btnBack = view.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                androidx.navigation.Navigation.findNavController(view).popBackStack();
            });
        }

        setupCardInteractions(view);
        setupSearchAndFilter(view);
        setupPagination(view);

        if (btnMarcarTodasLeidas != null) {
            btnMarcarTodasLeidas.setOnClickListener(v -> {
                for (NotifItem item : notifItems) {
                    TextView tvStatus = view.findViewById(item.statusId);
                    if (tvStatus != null) {
                        tvStatus.setText("LEÍDA");
                        tvStatus.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.colorSurfaceVariant));
                        tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOutline));
                    }
                }
                Toast.makeText(requireContext(), "Todas las notificaciones marcadas como leídas", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupCardInteractions(View root) {
        for (NotifItem item : notifItems) {
            MaterialCardView card = root.findViewById(item.cardId);
            TextView tvTitle = root.findViewById(item.titleId);
            String title = tvTitle != null ? tvTitle.getText().toString() : "Notificación";

            View btnDismiss = root.findViewById(item.dismissId);
            if (btnDismiss != null && card != null) {
                btnDismiss.setOnClickListener(v -> {
                    card.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Notificación descartada", Toast.LENGTH_SHORT).show();
                });
            }

            if (card != null) {
                card.setOnClickListener(v -> {
                    TextView tvStatus = root.findViewById(item.statusId);
                    if (tvStatus != null) {
                        tvStatus.setText("LEÍDA");
                        tvStatus.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.colorSurfaceVariant));
                        tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOutline));
                    }
                    Toast.makeText(requireContext(), title, Toast.LENGTH_SHORT).show();
                });
            }
        }
    }

    private void setupSearchAndFilter(View root) {
        if (etBuscarNotificacion != null) {
            etBuscarNotificacion.addTextChangedListener(new TextWatcher() {
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

        if (chipGroupNotif != null) {
            chipGroupNotif.setOnCheckedChangeListener((group, checkedId) -> applyFilters(root));
        }
    }

    private void applyFilters(View root) {
        String query = "";
        if (etBuscarNotificacion != null && etBuscarNotificacion.getText() != null) {
            query = etBuscarNotificacion.getText().toString().trim().toLowerCase();
        }

        int checkedId = chipGroupNotif != null ? chipGroupNotif.getCheckedChipId() : R.id.chip_todas;

        for (NotifItem item : notifItems) {
            MaterialCardView card = root.findViewById(item.cardId);
            if (card == null) continue;

            TextView tvTitle = root.findViewById(item.titleId);
            TextView tvDesc = root.findViewById(item.descId);
            TextView tvCat = root.findViewById(item.categoryId);
            TextView tvStatus = root.findViewById(item.statusId);

            String title = tvTitle != null ? tvTitle.getText().toString().toLowerCase() : "";
            String desc = tvDesc != null ? tvDesc.getText().toString().toLowerCase() : "";
            String cat = tvCat != null ? tvCat.getText().toString().toUpperCase() : "";
            String status = tvStatus != null ? tvStatus.getText().toString().toUpperCase() : "";

            boolean matchesCategory = true;
            if (checkedId == R.id.chip_no_leidas) {
                matchesCategory = status.contains("NUEVA");
            } else if (checkedId == R.id.chip_contratos) {
                matchesCategory = cat.contains("CONTRATOS");
            } else if (checkedId == R.id.chip_propiedades) {
                matchesCategory = cat.contains("PROPIEDADES");
            } else if (checkedId == R.id.chip_leads) {
                matchesCategory = cat.contains("LEADS");
            }

            boolean matchesQuery = query.isEmpty() || title.contains(query) || desc.contains(query) || cat.toLowerCase().contains(query);

            card.setVisibility(matchesCategory && matchesQuery ? View.VISIBLE : View.GONE);
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

        if (currentPage == 1) {
            if (layoutPage1Notificaciones != null) layoutPage1Notificaciones.setVisibility(View.VISIBLE);
            if (layoutPage2Notificaciones != null) layoutPage2Notificaciones.setVisibility(View.GONE);
        } else {
            if (layoutPage1Notificaciones != null) layoutPage1Notificaciones.setVisibility(View.GONE);
            if (layoutPage2Notificaciones != null) layoutPage2Notificaciones.setVisibility(View.VISIBLE);
        }

        if (scrollViewNotificaciones != null) {
            scrollViewNotificaciones.smoothScrollTo(0, 0);
        }

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
