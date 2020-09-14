package com.nocola.cordova.plugin;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;

import com.csl.cs108library4a.Cs108Library4A;
import com.csl.cs108library4a.ReaderDevice;
import com.csl.cs108library4a.R;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;

import java.util.ArrayList;

public class ReaderListAdapter extends ArrayAdapter<ReaderDevice> {
    final boolean DEBUG = false;
    private final Context context;
    private final int resourceId;
    private final ArrayList<ReaderDevice> readersList;
    private boolean select4detail, select4Rssi, selectDupElim, select4Extra1, select4Extra2;

    private static CordovaPlugin that;
    public static Context mContext = that.cordova.getActivity();
    private static CordovaInterface layout;
    public static TextView mLogView = new TextView(layout.getContext());
    public static Cs108Library4A mCs108Library4a = new Cs108Library4A(mContext, mLogView);

    public static class Config {
        public String configPassword, configPower, config0, config1, config2;
    }

    ;
    public static Config config = new Config();

    public ReaderListAdapter(Context context, int resourceId, ArrayList<ReaderDevice> readersList, boolean select4detail, boolean select4Rssi) {
        super(context, resourceId, readersList);
        this.context = context;
        this.resourceId = resourceId;
        this.readersList = readersList;
        this.select4detail = select4detail;
        this.select4Rssi = select4Rssi;
        select4Extra1 = false;
        select4Extra2 = false;
    }

    public ReaderListAdapter(Context context, int resourceId, ArrayList<ReaderDevice> readersList, boolean select4detail, boolean select4Rssi, boolean selectDupElim, boolean select4Extra1, boolean select4Extra2) {
        super(context, resourceId, readersList);
        this.context = context;
        this.resourceId = resourceId;
        this.readersList = readersList;
        this.select4detail = select4detail;
        this.select4Rssi = select4Rssi;
        this.selectDupElim = selectDupElim;
        this.select4Extra1 = select4Extra1;
        this.select4Extra2 = select4Extra2;
        mCs108Library4a.appendToLog("select4Extra1 = " + select4Extra1 + ", select4Extra2 = " + select4Extra2);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ReaderDevice reader = readersList.get(position);
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(resourceId, null);
        }

        CheckedTextView checkedTextView = (CheckedTextView) convertView.findViewById(that.cordova.getActivity().getApplication().getResources().getIdentifier("reader_checkedtextview", "id", that.cordova.getActivity().getApplication().getPackageName()));
        String text1 = "";
        if (reader.getName() != null) {
            if (reader.getName().length() != 0) {
                text1 += reader.getName();
            }
        }
        if (reader.getAddress() != null) {
            if (reader.getAddress().length() != 0) {
                if (text1.length() != 0) text1 += "\n";
                text1 += reader.getAddress();
            }
        }
        checkedTextView.setText(text1);
        if (reader.getSelected()) {
            checkedTextView.setChecked(true);
        } else {
            checkedTextView.setChecked(false);
        }

        TextView countTextView = (TextView) convertView.findViewById(that.cordova.getActivity().getApplication().getResources().getIdentifier("reader_count", "id", that.cordova.getActivity().getApplication().getPackageName()));
        if (reader.getCount() != 0) {
            countTextView.setText(String.valueOf(reader.getCount()));
        } else {
            countTextView.setVisibility(View.GONE);
        }

        if (select4Rssi) {
            TextView rssiTextView = (TextView) convertView.findViewById(that.cordova.getActivity().getApplication().getResources().getIdentifier("reader_rssi", "id", that.cordova.getActivity().getApplication().getPackageName()));
            rssiTextView.setVisibility(View.VISIBLE);
            double rssiValue = reader.getRssi();
            if (mCs108Library4a.getRssiDisplaySetting() != 0 && rssiValue > 0)
                rssiValue -= 106.98;
            rssiTextView.setText(String.format("%.1f", rssiValue));
        }

        if (select4Extra1) {
            TextView portTextView = (TextView) convertView.findViewById(that.cordova.getActivity().getApplication().getResources().getIdentifier("reader_extra1", "id", that.cordova.getActivity().getApplication().getPackageName()));
            portTextView.setVisibility(View.VISIBLE);
            int portValue = reader.getPort() + 1;
            portTextView.setText(String.valueOf(portValue));
        }

