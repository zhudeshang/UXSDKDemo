package com.dji.uxsdkdemo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import dji.common.error.DJIError;
import dji.common.realname.AircraftBindingState;
import dji.common.realname.AppActivationState;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.realname.AppActivationManager;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.sdkmanager.LiveStreamManager;
import dji.sdk.useraccount.UserAccountManager;



public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String liveShowUrl = "please input your live show url here";


    private static final String TAG = MainActivity.class.getName();



    private AppActivationManager appActivationManager;
    private AppActivationState.AppActivationStateListener activationStateListener;
    protected Button loginBtn;
    protected Button way_point1;
    protected Button appActivationStateTV;
    private AircraftBindingState.AircraftBindingStateListener bindingStateListener;


    private LiveStreamManager.OnLiveChangeListener listener;
    private LiveStreamManager.LiveStreamVideoSource currentVideoSource = LiveStreamManager.LiveStreamVideoSource.Primary;

    private EditText showUrlInputEdit;
    private Button startLiveShowBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 当编译和目标版本高于 22 时，在运行时请求
        // 以下权限以确保
        // SDK 正常工作
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }

        setContentView(R.layout.activity_main);
        initUI();
        initData();
    }
    private void initUI(){

        way_point1 = (Button) findViewById(R.id.way_point1);
        way_point1.setOnClickListener(this);

        appActivationStateTV = (Button) findViewById(R.id.tv_activation_state_info);
        loginBtn = (Button) findViewById(R.id.btn_login);
        loginBtn.setOnClickListener(this);

        showUrlInputEdit = (EditText) findViewById(R.id.edit_live_show_url_input);
        showUrlInputEdit.setText(liveShowUrl);

        startLiveShowBtn = (Button) findViewById(R.id.btn_start_live_show);
        startLiveShowBtn.setOnClickListener(this);
    }
    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btn_start_live_show:
                startLiveShow();
                break;
            case R.id.way_point1:{
                startActivity(MainActivity.this, Waypoint1Activity.class);
                break;
            }
            case R.id.btn_login:{
                login();
                break;
            }
