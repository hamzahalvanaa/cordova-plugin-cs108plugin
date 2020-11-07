package id.nocola.cordova.plugin.cs108.tasks;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.pertamina.physicalscheckassets.MainActivity;

import java.util.ArrayList;

import id.nocola.cordova.plugin.cs108.Cs108Library4A;
import id.nocola.cordova.plugin.cs108.ReaderDevice;
import id.nocola.cordova.plugin.cs108.adapters.ReaderListAdapter;

public class DeviceConnectTask extends AsyncTask<Void, String, Integer> {
    private final ReaderDevice connectingDevice;
    private String prgressMsg;
    int waitTime;
    private CustomProgressDialog progressDialog;
    private int setting;

    long connectTimeMillis;
    boolean bConnecting = false;

    private ReaderListAdapter readerListAdapter;
    Activity currentActivity;
    final boolean DEBUG = false;
    @SuppressLint("StaticFieldLeak")
    private static Cs108Library4A mCs108Library4a = MainActivity.mCs108Library4a;
    private ArrayList<ReaderDevice> readersList = MainActivity.sharedObjects.readersList;

    public DeviceConnectTask(Activity activity, ReaderDevice connectingDevice, String prgressMsg) {
        this.currentActivity = activity;
        this.connectingDevice = connectingDevice;
        this.prgressMsg = prgressMsg;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        MainActivity.mCs108Library4a.appendToLog("start of Connection with mrfidToWriteSize = " + mCs108Library4a.mrfidToWriteSize());
        mCs108Library4a.connect(connectingDevice);
        waitTime = 20;
        setting = -1;
        progressDialog = new CustomProgressDialog(currentActivity, prgressMsg);
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
            Toast.makeText(currentActivity.getApplicationContext(), "Setup problem after connection. Disconnect", Toast.LENGTH_SHORT).show();
        } else {
            mCs108Library4a.isBleConnected();
            Toast.makeText(currentActivity.getApplicationContext(), "Unable to connect device", Toast.LENGTH_SHORT).show();
        }
        super.onCancelled();
        mCs108Library4a.disconnect(false);
        mCs108Library4a.appendToLog("done");

        bConnecting = false;
    }

    protected void onPostExecute(Integer result) {
        if (DEBUG)
            mCs108Library4a.appendToLog("onPostExecute(): setting = " + setting + ", waitTime = " + waitTime);
//        ReaderDevice readerDevice = readersList.get(position);
//        readerDevice.setConnected(true);
//        readersList.set(position, readerDevice);
//        readerListAdapter.notifyDataSetChanged();

        String connectedBleAddress = connectingDevice.getAddress();
        if (connectedBleAddress.matches(MainActivity.sharedObjects.connectedBleAddressOld) == false)
            MainActivity.sharedObjects.versioinWarningShown = false;
        MainActivity.sharedObjects.connectedBleAddressOld = connectedBleAddress;
        MainActivity.sharedObjects.barsList.clear();
        MainActivity.sharedObjects.tagsList.clear();
        MainActivity.sharedObjects.tagsIndexList.clear();

        Toast.makeText(currentActivity.getApplicationContext(), "BLE is connected", Toast.LENGTH_SHORT).show();

        connectTimeMillis = System.currentTimeMillis();
        super.onPostExecute(result);
//        currentActivity.onBackPressed();
        bConnecting = false;
        MainActivity.mCs108Library4a.appendToLog("end of Connection with mrfidToWriteSize = " + mCs108Library4a.mrfidToWriteSize());
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
