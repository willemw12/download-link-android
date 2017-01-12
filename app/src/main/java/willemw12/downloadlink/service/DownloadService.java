package willemw12.downloadlink.service;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import willemw12.downloadlink.R;
import willemw12.downloadlink.activity.MainActivity;

public class DownloadService extends Service {

    public final int WIFI_LOCK_TIMEOUT_REPEAT_WHEN_ON_CHARGER = 3;
    public final long WIFI_LOCK_TIMEOUT_MS = 10 * 60000L;

    //private static final String TAG = DownloadService.class.getSimpleName();
    private static final String TAG = "DownloadLinkService";

    private static final String WIFI_LOCK_TAG = "DownloadLinkWifiLock";

    private final int WIFI_LOCK_NOTIFICATION_ID = 1;

    private static final List<Long> downloadIds = new ArrayList<Long>();

    private DownloadReceiver downloadReceiver;
    private ServiceHandler serviceHandler;
    private WifiManager.WifiLock wifiLock;

    // Receive download intents from the Download Manager
    private class DownloadReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.d(TAG, "onReceive: Intent: " + intent);

            String action = intent.getAction();
            if (action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                Log.d(TAG, "onReceive: Download complete");

                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);

                //DownloadManager.Query query = new DownloadManager.Query();
                //query.setFilterById(downloadId);
                //Cursor cursor = dm.query(query);
                //cursor.getCount()
                //int index = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                //if (cursor.moveToFirst()) {
                //    if (cursor.getInt(index) != DownloadManager.STATUS_SUCCESSFUL) {
                //        ...
                //    }
                //}
                //cursor.close();

