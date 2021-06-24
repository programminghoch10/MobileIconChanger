package com.programminghoch10.mobileiconchanger;

import android.annotation.SuppressLint;
import android.content.Context;
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
import java.util.TreeMap;

public class SettingsActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
	
	public static final String sharedPreferencesName = "fake5GIcon";
	private static final String TAG = "MobileIconChanger";
	private static boolean xposedActive = false;
	
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
		IconProvider.tintAllIcons(getColor(R.color.iconTint));
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
			for (Map.Entry<String, IconProvider.Icon> icon : IconProvider.getSystemIcons().entrySet()) {
				Log.d(TAG, "onCreatePreferences: adding icon " + icon.getKey());
				IconPreference iconPreference = new IconPreference(getContext());
				iconPreference.setTitle(icon.getValue().name);
				iconPreference.setKey(icon.getKey());
				iconPreference.setIcon(icon.getValue().drawable);
				iconPreference.setPersistent(true);
				iconPreference.setPreview(getDrawableFromString(icon.getKey()));
				iconPreference.setFragment(IconFragment.class.getName());
				iconCategory.addPreference(iconPreference);
			}
			if (IconProvider.getSystemIcons().size() == 0) {
				Preference noIcon = new Preference(getContext());
				noIcon.setTitle(R.string.title_noIcons);
				noIcon.setSummary(R.string.summary_noIcons);
				noIcon.setSelectable(false);
				iconCategory.addPreference(noIcon);
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
			for (Map.Entry<String, IconProvider.Icon> entry : IconProvider.getSystemIcons().entrySet()) {
				IconPreference preference = findPreference(entry.getKey());
				if (preference == null) continue;
				preference.setPreview(getDrawableFromString(entry.getKey()));
				String selectedKey = getSelected(entry.getKey());
				preference.setSummary(null);
				preference.setValue(selectedKey);
				if (selectedKey == null) continue;
				IconProvider.Icon systemIcon = IconProvider.getSystemIcons().get(entry.getKey());
				if (systemIcon == null) continue;
				if (selectedKey.equals("hide")) {
					preference.setSummary(String.format(getString(R.string.hidden_notice), systemIcon.name));
				}
				IconProvider.Icon icon = IconProvider.getIcons().get(selectedKey);
				if (icon == null) continue;
				preference.setSummary(String.format(getString(R.string.change_notice), systemIcon.name, icon.category, icon.name));
			}
		}
		
		private Drawable getDrawableFromString(String key) {
			String icon = getSelected(key);
			if (icon == null) return null;
			if (icon.startsWith("system_")) return IconProvider.getSystemIcons().get(icon).drawable;
			if (icon.equals("hide")) return null;
			int id = getContext().getResources().getIdentifier(icon, "drawable", BuildConfig.APPLICATION_ID);
			if (id == Resources.ID_NULL) return null;
			Log.d(TAG, "getPreview: generated preview for key=" + key + " icon=" + icon);
			return ResourcesCompat.getDrawable(getContext().getResources(), id, getContext().getTheme());
		}
		
		private String getSelected(String key) {
			if (key == null) return null;
			SharedPreferences sharedPreferences = getContext().getSharedPreferences(sharedPreferencesName + "-" + key, MODE_PRIVATE);
			String icon = null;
			for (Map.Entry<String, ?> entry : sharedPreferences.getAll().entrySet()) {
				if (!entry.getValue().getClass().equals(Boolean.class)) continue;
				Map.Entry<String, Boolean> boolEntry = (Map.Entry<String, Boolean>) entry;
				if (boolEntry.getValue()) icon = boolEntry.getKey();
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
		
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			Log.d(TAG, "onCreatePreferences: rootKey=" + rootKey);
			Context context = getContext();
			assert context != null;
			getPreferenceManager().setSharedPreferencesName(sharedPreferencesName + "-" + rootKey);
			PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(context);
			
			//setup none button
			RadioButtonPreference noneSelector = new RadioButtonPreference(context);
			noneSelector.setTitle(getString(R.string.title_none));
			noneSelector.setSummary(R.string.summary_none);
			noneSelector.setChecked(true);
			noneSelector.setOnPreferenceChangeListener((preference, newValue) -> {
				for (RadioButtonPreference radioButton1 : radioButtons) {
					radioButton1.setChecked(false);
				}
				return true;
			});
			noneSelector.setOnPreferenceClickListener(preference -> {
				getActivity().onBackPressed();
				return false;
			});
			preferenceScreen.addPreference(noneSelector);
			radioButtons.add(noneSelector);
			
			//setup hide button
			RadioButtonPreference hidePreference = new RadioButtonPreference(getContext());
			hidePreference.setTitle(R.string.title_hide);
			hidePreference.setSummary(R.string.summary_hide);
			hidePreference.setKey("hide");
			hidePreference.setOnPreferenceChangeListener((preference, newValue) -> {
				for (RadioButtonPreference radioButton1 : radioButtons) {
					radioButton1.setChecked(false);
				}
				return true;
			});
			hidePreference.setOnPreferenceClickListener(preference -> {
				getActivity().onBackPressed();
				return false;
			});
			preferenceScreen.addPreference(hidePreference);
			radioButtons.add(hidePreference);
			if (hidePreference.isChecked()) noneSelector.setChecked(false);
			
			//setup categories
			Map<String, PreferenceCategory> categoryMap = new TreeMap<>();
			for (Map.Entry<String, IconProvider.Icon> entry : IconProvider.getIcons().entrySet()) {
				String category = entry.getValue().category;
				if (!categoryMap.containsKey(category)) {
					PreferenceCategory preferenceCategory = new PreferenceCategory(context);
					preferenceCategory.setTitle(category);
					preferenceCategory.setInitialExpandedChildrenCount(0);
					categoryMap.put(category, preferenceCategory);
					preferenceScreen.addPreference(preferenceCategory);
					Log.d(TAG, "onCreatePreferences: created category " + category);
				}
			}
			if (categoryMap.size() == 0) throw new IllegalStateException("no categories setup");
			
			// iterate over icons creating radiobuttons and sorting into categories
			for (Map.Entry<String, IconProvider.Icon> entry : IconProvider.getIcons().entrySet()) {
				if (entry.getKey().equals(rootKey)) continue;
				RadioButtonPreference radioButton = new RadioButtonPreference(context);
				radioButton.setKey(entry.getKey());
				radioButton.setTitle(entry.getValue().name);
				Log.d(TAG, "onCreatePreferences: add radio button key=" + entry.getKey() + " icon=" + entry.getValue());
				radioButton.setIcon(entry.getValue().drawable);
				radioButton.setOnPreferenceChangeListener((preference, newValue) -> {
					for (RadioButtonPreference radioButton1 : radioButtons) {
						radioButton1.setChecked(false);
					}
					return true;
				});
				radioButton.setOnPreferenceClickListener(preference -> {
					getActivity().onBackPressed();
					return false;
				});
				radioButtons.add(radioButton);
				PreferenceCategory category = categoryMap.get(entry.getValue().category);
				category.addPreference(radioButton);
				if (radioButton.isChecked()) {
					category.setInitialExpandedChildrenCount(Integer.MAX_VALUE);
					noneSelector.setChecked(false);
				}
			}
			
			setPreferenceScreen(preferenceScreen);
		}
	}
}