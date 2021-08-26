package com.dji.uxsdkdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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

import dji.common.error.DJIError;
import dji.common.mission.activetrack.ActiveTrackMissionEvent;
import dji.common.realname.AircraftBindingState;
import dji.common.realname.AppActivationState;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.activetrack.ActiveTrackMissionOperatorListener;
import dji.sdk.realname.AppActivationManager;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.sdkmanager.LiveStreamManager;
import dji.sdk.useraccount.UserAccountManager;

public class HomeActivity extends DemoBaseActivity implements ActiveTrackMissionOperatorListener, View.OnClickListener {

    private static final String TAG = HomeActivity.class.getName();
    public boolean isLogin=false;
    public boolean isConnect=false;

    private AppActivationManager appActivationManager;
    private AppActivationState.AppActivationStateListener activationStateListener;

    //飞机绑定状态监听器
    private AircraftBindingState.AircraftBindingStateListener bindingStateListener;

    protected Button loginBtn;
    protected Button beginFlyBtn;
    protected Button setting;
    protected TextView bindingStateTV;
    protected TextView appActivationStateTV;
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


        setContentView(R.layout.activity_home);

        initUI();
        initData();
    }
    private void initUI(){


        bindingStateTV = (TextView) findViewById(R.id.tv_binding_state_info);
        appActivationStateTV = (TextView) findViewById(R.id.tv_activation_state_info);


        loginBtn = (Button) findViewById(R.id.btn_login);
        loginBtn.setOnClickListener(this);
        setting = (Button) findViewById(R.id.btn_setting);
        setting.setOnClickListener(this);
        beginFlyBtn = (Button) findViewById(R.id.begin_fly);
        beginFlyBtn.setOnClickListener(this);
    }


    private String userName;
    private String userPassword;
    private EditText view_url;
    private EditText userPasswordEdit;
    private View alertDialogView;

    private void setting(View v){
        SharedPreferences sharedPreferences =
                getSharedPreferences("setting", Context.MODE_PRIVATE);

        AlertDialog.Builder loginAlertDialog = new AlertDialog.Builder (HomeActivity.this);
        alertDialogView = getLayoutInflater ().inflate (R.layout.alertdialog_layout, null, false);
        loginAlertDialog.setView (alertDialogView);



        final EditText et = alertDialogView.findViewById (R.id.view_url);
        et.setText(sharedPreferences.getString("url", "请输入直播地址!"));
        loginAlertDialog.setPositiveButton ("OK", new DialogInterface.OnClickListener () {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                view_url = alertDialogView.findViewById (R.id.view_url);

                SharedPreferences sharedPreferences = getSharedPreferences("setting", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();//获取编辑器
                editor.putString("url", view_url.getText ().toString ());
                editor.commit();//提交修改

//                userPasswordEdit = alertDialogView.findViewById (R.id.nian_text);
//                userName = view_url.getText ().toString ();
//                userPassword = userPasswordEdit.getText ().toString ();
            }
        });

        loginAlertDialog.show ();
    }


    private void loginAccount(){

        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Toast.makeText(getApplicationContext(),"登录成功!",Toast.LENGTH_LONG).show();
                        if(userAccountState.name().equals("AUTHORIZED")) {
                            handler.sendMessage(handler.obtainMessage(COMPLETED, "Login"));
                        }
                    }
                    @Override
                    public void onFailure(DJIError error) {
                        Toast.makeText(getApplicationContext(),"登录失败:"
                                + error.getDescription(),Toast.LENGTH_LONG).show();

                    }
                });

    }



    private static final int COMPLETED = 0;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == COMPLETED) {
                if(msg.obj.toString().equals("Login")) {
                    appActivationStateTV.setText("已登录"); //UI更改操作
                    isLogin=true;
//                    loginBtn.setText("注销");
                }
                else {
                    appActivationStateTV.setText("未登录");
//                    loginBtn.setText("登录");
                }
            }
        }
    };

    private void initData(){
        setUpListener();

        appActivationManager = DJISDKManager.getInstance().getAppActivationManager();

        if (appActivationManager != null) {
            appActivationManager.addAppActivationStateListener(activationStateListener);
            appActivationManager.addAircraftBindingStateListener(bindingStateListener);
            HomeActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    appActivationStateTV.setText("" + (appActivationManager.getAppActivationState().equals(AppActivationState.ACTIVATED)?"已登录":"未登录"));
                    bindingStateTV.setText( ""+(appActivationManager.getAircraftBindingState().equals(AircraftBindingState.BOUND)?"已连接无人机":"未连接无人机"));
                }
            });
        }
    }


    private void setUpListener() {
        // Example of Listener
        activationStateListener = new AppActivationState.AppActivationStateListener() {
            @Override
            public void onUpdate(final AppActivationState appActivationState) {
                HomeActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        appActivationStateTV.setText(  (appActivationState.equals(AppActivationState.ACTIVATED)?"已登录":"未登录"));
                    }
                });
            }
        };

        bindingStateListener = new AircraftBindingState.AircraftBindingStateListener() {

            @Override
            public void onUpdate(final AircraftBindingState bindingState) {
                HomeActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        bindingStateTV.setText(bindingState.equals(AircraftBindingState.BOUND)?"已连接无人机":"未连接无人机");
                    }
                });
            }
        };
    }
    public static void startActivity(Context context, Class activity) {
        Intent intent = new Intent(context, activity);
        context.startActivity(intent);
    }
    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btn_login:{
                loginAccount();
                break;
            }
            case R.id.begin_fly:{
                if(isLogin)
                startActivity(HomeActivity.this, MainActivity.class);
                else {
                   ToastUtils("请先进行登录!");
                }
                break;
            }
            case R.id.btn_setting:{
                setting(v);
                break;
            }
            default:
                break;
        }
    }

    @Override
    public void onUpdate(ActiveTrackMissionEvent activeTrackMissionEvent) {

    }
    public void  ToastUtils(String string){
        Toast.makeText(getApplicationContext(),string,Toast.LENGTH_LONG).show();
    }

    private void tearDownListener() {
        if (activationStateListener != null) {
            appActivationManager.removeAppActivationStateListener(activationStateListener);
            HomeActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    appActivationStateTV.setText("Unknown");
                }
            });
        }
        if (bindingStateListener !=null) {
            appActivationManager.removeAircraftBindingStateListener(bindingStateListener);
            HomeActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    bindingStateTV.setText("Unknown");
                }
            });
        }
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
}