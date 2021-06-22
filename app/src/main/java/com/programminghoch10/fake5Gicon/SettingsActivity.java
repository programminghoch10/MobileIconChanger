package com.programminghoch10.fake5Gicon;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity {
	
	private static boolean xposedActive = false;
	public static final String sharedPreferencesName = "fake5GIcon";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		if (!xposedActive) {
			setContentView(R.layout.settings_activity_noxposed);
			return;
		}
		setContentView(R.layout.settings_activity);
		if (savedInstanceState == null) {
			getSupportFragmentManager()
					.beginTransaction()
					.replace(R.id.settings, new SettingsFragment())
					.commit();
		}
	}
	
	public static class SettingsFragment extends PreferenceFragmentCompat {
		@SuppressLint("WorldReadableFiles")
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
			getPreferenceManager().setSharedPreferencesName(sharedPreferencesName);
			setPreferencesFromResource(R.xml.root_preferences, rootKey);
		}
	}
}