package com.programminghoch10.mobileiconchanger;

import com.crossbowffs.remotepreferences.RemotePreferenceProvider;

public class PreferencesProvider extends RemotePreferenceProvider {
	private static final String[] files = {"systemIcons"};
	
	public PreferencesProvider() {
		super(BuildConfig.APPLICATION_ID + ".PreferencesProvider", files);
	}
	
	@Override
	protected boolean checkAccess(String prefFileName, String prefKey, boolean write) {
		//only allow access to file
		if (!prefFileName.equals(files[0])) return false;
		//only give systemui access
		if ("com.android.systemui".equals(getCallingPackage())) return true;
		//prevent access from any other package
		return BuildConfig.APPLICATION_ID.equals(getCallingPackage());
	}
}