                releaseWifiLock(downloadId);
            }
        }
    }

    // Perform WiFi lock timeouts
    private final class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: starting, msg id: " + msg.arg1);

            try {
                for (int i = 0; i < WIFI_LOCK_TIMEOUT_REPEAT_WHEN_ON_CHARGER; i++) {
                    Thread.sleep(WIFI_LOCK_TIMEOUT_MS);
                    if (!isOnCharger()) {
                        // Shorter timeout when not on a charger
                        break;
                    }
                }

                long downloadId = (long) msg.obj;
                releaseWifiLock(downloadId);

            } catch (InterruptedException exc) {
                // Restore interrupt status
                Thread.currentThread().interrupt();
            }

            // Stop when all started messages have been processed
            stopSelf(msg.arg1);
            Log.d(TAG, "handleMessage: stopping, msg id: " + msg.arg1);
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");

        HandlerThread thread = new HandlerThread("DownloadLinkThread", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        Looper serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);

        downloadReceiver = new DownloadReceiver();
        registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Log.d(TAG, "onStartCommand: intent: " + intent);

        String action = intent.getAction();
        if (MainActivity.INTENT_ACTION_DOWNLOAD.equals(action)) {
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
            acquireWifiLock(downloadId);

            // Start WiFi lock timer
            Message msg = serviceHandler.obtainMessage();
            msg.arg1 = startId;
            msg.obj = downloadId;
            serviceHandler.sendMessage(msg);

        } else if (MainActivity.INTENT_ACTION_NOTIFY.equals(action)) {
            stopSelf();
        }

        //} else {
        //    Log.d(TAG, "onStartCommand: Unknown intent: '" + action);
        //    throw new UnsupportedOperationException("Unknown intent: '" + action);

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        if (downloadReceiver != null) {
            unregisterReceiver(downloadReceiver);
            downloadReceiver = null;
        }

        releaseWifiLock();
    }

    private void startNotifyWifiLocked() {
        //SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        //boolean showNotifications = sharedPrefs.getBoolean(SettingsActivity.PREF_SHOW_NOTIFICATIONS_KEY, true);
        //if (! showNotifications) {
        //    return;
        //}

        //Notification.Builder builder = new Notification.Builder(this);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getText(R.string.notif_wifi_lock_title))
                // Non-swipable notification
                .setOngoing(true)
                //.setPriority(isOnCharger() ? Notification.PRIORITY_MIN : Notification.PRIORITY_LOW);
                .setPriority(Notification.PRIORITY_MIN);

        //        .setUsesChronometer(true);
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {    // Build.VERSION_CODES.NOUGAT
        //    builder.setChronometerCountDown(true)
        //           .setWhen(System.currentTimeMillis() + WIFI_LOCK_TIMEOUT_MS);
        //}

        //// Enable displaying the main activity when the notification is tapped
        //Intent mainIntent = new Intent(this, MainActivity.class);
        //TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        //stackBuilder.addParentStack(MainActivity.class);
        //stackBuilder.addNextIntent(mainIntent);
        //PendingIntent mainPendingIntent =
        //        stackBuilder.getPendingIntent(
        //                0,
        //                PendingIntent.FLAG_UPDATE_CURRENT
        //        );
        //builder.setContentIntent(mainPendingIntent);

        Intent notifyIntent = new Intent(MainActivity.INTENT_ACTION_NOTIFY, null, this, DownloadService.class);
        PendingIntent notifyPendingIntent = PendingIntent.getService(this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            // Tap button to cancel Wifi lock
            builder.addAction(R.drawable.ic_cancel_black_24dp, getText(R.string.label_cancel), notifyPendingIntent);
        } else {
            // Tap notification to cancel Wifi lock
            builder.setContentText(getText(R.string.notif_wifi_lock_text))
                   .setContentIntent(notifyPendingIntent);
        }

        Notification wifiLockNotification = builder.build();

        //NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //notificationManager.notify(WIFI_LOCK_NOTIFICATION_ID, wifiLockNotification);
        // Keep the service alive by running it in the foreground
        startForeground(WIFI_LOCK_NOTIFICATION_ID, wifiLockNotification);
    }

    private void cancelNotifyWifiLocked() {
        //SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        //boolean showNotifications = sharedPrefs.getBoolean(SettingsActivity.PREF_SHOW_NOTIFICATIONS_KEY, true);
        //if (! showNotifications) {
        //    return;
        //}

        //NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //notificationManager.cancel(WIFI_LOCK_NOTIFICATION_ID);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {    // Build.VERSION_CODES.NOUGAT
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
    }


    // Handle WiFi locks

    protected void acquireWifiLock(long downloadId) {
        if (wifiLock == null) {
            WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            //wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, WIFI_LOCK_TAG);
            wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, WIFI_LOCK_TAG);
            wifiLock.setReferenceCounted(false);
        }

        wifiLock.acquire();
        downloadIds.add(downloadId);
        startNotifyWifiLocked();
        Log.i(TAG, "WiFi lock acquired");
        Log.d(TAG, "acquireWifiLock: Download ID " + downloadId);
    }

    /**
     * Unregister all downloads from the Wifi lock and release the Wifi lock.
     */
    private void releaseWifiLock() {
        // Unregister all downloads
        downloadIds.clear();

        if (wifiLock != null) {
            if (wifiLock.isHeld()) {
                wifiLock.release();
            }
            wifiLock = null;

            // Stop all WiFi lock timers
            serviceHandler.getLooper().quit();

            cancelNotifyWifiLocked();
            Log.i(TAG, "WiFi lock released");
        }
    }

    /**
     * Unregister @downloadId from the Wifi lock.
     * Release Wifi lock when all registered downloads are done.
     */
    protected void releaseWifiLock(long downloadId) {
        int index = downloadIds.indexOf(downloadId);
        if (index >= 0) {
            // Unregister download
            downloadIds.remove(index);
            if (downloadIds.isEmpty()) {
                releaseWifiLock();
            }
            Log.d(TAG, "releaseWifiLock: Download ID " + downloadId);
            //} else {
            //    Log.d(TAG, "releaseWifiLock: Unknown download ID " + downloadId);
        }
    }


    // Util

    //public boolean isOnAcCharger() {
    public boolean isOnCharger() {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, intentFilter);
        if (batteryStatus == null) {
            return false;
        }

        int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
        boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

        //return acCharge;
        return usbCharge || acCharge;
    }
}

