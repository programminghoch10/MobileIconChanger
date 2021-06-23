package com.programminghoch10.fake5Gicon;

import android.content.res.XResources;
import android.graphics.drawable.Drawable;

import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
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
		
		//gather available system icons for replacement
		XResources resources = resparam.res;
		String path = (String) XposedHelpers.findField(resources.getClass(), "mResDir").get(resources);
		ZipFile zipFile = new ZipFile(path);
		String[] iconList = zipFile.stream()
				.filter(e -> e.getName().startsWith("res/drawable/ic_") && e.getName().endsWith("mobiledata.xml"))
				.map(e -> e.getName().replace("res/drawable/", "").replace(".xml", ""))
				.toArray(String[]::new);
		Map<String, Drawable> map = new HashMap<>();
		for (String icon : iconList) {
			int drawableID = resources.getIdentifier(icon, "drawable", systemUI);
			Drawable drawable = resources.getDrawable(drawableID);
			map.put(icon, drawable);
		}
		XposedHelpers.setStaticObjectField(IconProvider.class, "systemIcons", map);
		
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
	}
}
