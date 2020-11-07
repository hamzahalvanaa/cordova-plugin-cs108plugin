package id.nocola.cordova.plugin.cs108;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import com.pertamina.physicalscheckassets.MainActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import id.nocola.cordova.plugin.cs108.tasks.AccessTask;
import id.nocola.cordova.plugin.cs108.tasks.DeviceConnectTask;
import id.nocola.cordova.plugin.cs108.tasks.DeviceScanTask;
import id.nocola.cordova.plugin.cs108.tasks.InventoryRfidTask;
import id.nocola.cordova.plugin.cs108.tasks.SaveList2ExternalTask;

/**
 * This class echoes a string called from JavaScript.
 */
public class Cs108Plugin extends CordovaPlugin {
    private static final String TAG = "Cs108Plugin";
    final boolean DEBUG = false;
    private DeviceScanTask deviceScanTask;
    private DeviceConnectTask deviceConnectTask;
    private InventoryRfidTask inventoryRfidTask;
    private AccessTask accessTask;
    public static ReaderDevice tagSelected;
    Handler mHandler = new Handler();

    public static Cs108Library4A mCs108Library4a = MainActivity.mCs108Library4a;

    private ArrayList<ReaderDevice> readersList = MainActivity.sharedObjects.readersList;
    private static CallbackContext currentCallback = null;
    CallbackContext discoverCallback = null;

    boolean bConnecting = false;
    private String mDid = null;
    private boolean bMultiBank = false, bMultiBankInventory = false, bBapInventory = false, bctesiusInventory = false;

    private ListView rfidListView;
    private TextView rfidEmptyView;
    private TextView rfidRunTime, rfidVoltageLevel;
    private TextView rfidYieldView;
    private TextView rfidRateView;
    private Button button;

    String accEpcValue = "";
    String accXpcValue = "";
    String accTidValue = "";
    String accUserValue = "";

    enum ReadWriteTypes {
        NULL, RESERVE, PC, EPC, XPC, TID, USER, EPC1
    }

    Spinner spinnerSelectBank, spinnerRWSelectEpc1;
    boolean operationRead = false;
    ReadWriteTypes readWriteTypes;
    EditText editTextRWSelectOffset, editTextAccessRWAccPassword, editTextAccessRWKillPwd, editTextAccessRWAccPwd, editTextAccPc, editTExtAccessRWXpc;
    EditText editTextTidValue, editTextUserValue, editTextEpcValue, editTextaccessRWAntennaPower;
    TextView textViewEpcLength;
    private Button buttonRead;
    private Button buttonWrite;
    String strPCValueRef = "";
    ReaderDevice tagListSelected;
    String newTagSelected;

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        LOG.d(TAG, "action%s", action);
        currentCallback = callbackContext;

        if (action.equals("showToast")) {

            showToast(args);

        } else if (action.equals("startFindDevices")) {

            findForDevices(callbackContext);
            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
//            callbackContext.sendPluginResult(result);
            return true;

        } else if (action.equals("stopFindDevices")) {

            stopFindDevices(callbackContext);
            return true;

        } else if (action.equals("connect")) {

            connect(args, callbackContext);
            return true;

        } else if (action.equals("disconnect")) {

            disconnect(callbackContext);
            return true;

        } else if (action.equals("beginInventory")) {

            setNotificationListener(callbackContext);
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            return true;

        } else if (action.equals("startScan")) {

            startScan(callbackContext);
            return true;

        } else if (action.equals("stopScan")) {

            stopScan(callbackContext);
            return true;

        } else if (action.equals("clearTagList")) {

            clearTagsList(callbackContext);
            return true;

        } else if (action.equals("writeTag")) {

            beginWriteTag(args, callbackContext);
            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            return true;

        } else if (action.equals("getDeviceInfo")) {

            if (mCs108Library4a.isBleConnected()) {
                getDeviceInfo(callbackContext);
                return true;
            } else {
                callbackContext.error("Ble not connected!");
                return false;
            }

        }
        return false;
    }

    public void showToast(final JSONArray args) {
        String message = "";
        String duration = "";
        try {
            JSONObject options = args.getJSONObject(0);
            message = options.getString("message");
            duration = options.getString("duration");
        } catch (JSONException e) {
            currentCallback.error("Error encountered: " + e.getMessage());
        }
        // Create the toast
        Toast toast = Toast.makeText(cordova.getActivity(), message,
                "long".equals(duration) ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        // Display toast
        toast.show();
        // Send a positive result to the callbackContext
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, "here comes the callback");
        currentCallback.sendPluginResult(pluginResult);
    }

    private void findForDevices(CallbackContext context) throws JSONException {
        discoverCallback = context;
        this.cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                boolean operating = false;
                if (mCs108Library4a.isBleConnected()) operating = true;
                if (!operating && deviceScanTask != null) {
                    if (!deviceScanTask.isCancelled()) operating = true;
                }
                if (!operating && deviceConnectTask != null) {
                    if (!deviceConnectTask.isCancelled()) operating = true;
                }
                if (!operating) {
                    deviceScanTask = new DeviceScanTask(cordova.getActivity(), context);
                    deviceScanTask.execute();
                    mCs108Library4a.appendToLog("Started DeviceScanTask");
                }
            }
        });
