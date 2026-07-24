package com.example.vigiaapp;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BdAdminDataFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BdAdminDataFragment extends Fragment {
    private static final String DB_NAME = "vigiadb.db";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private final ActivityResultLauncher<String[]> seleccionarBaseDatosLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::mostrarOpcionesCargaBd);

    private static class ColumnInfo {
        final String name;
        final boolean primaryKey;

        ColumnInfo(String name, boolean primaryKey) {
            this.name = name;
            this.primaryKey = primaryKey;
        }
    }

    public BdAdminDataFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment BdAdminDataFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static BdAdminDataFragment newInstance(String param1, String param2) {
        BdAdminDataFragment fragment = new BdAdminDataFragment();
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
        return inflater.inflate(R.layout.fragment_bd_admin_data, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button btnCargarBd = view.findViewById(R.id.btnCargarBd);
        Button btnCopiaSeguridad = view.findViewById(R.id.btnCopiaSeguridad);
        Button btnExportarBd = view.findViewById(R.id.btnExportarBd);

        btnCargarBd.setOnClickListener(v -> seleccionarBaseDatosLauncher.launch(new String[]{
                "application/vnd.sqlite3",
                "application/octet-stream",
                "*/*"
        }));
        btnCopiaSeguridad.setOnClickListener(v -> makeBackup());
        btnExportarBd.setOnClickListener(v -> exportDatabase());
    }

    private void mostrarOpcionesCargaBd(@Nullable Uri uri) {
        if (!isAdded() || uri == null) {
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.cargar_bd)
                .setItems(new CharSequence[]{
                                getString(R.string.sobreescribir_datos_existentes),
                                getString(R.string.agregar_datos_nuevos)
                        },
                        (dialog, which) -> importarBaseDatosDesdeUri(uri, which == 0))
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void importarBaseDatosDesdeUri(@NonNull Uri uri, boolean sobrescribir) {
        if (!isAdded() || uri == null) {
            return;
        }

        File destino = requireContext().getDatabasePath(DB_NAME);
        File directorioDb = destino.getParentFile();
        if (directorioDb != null && !directorioDb.exists() && !directorioDb.mkdirs()) {
            Toast.makeText(requireContext(), getString(R.string.error_cargar_bd, getString(R.string.archivo_bd_invalido)), Toast.LENGTH_SHORT).show();
            return;
        }

        File temporal = new File(requireContext().getCacheDir(), "import_" + System.currentTimeMillis() + ".db");
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                Toast.makeText(requireContext(), R.string.archivo_bd_invalido, Toast.LENGTH_SHORT).show();
                return;
            }

            copyStreamToFile(inputStream, temporal);
            if (sobrescribir || !destino.exists()) {
                if (!sobrescribir) {
                    Toast.makeText(requireContext(), R.string.sin_bd_local_se_importara, Toast.LENGTH_SHORT).show();
                }
                copyFile(temporal, destino);
            } else {
                mergeDatabaseFiles(temporal, destino);
            }
            Toast.makeText(
                    requireContext(),
                    (sobrescribir ? getString(R.string.bd_cargada_exito) : getString(R.string.bd_actualizada_exito))
                            + ". " + getString(R.string.reiniciar_app_bd),
                    Toast.LENGTH_LONG
            ).show();
        } catch (IOException e) {
            Toast.makeText(requireContext(), getString(R.string.error_cargar_bd, e.getMessage()), Toast.LENGTH_LONG).show();
        } finally {
            if (temporal.exists()) {
                //noinspection ResultOfMethodCallIgnored
                temporal.delete();
            }
        }
    }

    private void makeBackup() {
        try {
            File currentDB = requireContext().getDatabasePath(DB_NAME);
            File backupDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File backupDB = new File(backupDir, buildDatedFileName("backup"));

            if (!currentDB.exists()) {
                Toast.makeText(requireContext(), "No se encontró la base de datos local", Toast.LENGTH_SHORT).show();
                return;
            }

            copyFile(currentDB, backupDB);
            Toast.makeText(requireContext(), "Copia de seguridad creada en: " + backupDB.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Error al crear copia de seguridad: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void exportDatabase() {
        try {
            File currentDB = requireContext().getDatabasePath(DB_NAME);
            File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File exportDB = new File(exportDir, buildDatedFileName("export"));

            if (!currentDB.exists()) {
                Toast.makeText(requireContext(), "No se encontró la base de datos local", Toast.LENGTH_SHORT).show();
                return;
            }

            copyFile(currentDB, exportDB);
            Toast.makeText(requireContext(), "Base de datos exportada a: " + exportDB.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Error al exportar base de datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists() && !destFile.createNewFile()) {
            throw new IOException("No se pudo crear el archivo destino");
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    private void copyStreamToFile(InputStream source, File destFile) throws IOException {
        if (!destFile.exists() && !destFile.createNewFile()) {
            throw new IOException("No se pudo crear el archivo destino");
        }

        try (OutputStream outputStream = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = source.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        }
    }

    private String buildDatedFileName(String suffix) {
        String baseName = DB_NAME.endsWith(".db") ? DB_NAME.substring(0, DB_NAME.length() - 3) : DB_NAME;
        String date = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return baseName + "_" + suffix + "_" + date + ".db";
    }

    private void mergeDatabaseFiles(File sourceFile, File targetFile) throws IOException {
        SQLiteDatabase sourceDb = null;
        SQLiteDatabase targetDb = null;

        try {
            sourceDb = SQLiteDatabase.openDatabase(sourceFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            targetDb = SQLiteDatabase.openDatabase(
                    targetFile.getAbsolutePath(),
                    null,
                    SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.CREATE_IF_NECESSARY
            );

            List<String> tables = getUserTables(sourceDb);
            targetDb.beginTransaction();
            for (String tableName : tables) {
                ensureTableExists(sourceDb, targetDb, tableName);
                mergeTable(sourceDb, targetDb, tableName);
            }
            targetDb.setTransactionSuccessful();
        } catch (SQLiteException e) {
            throw new IOException(getString(R.string.error_fusionar_bd) + ": " + e.getMessage(), e);
        } finally {
            if (targetDb != null) {
                if (targetDb.inTransaction()) {
                    targetDb.endTransaction();
                }
                targetDb.close();
            }
            if (sourceDb != null) {
                sourceDb.close();
            }
        }
    }

    private List<String> getUserTables(SQLiteDatabase database) {
        List<String> tables = new ArrayList<>();
        try (Cursor cursor = database.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name != 'android_metadata'",
                null
        )) {
            while (cursor.moveToNext()) {
                tables.add(cursor.getString(0));
            }
        }
        return tables;
    }

    private void ensureTableExists(SQLiteDatabase sourceDb, SQLiteDatabase targetDb, String tableName) {
        try (Cursor targetCursor = targetDb.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                new String[]{tableName}
        )) {
            if (targetCursor.moveToFirst()) {
                return;
            }
        }

        try (Cursor sourceCursor = sourceDb.rawQuery(
                "SELECT sql FROM sqlite_master WHERE type='table' AND name=?",
                new String[]{tableName}
        )) {
            if (sourceCursor.moveToFirst()) {
                String createStatement = sourceCursor.getString(0);
                if (createStatement != null && !createStatement.trim().isEmpty()) {
                    targetDb.execSQL(createStatement);
                }
            }
        }
    }

    private void mergeTable(SQLiteDatabase sourceDb, SQLiteDatabase targetDb, String tableName) {
        List<ColumnInfo> columns = getTableColumns(sourceDb, tableName);
        List<String> keyColumns = getKeyColumns(columns);

        try (Cursor sourceCursor = sourceDb.rawQuery("SELECT * FROM " + escapeIdentifier(tableName), null)) {
            while (sourceCursor.moveToNext()) {
                ContentValues values = buildContentValues(sourceCursor, columns);

                if (keyColumns.isEmpty()) {
                    if (!existsMatchingRow(targetDb, tableName, columns, values)) {
                        targetDb.insert(tableName, null, values);
                    }
                    continue;
                }

                String[] whereData = buildWhereClause(keyColumns, values);
                int updated = targetDb.update(tableName, values, whereData[0], extractArgs(whereData));
                if (updated == 0) {
                    targetDb.insert(tableName, null, values);
                }
            }
        }
    }

    private List<ColumnInfo> getTableColumns(SQLiteDatabase database, String tableName) {
        List<ColumnInfo> columns = new ArrayList<>();
        try (Cursor cursor = database.rawQuery("PRAGMA table_info(" + escapeIdentifier(tableName) + ")", null)) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                boolean isPk = cursor.getInt(cursor.getColumnIndexOrThrow("pk")) > 0;
                columns.add(new ColumnInfo(name, isPk));
            }
        }
        return columns;
    }

    private List<String> getKeyColumns(List<ColumnInfo> columns) {
        List<String> keys = new ArrayList<>();
        for (ColumnInfo column : columns) {
            if (column.primaryKey) {
                keys.add(column.name);
            }
        }
        if (!keys.isEmpty()) {
            return keys;
        }

        for (ColumnInfo column : columns) {
            String normalized = column.name == null ? "" : column.name.trim().toLowerCase(Locale.ROOT);
            if ("id".equals(normalized) || "id_registro".equals(normalized) || "idregistro".equals(normalized) || normalized.contains("codigo")) {
                keys.add(column.name);
                break;
            }
        }
        return keys;
    }

    private ContentValues buildContentValues(Cursor cursor, List<ColumnInfo> columns) {
        ContentValues values = new ContentValues();
        for (int i = 0; i < columns.size(); i++) {
            String columnName = columns.get(i).name;
            if (columnName == null) {
                continue;
            }

            switch (cursor.getType(i)) {
                case Cursor.FIELD_TYPE_NULL:
                    values.putNull(columnName);
                    break;
                case Cursor.FIELD_TYPE_INTEGER:
                    values.put(columnName, cursor.getLong(i));
                    break;
                case Cursor.FIELD_TYPE_FLOAT:
                    values.put(columnName, cursor.getDouble(i));
                    break;
                case Cursor.FIELD_TYPE_STRING:
                    values.put(columnName, cursor.getString(i));
                    break;
                case Cursor.FIELD_TYPE_BLOB:
                    values.put(columnName, cursor.getBlob(i));
                    break;
                default:
                    values.put(columnName, cursor.getString(i));
                    break;
            }
        }
        return values;
    }

    private boolean existsMatchingRow(SQLiteDatabase database, String tableName, List<ColumnInfo> columns, ContentValues values) {
        List<String> comparableColumns = new ArrayList<>();
        for (ColumnInfo column : columns) {
            Object value = values.get(column.name);
            if (!(value instanceof byte[])) {
                comparableColumns.add(column.name);
            }
        }

        if (comparableColumns.isEmpty()) {
            return false;
        }

        String[] whereData = buildWhereClause(comparableColumns, values);
        try (Cursor cursor = database.query(
                tableName,
                new String[]{"COUNT(*)"},
                whereData[0],
                extractArgs(whereData),
                null,
                null,
                null
        )) {
            return cursor.moveToFirst() && cursor.getInt(0) > 0;
        }
    }

    private String[] buildWhereClause(List<String> columns, ContentValues values) {
        StringBuilder where = new StringBuilder();
        List<String> args = new ArrayList<>();

        for (String column : columns) {
            if (where.length() > 0) {
                where.append(" AND ");
            }

            Object value = values.get(column);
            where.append(escapeIdentifier(column));
            if (value == null) {
                where.append(" IS NULL");
            } else {
                where.append(" = ?");
                args.add(String.valueOf(value));
            }
        }

        List<String> data = new ArrayList<>();
        data.add(where.toString());
        data.addAll(args);
        return data.toArray(new String[0]);
    }

    private String[] extractArgs(String[] whereData) {
        if (whereData.length <= 1) {
            return null;
        }
        String[] args = new String[whereData.length - 1];
        System.arraycopy(whereData, 1, args, 0, args.length);
        return args;
    }

    private String escapeIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }
}
