package com.programminghoch10.fake5Gicon;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.preference.PreferenceViewHolder;
import androidx.preference.TwoStatePreference;

public class IconPreference extends TwoStatePreference {
	ImageView imageView = null;
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
	
	@Override
	public void onBindViewHolder(PreferenceViewHolder holder) {
		imageView = (ImageView) holder.findViewById(R.id.imageView);
		imageView.setImageDrawable(drawable);
		super.onBindViewHolder(holder);
	}
}
