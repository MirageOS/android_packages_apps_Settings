package com.android.settings.personalise;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class PifDataPreference extends Preference {

    private static final String TAG = "PifDataPref";
    private ActivityResultLauncher<Intent> mFilePickerLauncher;

    public PifDataPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.pref_with_delete);
    }

    public void setFilePickerLauncher(ActivityResultLauncher<Intent> launcher) {
        this.mFilePickerLauncher = launcher;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        TextView title = (TextView) holder.findViewById(R.id.title);
        TextView summary = (TextView) holder.findViewById(R.id.summary);
        ImageButton deleteButton = (ImageButton) holder.findViewById(R.id.delete_button);

        title.setText(getTitle());
        summary.setText(getSummary());
        deleteButton.setContentDescription(
                getContext().getString(R.string.personalise_delete_description));

        holder.itemView.setOnClickListener(v -> {
            if (mFilePickerLauncher != null) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                        "application/json", "text/json", "text/plain",
                        "application/octet-stream"});
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                mFilePickerLauncher.launch(intent);
            }
        });

        deleteButton.setOnClickListener(v -> {
            Settings.Secure.putString(getContext().getContentResolver(),
                    Settings.Secure.PIF_DATA, null);
            Toast.makeText(getContext(), R.string.personalise_pif_cleared,
                    Toast.LENGTH_SHORT).show();
            callChangeListener(null);

            killPackages();
        });
    }

    public void handleFileSelected(Uri uri) {
        if (!PersonaliseFileUtils.matches(getContext(), uri, ".json",
                "application/json", "text/json")) {
            Toast.makeText(getContext(), R.string.personalise_invalid_file,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        try (InputStream inputStream = getContext().getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                Toast.makeText(getContext(), R.string.personalise_pif_read_failed,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line).append('\n');
            }

            String json = jsonContent.toString();
            try {
                new JSONObject(json);
            } catch (JSONException e) {
                Log.w(TAG, "Invalid PIF JSON", e);
                Toast.makeText(getContext(), R.string.personalise_pif_invalid,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Settings.Secure.putString(getContext().getContentResolver(),
                    Settings.Secure.PIF_DATA, json);
            Toast.makeText(getContext(), R.string.personalise_pif_loaded,
                    Toast.LENGTH_SHORT).show();
            callChangeListener(json);

            killPackages();
        } catch (IOException e) {
            Log.e(TAG, "Failed to read JSON file", e);
            Toast.makeText(getContext(), R.string.personalise_pif_read_failed,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void killPackages() {
        try {
            ActivityManager am = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
            String[] packages = { "com.google.android.gms", "com.android.vending" };
            for (String pkg : packages) {
                am.getClass()
                  .getMethod("forceStopPackage", String.class)
                  .invoke(am, pkg);
                Log.i(TAG, pkg + " process killed");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to kill packages", e);
        }
    }
}
