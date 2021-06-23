# [MobileIconChanger](https://github.com/programminghoch10/MobileIconChanger)
Change your mobile data icons! For example you can change the 3G/4G icons into 5G!

![logo](logo.png)

This module enables you to replace any of the mobile data icons with any other mobile data icon.

This module contains local copies of all mobile data icons available in `LineageOS 18.1` / `Android 11`. 
Although before `Android 10` a `5G` icon was not included in the OS, using one is possible with this module.
This has been developed and tested on `LineageOS 17.1`, 
but should work on any Android `>= 8.1`.

## Inspiration
Well there sadly is a thing called "Wish.com".
On there a lot of phones called "`Samsung Galaxy S21 Ultra 5G`" get sold for 120 freedom eagles.
What one gets is totally useless (obviously),
but it's funny how the Android on these phones shows `5G`, 
although the hardware is only capable of `3G`.
Well it kinda became a meme so here we go.

There is also the Magisk module [Fake_5G_icon](https://github.com/E7KMbb/Fake_5G_icon), but I **do not** recommended using it, because live swapping stuff is a job for XPosed and not for Magisk.
Using this Magisk module has a much higher probability of breaking stuff in wrong ways and it may also prevent other mods from working properly.

## Installation

1. Install the module on a system with a running XPosed framework with API version `>=93`.
1. Activate the resource hooks within your XPosed framework manager.
1. Activate the module. The package `SystemUI` should automatically be selected.
1. Restart your phone.  
   _This is necessary as the module will scan for icons to replace when `SystemUI` initializes._  
   _If you need to do it without restart: Open the configuration screen, click on `Restart SystemUI`, then force-close the module from the app settings before opening the configuration screen again._ 
1. You are now ready to use the module. You will need to open the [Configuration](#configuration) screen to set it all up, as the module won't do anything yet.

## Configuration

_I don't like it when every XPosed module adds yet another app icon to the launcher. This is why this modules settings are hidden within the App-Info screen._

Here is how to open the settings:

- Route 1:
    - Go to the module list within your XPosed manager
    - Long press on `CameraControl` and select App Info
    - On the bottom, click on `Advanced`
    - Click on `Additional settings in the app`
- Route 2:
    - Go into device settings
    - Click on apps
    - Somehow tell the device to show all apps
    - Select `CameraControl`
    - On the bottom, click on `Advanced`
    - Click on `Additional settings in the app`
- Route 3:
    - Open up an ADB shell
    - Run command `am start-activity com.programminghoch10.mobileiconchanger/.SettingsActivity`

**Additional notes:**

The icons listed are all the icons your system has for showing a mobile data connection. 
Just because the system has them, does not mean it uses them. 
For example your system may have a `5G` icon, 
but the phone is not capable of operating on `5G` networks due to missing hardware. 
The system will then _(obviously)_ never use the `5G` icon and replacing it is useless.
You will need to look yourself, which of the icons are used.

Since this module hard-relinks the icon resources, a `SystemUI` restart is necessary to apply changes. 
Conveniently there is a button for that right at the bottom. 
You will need to give the app root access for this operation.

## Thank me

You want to thank me for my work?

Currently I don't take donations, 
but I hate them "`You will have to pay a small fee for all features`" modules anyways.

Just thank me by starring the [GitHub Repo](https://github.com/programminghoch10/MobileIconChanger) and telling your friends.

Have a great day!
