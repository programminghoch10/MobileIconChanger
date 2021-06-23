package com.programminghoch10.fake5Gicon;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
	
	private static boolean xposedActive = false;
	public static final String sharedPreferencesName = "fake5GIcon";
	private static final String TAG = "Fake5GIcon";
	private static Map<String, IconProvider.Icon> icons;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			//TODO: fix back button in action bar not working
			//actionBar.setDisplayHomeAsUpEnabled(true);
		}
		if (!xposedActive) {
			setContentView(R.layout.settings_activity_noxposed);
			return;
		}
		IconProvider.collectIcons(this);
		icons = IconProvider.getIcons();
		setContentView(R.layout.settings_activity);
		if (savedInstanceState == null) {
			getSupportFragmentManager()
					.beginTransaction()
					.replace(R.id.settings, new SettingsFragment())
					.commit();
		}
	}
	
	@Override
	public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
		Log.d(TAG, "onPreferenceStartFragment: pref="+pref+" caller="+caller.getClass().getName());
		Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(getClassLoader(), pref.getFragment());
		Bundle args = pref.getExtras();
		args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.getKey());
		fragment.setArguments(args);
		getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.settings, fragment)
				.addToBackStack(null)
				.commit();
		return true;
	}
	
	public static class SettingsFragment extends PreferenceFragmentCompat implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
		@SuppressLint("WorldReadableFiles")
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
			getPreferenceManager().setSharedPreferencesName(sharedPreferencesName);
			setPreferencesFromResource(R.xml.root_preferences, rootKey);
		}
		
		@Override
		public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
			Log.d(TAG, "onPreferenceStartFragment: pref="+pref.getKey()+" caller="+caller.getClass().getName());
			return false;
		}
	}
	
	public static class IconFragment extends PreferenceFragmentCompat {
		private final List<RadioButtonPreference> radioButtons = new LinkedList<>();
		
		@SuppressLint("WorldReadableFiles")
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			Log.d(TAG, "onCreatePreferences: rootKey="+rootKey);
			getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
			getPreferenceManager().setSharedPreferencesName(sharedPreferencesName + "-" + rootKey);
			PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(getContext());
			for (Map.Entry<String, IconProvider.Icon> entry : icons.entrySet()) {
				RadioButtonPreference radioButton = new RadioButtonPreference(getContext());
				radioButton.setKey(entry.getKey());
				radioButton.setTitle(entry.getValue().name);
				radioButton.setIcon(entry.getValue().drawable);
				radioButton.setOnPreferenceChangeListener((preference, newValue) -> {
					for (RadioButtonPreference radioButton1 : radioButtons) {
						radioButton1.setChecked(false);
					}
					return true;
				});
				radioButtons.add(radioButton);
				preferenceScreen.addPreference(radioButton);
			}
			setPreferenceScreen(preferenceScreen);
		}
	}
}