/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package com.pertamina.physicalscheckassets;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;

import org.apache.cordova.*;

import id.nocola.cordova.plugin.cs108.Cs108Library4A;
import id.nocola.cordova.plugin.cs108.ReaderDevice;
import id.nocola.cordova.plugin.cs108.SensorConnector;
import id.nocola.cordova.plugin.cs108.SharedObjects;
import id.nocola.cordova.plugin.cs108.tasks.DeviceConnectTask;

public class MainActivity extends CordovaActivity {
    public static boolean activityActive = false;

    public static Context mContext;
    public static Cs108Library4A mCs108Library4a;
    public static SharedObjects sharedObjects;
    public static SensorConnector mSensorConnector;
    public static ReaderDevice tagSelected;
    Handler mHandler = new Handler();

    final boolean DEBUG = false;

    public static String mDid; public static int selectHold; public static int selectFor;
    public static class Config {
        public String config1, config2;
    }

    ;
    public static Config config = new Config();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mContext = this;
        sharedObjects = new SharedObjects(mContext);
        mCs108Library4a = new Cs108Library4A(mContext);
        mSensorConnector = new SensorConnector(mContext);

        super.onCreate(savedInstanceState);

        // enable Cordova apps to be started in the background
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("cdvStartInBackground", false)) {
            moveTaskToBack(true);
        }

        // Set by <content src="index.html" /> in config.xml
        loadUrl(launchUrl);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        MainActivity.mCs108Library4a.connect(null);
        if (DEBUG) mCs108Library4a.appendToLog("MainActivity.onRestart()");
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (DEBUG) mCs108Library4a.appendToLog("MainActivity.onStart()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityActive = true;
        wedged = false;
        if (DEBUG) mCs108Library4a.appendToLog("MainActivity.onResume()");
    }

    @Override
    protected void onPause() {
        if (DEBUG) mCs108Library4a.appendToLog("MainActivity.onPause()");
        activityActive = false;
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (DEBUG) mCs108Library4a.appendToLog("MainActivity.onStop()");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (DEBUG) mCs108Library4a.appendToLog("MainActivity.onDestroy()");
        if (true) {
            mCs108Library4a.disconnect(true);
        }
        super.onDestroy();
    }

    boolean configureDisplaying = false;
    private final Runnable configureRunnable = new Runnable() {
        @Override
        public void run() {
            MainActivity.mCs108Library4a.appendToLog("AAA: mrfidToWriteSize = " + mCs108Library4a.mrfidToWriteSize());
            if (mCs108Library4a.mrfidToWriteSize() != 0) {
                MainActivity.mCs108Library4a.mrfidToWritePrint();
                configureDisplaying = true;
                mHandler.postDelayed(configureRunnable, 500);
            } else {
                configureDisplaying = false;
                progressDialog.dismiss();
            }
        }
    };

    DeviceConnectTask.CustomProgressDialog progressDialog;
    public static boolean permissionRequesting;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        MainActivity.mCs108Library4a.appendToLog("onRequestPermissionsResult ====");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionRequesting = false;
    }

    public static boolean wedged = false;

}
