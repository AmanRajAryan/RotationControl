package aman.rotationcontrol;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class RotationLockService extends Service {
    
    private WindowManager windowManager;
    private FrameLayout overlayView;
    private int currentRotation = 0;
    private static final String CHANNEL_ID = "RotationLockChannel";
    
    private static final String ACTION_PORTRAIT = "aman.rotationcontrol.PORTRAIT";
    private static final String ACTION_LANDSCAPE = "aman.rotationcontrol.LANDSCAPE";
    private static final String ACTION_REVERSE_PORTRAIT = "aman.rotationcontrol.REVERSE_PORTRAIT";
    private static final String ACTION_REVERSE_LANDSCAPE = "aman.rotationcontrol.REVERSE_LANDSCAPE";
    private static final String ACTION_STOP = "aman.rotationcontrol.STOP";
    
    private BroadcastReceiver actionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_PORTRAIT:
                        changeRotation(0);
                        break;
                    case ACTION_LANDSCAPE:
                        changeRotation(1);
                        break;
                    case ACTION_REVERSE_PORTRAIT:
                        changeRotation(8);
                        break;
                    case ACTION_REVERSE_LANDSCAPE:
                        changeRotation(9);
                        break;
                    case ACTION_STOP:
                        stopSelf();
                        break;
                }
            }
        }
    };
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Rotation Lock Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps rotation locked");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PORTRAIT);
        filter.addAction(ACTION_LANDSCAPE);
        filter.addAction(ACTION_REVERSE_PORTRAIT);
        filter.addAction(ACTION_REVERSE_LANDSCAPE);
        filter.addAction(ACTION_STOP);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(actionReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(actionReceiver, filter);
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            currentRotation = intent.getIntExtra("rotation", 0);
        }
        
        startForeground(1, createNotification());
        createOverlay();
        
        return START_STICKY;
    }
    
    private void changeRotation(int newRotation) {
        currentRotation = newRotation;
        
        if (overlayView != null && windowManager != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        createOverlay();
        
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, createNotification());
    }
    
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent;
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, flags);
        
        String rotationText = getRotationText();
        
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }
        
        builder.setContentTitle("Rotation Locked")
            .setContentText("Currently: " + rotationText)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true);
        
        int actionFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            actionFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        
        Intent portraitIntent = new Intent(ACTION_PORTRAIT);
        PendingIntent portraitPendingIntent = PendingIntent.getBroadcast(
            this, 100, portraitIntent, actionFlags
        );
        builder.addAction(new Notification.Action.Builder(
            android.R.drawable.ic_menu_crop,
            "Portrait",
            portraitPendingIntent
        ).build());
        
        Intent landscapeIntent = new Intent(ACTION_LANDSCAPE);
        PendingIntent landscapePendingIntent = PendingIntent.getBroadcast(
            this, 101, landscapeIntent, actionFlags
        );
        builder.addAction(new Notification.Action.Builder(
            android.R.drawable.ic_menu_rotate,
            "Landscape",
            landscapePendingIntent
        ).build());
        
        Intent reversePortraitIntent = new Intent(ACTION_REVERSE_PORTRAIT);
        PendingIntent reversePortraitPendingIntent = PendingIntent.getBroadcast(
            this, 102, reversePortraitIntent, actionFlags
        );
        builder.addAction(new Notification.Action.Builder(
            android.R.drawable.ic_menu_revert,
            "Rev Port",
            reversePortraitPendingIntent
        ).build());
        
        return builder.build();
    }
    
    private Notification createExpandedNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent;
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, flags);
        
        String rotationText = getRotationText();
        
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }
        
        builder.setContentTitle("Rotation Locked")
            .setContentText("Currently: " + rotationText + " - Tap buttons to change")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true);
        
        int actionFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            actionFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        
        builder.addAction(createAction(ACTION_PORTRAIT, "Portrait", android.R.drawable.ic_menu_crop, 100, actionFlags));
        
        builder.addAction(createAction(ACTION_LANDSCAPE, "Landscape", android.R.drawable.ic_menu_rotate, 101, actionFlags));
        
        builder.addAction(createAction(ACTION_REVERSE_PORTRAIT, "Rev Port", android.R.drawable.ic_menu_revert, 102, actionFlags));
        
        return builder.build();
    }
    
    private Notification.Action createAction(String action, String title, int icon, int requestCode, int flags) {
        Intent intent = new Intent(action);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, flags);
        return new Notification.Action.Builder(icon, title, pendingIntent).build();
    }
    
    private String getRotationText() {
        switch (currentRotation) {
            case 0: return "Portrait (0°)";
            case 1: return "Landscape (90°)";
            case 8: return "Reverse Portrait (180°)";
            case 9: return "Reverse Landscape (270°)";
            default: return "Unknown";
        }
    }
    
    private void createOverlay() {
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        
        overlayView = new FrameLayout(this);
        
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            1, 1,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        );
        
        params.screenOrientation = getScreenOrientation(currentRotation);
        params.gravity = Gravity.TOP | Gravity.START;
        
        try {
            windowManager.addView(overlayView, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private int getScreenOrientation(int rotation) {
        switch (rotation) {
            case 0:
                return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            case 1:
                return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            case 8:
                return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            case 9:
                return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            default:
                return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        try {
            unregisterReceiver(actionReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (overlayView != null && windowManager != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