//            case R.id.btn_setting:{
//                setting();
//                break;
//            }
            default:
                break;
        }
    }

    public static void startActivity(Context context, Class activity) {
        Intent intent = new Intent(context, activity);
        context.startActivity(intent);
    }



    private void initData(){
        setUpListener();

        appActivationManager = DJISDKManager.getInstance().getAppActivationManager();

        if (appActivationManager != null) {
            appActivationManager.addAppActivationStateListener(activationStateListener);
            appActivationManager.addAircraftBindingStateListener(bindingStateListener);
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    appActivationStateTV.setText("" + appActivationManager.getAppActivationState());
                }
            });
        }
        listener = new LiveStreamManager.OnLiveChangeListener() {
            @Override
            public void onStatusChanged(int i) {
                ToastUtils("status changed : " + i);
            }
        };
    }
    private void setUpListener() {
        // Example of Listener
        activationStateListener = new AppActivationState.AppActivationStateListener() {
            @Override
            public void onUpdate(final AppActivationState appActivationState) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        appActivationStateTV.setText("" + appActivationState);
                    }
                });
            }
        };

        bindingStateListener = new AircraftBindingState.AircraftBindingStateListener() {

            @Override
            public void onUpdate(final AircraftBindingState bindingState) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        bindingStateTV.setText("" + bindingState);
                    }
                });
            }
        };

        if (isLiveStreamManagerOn()){
            DJISDKManager.getInstance().getLiveStreamManager().registerListener(listener);
        }
    }
    private boolean isLiveStreamManagerOn() {
        if (DJISDKManager.getInstance().getLiveStreamManager() == null) {
            Toast.makeText(getApplicationContext(),"No live stream manager!",Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }
    private void tearDownListener() {
        if (activationStateListener != null) {
            appActivationManager.removeAppActivationStateListener(activationStateListener);
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    appActivationStateTV.setText("Unknown");
                }
            });
        }

        if (bindingStateListener !=null) {
            appActivationManager.removeAircraftBindingStateListener(bindingStateListener);
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    bindingStateTV.setText("Unknown");
                }
            });
        }

        if (isLiveStreamManagerOn()){
            DJISDKManager.getInstance().getLiveStreamManager().unregisterListener(listener);
        }
    }
    public boolean isLog=false;
    private void  login(){
       if(isLog){
           logoutAccount();
       }else {
           loginAccount();
       }
    }
    private void loginAccount(){

        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Toast.makeText(getApplicationContext(),"Login Success",Toast.LENGTH_LONG).show();
//                        appActivationStateTV.setText("Login");
                        if(userAccountState.name().equals("AUTHORIZED")) {
                            handler.sendMessage(handler.obtainMessage(COMPLETED, "Login"));
                        }
                    }
                    @Override
                    public void onFailure(DJIError error) {
                        Toast.makeText(getApplicationContext(),"Login Error:"
                                + error.getDescription(),Toast.LENGTH_LONG).show();

                    }
                });

    }
    private void logoutAccount(){
        UserAccountManager.getInstance().logoutOfDJIUserAccount(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (null == error) {
                    Toast.makeText(getApplicationContext(),"Login Success",Toast.LENGTH_LONG).show();
                    Log.d(TAG,"Logout Success");
                    handler.sendMessage(handler.obtainMessage(COMPLETED, "Logout"));

                } else {
                    Toast.makeText(getApplicationContext(),"Logout Error:"
                            + error.getDescription(),Toast.LENGTH_LONG).show();
                    Log.d(TAG,"Logout Error:"
                            + error.getDescription());
                }
            }
        });
    }
    private static final int COMPLETED = 0;
    private static final int LiveStre = 0;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == COMPLETED) {
                if(msg.obj.toString().equals("Login")) {
                    appActivationStateTV.setText("已登录"); //UI更改操作
                    loginBtn.setText("注销");
                }
                else {
                    appActivationStateTV.setText("未登录");
                    loginBtn.setText("登录");
                }
            }
            else if (msg.what == LiveStre){
                if(msg.obj.toString().equals("0")) {
                    Toast.makeText(getApplicationContext(),"开始直播!",Toast.LENGTH_LONG).show();
                }
            }
        }
    };
//    private void setting(){
//        Intent intent = new Intent(this, Setting.class);
//        startActivity(intent);
//    }
    void startLiveShow() {
    ToastUtils("Start Live Show");
    if (!isLiveStreamManagerOn()) {
        return;
    }
    if (DJISDKManager.getInstance().getLiveStreamManager().isStreaming()) {
        ToastUtils("already started!");
        return;
    }
    new Thread() {
        @Override
        public void run() {
            DJISDKManager.getInstance().getLiveStreamManager().setLiveUrl(liveShowUrl);
            int result = DJISDKManager.getInstance().getLiveStreamManager().startStream();

            handler.sendMessage(handler.obtainMessage(LiveStre, result));

            DJISDKManager.getInstance().getLiveStreamManager().setStartTime();
        }
    }.start();
}

    private void changeVideoSource() {
        if (!isLiveStreamManagerOn()) {
            return;
        }
        if (DJISDKManager.getInstance().getLiveStreamManager().isStreaming()) {
            ToastUtils("在更改直播源之前，您应该停止直播!");
            return;
        }
        currentVideoSource = (currentVideoSource == LiveStreamManager.LiveStreamVideoSource.Primary) ?
                LiveStreamManager.LiveStreamVideoSource.Secoundary :
                LiveStreamManager.LiveStreamVideoSource.Primary;
        DJISDKManager.getInstance().getLiveStreamManager().setVideoSource(currentVideoSource);

        ToastUtils("改变成功!直播源: " + currentVideoSource.name());
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        setUpListener();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
//        tearDownListener();
        super.onDestroy();
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    public void onReturn(View view){
        Log.e(TAG, "onReturn");
        this.finish();
    }

    public void  ToastUtils(String string){
        Toast.makeText(getApplicationContext(),string,Toast.LENGTH_LONG).show();
    }
}

