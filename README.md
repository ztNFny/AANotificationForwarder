![Logo](https://github.com/ztNFny/AANotificationForwarder/raw/master/logo.png)
# AA Notification Forwarder

Companion app for *Android Auto* to allow any apps notifications to be shown on the cars screen.

AA shows only notifications from messaging apps - but what if you want to get notified about mails and similar stuff? This is where this app comes to the rescue!

Notifications by configured apps are re-sent by *AA Notification Forwarder* and displayed on your cars screen.

### Requirements
- Android 11 or higher
- Android Auto (duh!)

### Download
See [Releases](https://github.com/ztNFny/AANotificationForwarder/releases/).
This will NOT be published on Google Play Store as sending non-messenger notifications to AA is not allowed.

### Installation
On Android 13 sideloaded apps are not able to get notification access by default. Your options:
- Use apps like [King Installer](https://gitlab.com/annexhack/king-installer) that pretend the app was installed from Play Store
- Sideload and manually enable restricted settings (workflow might be different on non-stock Androids):
    1. Sideload the app
    2. Try to enable notification access and get a "Restricted Setting" dialog
    3. Go to Settings - Apps - See all apps, find *AA Notification Forwarder* and select it
    4. In the overflow menu (3 dots in upper right corner) select *Allow restricted settings*
    5. Enabling notification access should now work.

### Limitations
While a "Reply" button will be shown for every notification, the reply functionality doesn't (and will never) work. The button is inserted automatically by Android Auto and is required to make the notifications show up - making it work would however require code specific to each app that has its notifications forwarded.

### Contributions
Contributions via PR / Issue are welcome

### License
*AutoConnectionDetector* is taken from https://stackoverflow.com/a/75292070

All  the other code  was created by me and is under [Creative Commons Attribution-NonCommercial 4.0 International License](http://creativecommons.org/licenses/by-nc/4.0/)

Icons based on [Google Material Design icons](https://github.com/google/material-design-icons), licenses under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
