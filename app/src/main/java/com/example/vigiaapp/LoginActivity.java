package com.example.vigiaapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.graphics.Typeface;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.vigiaapp.api.ApiClient;
import com.example.vigiaapp.api.ApiService;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.example.vigiaapp.Archivos.LoginRequest;
import com.example.vigiaapp.Archivos.LoginResponse;
import com.example.vigiaapp.Archivos.Grupo;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    public static final String EXTRA_IS_ADMIN = "is_admin";
    private static final String PREFS_SESSION = "vigia_session";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_IS_ADMIN = "is_admin";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_USER_ID = "user_id";

    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private Button btnLogin;
    private Button btnRegister;
    private Group loginGroup;
    private FrameLayout fragmentContainer;
    private View authHeaderContainer;
    private TextView tvHeaderTitle;
    private ImageButton btnBackNavigation;
    private DrawerLayout drawerLayout;
    private ImageButton btnNavigationView;
    private NavigationView navigationView;
    private ApiService apiService;
    private EditText etScannerGlobal;
    private final Handler scannerHandler = new Handler(Looper.getMainLooper());
    private Runnable scannerRunnable;
    private Runnable focusRetryRunnable;
    private boolean procesandoScanner;
    private boolean buscandoEscaneo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        loginGroup = findViewById(R.id.loginGroup);
        fragmentContainer = findViewById(R.id.fragmentContainer);
        authHeaderContainer = findViewById(R.id.authHeaderContainer);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        btnBackNavigation = findViewById(R.id.btnBackNavigation);
        drawerLayout = findViewById(R.id.drawerLayout);
        btnNavigationView = findViewById(R.id.btnNavigationView);
        navigationView = findViewById(R.id.navigationView);

        apiService = ApiClient.getClient().create(ApiService.class);
        getSupportFragmentManager().addOnBackStackChangedListener(
                this::syncFragmentVisibility
        );
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(androidx.fragment.app.FragmentManager fm, Fragment f) {
                if (f.getId() == R.id.fragmentContainer) {
                    updateAuthenticatedHeader(f);
                    enfocarScannerGlobal();
                }
            }
        }, false);

        btnBackNavigation.setOnClickListener(v -> getSupportFragmentManager().popBackStack());
        btnNavigationView.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));
        navigationView.setNavigationItemSelectedListener(this::handleNavigationItemSelected);
        inicializarScannerGlobal();

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
                String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

                if (validateInputs(username, password)) {
                    performLogin(username, password);
                }
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
                showFragmentContainer();
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new RegistroFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        if (savedInstanceState != null) {
            syncFragmentVisibility();
        } else if (isSessionActive()) {
            restoreSession();
        } else {
            showLoginForm();
        }
    }

    private boolean validateInputs(String username, String password) {
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, "Por favor ingrese su usuario", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Por favor ingrese su contraseÃ±a", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void performLogin(String username, String password) {
        hideKeyboard();
        LoginRequest loginRequest = new LoginRequest(username, password);
        Call<LoginResponse> call = apiService.login(loginRequest);

        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(LoginActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();

                    String rol = response.body().getUsuario().getNombreRol();
                    Integer idRol = response.body().getUsuario().getIdRol();
                    String usuarioRespuesta = response.body().getUsuario().getUsuario();
                    Long usuarioId = response.body().getUsuario().getId();
                    boolean isAdmin = isAdminRole(rol, idRol, usuarioRespuesta, username);
                    saveSession(isAdmin,
                            usuarioRespuesta != null && !usuarioRespuesta.trim().isEmpty() ? usuarioRespuesta.trim() : username,
                            usuarioId);

                    if (isAdmin) {
                        openAdminPanel();
                    } else {
                        openUserPanel();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, extractErrorMessage(response), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Error de conexiÃ³n: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String extractErrorMessage(Response<LoginResponse> response) {
        if (response.errorBody() == null) {
            return "Credenciales incorrectas";
        }

        try {
            String errorJson = response.errorBody().string();
            LoginResponse errorResponse = new Gson().fromJson(errorJson, LoginResponse.class);
            if (errorResponse != null && errorResponse.getMessage() != null && !errorResponse.getMessage().isEmpty()) {
                return errorResponse.getMessage();
            }
        } catch (IOException ignored) {
        }

        return "Credenciales incorrectas";
    }

    private boolean isAdminRole(String rol, Integer idRol, String usuarioRespuesta, String usernameIngresado) {
        if (idRol != null && idRol == 1) {
            return true;
        }

        if (rol != null) {
            String normalizedRole = rol.trim().toUpperCase();
            if (normalizedRole.equals("ADMIN")
                    || normalizedRole.equals("ADMINISTRADOR")
                    || normalizedRole.contains("ADMIN")) {
                return true;
            }
        }

        return isAdminUsername(usuarioRespuesta) || isAdminUsername(usernameIngresado);
    }

    private boolean isAdminUsername(String username) {
        if (username == null) {
            return false;
        }

        String normalizedUsername = username.trim().toUpperCase();
        return normalizedUsername.equals("ADMIN")
                || normalizedUsername.equals("ADMINISTRADOR")
                || normalizedUsername.contains("ADMIN");
    }

    private void openAdminPanel() {
        showFragmentContainer();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new PanelAdministradorFragment())
                .commit();
    }

    private void openUserPanel() {
        showFragmentContainer();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new PanelUsuarioFragment())
                .commit();
    }

    private void showFragmentContainer() {
        hideKeyboard();
        loginGroup.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);
        enfocarScannerGlobal();
        if (!isSessionActive()) {
            authHeaderContainer.setVisibility(View.GONE);
            btnNavigationView.setVisibility(View.GONE);
            navigationView.setVisibility(View.GONE);
        }
    }

    private void showLoginForm() {
        drawerLayout.closeDrawer(GravityCompat.END);
        fragmentContainer.setVisibility(View.GONE);
        loginGroup.setVisibility(View.VISIBLE);
        authHeaderContainer.setVisibility(View.GONE);
        btnNavigationView.setVisibility(View.GONE);
        navigationView.setVisibility(View.GONE);
        if (etScannerGlobal != null) etScannerGlobal.clearFocus();
    }

    private void syncFragmentVisibility() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (currentFragment != null) {
            if (!isAuthorizedForFragment(currentFragment)) {
                return;
            }
            showFragmentContainer();
            updateAuthenticatedHeader(currentFragment);
        } else {
            showLoginForm();
        }
    }

    private boolean isAuthorizedForFragment(Fragment fragment) {
        if (fragment instanceof InventarioAdminFragment && !isAdminSession()) {
            Toast.makeText(this, "Solo el administrador puede acceder a Inventario.", Toast.LENGTH_SHORT).show();
            openUserPanel();
            return false;
        }

        return true;
    }

    public void logout() {
        clearSession();
        drawerLayout.closeDrawer(GravityCompat.END);
        getSupportFragmentManager().popBackStackImmediate(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);

        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (currentFragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .remove(currentFragment)
                    .commitNow();
        }

        etUsername.setText("");
        etPassword.setText("");
        if (etScannerGlobal != null) etScannerGlobal.setText("");
        showLoginForm();
    }

    public void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.cerrar_sesion_confirmacion)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.aceptar, (dialog, which) -> logout())
                .show();
    }

    private boolean handleNavigationItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuCerrarSesion) {
            drawerLayout.closeDrawer(GravityCompat.END);
            showLogoutConfirmation();
            return true;
        }
        return false;
    }

    private void updateAuthenticatedHeader(Fragment fragment) {
        if (fragment == null || !isSessionActive()) {
            authHeaderContainer.setVisibility(View.GONE);
            btnNavigationView.setVisibility(View.GONE);
            navigationView.setVisibility(View.GONE);
            return;
        }

        authHeaderContainer.setVisibility(View.VISIBLE);
        btnBackNavigation.setVisibility(canNavigateBack() ? View.VISIBLE : View.GONE);
        btnNavigationView.setVisibility(View.VISIBLE);
        navigationView.setVisibility(View.VISIBLE);
        tvHeaderTitle.setText(getHeaderTitle(fragment));
    }

    private boolean canNavigateBack() {
        return getSupportFragmentManager().getBackStackEntryCount() > 0;
    }

    private String getHeaderTitle(Fragment fragment) {
        if (fragment instanceof PanelAdministradorFragment) {
            return getString(R.string.panel_administrador_titulo);
        }
        if (fragment instanceof PanelUsuarioFragment) {
            return getString(R.string.panel_usuario_titulo);
        }
        if (fragment instanceof MenuPrincipalFragment) {
            return getString(R.string.menu_principal_titulo);
        }
        if (fragment instanceof UsuariosRegistradosFragment) {
            return getString(R.string.usuarios_registrados_titulo);
        }
        if (fragment instanceof ReportesAdminFragment) {
            return getString(R.string.reportes_titulo);
        }
        if (fragment instanceof BdAdminDataFragment) {
            return getString(R.string.base_de_datos_titulo);
        }
        if (fragment instanceof HistorialAdminFragment) {
            return "Historial Administrador";
        }
        if (fragment instanceof HistorialUsuarioFragment) {
            return "Historial usuario";
        }
        if (fragment instanceof GrupoInventarioFragment) {
            return ((GrupoInventarioFragment) fragment).getTituloPantalla();
        }
        if (fragment instanceof GrupoInventarioAdminFragment) {
            return ((GrupoInventarioAdminFragment) fragment).getTituloPantalla();
        }
        if (fragment instanceof InventarioAdminFragment) {
            return getString(R.string.inventario_admin_titulo);
        }
        if (fragment instanceof InventarioFragment) {
            return getString(R.string.inventario_opcion);
        }
        return "";
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view == null) {
            view = fragmentContainer;
        }

        if (view != null) {
            IBinder windowToken = view.getWindowToken();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && windowToken != null) {
                imm.hideSoftInputFromWindow(windowToken, 0);
            }
            view.clearFocus();
        }
    }

    private void inicializarScannerGlobal() {
        if (fragmentContainer == null || etScannerGlobal != null) {
            return;
        }
        etScannerGlobal = new EditText(this);
        etScannerGlobal.setLayoutParams(new FrameLayout.LayoutParams(1, 1));
        etScannerGlobal.setAlpha(0f);
        etScannerGlobal.setFocusable(true);
        etScannerGlobal.setFocusableInTouchMode(true);
        etScannerGlobal.setCursorVisible(false);
        etScannerGlobal.setLongClickable(false);
        etScannerGlobal.setTextIsSelectable(false);
        etScannerGlobal.setInputType(InputType.TYPE_NULL);
        etScannerGlobal.setShowSoftInputOnFocus(false);
        fragmentContainer.addView(etScannerGlobal);
        etScannerGlobal.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_NULL) {
                String codigo = v.getText() != null ? v.getText().toString() : "";
                limpiarScannerGlobal();
                enfocarScannerGlobal();
                procesarEscaneoGlobal(codigo);
                return true;
            }
            return false;
        });
        etScannerGlobal.setOnKeyListener((v, keyCode, event) -> {
            if (event != null && event.getAction() == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_TAB)) {
                String codigo = etScannerGlobal.getText() != null ? etScannerGlobal.getText().toString() : "";
                limpiarScannerGlobal();
                enfocarScannerGlobal();
                procesarEscaneoGlobal(codigo);
                return true;
            }
            return false;
        });
        etScannerGlobal.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (procesandoScanner) return;
                String texto = s != null ? s.toString() : "";
                if (texto.contains("\n") || texto.contains("\r")) {
                    String codigo = texto;
                    limpiarScannerGlobal();
                    enfocarScannerGlobal();
                    procesarEscaneoGlobal(codigo);
                    return;
                }
                if (scannerRunnable != null) scannerHandler.removeCallbacks(scannerRunnable);
                scannerRunnable = () -> {
                    if (procesandoScanner) return;
                    String codigo = etScannerGlobal != null && etScannerGlobal.getText() != null ? etScannerGlobal.getText().toString() : "";
                    if (codigo.trim().isEmpty()) return;
                    limpiarScannerGlobal();
                    enfocarScannerGlobal();
                    procesarEscaneoGlobal(codigo);
                };
                scannerHandler.postDelayed(scannerRunnable, 250);
            }
        });
    }

    private void enfocarScannerGlobal() {
        if (etScannerGlobal == null || fragmentContainer == null || !isSessionActive()) {
            return;
        }
        if (focusRetryRunnable != null) {
            scannerHandler.removeCallbacks(focusRetryRunnable);
        }
        focusRetryRunnable = () -> {
            if (etScannerGlobal == null || fragmentContainer == null || !isSessionActive()) {
                return;
            }
            if (!fragmentContainer.isShown()) {
                scannerHandler.postDelayed(focusRetryRunnable, 120);
                return;
            }
            etScannerGlobal.requestFocus();
            if (etScannerGlobal.getText() != null) {
                etScannerGlobal.setSelection(etScannerGlobal.getText().length());
            }
            ocultarTeclado(etScannerGlobal);
        };
        scannerHandler.post(focusRetryRunnable);
    }

    private void ocultarTeclado(View tokenView) {
        if (tokenView == null) return;
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && tokenView.getWindowToken() != null) {
            imm.hideSoftInputFromWindow(tokenView.getWindowToken(), 0);
        }
    }

    private void limpiarScannerGlobal() {
        if (etScannerGlobal == null) return;
        procesandoScanner = true;
        etScannerGlobal.setText("");
        procesandoScanner = false;
    }

    private void procesarEscaneoGlobal(String codigo) {
        if (!isSessionActive() || fragmentContainer == null || !fragmentContainer.isShown() || apiService == null) {
            return;
        }
        if (buscandoEscaneo) {
            return;
        }
        String limpio = limpiarCodigoEscaneado(codigo);
        if (limpio.isEmpty()) {
            return;
        }
        buscandoEscaneo = true;
        Toast.makeText(this, "Buscando artículo...", Toast.LENGTH_SHORT).show();
        List<String> candidatos = extraerCandidatosCodigo(limpio);
        apiService.getGrupos().enqueue(new Callback<List<Grupo>>() {
            @Override
            public void onResponse(Call<List<Grupo>> call, Response<List<Grupo>> response) {
                if (isFinishing()) return;
                List<Grupo> grupos = response.isSuccessful() ? response.body() : null;
                if (grupos == null || grupos.isEmpty()) {
                    buscandoEscaneo = false;
                    Toast.makeText(LoginActivity.this, "No hay grupos para buscar.", Toast.LENGTH_SHORT).show();
                    enfocarScannerGlobal();
                    return;
                }
                buscarEnGrupo(grupos, 0, candidatos);
            }

            @Override
            public void onFailure(Call<List<Grupo>> call, Throwable t) {
                if (isFinishing()) return;
                buscandoEscaneo = false;
                Toast.makeText(LoginActivity.this, "Error de conexión: " + (t != null ? t.getMessage() : ""), Toast.LENGTH_SHORT).show();
                enfocarScannerGlobal();
            }
        });
    }

    private void buscarEnGrupo(List<Grupo> grupos, int index, List<String> candidatos) {
        if (isFinishing()) return;
        if (index >= grupos.size()) {
            buscandoEscaneo = false;
            Toast.makeText(this, "Producto no encontrado.", Toast.LENGTH_SHORT).show();
            enfocarScannerGlobal();
            return;
        }
        Grupo grupo = grupos.get(index);
        String identificador = "";
        if (grupo != null) {
            String tabla = grupo.getNombreTabla();
            String nombre = grupo.getNombreGrupo();
            identificador = tabla != null && !tabla.trim().isEmpty() ? tabla.trim() : (nombre != null ? nombre.trim() : "");
        }
        if (identificador.isEmpty()) {
            buscarEnGrupo(grupos, index + 1, candidatos);
            return;
        }
        final String identificadorFinal = identificador;
        final String nombreGrupoFinal = grupo != null && grupo.getNombreMostrado() != null && !grupo.getNombreMostrado().trim().isEmpty()
                ? grupo.getNombreMostrado().trim()
                : identificadorFinal;
        apiService.getRegistrosGrupo(identificadorFinal).enqueue(new Callback<List<LinkedHashMap<String, Object>>>() {
            @Override
            public void onResponse(Call<List<LinkedHashMap<String, Object>>> call, Response<List<LinkedHashMap<String, Object>>> response) {
                if (isFinishing()) return;
                List<LinkedHashMap<String, Object>> registros = response.isSuccessful() ? response.body() : null;
                LinkedHashMap<String, Object> registro = buscarRegistroPorCodigo(registros, candidatos);
                if (registro != null) {
                    mostrarDialogEntradaSalida(nombreGrupoFinal, identificadorFinal, registro);
                    return;
                }
                buscarEnGrupo(grupos, index + 1, candidatos);
            }

            @Override
            public void onFailure(Call<List<LinkedHashMap<String, Object>>> call, Throwable t) {
                if (isFinishing()) return;
                buscarEnGrupo(grupos, index + 1, candidatos);
            }
        });
    }

    private void mostrarDialogEntradaSalida(String nombreGrupo, String identificador, LinkedHashMap<String, Object> registro) {
        Long registroId = obtenerIdRegistro(registro);
        if (registroId == null) {
            buscandoEscaneo = false;
            Toast.makeText(this, "No se pudo obtener el id del registro.", Toast.LENGTH_SHORT).show();
            enfocarScannerGlobal();
            return;
        }
        String columnaStock = obtenerColumnaStock(registro);
        if (columnaStock == null) {
            buscandoEscaneo = false;
            Toast.makeText(this, "No se encontró la columna de stock.", Toast.LENGTH_SHORT).show();
            enfocarScannerGlobal();
            return;
        }
        Integer stockActual = obtenerStockActual(registro, columnaStock);
        if (stockActual == null) {
            buscandoEscaneo = false;
            Toast.makeText(this, "Stock inválido.", Toast.LENGTH_SHORT).show();
            enfocarScannerGlobal();
            return;
        }
        Bitmap imagen = obtenerImagenRegistro(registro);
        View carta = crearCartaProducto(imagen, registro, columnaStock, stockActual);
        String articuloHistorial = construirDescripcionProducto(registro);
        int stockAnterior = stockActual;
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(carta)
                .setNegativeButton(R.string.cancelar, (d, w) -> { buscandoEscaneo = false; })
                .setPositiveButton("Entrada", (d, w) -> actualizarStockGlobal(nombreGrupo, identificador, registroId, 1, stockAnterior, columnaStock, articuloHistorial))
                .setNeutralButton("Salida", (d, w) -> {
                    if (stockActual <= 0) {
                        Toast.makeText(LoginActivity.this, R.string.stock_no_disponible, Toast.LENGTH_SHORT).show();
                        buscandoEscaneo = false;
                        enfocarScannerGlobal();
                        return;
                    }
                    actualizarStockGlobal(nombreGrupo, identificador, registroId, -1, stockAnterior, columnaStock, articuloHistorial);
                })
                .create();
        dialog.setOnShowListener(d -> {
            Button bPos = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button bNeg = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            Button bNeu = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (bPos != null) { bPos.setFocusable(false); bPos.setFocusableInTouchMode(false); }
            if (bNeg != null) { bNeg.setFocusable(false); bNeg.setFocusableInTouchMode(false); }
            if (bNeu != null) { bNeu.setFocusable(false); bNeu.setFocusableInTouchMode(false); }
        });
        dialog.setOnDismissListener(d -> { buscandoEscaneo = false; enfocarScannerGlobal(); });
        dialog.show();
    }

    private void actualizarStockGlobal(String nombreGrupo,
                                       String identificador,
                                       long registroId,
                                       int delta,
                                       int stockAnterior,
                                       String columnaStock,
                                       String articuloHistorial) {
        if (apiService == null) return;
        Long usuarioId = obtenerUsuarioIdSesion();
        if (usuarioId == null) {
            resolverYGuardarUsuarioId(() -> ejecutarActualizarStockGlobal(identificador, registroId, delta));
            return;
        }
        ejecutarActualizarStockGlobal(identificador, registroId, delta);
    }

    private void ejecutarActualizarStockGlobal(String identificador, long registroId, int delta) {
        if (apiService == null) return;
        Long usuarioId = obtenerUsuarioIdSesion();
        Call<LinkedHashMap<String, Object>> call = delta > 0
                ? apiService.aumentarStockRegistro(identificador, registroId, usuarioId)
                : apiService.reducirStockRegistro(identificador, registroId, usuarioId);
        call.enqueue(new Callback<LinkedHashMap<String, Object>>() {
            @Override
            public void onResponse(Call<LinkedHashMap<String, Object>> call, Response<LinkedHashMap<String, Object>> response) {
                if (isFinishing()) return;
                if (response.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, R.string.stock_actualizado_exito, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LoginActivity.this, getString(R.string.stock_actualizado_error_http, response.code()), Toast.LENGTH_LONG).show();
                }
                buscandoEscaneo = false;
                enfocarScannerGlobal();
            }

            @Override
            public void onFailure(Call<LinkedHashMap<String, Object>> call, Throwable t) {
                if (isFinishing()) return;
                Toast.makeText(LoginActivity.this, "Error de conexión: " + (t != null ? t.getMessage() : ""), Toast.LENGTH_SHORT).show();
                buscandoEscaneo = false;
                enfocarScannerGlobal();
            }
        });
    }

    private String limpiarCodigoEscaneado(String codigo) {
        if (codigo == null) return "";
        String limpio = codigo.replace("\r", "").replace("\n", "").trim();
        if (limpio.contains("\t")) limpio = limpio.replace("\t", "").trim();
        return limpio;
    }

    private List<String> extraerCandidatosCodigo(String escaneo) {
        ArrayList<String> candidatos = new ArrayList<>();
        String base = limpiarCodigoEscaneado(escaneo);
        if (!base.isEmpty()) candidatos.add(base);
        String alfanumerico = base.replaceAll("[^A-Za-z0-9]+", "");
        if (!alfanumerico.isEmpty() && !alfanumerico.equalsIgnoreCase(base)) candidatos.add(alfanumerico);
        Matcher m = Pattern.compile("(?i)(codigo|code|qr|barra|barras|barcode|ean|upc|sku)\\s*[:=]\\s*([A-Za-z0-9\\-_.]+)").matcher(base);
        while (m.find()) {
            String g = m.group(2);
            if (g != null && !g.trim().isEmpty()) candidatos.add(g.trim());
        }
        ArrayList<String> unicos = new ArrayList<>();
        for (String c : candidatos) {
            if (c == null) continue;
            String t = c.trim();
            if (t.isEmpty()) continue;
            boolean existe = false;
            for (String u : unicos) if (u.equalsIgnoreCase(t)) { existe = true; break; }
            if (!existe) unicos.add(t);
        }
        return unicos;
    }

    private String normalizarIdentificadorLocal(String valor) {
        if (valor == null) return "";
        return valor.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+", "").replaceAll("_+$", "");
    }

    private String normalizarValorComparacion(String valor) {
        if (valor == null) return "";
        return valor.trim().toLowerCase().replaceAll("[^a-z0-9]+", "");
    }

    private LinkedHashMap<String, Object> buscarRegistroPorCodigo(List<LinkedHashMap<String, Object>> registros, List<String> candidatos) {
        if (registros == null || registros.isEmpty() || candidatos == null || candidatos.isEmpty()) return null;
        for (LinkedHashMap<String, Object> registro : registros) {
            if (registro == null) continue;
            for (Map.Entry<String, Object> entry : registro.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) continue;
                String clave = normalizarIdentificadorLocal(entry.getKey());
                if (!clave.contains("codigo") && !clave.contains("code") && !clave.contains("qr") && !clave.contains("barra") && !clave.contains("barcode") && !clave.contains("ean") && !clave.contains("upc") && !clave.contains("sku")) continue;
                String valor = String.valueOf(entry.getValue()).trim();
                String valorNorm = normalizarValorComparacion(valor);
                for (String candidato : candidatos) {
                    if (candidato == null || candidato.trim().isEmpty()) continue;
                    if (valor.equalsIgnoreCase(candidato)) return registro;
                    String candidatoNorm = normalizarValorComparacion(candidato);
                    if (!valorNorm.isEmpty() && !candidatoNorm.isEmpty()) {
                        if (valorNorm.equals(candidatoNorm)) return registro;
                        if (valorNorm.length() >= 4 && candidatoNorm.length() >= 4) {
                            if (valorNorm.contains(candidatoNorm) || candidatoNorm.contains(valorNorm)) return registro;
                        }
                    }
                }
            }
        }
        return null;
    }

    private String construirDescripcionProducto(Map<String, Object> registro) {
        if (registro == null) return "Producto";
        String codigo = "", descripcion = "";
        for (Map.Entry<String, Object> entry : registro.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            String clave = normalizarIdentificadorLocal(entry.getKey());
            String valor = String.valueOf(entry.getValue()).trim();
            if (valor.isEmpty()) continue;
            if (codigo.isEmpty() && clave.contains("codigo")) codigo = valor;
            else if (descripcion.isEmpty() && (clave.contains("descripcion") || clave.contains("nombre"))) descripcion = valor;
        }
        if (!codigo.isEmpty() && !descripcion.isEmpty()) return codigo + " - " + descripcion;
        if (!codigo.isEmpty()) return codigo;
        if (!descripcion.isEmpty()) return descripcion;
        for (Map.Entry<String, Object> entry : registro.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            String claveNormalizada = entry.getKey().trim().toLowerCase();
            if (claveNormalizada.equals("id") || claveNormalizada.equals("idregistro") || claveNormalizada.equals("id_registro")) continue;
            String valor = String.valueOf(entry.getValue()).trim();
            if (!valor.isEmpty()) return valor;
        }
        return "Producto";
    }

    private Long obtenerIdRegistro(Map<String, Object> registro) {
        if (registro == null) return null;
        String[] claves = {"id", "idRegistro", "id_registro"};
        for (String clave : claves) {
            Object valor = registro.get(clave);
            if (valor instanceof Number) return ((Number) valor).longValue();
            if (valor != null) {
                try { return Long.valueOf(String.valueOf(valor)); }
                catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    private String obtenerColumnaStock(Map<String, Object> registro) {
        if (registro == null) return null;
        String buscada = normalizarIdentificadorLocal(getString(R.string.stock_columna_predeterminada));
        for (Map.Entry<String, Object> entry : registro.entrySet()) {
            if (entry.getKey() == null) continue;
            if (normalizarIdentificadorLocal(entry.getKey()).equals(buscada)) return entry.getKey();
        }
        return null;
    }

    private Integer obtenerStockActual(Map<String, Object> registro, String columnaStock) {
        if (registro == null || columnaStock == null) return null;
        Object valorObj = registro.get(columnaStock);
        if (valorObj instanceof Number) return ((Number) valorObj).intValue();
        String valor = String.valueOf(valorObj != null ? valorObj : "").trim();
        if (valor.isEmpty()) return 0;
        try { return (int) Double.parseDouble(valor); }
        catch (NumberFormatException ignored) { return null; }
    }

    private String formatearNombreColumna(String nombre) {
        if (nombre == null) return "";
        String limpia = nombre.trim().replace('_', ' ');
        if (limpia.isEmpty()) return "";
        String[] palabras = limpia.split("\\s+");
        StringBuilder resultado = new StringBuilder();
        for (String palabra : palabras) {
            if (palabra.isEmpty()) continue;
            if (resultado.length() > 0) resultado.append(' ');
            resultado.append(Character.toUpperCase(palabra.charAt(0)));
            if (palabra.length() > 1) resultado.append(palabra.substring(1).toLowerCase());
        }
        return resultado.toString();
    }

    private boolean esColumnaDeFoto(String nombreColumna) {
        if (nombreColumna == null) return false;
        String normalizado = nombreColumna.trim().toLowerCase();
        return normalizado.contains("foto") || normalizado.contains("imagen") || normalizado.contains("image") || normalizado.contains("photo");
    }

    private Bitmap obtenerImagenRegistro(Map<String, Object> valores) {
        if (valores == null) return null;
        for (Map.Entry<String, Object> entry : valores.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            if (!esColumnaDeFoto(entry.getKey())) continue;
            Bitmap bitmap = decodificarImagenBase64(String.valueOf(entry.getValue()));
            if (bitmap != null) return bitmap;
        }
        return null;
    }

    private Bitmap decodificarImagenBase64(String valor) {
        try {
            byte[] bytes = Base64.decode(valor, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception ignored) {
            return null;
        }
    }

    private View crearCartaProducto(Bitmap imagen, Map<String, Object> registro, String columnaStock, int stockActual) {
        String titulo = construirDescripcionProducto(registro);
        MaterialCardView card = new MaterialCardView(this);
        card.setUseCompatPadding(false);
        card.setCardElevation(0f);
        card.setRadius(0f);
        card.setCardBackgroundColor(0xFFFFFFFF);
        ViewGroup.MarginLayoutParams cardParams = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int m = dpToPx(10);
        cardParams.setMargins(m, m, m, 0);
        card.setLayoutParams(cardParams);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        card.addView(root);

        ImageView iv = new ImageView(this);
        LinearLayout.LayoutParams ivParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(180));
        iv.setLayoutParams(ivParams);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setBackgroundColor(0xFFEFEFEF);
        iv.setAdjustViewBounds(true);
        iv.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));
        if (imagen != null) iv.setImageBitmap(imagen);
        else iv.setImageResource(android.R.drawable.ic_menu_report_image);
        root.addView(iv);

        LinearLayout textos = new LinearLayout(this);
        textos.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textosParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int p = dpToPx(14);
        textosParams.setMargins(p, p, p, p);
        textos.setLayoutParams(textosParams);
        root.addView(textos);

        TextView tvTitulo = new TextView(this);
        tvTitulo.setText(!TextUtils.isEmpty(titulo) ? titulo : "Producto");
        tvTitulo.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitulo.setTextSize(18);
        textos.addView(tvTitulo);

        if (registro != null) {
            String stockKeyNorm = normalizarIdentificadorLocal(columnaStock);
            for (Map.Entry<String, Object> entry : registro.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) continue;
                String claveRaw = entry.getKey();
                String claveNorm = normalizarIdentificadorLocal(claveRaw);
                if (claveNorm.equals("id") || claveNorm.equals("idregistro") || claveNorm.equals("id_registro")) continue;
                if (esColumnaDeFoto(claveRaw)) continue;
                if (!stockKeyNorm.isEmpty() && claveNorm.equals(stockKeyNorm)) continue;
                String valor = String.valueOf(entry.getValue()).trim();
                if (valor.isEmpty() || valor.equalsIgnoreCase("null")) continue;
                TextView tvLinea = new TextView(this);
                tvLinea.setText(formatearNombreColumna(claveRaw) + ": " + valor);
                tvLinea.setTextSize(14);
                textos.addView(tvLinea);
            }
        }

        TextView tvStock = new TextView(this);
        tvStock.setText("Stock actual: " + stockActual);
        tvStock.setTextSize(14);
        textos.addView(tvStock);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.addView(card);
        return scrollView;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void restoreSession() {
        if (isAdminSession()) {
            openAdminPanel();
        } else {
            openUserPanel();
        }
    }

    private void saveSession(boolean isAdmin) {
        SharedPreferences preferences = getSharedPreferences(PREFS_SESSION, MODE_PRIVATE);
        preferences.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putBoolean(KEY_IS_ADMIN, isAdmin)
                .apply();
    }

    private void saveSession(boolean isAdmin, String username) {
        SharedPreferences preferences = getSharedPreferences(PREFS_SESSION, MODE_PRIVATE);
        preferences.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putBoolean(KEY_IS_ADMIN, isAdmin)
                .putString(KEY_USERNAME, obtenerValorSeguroSesion(username))
                .apply();
    }

    private void saveSession(boolean isAdmin, String username, Long userId) {
        SharedPreferences preferences = getSharedPreferences(PREFS_SESSION, MODE_PRIVATE);
        preferences.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putBoolean(KEY_IS_ADMIN, isAdmin)
                .putString(KEY_USERNAME, obtenerValorSeguroSesion(username))
                .putLong(KEY_USER_ID, userId != null ? userId : -1L)
                .apply();
    }

    private String obtenerUsuarioSesion() {
        SharedPreferences preferences = getSharedPreferences(PREFS_SESSION, MODE_PRIVATE);
        return obtenerValorSeguroSesion(preferences.getString(KEY_USERNAME, ""));
    }

    private Long obtenerUsuarioIdSesion() {
        SharedPreferences preferences = getSharedPreferences(PREFS_SESSION, MODE_PRIVATE);
        long id = preferences.getLong(KEY_USER_ID, -1L);
        return id > 0L ? id : null;
    }

    private void guardarUsuarioIdSesion(Long userId) {
        SharedPreferences preferences = getSharedPreferences(PREFS_SESSION, MODE_PRIVATE);
        preferences.edit().putLong(KEY_USER_ID, userId != null ? userId : -1L).apply();
    }

    private void resolverYGuardarUsuarioId(Runnable onComplete) {
        if (apiService == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        String username = obtenerUsuarioSesion();
        if (username == null || username.trim().isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }
        apiService.getUsuarios().enqueue(new Callback<List<com.example.vigiaapp.Archivos.Usuario>>() {
            @Override
            public void onResponse(Call<List<com.example.vigiaapp.Archivos.Usuario>> call,
                                   Response<List<com.example.vigiaapp.Archivos.Usuario>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    if (onComplete != null) onComplete.run();
                    return;
                }
                Long encontrado = null;
                for (com.example.vigiaapp.Archivos.Usuario u : response.body()) {
                    if (u == null || u.getUsuario() == null) continue;
                    if (u.getUsuario().trim().equalsIgnoreCase(username.trim())) {
                        encontrado = u.getId();
                        break;
                    }
                }
                if (encontrado != null) guardarUsuarioIdSesion(encontrado);
                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onFailure(Call<List<com.example.vigiaapp.Archivos.Usuario>> call, Throwable t) {
                if (onComplete != null) onComplete.run();
            }
        });
    }

    private String obtenerValorSeguroSesion(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private boolean isSessionActive() {
        SharedPreferences preferences = getSharedPreferences(PREFS_SESSION, MODE_PRIVATE);
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    private boolean isAdminSession() {
        SharedPreferences preferences = getSharedPreferences(PREFS_SESSION, MODE_PRIVATE);
        return preferences.getBoolean(KEY_IS_ADMIN, false);
    }

    private void clearSession() {
        SharedPreferences preferences = getSharedPreferences(PREFS_SESSION, MODE_PRIVATE);
        preferences.edit().clear().apply();
    }
}
