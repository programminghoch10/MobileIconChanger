package com.programminghoch10.fake5Gicon;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
	
	public static final String sharedPreferencesName = "fake5GIcon";
	private static final String TAG = "Fake5GIcon";
	private static boolean xposedActive = false;
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
		IconProvider.collectSystemIcons(this);
		IconProvider.tintAllIcons(getColor(R.color.iconTint));
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
		Log.d(TAG, "onPreferenceStartFragment: pref=" + pref + " caller=" + caller.getClass().getName());
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
			
			//add icons
			PreferenceCategory iconCategory = getPreferenceManager().findPreference("icons");
			for (Map.Entry<String, IconProvider.Icon> icon : IconProvider.systemIcons.entrySet()) {
				Log.d(TAG, "onCreatePreferences: adding icon " + icon.getKey());
				IconPreference iconPreference = new IconPreference(getContext());
				iconPreference.setTitle(icon.getValue().name);
				iconPreference.setKey(icon.getKey());
				iconPreference.setIcon(icon.getValue().drawable);
				iconPreference.setPersistent(true);
				iconPreference.setPreview(getPreview(icon.getKey()));
				iconPreference.setFragment(IconFragment.class.getName());
				iconCategory.addPreference(iconPreference);
			}
			
			//add restart systemui
			Preference restartSystemUIPreference = new Preference(getContext());
			restartSystemUIPreference.setTitle(getString(R.string.title_systemui));
			restartSystemUIPreference.setSummary(getString(R.string.summary_systemui));
			restartSystemUIPreference.setOnPreferenceClickListener(preference -> {
				Log.i(TAG, "onClick: Trying to restart SystemUI");
				try {
					Runtime.getRuntime().exec("su -c killall com.android.systemui");
					return true;
				} catch (IOException ignored) {
					return false;
				}
			});
			((PreferenceCategory) getPreferenceManager().findPreference("systemui")).addPreference(restartSystemUIPreference);
			
			//updatePreferences();
		}
		
		@Override
		public void onResume() {
			super.onResume();
			Log.d(TAG, "onResume: updating previews");
			updatePreferences();
		}
		
		@Override
		public void onStop() {
			updatePreferences();
			super.onStop();
		}
		
		private void updatePreferences() {
			for (Map.Entry<String, IconProvider.Icon> entry : IconProvider.systemIcons.entrySet()) {
				IconPreference preference = findPreference(entry.getKey());
				if (preference == null) continue;
				preference.setPreview(getPreview(entry.getKey()));
				String selectedKey = getSelected(entry.getKey());
				if (selectedKey == null) continue;
				preference.setValue(selectedKey);
				IconProvider.Icon icon = icons.get(selectedKey);
				IconProvider.Icon systemIcon = IconProvider.systemIcons.get(entry.getKey());
				if (icon == null || systemIcon == null) continue;
				preference.setSummary(String.format(getString(R.string.change_notice), systemIcon.name, icon.name));
			}
		}
		
		private Drawable getPreview(String key) {
			String icon = getSelected(key);
			if (icon == null) return null;
			Log.d(TAG, "getPreview: key=" + key + " icon=" + icon);
			int id = getContext().getResources().getIdentifier(icon, "drawable", BuildConfig.APPLICATION_ID);
			if (id == Resources.ID_NULL) return null;
			return ResourcesCompat.getDrawable(getContext().getResources(), id, getContext().getTheme());
		}
		
		private String getSelected(String key) {
			if (key == null) return null;
			SharedPreferences sharedPreferences = getContext().getSharedPreferences(sharedPreferencesName + "-" + key, MODE_PRIVATE);
			String icon = null;
			for (Map.Entry<String, ?> entry : sharedPreferences.getAll().entrySet()) {
				if (!entry.getValue().getClass().equals(Boolean.class)) continue;
				Map.Entry<String, Boolean> boolEntry = (Map.Entry<String, Boolean>) entry;
				if (boolEntry.getValue().booleanValue()) icon = boolEntry.getKey();
			}
			return icon;
		}
		
		@Override
		public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
			Log.d(TAG, "onPreferenceStartFragment: pref=" + pref.getKey() + " caller=" + caller.getClass().getName());
			return false;
		}
	}
	
	public static class IconFragment extends PreferenceFragmentCompat {
		private final List<RadioButtonPreference> radioButtons = new LinkedList<>();
		
		@SuppressLint("WorldReadableFiles")
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			Log.d(TAG, "onCreatePreferences: rootKey=" + rootKey);
			getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
			getPreferenceManager().setSharedPreferencesName(sharedPreferencesName + "-" + rootKey);
			PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(getContext());
			Preference noneSelector = new Preference(getContext());
			noneSelector.setTitle(getString(R.string.title_none));
			noneSelector.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					for (RadioButtonPreference radioButton : radioButtons) {
						radioButton.setChecked(false);
					}
					return false;
				}
			});
			preferenceScreen.addPreference(noneSelector);
			for (Map.Entry<String, IconProvider.Icon> entry : icons.entrySet()) {
				RadioButtonPreference radioButton = new RadioButtonPreference(getContext());
				radioButton.setKey(entry.getKey());
				radioButton.setTitle(entry.getValue().name);
				Log.d(TAG, "onCreatePreferences: add radio button key=" + entry.getKey() + " title=" + entry.getValue().name);
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