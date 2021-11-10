package com.example.foreground_location;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.embedding.engine.plugins.lifecycle.HiddenLifecycleReference;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;


/**
 * ForegroundLocationPlugin
 */
public class ForegroundLocationPlugin implements FlutterPlugin, ActivityAware, PluginRegistry.RequestPermissionsResultListener {
    private final String TAG = ForegroundLocationPlugin.class.getSimpleName();

    private static final String EVENT_CHANNEL_NAME = "foreground_location/event_handler";
    private static final String METHOD_CHANNEL_NAME = "foreground_location/method_channel";

    private MethodChannel methodChannel;
    private EventChannel eventChannel;

    private ActivityPluginBinding activityBinding;

    // Used in checking for runtime permissions.
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    // A reference to the service used to get location updates.
    private LocationUpdatesService mService = null;

    // Tracks the bound state of the service.
    private boolean bound = false;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            LocationUpdatesService.LocalBinder binder = (LocationUpdatesService.LocalBinder) service;
            mService = binder.getService();

            eventChannel.setStreamHandler(mService);
            mService.requestLocationUpdates();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            mService = null;

            methodChannel.setMethodCallHandler(null);
            eventChannel.setStreamHandler(null);

            bound = false;
        }
    };

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        methodChannel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), METHOD_CHANNEL_NAME);
        eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), EVENT_CHANNEL_NAME);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        methodChannel = null;
        eventChannel = null;
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        this.activityBinding = binding;
        Log.d(TAG, "onAttachedToActivity");

        ((HiddenLifecycleReference) activityBinding.getLifecycle()).getLifecycle().addObserver(new LifecycleObserver() {

            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            void onCreate() {
                // Check that the user hasn't revoked permissions by going to Settings.
                if (Utils.requestingLocationUpdates(binding.getActivity())) {
                    if (!checkPermissions()) {
                        requestPermissions();
                    }
                }

                if (!checkPermissions()) {
                    requestPermissions();
                }
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            void onStart() {
                binding.getActivity().bindService(
                        new Intent(binding.getActivity(), LocationUpdatesService.class),
                        mServiceConnection, Context.BIND_AUTO_CREATE
                );
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            void onStop() {
                if (bound) {
                    // Unbind from the service. This signals to the service that this activity is no longer
                    // in the foreground, and the service can respond by promoting itself to a foreground
                    // service.
                    activityBinding.getActivity().unbindService(mServiceConnection);
                    bound = false;
                }
            }
        });

    }

    @Override
    public void onDetachedFromActivity() {
        Log.d(TAG, "onDetachedFromActivity");
        activityBinding = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        this.activityBinding = binding;

        binding.getActivity().bindService(
                new Intent(binding.getActivity(), LocationUpdatesService.class),
                mServiceConnection, Context.BIND_AUTO_CREATE
        );
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity();
    }

    /**
     * Returns the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(activityBinding.getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(activityBinding.getActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            ActivityCompat.requestPermissions(activityBinding.getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(activityBinding.getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                mService.requestLocationUpdates();
            } else {
                // Permission denied.
//                Intent intent = new Intent();
//                intent.setAction(
//                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                Uri uri = Uri.fromParts("package",
//                        BuildConfig.APPLICATION_ID, null);
//                intent.setData(uri);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(intent);
            }
        }

        return false;
    }
}