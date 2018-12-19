package com.hwbluesky.weimai.wificonnectesp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.espressif.iot.esptouch.EsptouchTask;
import com.espressif.iot.esptouch.IEsptouchListener;
import com.espressif.iot.esptouch.IEsptouchResult;
import com.espressif.iot.esptouch.IEsptouchTask;
import com.espressif.iot.esptouch.task.__IEsptouchTask;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "WifiConnectActivity";

    private EditText mTvApSsid;
    private TextView mTvNoWifi;
    private EditText mEdtApPassword;
    private Button mBtnConfirm;

    private EspWifiAdminSimple mWifiAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWifiAdmin = new EspWifiAdminSimple(this);
        mTvApSsid = (EditText) findViewById(R.id.tvApSssidConnected);
        mTvNoWifi = (TextView) findViewById(R.id.tvNoWifi);
        mEdtApPassword = (EditText) findViewById(R.id.edtApPassword);
        mBtnConfirm = (Button) findViewById(R.id.btnConfirm);
        mBtnConfirm.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view == mBtnConfirm) {

            String apSsid = mWifiAdmin.getWifiConnectedSsid();
            if(apSsid != null){

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Confirm");
                builder.setMessage("The device is waiting to configure the WiFi phase");
                builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();

                        String apSsid = mTvApSsid.getText().toString();
                        String apPassword = mEdtApPassword.getText().toString();
                        String apBssid = mWifiAdmin.getWifiConnectedBssid();
                        Boolean isSsidHidden = false;
                        String isSsidHiddenStr = "NO";
                        if (isSsidHidden)
                        {
                            isSsidHiddenStr = "YES";
                        }
                        if (__IEsptouchTask.DEBUG) {
                            Log.d(TAG, "mBtnConfirm is clicked, mEdtApSsid = " + apSsid
                                    + ", " + " mEdtApPassword = " + apPassword);
                        }
                        new EsptouchAsyncTask3().execute(apSsid, apBssid, apPassword,
                                isSsidHiddenStr);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        if (__IEsptouchTask.DEBUG) {
                            Log.d(TAG, "Canceled" );
                        }
                    }
                });
                builder.create().show();
            }
            else {
                String title = "Tip";
                String message = "Please connect your phone to WiFi first";
                DialogUtil.showAlertDialog(MainActivity.this,title,message);
            }


        }
    }

    @Override
    protected void onResume() {
        super.onResume();


        // display the connected ap's ssid
        String apSsid = mWifiAdmin.getWifiConnectedSsid();
        if (apSsid != null) {
            mTvApSsid.setText(apSsid);
            mTvNoWifi.setVisibility(View.GONE);
        } else {
            mTvApSsid.setText("");
            mTvNoWifi.setVisibility(View.VISIBLE);
        }
    }

    private void onEsptoucResultAddedPerform(final IEsptouchResult result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String title = "Connected";
                DialogUtil.showAlertDialog(MainActivity.this,title,"");
            }

        });
    }

    private IEsptouchListener myListener = new IEsptouchListener() {

        @Override
        public void onEsptouchResultAdded(final IEsptouchResult result) {
            onEsptoucResultAddedPerform(result);
        }
    };

    private class EsptouchAsyncTask3 extends AsyncTask<String,Void,List<IEsptouchResult>> {
        private ProgressDialog mProgressDialog;

        private IEsptouchTask mEsptouchTask;
        // without the lock, if the user tap confirm and cancel quickly enough,
        // the bug will arise. the reason is follows:
        // 0. task is starting created, but not finished
        // 1. the task is cancel for the task hasn't been created, it do nothing
        // 2. task is created
        // 3. Oops, the task should be cancelled, but it is running
        private final Object mLock = new Object();

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog
                    .setMessage("Waiting...");
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    synchronized (mLock) {
                        if (__IEsptouchTask.DEBUG) {
                            Log.i(TAG, "progress dialog is canceled");
                        }
                        if (mEsptouchTask != null) {
                            mEsptouchTask.interrupt();
                        }
                    }
                }
            });
            mProgressDialog.show();
        }

        @Override
        protected List<IEsptouchResult> doInBackground(String... params) {
            int taskResultCount = -1;
            synchronized (mLock) {
                // !!!NOTICE
                String apSsid = mWifiAdmin.getWifiConnectedSsidAscii(params[0]);
                String apBssid = params[1];
                String apPassword = params[2];
                String isSsidHiddenStr = params[3];
                boolean isSsidHidden = false;
                if (isSsidHiddenStr.equals("YES")) {
                    isSsidHidden = true;
                }
                taskResultCount = 1;
                mEsptouchTask = new EsptouchTask(apSsid, apBssid, apPassword,
                        isSsidHidden,MainActivity.this);
                mEsptouchTask.setEsptouchListener(myListener);
            }
            List<IEsptouchResult> resultList = mEsptouchTask.executeForResults(taskResultCount);
            return resultList;
        }

        @Override
        protected void onPostExecute(List<IEsptouchResult> result) {
            mProgressDialog.dismiss();

            IEsptouchResult firstResult = result.get(0);
            // check whether the task is cancelled and no results received
            if (!firstResult.isCancelled()) {
                int count = 0;
                // max results to be displayed, if it is more than maxDisplayCount,
                // just show the count of redundant ones
                final int maxDisplayCount = 5;
                // the task received some results including cancelled while
                // executing before receiving enough results
                if (firstResult.isSuc()) {
                    StringBuilder sb = new StringBuilder();
                    for (IEsptouchResult resultInList : result) {
                        sb.append("Esptouch success, bssid = "
                                + resultInList.getBssid()
                                + ",InetAddress = "
                                + resultInList.getInetAddress()
                                .getHostAddress() + "\n");
                        count++;
                        if (count >= maxDisplayCount) {
                            break;
                        }
                    }
                    if (count < result.size()) {
                        sb.append("\nthere's " + (result.size() - count)
                                + " more result(s) without showing\n");
                    }
                } else {
                    DialogUtil.showAlertDialog(MainActivity.this,"Failed","");
                }
            }
        }
    }
}
