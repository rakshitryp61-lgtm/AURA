# Aura – Android widget v2 (glow inside the widget, rotating border, live lyrics)

## Idea
The widget is drawn as one bitmap. A transparent strip (18dp) runs around the card; the blurred
glow and the rotating gradient ring are painted INSIDE that strip, so nothing is drawn outside
the widget's bounds. Tap the cover to flip between cover view and 3-line lyrics view.

## Setup
1. Android Studio -> New Project -> Empty Activity. Package: com.example.aura
2. Copy AuraStore.kt, Glow.kt, Lyrics.kt, AuraWidget.kt, AuraListener.kt into app/src/main/java/com/example/aura/
3. Copy aura_widget_info.xml into app/src/main/res/xml/
4. app/build.gradle(.kts) dependencies:
   implementation("androidx.glance:glance-appwidget:1.1.0")
   implementation("androidx.palette:palette-ktx:1.0.0")
5. AndroidManifest.xml: add above <application>:
   <uses-permission android:name="android.permission.INTERNET"/>
   and inside <application>:
   <receiver android:name=".AuraReceiver" android:exported="true">
     <intent-filter><action android:name="android.appwidget.action.APPWIDGET_UPDATE"/></intent-filter>
     <meta-data android:name="android.appwidget.provider" android:resource="@xml/aura_widget_info"/>
   </receiver>
   <service android:name=".AuraListener" android:exported="true" android:label="Aura"
     android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
     <intent-filter><action android:name="android.service.notification.NotificationListenerService"/></intent-filter>
   </service>
6. Run. Settings -> Notification access -> enable Aura. Battery -> Unrestricted.
7. Long-press home screen -> Widgets -> Aura.

## Tuning (AuraListener.kt)
TICK_MS = 1000 and DEGREES_PER_TICK = 12 -> border steps once per second while the screen is on
and music plays. Try 700 / 8 for smoother motion (more battery).
