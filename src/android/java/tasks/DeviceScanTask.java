package id.nocola.cordova.plugin.cs108.tasks;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import com.pertamina.physicalscheckassets.MainActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import id.nocola.cordova.plugin.cs108.Cs108Connector;
import id.nocola.cordova.plugin.cs108.Cs108Library4A;
import id.nocola.cordova.plugin.cs108.ReaderDevice;
import id.nocola.cordova.plugin.cs108.adapters.ReaderListAdapter;

public class DeviceScanTask extends AsyncTask<Void, String, String> {
    private long timeMillisUpdate = System.currentTimeMillis();
    boolean usbDeviceFound = false;
    ArrayList<ReaderDevice> readersListOld = new ArrayList<ReaderDevice>();
    boolean wait4process = false;
    boolean scanning = false;

    private ReaderListAdapter readerListAdapter;
    private static CallbackContext currentCallback;

    @SuppressLint("StaticFieldLeak")
    public static Activity currentActivity;
    @SuppressLint("StaticFieldLeak")
    public static Cs108Library4A mCs108Library4a = MainActivity.mCs108Library4a;

    final boolean DEBUG = false;

    public DeviceScanTask(Activity activity, CallbackContext context) {
        super();
        currentActivity = activity;
        currentCallback = context;
    }

    private ArrayList<Cs108Connector.Cs108ScanData> mScanResultList = new ArrayList<>();
    private ArrayList<ReaderDevice> readersList = MainActivity.sharedObjects.readersList;

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
            else currentActivity.invalidateOptionsMenu();
        }
        boolean listUpdated = false;
        while (mScanResultList.size() != 0) {
            Cs108Connector.Cs108ScanData scanResultA = mScanResultList.get(0);
            String json = new Gson().toJson(readersList);
            PluginResult result = new PluginResult(PluginResult.Status.OK, json);
            Log.e("readersList=", result.toString());
            result.setKeepCallback(true);
            currentCallback.sendPluginResult(result);
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
            currentActivity.invalidateOptionsMenu();
            scanning = false;
        }
//        if (listUpdated) readerListAdapter.notifyDataSetChanged();
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
