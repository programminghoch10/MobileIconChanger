package com.programminghoch10.fake5Gicon;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

public class IconPreference extends Preference {
	ImageView imageView = null;
	String value = null;
	Drawable drawable = null;
	
	public IconPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setWidgetLayoutResource(R.layout.preference_widget_icon);
	}
	
	public IconPreference(Context context) {
		this(context, null);
	}
	
	public void setPreview(Drawable drawable) {
		this.drawable = drawable;
		if (imageView == null) return;
		imageView.setImageDrawable(drawable);
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	@Override
	protected void onClick() {
		super.onClick();
		persistString(value);
		notifyChanged();
	}
	
	@Override
	public void onBindViewHolder(PreferenceViewHolder holder) {
		imageView = (ImageView) holder.findViewById(R.id.imageView);
		imageView.setImageDrawable(drawable);
		super.onBindViewHolder(holder);
	}
}
