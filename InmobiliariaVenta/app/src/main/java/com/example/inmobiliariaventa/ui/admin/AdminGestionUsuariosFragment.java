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

public class AdminGestionUsuariosFragment extends Fragment {

    private int currentPage = 1;
    private int startPage = 1;
    private final int maxPages = 8; // 48 usuarios / 6 por página

    private LinearLayout layoutPage1Users;
    private LinearLayout layoutPage2Users;
    private NestedScrollView scrollViewUsuarios;

    private MaterialCardView btnPagePrev, btnPageNext;
    private MaterialCardView cardPage1, cardPage2, cardPage3, cardPage4;
    private TextView tvPagePrev, tvPageNext;
    private TextView tvPage1, tvPage2, tvPage3, tvPage4;

    private TextInputEditText etBuscarUsuario;
    private ChipGroup chipGroupRoles;

    private static class UserItem {
        final int cardId;
        final int nameId;
        final int roleId;
        final int emailId;
        final int editId;
        final int deleteId;

        UserItem(int cardId, int nameId, int roleId, int emailId, int editId, int deleteId) {
            this.cardId = cardId;
            this.nameId = nameId;
            this.roleId = roleId;
            this.emailId = emailId;
            this.editId = editId;
            this.deleteId = deleteId;
        }
    }

    private final UserItem[] users = new UserItem[] {
        new UserItem(R.id.card_usuario_1, R.id.tv_nombre_1, R.id.tv_rol_1, R.id.tv_email_1, R.id.btn_edit_1, R.id.btn_delete_1),
        new UserItem(R.id.card_usuario_2, R.id.tv_nombre_2, R.id.tv_rol_2, R.id.tv_email_2, R.id.btn_edit_2, R.id.btn_delete_2),
        new UserItem(R.id.card_usuario_3, R.id.tv_nombre_3, R.id.tv_rol_3, R.id.tv_email_3, R.id.btn_edit_3, R.id.btn_delete_3),
        new UserItem(R.id.card_usuario_4, R.id.tv_nombre_4, R.id.tv_rol_4, R.id.tv_email_4, R.id.btn_edit_4, R.id.btn_delete_4),
        new UserItem(R.id.card_usuario_5, R.id.tv_nombre_5, R.id.tv_rol_5, R.id.tv_email_5, R.id.btn_edit_5, R.id.btn_delete_5),
        new UserItem(R.id.card_usuario_6, R.id.tv_nombre_6, R.id.tv_rol_6, R.id.tv_email_6, R.id.btn_edit_6, R.id.btn_delete_6),
        new UserItem(R.id.card_usuario_7, R.id.tv_nombre_7, R.id.tv_rol_7, R.id.tv_email_7, R.id.btn_edit_7, R.id.btn_delete_7),
        new UserItem(R.id.card_usuario_8, R.id.tv_nombre_8, R.id.tv_rol_8, R.id.tv_email_8, R.id.btn_edit_8, R.id.btn_delete_8),
        new UserItem(R.id.card_usuario_9, R.id.tv_nombre_9, R.id.tv_rol_9, R.id.tv_email_9, R.id.btn_edit_9, R.id.btn_delete_9),
        new UserItem(R.id.card_usuario_10, R.id.tv_nombre_10, R.id.tv_rol_10, R.id.tv_email_10, R.id.btn_edit_10, R.id.btn_delete_10),
        new UserItem(R.id.card_usuario_11, R.id.tv_nombre_11, R.id.tv_rol_11, R.id.tv_email_11, R.id.btn_edit_11, R.id.btn_delete_11),
        new UserItem(R.id.card_usuario_12, R.id.tv_nombre_12, R.id.tv_rol_12, R.id.tv_email_12, R.id.btn_edit_12, R.id.btn_delete_12)
    };

    public AdminGestionUsuariosFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_gestion_usuarios, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        layoutPage1Users = view.findViewById(R.id.layout_page_1_users);
        layoutPage2Users = view.findViewById(R.id.layout_page_2_users);
        scrollViewUsuarios = view.findViewById(R.id.scroll_view_usuarios);

        etBuscarUsuario = view.findViewById(R.id.et_buscar_usuario);
        chipGroupRoles = view.findViewById(R.id.chip_group_roles);

        View btnBack = view.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                androidx.navigation.Navigation.findNavController(view).popBackStack();
            });
        }

        setupUserActionButtons(view);
        setupSearchAndFilter(view);
        setupPagination(view);
    }

    private void setupUserActionButtons(View root) {
        for (UserItem item : users) {
            TextView tvNombre = root.findViewById(item.nameId);
            String name = tvNombre != null ? tvNombre.getText().toString() : "Usuario";

            View btnEdit = root.findViewById(item.editId);
            if (btnEdit != null) {
                btnEdit.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "Editar " + name, Toast.LENGTH_SHORT).show()
                );
            }

            View btnDelete = root.findViewById(item.deleteId);
            if (btnDelete != null) {
                btnDelete.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "Eliminar " + name, Toast.LENGTH_SHORT).show()
                );
            }
        }
    }

    private void setupSearchAndFilter(View root) {
        if (etBuscarUsuario != null) {
            etBuscarUsuario.addTextChangedListener(new TextWatcher() {
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

        if (chipGroupRoles != null) {
            chipGroupRoles.setOnCheckedChangeListener((group, checkedId) -> applyFilters(root));
        }
    }

    private void applyFilters(View root) {
        String query = "";
        if (etBuscarUsuario != null && etBuscarUsuario.getText() != null) {
            query = etBuscarUsuario.getText().toString().trim().toLowerCase();
        }

        int checkedId = chipGroupRoles != null ? chipGroupRoles.getCheckedChipId() : R.id.chip_todos;

        for (UserItem item : users) {
            MaterialCardView card = root.findViewById(item.cardId);
            if (card == null) continue;

            TextView tvNombre = root.findViewById(item.nameId);
            TextView tvRol = root.findViewById(item.roleId);
            TextView tvEmail = root.findViewById(item.emailId);

            String name = tvNombre != null ? tvNombre.getText().toString().toLowerCase() : "";
            String role = tvRol != null ? tvRol.getText().toString().toUpperCase() : "";
            String email = tvEmail != null ? tvEmail.getText().toString().toLowerCase() : "";

            boolean matchesRole = true;
            if (checkedId == R.id.chip_usuarios) {
                matchesRole = role.contains("USUARIO");
            } else if (checkedId == R.id.chip_admins) {
                matchesRole = role.contains("ADMIN");
            }

            boolean matchesQuery = query.isEmpty() || name.contains(query) || email.contains(query);

            card.setVisibility(matchesRole && matchesQuery ? View.VISIBLE : View.GONE);
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
            if (layoutPage1Users != null) layoutPage1Users.setVisibility(View.VISIBLE);
            if (layoutPage2Users != null) layoutPage2Users.setVisibility(View.GONE);
        } else {
            if (layoutPage1Users != null) layoutPage1Users.setVisibility(View.GONE);
            if (layoutPage2Users != null) layoutPage2Users.setVisibility(View.VISIBLE);
        }

        if (scrollViewUsuarios != null) {
            scrollViewUsuarios.smoothScrollTo(0, 0);
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

