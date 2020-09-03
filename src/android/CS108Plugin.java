package com.nocola.cordova.plugin;

// The native Toast API

import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.content.pm.PackageManager;
import android.content.Context;
import android.support.v4.app.Fragment;

// Cordova-required packages
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PermissionHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.csl.cs108library4a.Cs108Library4A;

public class CS108Plugin extends CordovaPlugin {
    final boolean DEBUG = false;
    public static boolean activityActive = false;
    static boolean wedged = false;

    // actions
    private static final String SHOW = "show";
    private static final String CONNECT = "connect";
    private static final String WEDGE = "wedge";
    private static final String SCAN = "scan";

    private static final String LOG_TAG = "CS108Plugin";

    // options
    private static final String DURATION_LONG = "long";

    private String[] permissions = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE
    };

    private CallbackContext callbackContext;

    public static Cs108Library4A mCs108Library4a;
    public static Context mContext;
    public static TextView mLogView;

    /**
     * Constructor.
     */
    public CS108Plugin() {
    }

    @Override
    public boolean execute(String action, JSONArray arg, final CallbackContext callbackContext) {
        this.callbackContext = callbackContext;

        if (action.equals(SHOW)) {
            showToast(arg);
        } else if (action.equals(CONNECT)) {
            // android permission auto add
            if (!hasPermission()) {
                requestPermissions(0);
            } else {
                connect();
            }
        } else {
            return false;
        }
        return true;
    }

    public void showToast(final JSONArray args) {
        String message = "";
        String duration = "";
        try {
            JSONObject options = args.getJSONObject(0);
            message = options.getString("message");
            duration = options.getString("duration");
        } catch (JSONException e) {
            callbackContext.error("Error encountered: " + e.getMessage());
        }
        // Create the toast
        Toast toast = Toast.makeText(cordova.getActivity(), message, DURATION_LONG.equals(duration) ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        // Display toast
        toast.show();
        // Send a positive result to the callbackContext
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
        callbackContext.sendPluginResult(pluginResult);
    }

    public void connect() {
        final CordovaPlugin that = this;
        FrameLayout layout = (FrameLayout) webView.getView().getParent();
        mContext = that.cordova.getActivity();
        mLogView = new TextView(layout.getContext());
        mCs108Library4a = new Cs108Library4A(mContext, mLogView);
        boolean scanning = false;
        if (scanning == false) {
            scanning = true;
            if (mCs108Library4a.scanLeDevice(true) == false) ;
            else cordova.getActivity().invalidateOptionsMenu();
        }
    }

    public boolean hasPermission() {
        for (String p : permissions) {
            if (!PermissionHelper.hasPermission(this, p)) {
                return false;
            }
        }
        return true;
    }

    public void requestPermissions(int requestCode) {
        PermissionHelper.requestPermissions(this, requestCode, permissions);
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        PluginResult result;
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                Log.d(LOG_TAG, "Permission Denied!");
                result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION);
                this.callbackContext.sendPluginResult(result);
                return;
            }
        }

        switch (requestCode) {
            case 0:
                connect();
                break;
        }
    }

    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
    }

}