//        String json = new Gson().toJson(readersList);
//        context.success(json);
    }

    private void stopFindDevices(CallbackContext context) {
        if (deviceScanTask != null) {
            deviceScanTask.cancel(true);
            context.success("Scan devices stopped.");
        }
        if (deviceConnectTask != null) {
            deviceConnectTask.cancel(true);
            context.success("Connect device stopped.");
        }
    }

    int batteryCount_old;
    boolean batteryUpdate = false;
    String strBatteryLow;

    private void getDeviceInfo(CallbackContext context) {
        currentCallback = context;
        this.cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                if (!MainActivity.mCs108Library4a.isBleConnected()) {
                    MainActivity.sharedObjects.batteryWarningShown = 0;
                    return;
                }
                int batteryCount = MainActivity.mCs108Library4a.getBatteryCount();
                Object deviceInfo = MainActivity.mCs108Library4a.getDeviceStatus(false);
                if (batteryCount_old != batteryCount)
                    strBatteryLow = MainActivity.mCs108Library4a.isBatteryLow();

                if (strBatteryLow == null) MainActivity.sharedObjects.batteryWarningShown = 0;
                else if (++MainActivity.sharedObjects.batteryWarningShown == 1) {
                    context.error(strBatteryLow + "% Battery Life Left, Please Recharge CS108 or Replace with Freshly Charged CS108B");
                } else if (false && MainActivity.sharedObjects.batteryWarningShown > 10)
                    MainActivity.sharedObjects.batteryWarningShown = 0;

                if (batteryCount_old == batteryCount) {
                    batteryUpdate = !batteryUpdate;
                } else {
                    batteryCount_old = batteryCount;
                    context.success(deviceInfo.toString());
                }
            }
        });
    }

    private void connect(final JSONArray args, CallbackContext context) {
        discoverCallback = context;
        try {
            JSONObject obj = args.getJSONObject(0);
            ReaderDevice readerDevice = new Gson().fromJson(obj.toString(), ReaderDevice.class);
            if (mCs108Library4a.isBleConnected()) {
                mCs108Library4a.disconnect(false);
            } else if (!mCs108Library4a.isBleConnected()) {
                boolean validStart = false;
                if (deviceConnectTask == null) {
                    validStart = true;
                } else if (deviceConnectTask.getStatus() == AsyncTask.Status.FINISHED) {
                    validStart = true;
                }
                if (validStart) {
                    bConnecting = true;
                    if (deviceScanTask != null) deviceScanTask.cancel(true);
                    MainActivity.mCs108Library4a.appendToLog("Connecting");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        deviceConnectTask = new DeviceConnectTask(this.cordova.getActivity(), readerDevice, "Connecting with " + readerDevice.getName());
                        deviceConnectTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    } else {
                        deviceConnectTask = new DeviceConnectTask(this.cordova.getActivity(), readerDevice, "Connecting with " + readerDevice.getName());
                        deviceConnectTask.execute();
                        context.success("BLE Connected to " + readerDevice.getName() + " with address " + readerDevice.getAddress());
                    }
                }
            }
        } catch (JSONException e) {
            context.error("Kesalahan terjadi: " + e.getMessage());
        }
    }

    private void disconnect(CallbackContext context) {
        discoverCallback = context;
        if (mCs108Library4a.isBleConnected()) {
            mCs108Library4a.disconnect(true);
            context.success("Device successfully disconnected.");
        } else {
            context.error("BLE not connected.");
        }
    }

    void setNotificationListener(final CallbackContext context) {
        MainActivity.mCs108Library4a.setNotificationListener(new Cs108Connector.NotificationListener() {
            @Override
            public void onChange() {
                MainActivity.mCs108Library4a.appendToLog("TRIGGER key is pressed.");
                beginInventory(true, context);
            }
        });
    }

    boolean needResetData = false;

    void resetSelectData() {
        MainActivity.mCs108Library4a.restoreAfterTagSelect();
        if (needResetData) {
            MainActivity.mCs108Library4a.setTagRead(0);
            MainActivity.mCs108Library4a.setAccessBank(1);
            MainActivity.mCs108Library4a.setAccessOffset(0);
            MainActivity.mCs108Library4a.setAccessCount(0);
            needResetData = false;
        }
    }

    private void beginInventory(boolean buttonTrigger, CallbackContext context) {
        currentCallback = context;
        if (buttonTrigger)
            MainActivity.mCs108Library4a.appendToLog("BARTRIGGER: getTriggerButtonStatus = " + MainActivity.mCs108Library4a.getTriggerButtonStatus());
        if (MainActivity.sharedObjects.runningInventoryBarcodeTask) {
            Toast.makeText(MainActivity.mContext, "Running barcode inventory", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean started = false;
        if (inventoryRfidTask != null)
            if (inventoryRfidTask.getStatus() == AsyncTask.Status.RUNNING) started = true;
        if (buttonTrigger && ((started && MainActivity.mCs108Library4a.getTriggerButtonStatus()) || (started == false && MainActivity.mCs108Library4a.getTriggerButtonStatus() == false))) {
            MainActivity.mCs108Library4a.appendToLog("BARTRIGGER: trigger ignore");
            return;
        }
        if (!started) {
            if (!MainActivity.mCs108Library4a.isBleConnected()) {
                currentCallback.error("Bluetooth is Not Connected, Please Connect");
                Toast.makeText(MainActivity.mContext, "Bluetooth is Not Connected, Please Connect", Toast.LENGTH_SHORT).show();
                return;
            } else if (MainActivity.mCs108Library4a.isRfidFailure()) {
                currentCallback.error("Rfid is disabled");
                Toast.makeText(MainActivity.mContext, "Rfid is disabled", Toast.LENGTH_SHORT).show();
                return;
            } else if (MainActivity.mCs108Library4a.mrfidToWriteSize() != 0) {
                String json = new Gson().toJson(MainActivity.mCs108Library4a.mrfidToWriteSize());
                Log.e("apasih", json);
                currentCallback.error("Updating data. Please wait.");
                Toast.makeText(MainActivity.mContext, "Updating data. Please wait.", Toast.LENGTH_SHORT).show();
                return;
            }
            startInventoryTask(context);
        } else {
            if (buttonTrigger)
                inventoryRfidTask.taskCancelReason = InventoryRfidTask.TaskCancelRReason.BUTTON_RELEASE;
            else inventoryRfidTask.taskCancelReason = InventoryRfidTask.TaskCancelRReason.STOP;
        }
    }

    private void startScan(CallbackContext context) {
        discoverCallback = context;
        beginInventory(false, context);
    }

    private void stopScan(CallbackContext context) {
        discoverCallback = context;
        beginInventory(true, context);
    }

    private void clearTagsList(CallbackContext context) {
        if (MainActivity.sharedObjects.runningInventoryRfidTask) return;
        MainActivity.tagSelected = null;
        MainActivity.sharedObjects.tagsList.clear();
        MainActivity.sharedObjects.tagsIndexList.clear();
        context.success("Tag list cleared.");
    }

    private void startInventoryTask(CallbackContext context) {
        currentCallback = context;
        int extra1Bank = -1, extra2Bank = -1;
//        int extra1Count = 0, extra2Count = 0;
//        int extra1Offset = 0, extra2Offset = 0;
        String mDid = this.mDid;

        if (mDid != null) {
            if (MainActivity.mDid != null && mDid.length() == 0) mDid = MainActivity.mDid;
            extra2Bank = 2;
//            extra2Offset = 0;
//            extra2Count = 2;
            if (mDid.matches("E200B0")) {
                extra1Bank = 2;
//                extra1Offset = 0;
//                extra1Count = 2;
                extra2Bank = 3;
//                extra2Offset = 0x2d;
//                extra2Count = 1;
            } else if (mDid.matches("E203510")) {
                extra1Bank = 2;
//                extra1Offset = 0;
//                extra1Count = 2;
                extra2Bank = 3;
//                extra2Offset = 8;
//                extra2Count = 2;
            } else if (mDid.matches("E280B12")) {
                extra1Bank = 2;
//                extra1Offset = 0;
//                extra1Count = 2;
                extra2Bank = 3;
//                extra2Offset = 0x120;
//                extra2Count = 1;
            } else if (mDid.matches("E282402")) {
                extra1Bank = 0;
//                extra1Offset = 11;
//                extra1Count = 1;
                extra2Bank = 0;
//                extra2Offset = 13;
//                extra2Count = 1;
            } else if (mDid.matches("E282403")) {
                extra1Bank = 0;
//                extra1Offset = 12;
//                extra1Count = 3;
                extra2Bank = 3;
//                extra2Offset = 8;
//                extra2Count = 4;
            } else if (mDid.matches("E282405")) {
                extra1Bank = 0;
//                extra1Offset = 10;
//                extra1Count = 5;
                extra2Bank = 3;
//                extra2Offset = 0x12;
//                extra2Count = 4;
            }
            if (mDid.matches("E280B12")) {
                if (MainActivity.mDid.matches("E280B12B")) {
                    MainActivity.mCs108Library4a.setSelectCriteria(1, true, 4, 0, 5, 1, 0x220, "8321");
                    MainActivity.mCs108Library4a.appendToLog("Hello123: Set Sense at Select !!!");
                } else { //if (MainActivity.mDid.matches("E280B12A")) {
                    MainActivity.mCs108Library4a.setSelectCriteriaDisable(1);
                    MainActivity.mCs108Library4a.appendToLog("Hello123: Set Sense at BOOT !!!");
                }
            } else if (mDid.matches("E203510")) {
                MainActivity.mCs108Library4a.setSelectCriteria(1, true, 7, 4, 0, 1, 32, mDid);
            } else if (mDid.matches("E28240")) {
                if (MainActivity.selectFor != 0) {
                    MainActivity.mCs108Library4a.setSelectCriteriaDisable(1);
                    MainActivity.mCs108Library4a.setSelectCriteriaDisable(2);
                    MainActivity.selectFor = 0;
                }
            } else if (mDid.matches("E282402")) {
                if (MainActivity.selectFor != 2) {
                    MainActivity.mCs108Library4a.setSelectCriteria(1, true, 4, 2, 0, 3, 0xA0, "20");
                    MainActivity.mCs108Library4a.setSelectCriteriaDisable(2);
                    MainActivity.selectFor = 2;
                }
            } else if (mDid.matches("E282403")) {
                if (MainActivity.selectFor != 3) {
                    MainActivity.mCs108Library4a.setSelectCriteria(1, true, 4, 2, 0, 3, 0xE0, "");
                    MainActivity.mCs108Library4a.setSelectCriteria(2, true, 4, 2, 0, 3, 0xD0, "1F");
                    MainActivity.selectFor = 3;
                }
            } else if (mDid.matches("E282405")) {
                if (MainActivity.selectFor != 5) {
                    MainActivity.mCs108Library4a.setSelectCriteria(1, true, 4, 5, MainActivity.selectHold, 3, 0x3B0, "00");
                    MainActivity.mCs108Library4a.setSelectCriteriaDisable(2);
                    MainActivity.selectFor = 5;
                }
            } else {
                if (MainActivity.selectFor != -1) {
                    MainActivity.mCs108Library4a.setSelectCriteriaDisable(1);
                    MainActivity.mCs108Library4a.setSelectCriteriaDisable(2);
                    MainActivity.selectFor = -1;
                }
            }
            boolean bNeedSelectedTagByTID = true;
            if (mDid.matches("E2806894")) {
                Log.i(TAG, "HelloK: Find E2806894 with MainActivity.mDid = " + MainActivity.mDid);
                if (MainActivity.mDid.matches("E2806894A")) {
                    Log.i(TAG, "HelloK: Find E2806894A");
                    MainActivity.mCs108Library4a.setInvBrandId(false);
                    MainActivity.mCs108Library4a.setSelectCriteriaDisable(1);
                } else if (MainActivity.mDid.matches("E2806894B")) {
                    Log.i(TAG, "HelloK: Find E2806894B");
                    MainActivity.mCs108Library4a.setInvBrandId(false);
                    MainActivity.mCs108Library4a.setSelectCriteria(1, true, 4, 0, 1, 0x203, "1", true);
                    bNeedSelectedTagByTID = false;
                } else if (MainActivity.mDid.matches("E2806894C")) {
                    Log.i(TAG, "HelloK: Find E2806894C");
                    MainActivity.mCs108Library4a.setInvBrandId(true);
                    MainActivity.mCs108Library4a.setSelectCriteria(1, true, 4, 0, 1, 0x204, "1", true);
                    bNeedSelectedTagByTID = false;
                }
            } else if (mDid.indexOf("E28011") == 0) bNeedSelectedTagByTID = false;
            Log.i(TAG, "HelloK: going to setSelectedTagByTID with mDid = " + mDid + " with extra1Bank = " + extra1Bank + ", extra2Bank = " + extra2Bank + ", bNeedSelectedTagByTID = " + bNeedSelectedTagByTID + ", bMultiBank = " + bMultiBank);
            if (bNeedSelectedTagByTID) MainActivity.mCs108Library4a.setSelectedTagByTID(mDid, 300);
        }

        if (bMultiBank == false) {
            MainActivity.mCs108Library4a.startOperation(Cs108Library4A.OperationTypes.TAG_INVENTORY_COMPACT);
            inventoryRfidTask = new InventoryRfidTask(this.cordova.getContext(), -1, -1, 0, 0, 0, 0,
                    false, MainActivity.mCs108Library4a.getInventoryBeep(),
                    MainActivity.sharedObjects.tagsList, true, null, null,
                    rfidRunTime, null, rfidVoltageLevel, rfidYieldView, button, rfidRateView, context);
        }
        inventoryRfidTask.execute();
    }

    private void beginWriteTag(JSONArray json, CallbackContext context) {
        try {
            JSONObject obj = json.getJSONObject(0);
            this.tagListSelected = new Gson().fromJson(obj.getJSONObject("tagList").toString(), ReaderDevice.class);
            this.newTagSelected = obj.getString("newTag");
            if (this.tagListSelected != null) {
                String strEpcValue = this.tagListSelected.getAddress();//this.tagListSelected.getString("address");
                String detail = this.tagListSelected.getDetails();//.getString("details");
                String header = "PC=";
                int index = detail.indexOf(header) + header.length();
                strPCValueRef = detail.substring(index, index + 4);
                operationRead = false;
                startAccessTask();

                updatePCEpc(strPCValueRef, strEpcValue);
            }
            MainActivity.mCs108Library4a.setSameCheck(false);
        } catch (JSONException e) {
            context.error("Error: " + e.getMessage());
        }
    }

    void startAccessTask() {
        if (!updating) {
            updating = true;
            bankProcessing = 0;
            restartAccessBank = -1;
            mHandler.removeCallbacks(updateRunnable);
            mHandler.post(updateRunnable);
        }
    }

    boolean updating = false;
    int bankProcessing = 0;
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            boolean rerunRequest = true;
            boolean taskRequest = false;
            if (accessTask == null) {
                if (DEBUG)
                    MainActivity.mCs108Library4a.appendToLog("AccessReadWriteFragment().updateRunnable(): NULL accessReadWriteTask");
                taskRequest = true;
            } else if (accessTask.getStatus() != AsyncTask.Status.FINISHED) {
                if (DEBUG)
                    MainActivity.mCs108Library4a.appendToLog("AccessReadWriteFragment().updateRunnable(): accessReadWriteTask.getStatus() =  " + accessTask.getStatus().toString());
            } else {
                taskRequest = true;
                if (DEBUG)
                    MainActivity.mCs108Library4a.appendToLog("AccessReadWriteFragment().updateRunnable(): FINISHED accessReadWriteTask");
            }
            if (processResult()) {
                rerunRequest = true;
            } else if (taskRequest) {
                bcheckBoxAll = false;
                boolean invalid = processTickItems();
                if (bankProcessing == 0 && bcheckBoxAll) rerunRequest = false;
                else if (bankProcessing++ != 0 && invalid) rerunRequest = false;
                else {
                    if (restartAccessBank != accessBank) {
                        restartAccessBank = accessBank;
                        restartCounter = 3;
                    }
                    if (DEBUG)
//                        MainActivity.mCs108Library4a.appendToLog("AccessReadWriteFragment().InventoryRfidTask(): tagID=" + editTextRWTagID.getText() + ", operationrRead=" + operationRead + ", accessBank=" + accessBank + ", accOffset=" + accOffset + ", accSize=" + accSize);
                        operationRead = true;
                    accessTask = new AccessTask(
                            operationRead, null,
                            invalid,
                            tagListSelected.getAddress(), 1, 32,
                            "00000000",
                            300,
                            (operationRead ? Cs108Connector.HostCommands.CMD_18K6CREAD : Cs108Connector.HostCommands.CMD_18K6CWRITE),
                            0, false, false, true,
                            null, null, null, null, null,
                            currentCallback);
                    accessTask.execute();
                    rerunRequest = true;
                }
            }
            if (rerunRequest) {
                mHandler.postDelayed(updateRunnable, 500);
                if (DEBUG)
                    MainActivity.mCs108Library4a.appendToLog("AccessReadWriteFragment().updateRunnable(): Restart");
            } else {
                if (bankProcessing == 0 && bcheckBoxAll) {
                    Toast.makeText(MainActivity.mContext, "no choice selected yet", Toast.LENGTH_SHORT).show();
                    currentCallback.error("No choice selected yet");
                }
                updating = false;
            }
        }
    };

    TextView textViewReserveOk, textViewPcOk, textViewEpcOk, textViewTidOk, textViewUserOk, textViewEpc1Ok;
    CheckBox checkBoxReserve, checkBoxPc, checkBoxEpc, checkBoxTid, checkBoxUser, checkBoxEpc1;
    int accessBank, accSize, accOffset;
    int restartCounter = 0;
    int restartAccessBank = -1;

    boolean processResult() {
        String strValue = "3400";
        String accessResult = null;
        if (accessTask == null) return false;
        else if (accessTask.getStatus() != AsyncTask.Status.FINISHED) return false;
        else {
            accessResult = accessTask.accessResult;
            if (DEBUG)
                MainActivity.mCs108Library4a.appendToLog("processResult(): accessResult = " + accessResult);
            if (accessResult == null) {
                if (readWriteTypes == ReadWriteTypes.RESERVE) {
//                    textViewReserveOk.setText("E");
//                    checkBoxReserve.setChecked(false);
                }
                if (readWriteTypes == ReadWriteTypes.PC) {
//                    textViewPcOk.setText("E");
//                    checkBoxPc.setChecked(false);
                }
                if (readWriteTypes == ReadWriteTypes.EPC) {
//                    textViewEpcOk.setText("E");
//                    checkBoxEpc.setChecked(false);
                }
                if (readWriteTypes == ReadWriteTypes.TID) {
//                    textViewTidOk.setText("E");
//                    checkBoxTid.setChecked(false);
                }
                if (readWriteTypes == ReadWriteTypes.USER) {
//                    textViewUserOk.setText("E");
//                    checkBoxUser.setChecked(false);
                }
                if (readWriteTypes == ReadWriteTypes.EPC1) {
//                    textViewEpc1Ok.setText("E");
//                    checkBoxEpc1.setChecked(false);
                }
            } else {
                if (DEBUG)
                    MainActivity.mCs108Library4a.appendToLog("accessResult = " + accessResult);
                if (readWriteTypes == ReadWriteTypes.RESERVE) {
//                    textViewReserveOk.setText("O");
//                    checkBoxReserve.setChecked(false);
                    readWriteTypes = ReadWriteTypes.NULL;
                    if (accessResult.length() == 0 || operationRead == false) {
                    } else if (accessResult.length() < 8) {
//                        editTextAccessRWKillPwd.setText(accessResult);
                    } else {
//                        editTextAccessRWKillPwd.setText(accessResult.substring(0, 8));
                    }
                    if (accessResult.length() <= 8) {
//                        editTextAccessRWAccPwd.setText("");
                    } else if (accessResult.length() < 16) {
//                        editTextAccessRWAccPwd.setText(accessResult.subSequence(8, accessResult.length()));
                    } else {
//                        editTextAccessRWAccPwd.setText(accessResult.subSequence(8, 16));
                    }
                } else if (readWriteTypes == ReadWriteTypes.PC) {
//                    textViewPcOk.setText("O");
//                    checkBoxPc.setChecked(false);
                    readWriteTypes = ReadWriteTypes.NULL;
                    if (operationRead) {
                        String newValue = "";
                        if (accessResult.length() <= 4) {
                            newValue = accessResult.subSequence(0, accessResult.length()).toString();
                        } else {
                            newValue = accessResult.subSequence(0, 4).toString();
                        }
//                        editTextAccPc.setText(newValue);
                    }
                    updatePCEpc(strValue, this.tagListSelected.getAddress());
                } else if (readWriteTypes == ReadWriteTypes.EPC) {
                    if (DEBUG)
                        MainActivity.mCs108Library4a.appendToLog("AccessReadWrite(). EPC DATA with accessBank = " + accessBank + ", with accessResult.length = " + accessResult.length());
//                    textViewEpcOk.setText("O");
//                    checkBoxEpc.setChecked(false);
                    readWriteTypes = ReadWriteTypes.NULL;
                    if (operationRead) {
                        String newValue = "";
                        if (accessResult.length() <= 4) {
                            newValue = accessResult.subSequence(0, accessResult.length()).toString();
                        } else {
                            newValue = accessResult.subSequence(0, 4).toString();
                        }
//                        editTextAccPc.setText(newValue);
                    }
                    updatePCEpc(strValue, this.tagListSelected.getAddress());

                    if (operationRead) {
                        String newValue = "";
                        if (accessResult.length() > 4) {
                            newValue = accessResult.subSequence(4, accessResult.length()).toString();
                        }
//                        editTextAccessRWEpc.setText(newValue);
                    }
                } else if (readWriteTypes == ReadWriteTypes.XPC) {
                    if (DEBUG)
                        MainActivity.mCs108Library4a.appendToLog("AccessReadWrite(). XPC DATA with accessBank = " + accessBank + ", with accessResult.length = " + accessResult.length() + ", with accessResult=" + accessResult);
                    readWriteTypes = ReadWriteTypes.NULL;
                    if (operationRead) {
                        String newValue = accessResult.toString();
//                        editTExtAccessRWXpc.setText(newValue);
                        accXpcValue = newValue;
                    } else {
//                        accXpcValue = editTExtAccessRWXpc.getText().toString();
                    }
                } else if (readWriteTypes == ReadWriteTypes.TID) {
//                    textViewTidOk.setText("O");
//                    checkBoxTid.setChecked(false);
                    readWriteTypes = ReadWriteTypes.NULL;
                    if (accessResult.length() == 0 || operationRead == false) {
                    }
//                    else editTextTidValue.setText(accessResult);
                } else if (readWriteTypes == ReadWriteTypes.USER) {
//                    textViewUserOk.setText("O");
//                    checkBoxUser.setChecked(false);
                    readWriteTypes = ReadWriteTypes.NULL;
                    if (operationRead) {
                        if (DEBUG)
                            MainActivity.mCs108Library4a.appendToLog("AccessReadWrite(). DATA with accessBank = " + accessBank);
//                        editTextUserValue.setText(accessResult);
                        accUserValue = accessResult;
                    } else {
//                        accUserValue = editTextUserValue.getText().toString();
                    }
                } else if (readWriteTypes == ReadWriteTypes.EPC1) {
//                    textViewEpc1Ok.setText("O");
//                    checkBoxEpc1.setChecked(false);
                    readWriteTypes = ReadWriteTypes.NULL;
                    if (accessResult.length() == 0 || operationRead == false) {
                    } else {
//                        editTextEpcValue.setText(accessResult);
                        accEpcValue = accessResult;
                    }
//                    if (operationRead == false) accEpcValue = editTextEpcValue.getText().toString();
                }
                accessResult = null;
            }
            accessTask = null;
            return true;
        }
    }

    boolean bcheckBoxAll = false;

    boolean processTickItems() {
        String writeData = "";
        boolean invalidRequest1 = false;

//        if (checkBoxReserve.isChecked() == true) {
//            textViewReserveOk.setText("");
//            accessBank = 0;
//            accOffset = 0;
//            accSize = 4;
//            readWriteTypes = ReadWriteTypes.RESERVE;
//            if (operationRead) {
//                editTextAccessRWKillPwd.setText("");
//                editTextAccessRWAccPwd.setText("");
//            } else {
//                String strValue = editTextAccessRWKillPwd.getText().toString();
//                String strValue1 = editTextAccessRWAccPwd.getText().toString();
//                if (strValue.length() != 8 || strValue1.length() != 8) {
//                    invalidRequest1 = true;
//                } else {
//                    writeData = strValue + strValue1;
//                }
//            }
//        } else if (checkBoxPc.isChecked() == true || ((checkBoxEpc.isChecked() == true) && (strPCValueRef.length() != 4))) {
//            textViewPcOk.setText("");
//            accessBank = 1;
//            accOffset = 1;
//            accSize = 1;
//            readWriteTypes = ReadWriteTypes.PC;
//            if (operationRead) {
//                editTextAccPc.setText("");
//            } else {
//                String strValue = editTextAccPc.getText().toString();
//                if (strValue.length() != 4) invalidRequest1 = true;
//                else writeData = strValue;
//            }
//        } else
//            if (checkBoxEpc.isChecked() == true) {
//            textViewEpcOk.setText("");
        accessBank = 1;
        accOffset = 1;
        accSize = 0;
        readWriteTypes = ReadWriteTypes.EPC;
        if (DEBUG)
            MainActivity.mCs108Library4a.appendToLog("processTickItems(): start EPC operation");
        if (operationRead) {
            if (strPCValueRef.length() != 4) accSize = 1;
            else {
                accSize = getPC2EpcWordCount(strPCValueRef) + 1;
//                editTextAccessRWEpc.setText("");
            }
        } else {
            String strValue = "3400";
            String strValue1 = this.newTagSelected;
            if (strValue1.length() == 0) {
                if (strValue.length() != 4) invalidRequest1 = true;
                else {
                    accSize = 1;
                    writeData = strValue;
                }
            } else {
                accSize += strValue1.length() / 4;
                if (strValue1.length() % 4 != 0) accSize++;
                if (strValue.length() == 4) {
                    int iPCWordCount = getPC2EpcWordCount(strValue);
                    if (iPCWordCount < accSize) accSize = iPCWordCount;
                    accSize++;
                    writeData = strValue + strValue1;
                } else {
                    accOffset = 2;
                    writeData = strValue1;
                }
            }
        }
//        }
//            else if (checkBoxTid.isChecked() == true) {
//            textViewTidOk.setText("");
//            accessBank = 2;
//            accOffset = 0;
//            accSize = 0;
//            readWriteTypes = ReadWriteTypes.TID;
////            EditText editTextTidValue = (EditText) getActivity().findViewById(R.id.accessRWTidValue);
//            if (operationRead) {
//                int iValue = 0;
//                try {
////                    EditText editTextTidOffset = (EditText) getActivity().findViewById(R.id.accessRWTidOffset);
////                    iValue = Integer.parseInt(editTextTidOffset.getText().toString());
//                } catch (Exception ex) {
//                }
//                accOffset = iValue;
//                iValue = 0;
//                try {
////                    EditText editTextTidLength = (EditText) getActivity().findViewById(R.id.accessRWTidLength);
////                    iValue = Integer.parseInt(editTextTidLength.getText().toString());
//                } catch (Exception ex) {
//                }
//                accSize = iValue;
//                editTextTidValue.setText("");
//            } else {
//                invalidRequest1 = true;
//                editTextTidValue.setText("");
//
//            }
//        } else if (checkBoxUser.isChecked() == true) {
//            textViewUserOk.setText("");
//            accessBank = 3;
//            accOffset = 0;
//            accSize = 0;
//            readWriteTypes = ReadWriteTypes.USER;
//            if (DEBUG)
//                MainActivity.mCs108Library4a.appendToLog("processTickItems(): start USER operation");
//            int iValue = 0;
//            try {
////                EditText editTextTidOffset = (EditText) getActivity().findViewById(R.id.accessRWUserOffset);
////                iValue = Integer.parseInt(editTextTidOffset.getText().toString());
//            } catch (Exception ex) {
//            }
//            accOffset = iValue;
//            iValue = 0;
//            try {
////                EditText editTextUserLength = (EditText) getActivity().findViewById(R.id.accessRWUserLength);
////                iValue = Integer.parseInt(editTextUserLength.getText().toString());
//            } catch (Exception ex) {
//            }
//            accSize = iValue;
//            if (operationRead) {
//                editTextUserValue.setText("");
//            } else {
//                String strValue = editTextUserValue.getText().toString();
//                if (strValue.length() >= 4 && strValue.matches(accUserValue) == false) {
//                    accSize = strValue.length() / 4;
//                    if (strValue.length() % 4 != 0) accSize++;
//                    writeData = strValue;
//                }
//            }
//        } else if (checkBoxEpc1.isChecked() == true) {
//            textViewEpc1Ok.setText("");
//            accessBank = spinnerRWSelectEpc1.getSelectedItemPosition();
//            accOffset = 0;
//            accSize = 0;
//            readWriteTypes = ReadWriteTypes.EPC1;
//            int iValue = 0;
//            try {
////                EditText editTextEpcOffset = (EditText) getActivity().findViewById(R.id.accessRWEpcOffset);
////                iValue = Integer.parseInt(editTextEpcOffset.getText().toString());
//            } catch (Exception ex) {
//            }
//            accOffset = iValue;
//            iValue = 0;
//            try {
////                EditText editTextEpcLength = (EditText) getActivity().findViewById(R.id.accessRWEpcLength);
////                iValue = Integer.parseInt(editTextEpcLength.getText().toString());
//            } catch (Exception ex) {
//            }
//            accSize = iValue;
//            if (operationRead) {
//                editTextEpcValue.setText("");
//            } else {
//                String strValue = editTextEpcValue.getText().toString();
//                if (strValue.length() >= 4 && strValue.matches(accEpcValue) == false) {
//                    accSize = strValue.length() / 4;
//                    if (strValue.length() % 4 != 0) accSize++;
//                    writeData = strValue;
//                }
//            }
//        } else {
//            invalidRequest1 = true;
//            bcheckBoxAll = true;
//        }

        if (restartAccessBank == accessBank) {
            if (restartCounter == 0) invalidRequest1 = true;
            else restartCounter--;
        }
        if (invalidRequest1 == false) {
//                if (MainActivity.mCs108Library4a.setFixedQParms(0, -1, false) == false) {
//                    invalidRequest1 = true;
//                }
        }
        if (invalidRequest1 == false) {
            if (MainActivity.mCs108Library4a.setAccessBank(accessBank) == false) {
                invalidRequest1 = true;
            }
        }
        if (invalidRequest1 == false) {
            if (MainActivity.mCs108Library4a.setAccessOffset(accOffset) == false) {
                invalidRequest1 = true;
            }
        }
        if (invalidRequest1 == false) {
            if (accSize == 0) {
                invalidRequest1 = true;
            } else if (MainActivity.mCs108Library4a.setAccessCount(accSize) == false) {
                invalidRequest1 = true;
            }
        }
        if (invalidRequest1 == false && operationRead == false) {
                /*if (invalidRequest1 == false) {
                    if (MainActivity.mCs108Library4a.mRfidDevice.mRx000Device.mRx000Setting.setAccessWriteDataSelect(0) == false)
                        invalidRequest1 = true;
                }*/
            if (invalidRequest1 == false) {
                if (DEBUG)
                    MainActivity.mCs108Library4a.appendToLog("AccessReadWriteFragment().writeData = " + writeData);
                if (MainActivity.mCs108Library4a.setAccessWriteData(writeData) == false) {
                    invalidRequest1 = true;
                }
            }
            //if (operationWrite == true) return true;
        }
        return invalidRequest1;
    }

    int getPC2EpcWordCount(String detail) {
        String detail2 = detail.substring(0, 1);
        int number2 = Integer.valueOf(detail2, 16) * 2;
        String detail3 = detail.substring(1, 2);
        int number3 = Integer.valueOf(detail3, 16);
        if ((number3 / 8) != 0) number2 += 1;
        return number2;
    }

    void updatePCEpc(String strPCValue, String strEpcValue) {
        boolean needPopup = false;
        if (strPCValue == null) strPCValue = "";
//        if (strPCValue.length() != 0) editTextAccPc.setText(strPCValue);
//        else strPCValue = strPCValueRef;
        if (strPCValueRef != null && strPCValue != null) {
            if (strPCValue.matches(strPCValueRef) == false && strPCValue.length() == 4) {
                needPopup = true;
                strPCValueRef = strPCValue;
            }
        }

        int iWordCount = getPC2EpcWordCount(strPCValue);
        if (strEpcValue != null) {
            this.tagListSelected.setAddress(strEpcValue);//put("address", strEpcValue);

//            tagSelected.setAddress(strEpcValue);
//            if (spinnerSelectBank.getSelectedItemPosition() == 0)
//                editTextRWTagID.setText(strEpcValue);
//            editTextAccessRWEpc.setText(strEpcValue);
        } else {
            if (iWordCount * 4 < this.tagListSelected.getAddress().length()) {
                needPopup = true;
                String strTemp = strEpcValue.substring(0, iWordCount * 4);
                tagSelected.setAddress(strEpcValue);
//                if (spinnerSelectBank.getSelectedItemPosition() == 0)
//                    editTextRWTagID.setText(strTemp);
            }
            if (iWordCount * 4 < strEpcValue.length()) {
                needPopup = true;
                String strTemp = strEpcValue.substring(0, iWordCount * 4);
//                editTextAccessRWEpc.setText(strTemp);
            }
//            if (editTextAccessRWEpc.getText().toString().length() != 0) {
//                String strTemp = editTextAccessRWEpc.getText().toString();
//                if (editTextRWTagID.getText().toString().matches(strTemp) == false) {
//                    // needPopup = true;
//                    tagSelected.setAddress(strEpcValue);
//                    if (spinnerSelectBank.getSelectedItemPosition() == 0)
//                        editTextRWTagID.setText(strTemp);
//                }
//            }
        }
//        editTextAccessRWEpc.addTextChangedListener(new GenericTextWatcher(editTextAccessRWEpc, iWordCount * 4));
//        String strTemp = editTextAccessRWEpc.getText().toString();
//        editTextAccessRWEpc.setText(strTemp);

        if (needPopup) {
            SaveList2ExternalTask.CustomPopupWindow customPopupWindow = new SaveList2ExternalTask.CustomPopupWindow(MainActivity.mContext);
            customPopupWindow.popupStart("Changing EPC Length will automatically modify to " + (iWordCount * 16) + " bits.", false);
        }
    }

}