        if (select4Extra2) {
            TextView portTextView = (TextView) convertView.findViewById(that.cordova.getActivity().getApplication().getResources().getIdentifier("reader_extra2", "id", that.cordova.getActivity().getApplication().getPackageName()));
            portTextView.setVisibility(View.VISIBLE);
            int codeStatus = reader.getStatus();
            int codeSensor = reader.getCodeSensor();
            int codeRssi = reader.getCodeRssi();
            float codeTempC = reader.getCodeTempC();
            String brand = reader.getBrand();
            String strExtra = "";
            if (codeStatus > reader.INVALID_STATUS) { //for Bap tags
                int portstatus = reader.getStatus();
                if (portstatus > reader.INVALID_STATUS) {
                    if ((portstatus & 2) == 0) strExtra += "Bat OK";
                    else strExtra += "Bat NG";
                    if ((portstatus & 4) != 0) strExtra += "\nTemper NG";
                }
            } else if (codeSensor > reader.INVALID_CODESENSOR && codeRssi > reader.INVALID_CODERSSI) { //for Axzon/Magnus tags
                strExtra = "SC=" + String.format("%d", codeSensor);
                int ocrssiMin = -1;
                int ocrssiMax = -1;
                boolean bValidOcrssi = false;
                ocrssiMax = Integer.parseInt(config.config1);
                ocrssiMin = Integer.parseInt(config.config2);
                if (ocrssiMax > 0 && ocrssiMin > 0 && (codeRssi > ocrssiMax || codeRssi < ocrssiMin))
                    strExtra += ("\n<font color=red>OCRSSI=" + String.format("%d", codeRssi) + "</font>");
                else {
                    bValidOcrssi = true;
                    strExtra += ("\nOCRSSI=" + String.format("%d", codeRssi));
                }
                if (codeTempC > reader.INVALID_CODETEMPC) {
                    if (bValidOcrssi || portTextView.getText().toString().indexOf("T=") >= 0)
                        strExtra += ("\nT=" + String.format("%.1f", codeTempC) + (char) 0x00B0 + "C");
                }
                int backport = reader.getBackport1();
                if (backport > reader.INVALID_BACKPORT)
                    strExtra += String.format("\nBP1=%d", backport);
                backport = reader.getBackport2();
                if (backport > reader.INVALID_BACKPORT)
                    strExtra += String.format("\nBP2=%d", backport);
            } else if (codeTempC > reader.INVALID_CODETEMPC) { //for Ctesius tags
                strExtra = ("T=" + String.format("%.1f", codeTempC) + (char) 0x00B0 + "C");
            } else if (reader.getDetails().contains("E2806894")) { //for code8 tags
                strExtra = ((brand != null) ? ("Brand=" + brand) : "");
            } else if (reader.getSensorData() < reader.INVALID_SENSORDATA) {
                strExtra = "SD=" + String.valueOf(reader.getSensorData());
            }
            if (strExtra.length() != 0) portTextView.setText(Html.fromHtml(strExtra));
        }

        TextView readerDetailA = (TextView) convertView.findViewById(that.cordova.getActivity().getApplication().getResources().getIdentifier("reader_detailA", "id", that.cordova.getActivity().getApplication().getPackageName()));
        TextView readerDetailB = (TextView) convertView.findViewById(that.cordova.getActivity().getApplication().getResources().getIdentifier("reader_detailB", "id", that.cordova.getActivity().getApplication().getPackageName()));
        if (reader.isConnected() || checkedTextView.isChecked() || select4detail == false) {
            readerDetailA.setText(reader.getDetails());
            readerDetailB.setText("");
            if (reader.isConnected()) {
                readerDetailB.setText("Connected");
            } else {
                int channel = reader.getChannel();
                int phase = reader.getPhase();
                String stringDetailB = null;
                if (channel != 0 || phase != 0) {
                    double dChannel = mCs108Library4a.getLogicalChannel2PhysicalFreq(reader.getChannel());
                    stringDetailB = "Phase=" + phase + "\n" + dChannel + "MHz";
                }
                if (stringDetailB != null) readerDetailB.setText(stringDetailB);
            }
            if (readerDetailA.getText().toString().length() != 0 || readerDetailB.getText().toString().length() != 0) {
                readerDetailA.setVisibility(View.VISIBLE);
                readerDetailB.setVisibility(View.VISIBLE);
            } else {
                readerDetailA.setVisibility(View.GONE);
                readerDetailB.setVisibility(View.GONE);
            }
        } else {
            readerDetailA.setVisibility(View.GONE);
            readerDetailB.setVisibility(View.GONE);
        }
        return convertView;
    }

    public boolean getSelectDupElim() {
        return selectDupElim;
    }

    public void setSelectDupElim(boolean selectDupElim) {
        this.selectDupElim = selectDupElim;
    }
}
