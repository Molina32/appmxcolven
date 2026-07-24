package com.example.vigiaapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.vigiaapp.api.ApiClient;
import com.example.vigiaapp.api.ApiService;
import com.example.vigiaapp.Archivos.LoginResponse;
import com.example.vigiaapp.Archivos.RegisterRequest;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegistroFragment extends Fragment {

    private TextInputEditText etUsername;
    private TextInputEditText etFirstName;
    private TextInputEditText etLastNamePaterno;
    private TextInputEditText etLastNameMaterno;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;
    private Button btnRegister;
    private ApiService apiService;

    public RegistroFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_registro, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etUsername = view.findViewById(R.id.etUsername);
        etFirstName = view.findViewById(R.id.etFirstName);
        etLastNamePaterno = view.findViewById(R.id.etLastNamePaterno);
        etLastNameMaterno = view.findViewById(R.id.etLastNameMaterno);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
        btnRegister = view.findViewById(R.id.btnRegister);

        apiService = ApiClient.getClient().create(ApiService.class);

        btnRegister.setOnClickListener(v -> {
            String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
            String firstName = etFirstName.getText() != null ? etFirstName.getText().toString().trim() : "";
            String lastNamePaterno = etLastNamePaterno.getText() != null ? etLastNamePaterno.getText().toString().trim() : "";
            String lastNameMaterno = etLastNameMaterno.getText() != null ? etLastNameMaterno.getText().toString().trim() : "";
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
            String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

            if (validateInputs(username, firstName, lastNamePaterno, lastNameMaterno, email, password, confirmPassword)) {
                performRegister(username, firstName, lastNamePaterno, lastNameMaterno, email, password, confirmPassword);
            }
        });
    }

    private boolean validateInputs(String username, String firstName, String lastNamePaterno, String lastNameMaterno, String email, String password, String confirmPassword) {
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(requireContext(), "Por favor ingrese su usuario", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(firstName)) {
            Toast.makeText(requireContext(), "Por favor ingrese su nombre", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(lastNamePaterno)) {
            Toast.makeText(requireContext(), "Por favor ingrese su apellido paterno", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(lastNameMaterno)) {
            Toast.makeText(requireContext(), "Por favor ingrese su apellido materno", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(requireContext(), "Por favor ingrese su correo electrÃ³nico", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(requireContext(), "Por favor ingrese su contraseÃ±a", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(requireContext(), "Por favor confirme su contraseÃ±a", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(requireContext(), "Las contraseÃ±as no coinciden", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void performRegister(String username, String firstName, String lastNamePaterno, String lastNameMaterno, String email, String password, String confirmPassword) {
        RegisterRequest registerRequest = new RegisterRequest(
                username,
                firstName,
                lastNamePaterno,
                lastNameMaterno,
                email,
                password,
                confirmPassword
        );

        Call<LoginResponse> call = apiService.register(registerRequest);
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                if (!isAdded()) {
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(requireContext(), response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                } else {
                    Toast.makeText(requireContext(), extractErrorMessage(response), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }

                Toast.makeText(requireContext(), "Error de conexiÃ³n: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String extractErrorMessage(Response<LoginResponse> response) {
        if (response.errorBody() == null) {
            return "Error al registrarse";
        }

        try {
            String errorJson = response.errorBody().string();
            LoginResponse errorResponse = new Gson().fromJson(errorJson, LoginResponse.class);
            if (errorResponse != null && errorResponse.getMessage() != null && !errorResponse.getMessage().isEmpty()) {
                return errorResponse.getMessage();
            }
        } catch (IOException ignored) {
        }

        return "Error al registrarse";
    }
}

