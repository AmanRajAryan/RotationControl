package aman.rotationcontrol;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.Manifest;

public class MainActivity extends Activity {
    
    private RadioGroup rotationGroup;
    private Button applyButton;
    private Button stopButton;
    private static final int REQUEST_OVERLAY_PERMISSION = 1001;
    private static final int REQUEST_WRITE_SETTINGS = 1002;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1003;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "RotationPrefs";
    private static final String KEY_ROTATION_MODE = "rotation_mode";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        rotationGroup = findViewById(R.id.rotationGroup);
        applyButton = findViewById(R.id.applyButton);
        stopButton = findViewById(R.id.stopButton);
        
        int savedRotation = prefs.getInt(KEY_ROTATION_MODE, -1);
        if (savedRotation != -1) {
            selectRotationButton(savedRotation);
        }
        
        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAndRequestPermissions();
            }
        });
        
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRotationLock();
            }
        });
    }
    
    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
                return;
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(MainActivity.this)) {
                requestOverlayPermission();
                return;
            }
        }
        
        if (!checkWriteSettingsPermission()) {
            requestWriteSettingsPermission();
            return;
        }
        
        applyRotationSetting();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show();
                checkAndRequestPermissions();
            } else {
                Toast.makeText(this, "Notification permission denied.", Toast.LENGTH_LONG).show();
                checkAndRequestPermissions();
            }
        }
    }
    
    private void selectRotationButton(int rotation) {
        if (rotation == -1) {
            ((RadioButton) findViewById(R.id.radioAuto)).setChecked(true);
        } else if (rotation == 0) {
            ((RadioButton) findViewById(R.id.radioPortrait)).setChecked(true);
        } else if (rotation == 1) {
            ((RadioButton) findViewById(R.id.radioLandscape)).setChecked(true);
        } else if (rotation == 8) {
            ((RadioButton) findViewById(R.id.radioReversePortrait)).setChecked(true);
        } else if (rotation == 9) {
            ((RadioButton) findViewById(R.id.radioReverseLandscape)).setChecked(true);
        }
    }
    
    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
            Toast.makeText(this, "Please grant 'Display over other apps' permission", Toast.LENGTH_LONG).show();
        }
    }
    
    private boolean checkWriteSettingsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.System.canWrite(this);
        }
        return true;
    }
    
    private void requestWriteSettingsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_WRITE_SETTINGS);
            Toast.makeText(this, "Please grant 'Modify system settings' permission", Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Overlay permission granted!", Toast.LENGTH_SHORT).show();
                    checkAndRequestPermissions();
                } else {
                    Toast.makeText(this, "Overlay permission denied.", Toast.LENGTH_LONG).show();
                }
            }
        } else if (requestCode == REQUEST_WRITE_SETTINGS) {
            if (checkWriteSettingsPermission()) {
                Toast.makeText(this, "Write settings permission granted!", Toast.LENGTH_SHORT).show();
                checkAndRequestPermissions();
            } else {
                Toast.makeText(this, "Write settings permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void applyRotationSetting() {
        int selectedId = rotationGroup.getCheckedRadioButtonId();
        int rotation = -1;
        String rotationName = "";
        
        if (selectedId == R.id.radioAuto) {
            rotation = -1;
            rotationName = "Auto";
            
            Intent serviceIntent = new Intent(this, RotationLockService.class);
            stopService(serviceIntent);
            
            try {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.ACCELEROMETER_ROTATION, 1);
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            prefs.edit().putInt(KEY_ROTATION_MODE, rotation).apply();
            Toast.makeText(this, "Auto-rotation enabled", Toast.LENGTH_SHORT).show();
            
        } else {
            if (selectedId == R.id.radioPortrait) {
                rotation = 0;
                rotationName = "Portrait";
            } else if (selectedId == R.id.radioLandscape) {
                rotation = 1;
                rotationName = "Landscape";
            } else if (selectedId == R.id.radioReversePortrait) {
                rotation = 8;
                rotationName = "Reverse Portrait";
            } else if (selectedId == R.id.radioReverseLandscape) {
                rotation = 9;
                rotationName = "Reverse Landscape";
            }
            
            prefs.edit().putInt(KEY_ROTATION_MODE, rotation).apply();
            
            try {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.ACCELEROMETER_ROTATION, 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            Intent serviceIntent = new Intent(this, RotationLockService.class);
            serviceIntent.putExtra("rotation", rotation);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            
            Toast.makeText(this, "Rotation locked to " + rotationName, Toast.LENGTH_LONG).show();
        }
    }
    
    private void stopRotationLock() {
        Intent serviceIntent = new Intent(this, RotationLockService.class);
        stopService(serviceIntent);
        
        try {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        prefs.edit().putInt(KEY_ROTATION_MODE, -1).apply();
        ((RadioButton) findViewById(R.id.radioAuto)).setChecked(true);
        
        Toast.makeText(this, "Rotation lock stopped", Toast.LENGTH_SHORT).show();
    }
}
