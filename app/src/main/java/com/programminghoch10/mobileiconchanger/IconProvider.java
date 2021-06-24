package com.programminghoch10.mobileiconchanger;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class IconProvider {
	// a list of icons as replacements
	private static final Map<String, Icon> icons = new TreeMap<>();
	private static final String TAG = "IconProvider";
	// a list of replaceable system icons
	private final static Map<String, Icon> systemIcons = new TreeMap<>();
	
	private static void collectSystemIcons(Context context) {
		SharedPreferences sharedPreferences = context.getSharedPreferences("systemIcons", Context.MODE_PRIVATE);
		systemIcons.clear();
		for (Map.Entry<String, ?> entry : sharedPreferences.getAll().entrySet()) {
			if (!entry.getValue().getClass().equals(String.class)) continue;
			Icon icon = new Icon();
			icon.key = entry.getKey();
			applyIconSystemMetaData(icon);
			String drawableString = (String) entry.getValue();
			Bitmap bitmap = StringToBitMap(drawableString);
			icon.drawable = new BitmapDrawable(context.getResources(), bitmap);
			Log.d(TAG, "collectSystemIcons: adding system icon key=" + icon.key + " name=" + icon.name);
			systemIcons.put(icon.key, icon);
		}
	}
	
	private static void applyIconSystemMetaData(Icon icon) {
		icon.key = "system_mobiledata_" + icon.key.replace("ic_", "").replace("_mobiledata", "");
		icon.name = icon.key.replace("system_mobiledata_", "").replace("_", " ").toUpperCase();
		icon.category = "SYSTEM";
	}
	
	static void tintAllIcons(int color) {
		for (Map.Entry<String, Icon> entry : icons.entrySet()) {
			Drawable drawable = entry.getValue().drawable;
			drawable.setTint(color);
			entry.getValue().drawable = drawable;
		}
		for (Map.Entry<String, Icon> entry : systemIcons.entrySet()) {
			Drawable drawable = entry.getValue().drawable;
			drawable.setTint(color);
			entry.getValue().drawable = drawable;
		}
	}
	
	static Map<String, Icon> getIcons() {
		return new TreeMap<>(icons);
	}
	
	static Map<String, Icon> getSystemIcons() {
		return new TreeMap<>(systemIcons);
	}
	
	static void collectIcons(Context context) {
		collectSystemIcons(context);
		List<Field> iconFields = Arrays.asList(R.drawable.class.getFields());
		iconFields = iconFields.stream().filter(s -> s.getName().contains("_mobiledata_")).collect(Collectors.toList());
		for (Field iconField : iconFields) {
			//Log.d(TAG, "collectIcons: Field=" + iconField + " name=" + iconField.getName() + " type=" + iconField.getType());
			int fieldValue = Resources.ID_NULL;
			try {
				fieldValue = iconField.getInt(context.getResources());
			} catch (Exception ignored) {
			}
			if (fieldValue != Resources.ID_NULL) {
				Drawable drawable = ResourcesCompat.getDrawable(context.getResources(), fieldValue, context.getTheme());
				if (drawable != null) {
					Icon icon = new Icon();
					icon.key = iconField.getName();
					applyIconMetaData(icon);
					icon.drawable = drawable;
					Log.d(TAG, "collectIcons: adding icon key=" + icon.key + " name=" + icon.name);
					icons.put(icon.key, icon);
				}
			}
		}
		for (Map.Entry<String, Icon> entry : systemIcons.entrySet()) {
			icons.put(entry.getKey(), entry.getValue());
			Log.d(TAG, "collectIcons: adding system icon key=" + entry.getValue().key + " name=" + entry.getValue().name);
		}
	}
	
	private static void applyIconMetaData(Icon icon) {
		String[] separated = icon.key.split("_mobiledata_");
		icon.name = separated[1].replace("_", " ").toUpperCase();
		icon.category = separated[0].replace("_", " ").toUpperCase();
	}
	
	public static String BitMapToString(Bitmap bitmap) {
		if (bitmap == null) return null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
		byte[] b = baos.toByteArray();
		return Base64.encodeToString(b, Base64.DEFAULT);
	}
	
	public static Bitmap StringToBitMap(String encodedString) {
		if (encodedString == null) return null;
		try {
			byte[] encodeByte = Base64.decode(encodedString, Base64.DEFAULT);
			return BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
		} catch (Exception e) {
			return null;
		}
	}
	
	static class Icon {
		String key;
		String name;
		String category;
		Drawable drawable;
		
		@NonNull
		@Override
		public String toString() {
			return "Icon@" + hashCode() + "[key=" + key + ",name=" + name + ",category=" + category + ",drawable=" + drawable + "]";
		}
	}
	
}
