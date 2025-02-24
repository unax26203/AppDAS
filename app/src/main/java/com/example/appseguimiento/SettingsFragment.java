package com.example.appseguimiento;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import com.example.appseguimiento.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Carga las preferencias desde el recurso XML
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}
