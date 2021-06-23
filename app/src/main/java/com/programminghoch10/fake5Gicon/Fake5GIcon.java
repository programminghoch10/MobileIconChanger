package com.programminghoch10.fake5Gicon;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.XResources;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.crossbowffs.remotepreferences.RemotePreferences;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Fake5GIcon implements IXposedHookInitPackageResources, IXposedHookLoadPackage {
	private static final String TAG = Fake5GIcon.class.getName();
	private static final String systemUI = "com.android.systemui";
	
	@Override
	public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
		if (!resparam.packageName.equals(systemUI)) return;
		XposedBridge.log("Replacing 4G icon with 5G icon.");
		
		Drawable icon_5g = resparam.res.getDrawable(resparam.res.getIdentifier("ic_5g_mobiledata", "drawable", "com.android.systemui"));
		Drawable icon_5gplus = resparam.res.getDrawable(resparam.res.getIdentifier("ic_5g_plus_mobiledata", "drawable", "com.android.systemui"));
		XResources.DrawableLoader loader_5g = new XResources.DrawableLoader() {
			@Override
			public Drawable newDrawable(XResources res, int id) throws Throwable {
				return icon_5g;
			}
		};
		XResources.DrawableLoader loader_5gplus = new XResources.DrawableLoader() {
			@Override
			public Drawable newDrawable(XResources res, int id) throws Throwable {
				return icon_5gplus;
			}
		};
		resparam.res.setReplacement("com.android.systemui", "drawable", "ic_4g_mobiledata", loader_5g);
		resparam.res.setReplacement("com.android.systemui", "drawable", "ic_4g_plus_mobiledata", loader_5gplus);
	}
	
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (lpparam.packageName.equals(BuildConfig.APPLICATION_ID)) {
			XposedHelpers.setStaticBooleanField(XposedHelpers.findClass(SettingsActivity.class.getName(), lpparam.classLoader), "xposedActive", true);
		}
		if (!lpparam.packageName.equals(systemUI)) return;
		
		Log.d(TAG, "handleLoadPackage: hooking systemui");
		
		XposedHelpers.findAndHookMethod(systemUI + ".SystemUIService", lpparam.classLoader, "onCreate", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				super.beforeHookedMethod(param);
				Log.d(TAG, "beforeHookedMethod: running systemui hook");
				
				//get sharedPreferences of module
				Context context = AndroidAppHelper.currentApplication().createPackageContext(BuildConfig.APPLICATION_ID, Context.CONTEXT_IGNORE_SECURITY);
				if (context == null) return;
				SharedPreferences sharedPreferences = new RemotePreferences(context, BuildConfig.APPLICATION_ID + ".PreferencesProvider", "systemIcons", true);
				if (sharedPreferences == null) return;
				
				//gather available system icons for replacement
				String path = lpparam.appInfo.sourceDir;
				ZipFile zipFile = new ZipFile(path);
				ZipEntry[] iconEntries = zipFile.stream()
						.filter(e -> e.getName().startsWith("res/drawable/ic_") && e.getName().endsWith("mobiledata.xml"))
						.toArray(ZipEntry[]::new);
				Log.d(TAG, "handleInitPackageResources: systemicons=" + Arrays.toString(iconEntries));
				
				//save icons in sharedPreferences
				SharedPreferences.Editor editor = sharedPreferences.edit();
				for (ZipEntry entry : iconEntries) {
					String key = entry.getName().replace("res/drawable/", "").replace(".xml", "");
					String icon = new BufferedReader(
							new InputStreamReader(
									zipFile.getInputStream(entry)
							)).lines().collect(
							Collectors.joining(
									"\n"
							)
					);
					editor.putString(key, icon);
				}
				editor.commit();
			}
		});
	}
}
