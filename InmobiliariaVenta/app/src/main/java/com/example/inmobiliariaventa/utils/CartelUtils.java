package com.example.inmobiliariaventa.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.inmobiliariaventa.R;
import com.google.android.material.snackbar.Snackbar;

public class CartelUtils {

    public static void mostrar(@Nullable View view, @Nullable String mensaje) {
        mostrarInterno(view, null, mensaje, Snackbar.LENGTH_SHORT, null, null);
    }

    public static void mostrarLargo(@Nullable View view, @Nullable String mensaje) {
        mostrarInterno(view, null, mensaje, Snackbar.LENGTH_LONG, null, null);
    }

    public static void mostrar(@Nullable Context context, @Nullable String mensaje) {
        mostrarInterno(null, context, mensaje, Snackbar.LENGTH_SHORT, null, null);
    }

    public static void mostrarLargo(@Nullable Context context, @Nullable String mensaje) {
        mostrarInterno(null, context, mensaje, Snackbar.LENGTH_LONG, null, null);
    }

    public static void mostrar(@Nullable Fragment fragment, @Nullable String mensaje) {
        if (fragment == null || !fragment.isAdded()) return;
        View v = fragment.getView();
        if (v != null) {
            mostrar(v, mensaje);
        } else {
            mostrar(fragment.getContext(), mensaje);
        }
    }

    public static void mostrarConAccion(
            @Nullable View view,
            @Nullable String mensaje,
            @NonNull String textoAccion,
            @NonNull View.OnClickListener onActionClick) {
        mostrarInterno(view, null, mensaje, Snackbar.LENGTH_LONG, textoAccion, onActionClick);
    }

    private static void mostrarInterno(
            @Nullable View anchorView,
            @Nullable Context fallbackContext,
            @Nullable String mensaje,
            int duration,
            @Nullable String actionText,
            @Nullable View.OnClickListener actionListener) {

        if (mensaje == null || mensaje.trim().isEmpty()) return;

        Context context = null;
        View targetView = null;

        if (anchorView != null) {
            context = anchorView.getContext();
            targetView = findSuitableParent(anchorView);
        }

        if (targetView == null) {
            Context ctx = (anchorView != null) ? anchorView.getContext() : fallbackContext;
            if (ctx != null) {
                context = ctx;
                Activity activity = findActivity(ctx);
                if (activity != null) {
                    targetView = activity.findViewById(android.R.id.content);
                    if (targetView == null) {
                        targetView = activity.getWindow().getDecorView();
                    }
                }
            }
        }

        if (targetView == null || context == null) {
            // Último recurso si no hay jerarquía de vistas disponible
            if (context != null) {
                Toast.makeText(context, mensaje, duration == Snackbar.LENGTH_LONG ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
            }
            return;
        }

        try {
            Snackbar snackbar = Snackbar.make(targetView, mensaje, duration)
                    .setTextColor(ContextCompat.getColor(context, R.color.white));

            if (actionText != null && actionListener != null) {
                snackbar.setAction(actionText, actionListener)
                        .setActionTextColor(ContextCompat.getColor(context, R.color.colorSnackbarAction));
            }

            View snackbarView = snackbar.getView();
            snackbarView.setBackgroundResource(R.drawable.bg_snackbar_favoritos);
            snackbarView.setBackgroundTintList(null);

            TextView tvText = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
            if (tvText != null) {
                tvText.setMaxLines(3);
                tvText.setTextSize(13.5f);
            }

            TextView tvAction = snackbarView.findViewById(com.google.android.material.R.id.snackbar_action);
            if (tvAction != null) {
                tvAction.setTypeface(Typeface.DEFAULT_BOLD);
                tvAction.setTextSize(13f);
            }

            float density = context.getResources().getDisplayMetrics().density;
            ViewGroup.LayoutParams lp = snackbarView.getLayoutParams();
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) lp;
                int marginH = (int) (16 * density);
                int marginB = (int) (10 * density);
                marginParams.setMargins(marginH, 0, marginH, marginB);
                snackbarView.setLayoutParams(marginParams);
            }

            if (lp instanceof FrameLayout.LayoutParams) {
                ((FrameLayout.LayoutParams) lp).gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            } else if (lp instanceof CoordinatorLayout.LayoutParams) {
                ((CoordinatorLayout.LayoutParams) lp).gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            }
            snackbarView.setElevation(8 * density);

            Activity act = findActivity(context);
            if (act != null) {
                View bottomNav = act.findViewById(R.id.bottom_navigation);
                if (bottomNav != null && bottomNav.getVisibility() == View.VISIBLE) {
                    snackbar.setAnchorView(bottomNav);
                }
            }

            snackbar.show();
        } catch (Exception e) {
            Toast.makeText(context, mensaje, duration == Snackbar.LENGTH_LONG ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
        }
    }

    private static View findSuitableParent(View view) {
        View fallback = null;
        do {
            if (view instanceof CoordinatorLayout) {
                return view;
            }
            if (view instanceof FrameLayout) {
                if (view.getId() == android.R.id.content) {
                    return view;
                }
                fallback = view;
            }
            if (view != null) {
                Object parent = view.getParent();
                view = parent instanceof View ? (View) parent : null;
            }
        } while (view != null);
        return fallback;
    }

    private static Activity findActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }
}
