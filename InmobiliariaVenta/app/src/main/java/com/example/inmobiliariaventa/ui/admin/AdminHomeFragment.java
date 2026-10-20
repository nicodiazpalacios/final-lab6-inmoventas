package com.example.inmobiliariaventa.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inmobiliariaventa.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class AdminHomeFragment extends Fragment {

    private ActividadRecienteAdapter actividadAdapter;

    public AdminHomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_admin_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Banner Modo Sin Conexión
        View cardModoOffline = view.findViewById(R.id.cardModoOffline);
        if (cardModoOffline != null && getContext() != null) {
            com.example.inmobiliariaventa.utils.NetworkUtils.getNetworkLiveData(requireContext())
                    .observe(getViewLifecycleOwner(), isOnline -> {
                        cardModoOffline.setVisibility(Boolean.TRUE.equals(isOnline) ? View.GONE : View.VISIBLE);
                    });
        }

        // 2. Search Container (idéntico a Home Usuario)
        View searchContainer = view.findViewById(R.id.searchContainer);
        if (searchContainer != null) {
            searchContainer.setOnClickListener(v -> {
                Navigation.findNavController(view).navigate(R.id.action_nav_admin_home_to_nav_admin_propiedades);
            });
        }

        // 3. Botón Publicar Nueva Propiedad
        View btnAdminNuevaPropiedad = view.findViewById(R.id.btnAdminNuevaPropiedad);
        if (btnAdminNuevaPropiedad != null) {
            btnAdminNuevaPropiedad.setOnClickListener(v -> {
                Navigation.findNavController(view).navigate(R.id.nav_admin_agregar_propiedad);
            });
        }

        // 4. FAB Agregar Propiedad
        FloatingActionButton fabAddProperty = view.findViewById(R.id.fab_add_property);
        if (fabAddProperty != null) {
            fabAddProperty.setOnClickListener(v -> {
                Navigation.findNavController(view).navigate(R.id.nav_admin_agregar_propiedad);
            });
        }

        // 5. Botón Alternar Modo Usuario
        View btnSwitchUserMode = view.findViewById(R.id.btnSwitchUserMode);
        if (btnSwitchUserMode != null) {
            btnSwitchUserMode.setOnClickListener(v -> {
                Navigation.findNavController(view).navigate(R.id.nav_inicio);
            });
        }

        // 6. Módulos de Gestión (Tarjetas de 100dp con bg_icon_circle)
        View cardPropiedades = view.findViewById(R.id.card_propiedades);
        if (cardPropiedades != null) {
            cardPropiedades.setOnClickListener(v -> {
                Navigation.findNavController(view).navigate(R.id.action_nav_admin_home_to_nav_admin_propiedades);
            });
        }

        View cardMensajes = view.findViewById(R.id.card_mensajes);
        android.widget.TextView tvMensajesSub = view.findViewById(R.id.tv_admin_mensajes_subtitulo);
        if (cardMensajes != null) {
            cardMensajes.setOnClickListener(v -> {
                Navigation.findNavController(view).navigate(R.id.action_nav_admin_home_to_nav_admin_mensajes);
            });

            if (tvMensajesSub != null && getContext() != null) {
                com.example.inmobiliariaventa.data.repository.MensajeContactoRepositoryImpl.getInstance(requireContext())
                        .sincronizarConNube(null);

                com.example.inmobiliariaventa.data.repository.MensajeContactoRepositoryImpl.getInstance(requireContext())
                        .getMensajesNoLeidosCount()
                        .observe(getViewLifecycleOwner(), unreadCount -> {
                            if (unreadCount != null && unreadCount > 0) {
                                tvMensajesSub.setText(unreadCount + " nuevos");
                                tvMensajesSub.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.colorPrimary));
                            } else {
                                tvMensajesSub.setText("Mensajes");
                                tvMensajesSub.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.colorOnSurface));
                            }
                        });
            }
        }

        View cardVentas = view.findViewById(R.id.card_ventas);
        if (cardVentas != null) {
            cardVentas.setOnClickListener(v -> {
                Navigation.findNavController(view).navigate(R.id.action_nav_admin_home_to_nav_admin_ventas);
            });
        }

        View cardAjustes = view.findViewById(R.id.card_ajustes);
        if (cardAjustes != null) {
            cardAjustes.setOnClickListener(v -> {
                Navigation.findNavController(view).navigate(R.id.action_nav_admin_home_to_nav_admin_configuracion);
            });
        }

        // 7. Cabecera Botón de Notificaciones
        View notificationsButton = view.findViewById(R.id.notificationsButton);
        if (notificationsButton != null) {
            notificationsButton.setOnClickListener(v -> {
                Navigation.findNavController(view).navigate(R.id.action_nav_admin_home_to_nav_admin_notificaciones);
            });
        }

        // 8. Botón Ver Todas las Notificaciones
        View btnVerTodas = view.findViewById(R.id.btnVerTodasNotificaciones);
        if (btnVerTodas != null) {
            btnVerTodas.setOnClickListener(v -> {
                Navigation.findNavController(view).navigate(R.id.action_nav_admin_home_to_nav_admin_notificaciones);
            });
        }

        // 9. Lista de Actividad Reciente (Novedades de inventario)
        RecyclerView rvActividadReciente = view.findViewById(R.id.rvActividadReciente);
        if (rvActividadReciente != null && getContext() != null) {
            rvActividadReciente.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
            rvActividadReciente.setHasFixedSize(true);
            rvActividadReciente.setNestedScrollingEnabled(false);

            List<ActividadAdmin> actividades = obtenerActividadesIniciales();
            actividadAdapter = new ActividadRecienteAdapter(actividades, actividad -> {
                // Al tocar una actividad, navegar al centro de notificaciones de admin
                try {
                    Navigation.findNavController(view).navigate(R.id.action_nav_admin_home_to_nav_admin_notificaciones);
                } catch (Exception ignored) {}
            });
            rvActividadReciente.setAdapter(actividadAdapter);
        }
    }

    private List<ActividadAdmin> obtenerActividadesIniciales() {
        List<ActividadAdmin> lista = new ArrayList<>();

        // 1. Cambio de precio
        lista.add(new ActividadAdmin(
                "act_1",
                "Apartamento Loft Skywalk",
                ActividadAdmin.TipoActividad.CAMBIO_PRECIO,
                "Hace 15 min",
                "$450,000 USD",
                "• Antes $485,000 USD (-7.2%)"
        ));

        // 2. Nueva propiedad subida
        lista.add(new ActividadAdmin(
                "act_2",
                "Residencia Los Olivos - San Benito",
                ActividadAdmin.TipoActividad.NUEVA_PROPIEDAD,
                "Hoy, 14:20 hs",
                "$420,000 USD",
                "• Publicación ingresada al catálogo"
        ));

        // 3. Propiedad dada de baja
        lista.add(new ActividadAdmin(
                "act_3",
                "Penthouse Torre Altagracia",
                ActividadAdmin.TipoActividad.DADA_DE_BAJA,
                "Ayer, 18:30 hs",
                "$1,750,000 USD",
                "• Retirada de catálogo (Pausada)"
        ));

        // 4. Cambio de precio
        lista.add(new ActividadAdmin(
                "act_4",
                "Villa Los Laureles - Santa Elena",
                ActividadAdmin.TipoActividad.CAMBIO_PRECIO,
                "19 Oct, 11:10 hs",
                "$850,000 USD",
                "• Antes $890,000 USD (Ajuste comercial)"
        ));

        // 5. Nueva propiedad subida
        lista.add(new ActividadAdmin(
                "act_5",
                "Casa Moderna Santa Elena",
                ActividadAdmin.TipoActividad.NUEVA_PROPIEDAD,
                "18 Oct, 16:45 hs",
                "$620,000 USD",
                "• Publicación activa para visitas"
        ));

        // 6. Propiedad dada de baja
        lista.add(new ActividadAdmin(
                "act_6",
                "Terreno Campestre San José",
                ActividadAdmin.TipoActividad.DADA_DE_BAJA,
                "17 Oct, 10:15 hs",
                "$310,000 USD",
                "• Baja por contrato finalizado"
        ));

        return lista;
    }
}
