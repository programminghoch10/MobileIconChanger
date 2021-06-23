package com.programminghoch10.fake5Gicon;

import android.annotation.SuppressLint;
import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.XResources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
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
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Fake5GIcon implements IXposedHookInitPackageResources, IXposedHookLoadPackage {
	private static final String TAG = Fake5GIcon.class.getName();
	private static final String systemUI = "com.android.systemui";
	
	private static final Map<String, Drawable> systemIcons = new HashMap<>();
	
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
	
	@Override
	public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
		if (!resparam.packageName.equals(systemUI)) return;
		XposedBridge.log("Replacing 4G icon with 5G icon.");
		
		XResources resources = resparam.res;
		String path = (String) XposedHelpers.findField(resources.getClass(), "mResDir").get(resources);
		ZipFile zipFile = new ZipFile(path);
		ZipEntry[] iconEntries = zipFile.stream()
				.filter(e -> e.getName().startsWith("res/drawable/ic_") && e.getName().endsWith("mobiledata.xml"))
				.toArray(ZipEntry[]::new);
		Log.d(TAG, "handleInitPackageResources: systemicons=" + Arrays.toString(iconEntries));
		systemIcons.clear();
		for (ZipEntry entry : iconEntries) {
			String key = entry.getName();
			int drawableID = resources.getIdentifier(key.replace("res/drawable/", "").replace(".xml", ""), "drawable", systemUI);
			Drawable drawable = resources.getDrawable(drawableID);
			systemIcons.put(key, drawable);
		}
		
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
			@SuppressLint("ApplySharedPref")
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
					Drawable drawable = systemIcons.get(entry.getName());
					Bitmap bitmap = drawableToBitmap(drawable);
					String icon = IconProvider.BitMapToString(bitmap);
					editor.putString(key, icon);
				}
				editor.commit();
			}
		});
	}
}
