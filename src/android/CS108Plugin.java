package com.nocola.cordova.plugin;

// The native Toast API

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.Keep;
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
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PermissionHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.csl.cs108library4a.Cs108Connector;
import com.csl.cs108library4a.Cs108Library4A;
import com.csl.cs108library4a.ReaderDevice;

import java.util.ArrayList;

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

    private static CordovaPlugin that;
    public static Context mContext = that.cordova.getActivity();
    private static CordovaInterface layout;
    public static TextView mLogView = new TextView(layout.getContext());
    public static Cs108Library4A mCs108Library4a = new Cs108Library4A(mContext, mLogView);
    public static SharedObjects sharedObjects;

   private DeviceScanTask deviceScanTask;
   private ReaderListAdapter readerListAdapter;
   private BluetoothAdapter.LeScanCallback mLeScanCallback;
   private ScanCallback mScanCallback;
   private ArrayList<ReaderDevice> readersList = sharedObjects.readersList;

   private ArrayList<Cs108Connector.Cs108ScanData> mScanResultList = new ArrayList<>();
   private Handler mHandler = new Handler();
   private DeviceConnectTask deviceConnectTask;

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
       cordova.getThreadPool().execute(new Runnable() {
           @Override
           public void run() {
               boolean operating = false;
               if (mCs108Library4a.isBleConnected()) operating = true;
               if (operating == false && deviceScanTask != null) {
                   if (deviceScanTask.isCancelled() == false) operating = true;
               }
               if (operating == false && deviceConnectTask != null) {
                   if (deviceConnectTask.isCancelled() == false) operating = true;
               }
               if (operating == false) {
                   deviceScanTask = new DeviceScanTask();
                   deviceScanTask.execute();
                   mCs108Library4a.appendToLog("Started DeviceScanTask");
                   // Send a positive result to the callbackContext
                   PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
                   callbackContext.sendPluginResult(pluginResult);
               }
           }
       });
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

   private class DeviceScanTask extends AsyncTask<Void, String, String> {
       private long timeMillisUpdate = System.currentTimeMillis();
       boolean usbDeviceFound = false;
       ArrayList<ReaderDevice> readersListOld = new ArrayList<ReaderDevice>();
       boolean wait4process = false;
       boolean scanning = false;

       @Override
       protected String doInBackground(Void... a) {
           while (isCancelled() == false) {
               if (wait4process == false) {
                   Cs108Connector.Cs108ScanData cs108ScanData = mCs108Library4a.getNewDeviceScanned();
                   if (cs108ScanData != null) mScanResultList.add(cs108ScanData);
                   if (scanning == false || mScanResultList.size() != 0 || System.currentTimeMillis() - timeMillisUpdate > 10000) {
                       wait4process = true;
                       publishProgress("");
                   }
               }
           }
           return "End of Asynctask()";
       }

       @Override
       protected void onProgressUpdate(String... output) {
           if (scanning == false) {
               scanning = true;
               if (mCs108Library4a.scanLeDevice(true) == false) cancel(true);
               else cordova.getActivity().invalidateOptionsMenu();
           }
           boolean listUpdated = false;
           while (mScanResultList.size() != 0) {
               Cs108Connector.Cs108ScanData scanResultA = mScanResultList.get(0);
               mCs108Library4a.appendToLog("Scan Result: " + scanResultA);
               mScanResultList.remove(0);
               if (false)
                   mCs108Library4a.appendToLog("scanResultA.device.getType() = " + scanResultA.device.getType() + ". scanResultA.rssi = " + scanResultA.rssi);
               if (scanResultA.device.getType() == BluetoothDevice.DEVICE_TYPE_LE && (true || scanResultA.rssi < 0)) {
                   boolean match = false;
                   for (int i = 0; i < readersList.size(); i++) {
                       if (readersList.get(i).getAddress().matches(scanResultA.device.getAddress())) {
                           ReaderDevice readerDevice1 = readersList.get(i);
                           int count = readerDevice1.getCount();
                           count++;
                           readerDevice1.setCount(count);
                           readerDevice1.setRssi(scanResultA.rssi);
                           readersList.set(i, readerDevice1);
                           listUpdated = true;
                           match = true;
                           break;
                       }
                   }
                   if (match == false) {
                       ReaderDevice readerDevice = new ReaderDevice(scanResultA.device, scanResultA.device.getName(), scanResultA.device.getAddress(), false, "", 1, scanResultA.rssi);
                       String strInfo = "";
                       if (scanResultA.device.getBondState() == 12) {
                           strInfo += "BOND_BONDED\n";
                       }
                       readerDevice.setDetails(strInfo + "scanRecord=" + mCs108Library4a.byteArrayToString(scanResultA.scanRecord));
                       readersList.add(readerDevice);
                       listUpdated = true;
                   }
               } else {
                   if (true)
                       mCs108Library4a.appendToLog("deviceScanTask: rssi=" + scanResultA.rssi + ", error type=" + scanResultA.device.getType());
               }
           }
           if (System.currentTimeMillis() - timeMillisUpdate > 10000) {
               timeMillisUpdate = System.currentTimeMillis();
               for (int i = 0; i < readersList.size(); i++) {
                   ReaderDevice readerDeviceNew = readersList.get(i);
                   boolean matched = false;
                   for (int k = 0; k < readersListOld.size(); k++) {
                       ReaderDevice readerDeviceOld = readersListOld.get(k);
                       if (readerDeviceOld.getAddress().matches(readerDeviceNew.getAddress())) {
                           matched = true;
                           if (readerDeviceOld.getCount() >= readerDeviceNew.getCount()) {
                               readersList.remove(i);
                               listUpdated = true;
                               readersListOld.remove(k);
                           } else readerDeviceOld.setCount(readerDeviceNew.getCount());
                           break;
                       }
                   }
                   if (matched == false) {
                       ReaderDevice readerDevice1 = new ReaderDevice(null, null, readerDeviceNew.getAddress(), false, null, readerDeviceNew.getCount(), 0);
                       readersListOld.add(readerDevice1);
                   }
               }
               if (DEBUG)
                   mCs108Library4a.appendToLog("Matched. Updated readerListOld with size = " + readersListOld.size());
               mCs108Library4a.scanLeDevice(false);
               cordova.getActivity().invalidateOptionsMenu();
               scanning = false;
           }
           if (listUpdated) readerListAdapter.notifyDataSetChanged();
           wait4process = false;
       }

       @Override
       protected void onCancelled() {
           super.onCancelled();
           mCs108Library4a.appendToLog("Stop Scanning 1A");
           deviceScanEnding();
       }

       @Override
       protected void onPostExecute(String result) {
           mCs108Library4a.appendToLog("Stop Scanning 1B");
           deviceScanEnding();
       }

       void deviceScanEnding() {
           mCs108Library4a.scanLeDevice(false);
       }
   }

   long connectTimeMillis;
   boolean bConnecting = false;

   private class DeviceConnectTask extends AsyncTask<Void, String, Integer> {
       private int position;
       private final ReaderDevice connectingDevice;
       private String prgressMsg;
       int waitTime;
       private CustomProgressDialog progressDialog;
       private int setting;

       DeviceConnectTask(int position, ReaderDevice connectingDevice, String prgressMsg) {
           this.position = position;
           this.connectingDevice = connectingDevice;
           this.prgressMsg = prgressMsg;
       }

       @Override
       protected void onPreExecute() {
           super.onPreExecute();

           mCs108Library4a.appendToLog("start of Connection with mrfidToWriteSize = " + mCs108Library4a.mrfidToWriteSize());
           mCs108Library4a.connect(connectingDevice);
           waitTime = 20;
           setting = -1;
           progressDialog = new CustomProgressDialog(cordova.getActivity(), prgressMsg);
           progressDialog.show();
       }

       @Override
       protected Integer doInBackground(Void... a) {
           do {
               try {
                   Thread.sleep(500);
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
               publishProgress("kkk ");
               if (mCs108Library4a.isBleConnected()) {
                   setting = 0;
                   break;
               }
           } while (--waitTime > 0);
           if (progressDialog.isShowing())
               progressDialog.dismiss();
           if (setting != 0 || waitTime <= 0) {
               cancel(true);
           }
           publishProgress("mmm ");
           return waitTime;
       }

       @Override
       protected void onProgressUpdate(String... output) {
       }

       @Override
       protected void onCancelled(Integer result) {
           if (true)
               mCs108Library4a.appendToLog("onCancelled(): setting = " + setting + ", waitTime = " + waitTime);
           if (setting >= 0) {
               Toast.makeText(cordova.getActivity(), "Setup problem after connection. Disconnect", Toast.LENGTH_SHORT).show();
           } else {
               mCs108Library4a.isBleConnected();
               Toast.makeText(cordova.getActivity(), "Unable to connect device", Toast.LENGTH_SHORT).show();
           }
           super.onCancelled();
           mCs108Library4a.disconnect(false);
           mCs108Library4a.appendToLog("done");

           bConnecting = false;
       }

       protected void onPostExecute(Integer result) {
           if (DEBUG)
               mCs108Library4a.appendToLog("onPostExecute(): setting = " + setting + ", waitTime = " + waitTime);
           ReaderDevice readerDevice = readersList.get(position);
           readerDevice.setConnected(true);
           readersList.set(position, readerDevice);
           readerListAdapter.notifyDataSetChanged();

           String connectedBleAddress = connectingDevice.getAddress();
           if (connectedBleAddress.matches(sharedObjects.connectedBleAddressOld) == false)
               sharedObjects.versioinWarningShown = false;
           sharedObjects.connectedBleAddressOld = connectedBleAddress;
           sharedObjects.barsList.clear();
           sharedObjects.tagsList.clear();
           sharedObjects.tagsIndexList.clear();

           Toast.makeText(cordova.getActivity().getApplicationContext(), "BLE is connected", Toast.LENGTH_SHORT).show();

           connectTimeMillis = System.currentTimeMillis();
           super.onPostExecute(result);
           cordova.getActivity().onBackPressed();
           bConnecting = false;
           mCs108Library4a.appendToLog("end of Connection with mrfidToWriteSize = " + mCs108Library4a.mrfidToWriteSize());
       }
   }

   public class CustomProgressDialog extends ProgressDialog {
       public CustomProgressDialog(Context context, String message) {
           super(context, ProgressDialog.STYLE_SPINNER);
           if (message == null) message = "Progressing. Please wait.";
           setTitle(null);
           setMessage(message);
           setCancelable(false);
       }
   }
}
