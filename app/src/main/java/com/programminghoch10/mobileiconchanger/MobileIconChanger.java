package com.programminghoch10.mobileiconchanger;

import android.annotation.SuppressLint;
import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.crossbowffs.remotepreferences.RemotePreferences;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MobileIconChanger implements IXposedHookInitPackageResources, IXposedHookLoadPackage, IXposedHookZygoteInit {
	private static final String TAG = MobileIconChanger.class.getName();
	private static final String systemUI = "com.android.systemui";
	private static final Map<String, Drawable> systemIcons = new HashMap<>();
	private static String modulePath = null;
	
	public static Bitmap drawableToBitmap(Drawable drawable) {
		if (drawable == null) return null;
		if (drawable instanceof BitmapDrawable) {
			BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
			if (bitmapDrawable.getBitmap() != null) {
				return bitmapDrawable.getBitmap();
			}
		}
		Bitmap bitmap = null;
		if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
			bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
		} else {
			bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
		}
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		return bitmap;
	}
	
	private static String normalizeZipFileName(String name) {
		return name.replace("res/drawable/", "").replace(".xml", "");
	}
	
	private static String getResourceNameForIcon(String icon) {
		if (!icon.startsWith("system_mobiledata_")) return null;
		return "ic_" + icon.replace("system_mobiledata_", "") + "_mobiledata";
	}
	
	@Override
	public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
		if (!resparam.packageName.equals(systemUI)) return;
		Log.d(TAG, "handleInitPackageResources: ");
		
		XResources resources = resparam.res;
		XModuleResources moduleResources = XModuleResources.createInstance(modulePath, resparam.res);
		
		String path = (String) XposedHelpers.findField(resources.getClass(), "mResDir").get(resources);
		ZipFile zipFile = new ZipFile(path);
		ZipEntry[] iconEntries = zipFile.stream()
				.filter(e -> e.getName().startsWith("res/drawable/ic_") && e.getName().endsWith("mobiledata.xml"))
				.toArray(ZipEntry[]::new);
		Log.d(TAG, "handleInitPackageResources: systemicons=" + Arrays.toString(iconEntries));
		systemIcons.clear();
		for (ZipEntry entry : iconEntries) {
			String key = entry.getName();
			int drawableID = resources.getIdentifier(normalizeZipFileName(key), "drawable", systemUI);
			Drawable drawable = resources.getDrawable(drawableID);
			systemIcons.put(key, drawable);
		}
		
		XSharedPreferences sharedPreferences = new XSharedPreferences(BuildConfig.APPLICATION_ID, SettingsActivity.sharedPreferencesName);
		Log.d(TAG, "handleInitPackageResources: file url = " + sharedPreferences.getFile().getPath());
		Log.d(TAG, "handleInitPackageResources: canread  = " + sharedPreferences.getFile().canRead());
		if (!sharedPreferences.getFile().canRead()) return;
		
		boolean replaceColor = sharedPreferences.getBoolean("replaceColor", false);
		int color = sharedPreferences.getInt("color", 0);
		Log.d(TAG, "handleInitPackageResources: entryset=" + sharedPreferences.getAll().size());
		for (Map.Entry<String, ?> entry : sharedPreferences.getAll().entrySet()) {
			Log.d(TAG, "handleInitPackageResources: entry " + entry.getKey() + " has value type " + entry.getValue().getClass());
			if (!entry.getValue().getClass().equals(String.class)) continue;
			Map.Entry<String, String> iconEntry = (Map.Entry<String, String>) entry;
			String key = getResourceNameForIcon(iconEntry.getKey());
			Log.d(TAG, "handleInitPackageResources: key=" + key);
			if (key == null) continue;
			String icon = (String) entry.getValue();
			String iconIdentifier = getResourceNameForIcon((String) entry.getValue());
			if (icon == null) continue;
			Log.d(TAG, "handleInitPackageResources: replacing " + key + " with " + icon);
			resources.setReplacement(systemUI, "drawable", key, new XResources.DrawableLoader() {
				@Override
				public Drawable newDrawable(XResources res, int id) throws Throwable {
					if (icon.equals("hide")) return new ColorDrawable(Color.TRANSPARENT);
					Drawable drawable = null;
					if (icon.startsWith("system_")) {
						int resId = resources.getIdentifier(iconIdentifier, "drawable", systemUI);
						if (resId == Resources.ID_NULL) return null;
						Log.d(TAG, "handleInitPackageResources: replacing " + key + " with system " + iconIdentifier);
						drawable = resources.getDrawable(resId);
					} else {
						int resId = moduleResources.getIdentifier(icon, "drawable", BuildConfig.APPLICATION_ID);
						if (resId == Resources.ID_NULL) return null;
						Log.d(TAG, "handleInitPackageResources: replacing " + key + " with module " + icon);
						drawable = moduleResources.getDrawable(resId);
					}
					
					if (replaceColor) {
						drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
					}
					
					return drawable;
				}
			});
		}
	}
	
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (lpparam.packageName.equals(BuildConfig.APPLICATION_ID)) {
			XposedHelpers.setStaticBooleanField(XposedHelpers.findClass(SettingsActivity.class.getName(), lpparam.classLoader), "xposedActive", true);
		}
		if (!lpparam.packageName.equals(systemUI)) return;
		
		Log.d(TAG, "handleLoadPackage: hooking systemui");
		
		XposedHelpers.findAndHookMethod(systemUI + ".SystemUIService", lpparam.classLoader, "onCreate", new XC_MethodHook() {
			@SuppressLint("ApplySharedPref")
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				super.beforeHookedMethod(param);
				Log.d(TAG, "beforeHookedMethod: running systemui hook");
				
				//get sharedPreferences of module
				Context context = AndroidAppHelper.currentApplication().createPackageContext(BuildConfig.APPLICATION_ID, Context.CONTEXT_IGNORE_SECURITY);
				Log.d(TAG, "beforeHookedMethod: context = " + context);
				if (context == null) return;
				SharedPreferences sharedPreferences = new RemotePreferences(context, BuildConfig.APPLICATION_ID + ".PreferencesProvider", "systemIcons", true);
				Log.d(TAG, "beforeHookedMethod: sharedPreferences=" + sharedPreferences);
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
					String key = normalizeZipFileName(entry.getName());
					Drawable drawable = systemIcons.get(entry.getName());
					Bitmap bitmap = drawableToBitmap(drawable);
					String icon = IconProvider.BitMapToString(bitmap);
					editor.putString(key, icon);
				}
				editor.commit();
			}
		});
	}
	
	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		modulePath = startupParam.modulePath;
		Log.d(TAG, "initZygote: modulePath=" + modulePath);
	}
}
