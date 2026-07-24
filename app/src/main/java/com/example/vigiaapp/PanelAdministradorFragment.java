package com.example.vigiaapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class PanelAdministradorFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    public PanelAdministradorFragment() {
    }

    public static PanelAdministradorFragment newInstance(String param1, String param2) {
        PanelAdministradorFragment fragment = new PanelAdministradorFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_panel_administrador, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View btnUsuarios = view.findViewById(R.id.btnUsuarios);
        View btnInventarioAdmin = view.findViewById(R.id.btnInventarioAdmin);
        View btnHistorial = view.findViewById(R.id.btnHistorial);
        View btnReportes = view.findViewById(R.id.btnReportes);
        View btnMovimientos = view.findViewById(R.id.btnMovimientos);
        View btnBaseDatos = view.findViewById(R.id.btnBaseDatos);

        btnUsuarios.setOnClickListener(v -> getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new UsuariosRegistradosFragment())
                .addToBackStack(null)
                .commit());

        btnInventarioAdmin.setOnClickListener(v -> getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new InventarioAdminFragment())
                .addToBackStack(null)
                .commit());

        btnHistorial.setOnClickListener(v -> getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new HistorialAdminFragment())
                .addToBackStack(null)
                .commit());

        btnReportes.setOnClickListener(v -> getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new ReportesAdminFragment())
                .addToBackStack(null)
                .commit());

        btnMovimientos.setOnClickListener(v -> getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new MovimientosAdminFragment())
                .addToBackStack(null)
                .commit());

        btnBaseDatos.setOnClickListener(v -> getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new BdAdminDataFragment())
                .addToBackStack(null)
                .commit());
    }
}
