package com.dji.uxsdkdemo;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SlidingDrawer;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.mission.activetrack.ActiveTrackMission;
import dji.common.mission.activetrack.ActiveTrackMissionEvent;
import dji.common.mission.activetrack.ActiveTrackMode;
import dji.common.mission.activetrack.ActiveTrackState;
import dji.common.mission.activetrack.ActiveTrackTargetState;
import dji.common.mission.activetrack.ActiveTrackTrackingState;
import dji.common.mission.activetrack.QuickShotMode;
import dji.common.mission.activetrack.SubjectSensingState;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.realname.AircraftBindingState;
import dji.common.realname.AppActivationState;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.ActionCallback;
import dji.keysdk.callback.SetCallback;
import dji.midware.media.DJIVideoDataRecver;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.activetrack.ActiveTrackMissionOperatorListener;
import dji.sdk.mission.activetrack.ActiveTrackOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import com.amap.api.maps2d.AMap.OnMapClickListener;
import dji.sdk.products.Aircraft;
import dji.sdk.realname.AppActivationManager;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.sdkmanager.LiveStreamManager;
import dji.sdk.useraccount.UserAccountManager;
import dji.ux.widget.MapWidget;


public class MainActivity extends DemoBaseActivity implements TextureView.SurfaceTextureListener, OnMapClickListener,CompoundButton.OnCheckedChangeListener, ActiveTrackMissionOperatorListener, View.OnTouchListener, View.OnClickListener {

    private String liveShowUrl = "please input your live show url here";


    private static final String TAG = MainActivity.class.getName();



    private AppActivationManager appActivationManager;
    private AppActivationState.AppActivationStateListener activationStateListener;
//    protected Button loginBtn;
    protected Button way_point1;
    protected Button open_track;
//    protected Button appActivationStateTV;
    //飞机绑定状态监听器
    private AircraftBindingState.AircraftBindingStateListener bindingStateListener;


    private LiveStreamManager.OnLiveChangeListener listener;
    private LiveStreamManager.LiveStreamVideoSource currentVideoSource = LiveStreamManager.LiveStreamVideoSource.Primary;

    private EditText showUrlInputEdit;
    private Button startLiveShowBtn;

    private ImageButton mStopBtn;
    private Button mConfirmBtn;
    private Button mRejectBtn;
    private ImageView mTrackingImage;
    private ImageView mSendRectIV;

    private RelativeLayout mBgLayout;
    private RelativeLayout.LayoutParams layoutParams;


    private MapView mapView;
    private AMap aMap;

    private Button locate, add, clear;
    private Button config, upload, start, stop;

    private boolean isAdd = false;

    private double droneLocationLat = 181, droneLocationLng = 181;
    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();
    private Marker droneMarker = null;

    private float altitude = 100.0f;
    private float mSpeed = 10.0f;

    private List<Waypoint> waypointList = new ArrayList<>();

    public static WaypointMission.Builder waypointMissionBuilder;
    private FlightController mFlightController;
    private WaypointMissionOperator instance;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.AUTO;


    public MainActivity() {
    }

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


        IntentFilter filter = new IntentFilter();
        filter.addAction(DemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);


        initMapView();
        addListener();
        onProductConnectionChange();
    }
    private void initUI(){

        way_point1 = (Button) findViewById(R.id.way_point1);
        way_point1.setOnClickListener(this);
        open_track = (Button) findViewById(R.id.btn_open_track);
        open_track.setOnClickListener(this);

//        appActivationStateTV = (Button) findViewById(R.id.tv_activation_state_info);
//        loginBtn = (Button) findViewById(R.id.btn_login);
//        loginBtn.setOnClickListener(this);


        startLiveShowBtn = (Button) findViewById(R.id.btn_start_live_show);
        startLiveShowBtn.setOnClickListener(this);


        mTrackingImage = (ImageView) findViewById(R.id.tracking_rst_rect_iv);

        mSendRectIV = (ImageView) findViewById(R.id.tracking_send_rect_iv);

        mConfirmBtn = (Button) findViewById(R.id.confirm_btn);
        mStopBtn = (ImageButton) findViewById(R.id.tracking_stop_btn);
        mRejectBtn = (Button) findViewById(R.id.reject_btn);

        mConfirmBtn.setOnClickListener(this);
        mStopBtn.setOnClickListener(this);
        mRejectBtn.setOnClickListener(this);



        mBgLayout = (RelativeLayout) findViewById(R.id.tracking_bg_layout);
        mBgLayout.setOnTouchListener(this);
    }


    private void initMapView() {

        if (aMap == null) {
            aMap = mapView.getMap();
            aMap.setOnMapClickListener(this);// add the listener for click for amap object
        }

        LatLng shenzhen = new LatLng(22.5362, 113.9454);
        aMap.addMarker(new MarkerOptions().position(shenzhen).title("Marker in Shenzhen"));
        aMap.moveCamera(CameraUpdateFactory.newLatLng(shenzhen));
    }
    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };

    private void onProductConnectionChange()
    {
        initFlightController();
        loginAccount();
    }
    private void loginAccount(){

        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Log.e(TAG, "Login Success");
                    }
                    @Override
                    public void onFailure(DJIError error) {
                        setResultToToast("Login Error:"
                                + error.getDescription());
                    }
                });
    }

    private void initFlightController() {

        BaseProduct product = DemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                mFlightController = ((Aircraft) product).getFlightController();
            }
        }

        if (mFlightController != null) {

            mFlightController.setStateCallback(
                    new FlightControllerState.Callback() {
                        @Override
                        public void onUpdate(FlightControllerState
                                                     djiFlightControllerCurrentState) {
                            droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                            droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
                            updateDroneLocation();
                        }
                    });

        }
    }

    //Add Listener for WaypointMissionOperator
    private void addListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().addListener(eventNotificationListener);
        }
    }


    private WaypointMissionOperatorListener eventNotificationListener = new WaypointMissionOperatorListener() {
        @Override
        public void onDownloadUpdate(WaypointMissionDownloadEvent downloadEvent) {

        }

        @Override
        public void onUploadUpdate(WaypointMissionUploadEvent uploadEvent) {

        }

        @Override
        public void onExecutionUpdate(WaypointMissionExecutionEvent executionEvent) {

        }

        @Override
        public void onExecutionStart() {

        }

        @Override
        public void onExecutionFinish(@Nullable final DJIError error) {
            setResultToToast("Execution finished: " + (error == null ? "Success!" : error.getDescription()));
        }
    };

    public WaypointMissionOperator getWaypointMissionOperator() {
        if (instance == null) {
            instance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        return instance;
    }

    @Override
    public void onMapClick(LatLng point) {
        if (isAdd == true){
            markWaypoint(point);
            Waypoint mWaypoint = new Waypoint(point.latitude, point.longitude, altitude);
            //Add Waypoints to Waypoint arraylist;
            if (waypointMissionBuilder != null) {
                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }else
            {
                waypointMissionBuilder = new WaypointMission.Builder();
                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }
        }else{
//            setResultToToast("Cannot Add Waypoint");
        }
    }
    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    // 根据MCU的状态更新无人机位置。
    private void updateDroneLocation(){

        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        //创建MarkerOptions对象
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(pos);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (droneMarker != null) {
                    droneMarker.remove();
                }

                if (checkGpsCoordination(droneLocationLat, droneLocationLng)) {
                    droneMarker = aMap.addMarker(markerOptions);
                }
            }
        });
    }

    private void markWaypoint(LatLng point){
        //创建MarkerOptions对象
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        Marker marker = aMap.addMarker(markerOptions);
        mMarkers.put(mMarkers.size(), marker);
    }private void cameraUpdate(){
        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        float zoomlevel = (float) 18.0;
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, zoomlevel);
        aMap.moveCamera(cu);

    }

    private void enableDisableAdd(){
        if (isAdd == false) {
            isAdd = true;
            add.setText("Exit");
        }else{
            isAdd = false;
            add.setText("Add");
        }
    } String nulltoIntegerDefalt(String value){
        if(!isIntValue(value)) value="0";
        return value;
    }

    boolean isIntValue(String val)
    {
        try {
            val=val.replace(" ","");
            Integer.parseInt(val);
        } catch (Exception e) {return false;}
        return true;
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
            case R.id.btn_open_track:{
                if(mBgLayout.getVisibility()==View.INVISIBLE) {
                    ToastUtils("启用跟踪模式");
                    mBgLayout.setVisibility(View.VISIBLE);
                }
                else{
                    ToastUtils("关闭跟踪模式");
                    mBgLayout.setVisibility(View.INVISIBLE);
                }
                break;
            }

            case R.id.confirm_btn:
                boolean isAutoTracking =
                        isAutoSensingSupported &&
                                (mActiveTrackOperator.isAutoSensingEnabled() ||
                                        mActiveTrackOperator.isAutoSensingForQuickShotEnabled());
                if (isAutoTracking) {
                    startAutoSensingMission();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mStopBtn.setVisibility(View.VISIBLE);
                            mRejectBtn.setVisibility(View.VISIBLE);
                            mConfirmBtn.setVisibility(View.INVISIBLE);
                        }
                    });
                } else {
                    trackingIndex = INVAVID_INDEX;
                    mActiveTrackOperator.acceptConfirmation(new CommonCallbacks.CompletionCallback() {

                        @Override
                        public void onResult(DJIError error) {
                            setResultToToast(error == null ? "接受确认成功!" : error.getDescription());
                        }
                    });

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mStopBtn.setVisibility(View.VISIBLE);
                            mRejectBtn.setVisibility(View.VISIBLE);
                            mConfirmBtn.setVisibility(View.INVISIBLE);
                        }
                    });

                }
                break;

            case R.id.tracking_stop_btn:
                trackingIndex = INVAVID_INDEX;
                mActiveTrackOperator.stopTracking(new CommonCallbacks.CompletionCallback() {

                    @Override
                    public void onResult(DJIError error) {
                        setResultToToast(error == null ? "停止跟踪成功!" : error.getDescription());
                    }
                });

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mTrackingImage != null) {
                            mTrackingImage.setVisibility(View.INVISIBLE);
                            mSendRectIV.setVisibility(View.INVISIBLE);
                            mStopBtn.setVisibility(View.INVISIBLE);
                            mRejectBtn.setVisibility(View.INVISIBLE);
                            mConfirmBtn.setVisibility(View.VISIBLE);
                        }
                    }
                });
                break;

            case R.id.reject_btn:
                trackingIndex = INVAVID_INDEX;
                mActiveTrackOperator.rejectConfirmation(new CommonCallbacks.CompletionCallback() {

                    @Override
                    public void onResult(DJIError error) {

                        setResultToToast(error == null ? "拒绝确认成功!" : error.getDescription());
                    }
                });
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mStopBtn.setVisibility(View.VISIBLE);
                        mRejectBtn.setVisibility(View.VISIBLE);
                        mConfirmBtn.setVisibility(View.INVISIBLE);
                    }
                });
                break;

//            case R.id.tracking_drawer_control_ib:
//                if (mPushInfoSd.isOpened()) {
//                    mPushInfoSd.animateClose();
//                } else {
//                    mPushInfoSd.animateOpen();
//                }
//                break;

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
//                    appActivationStateTV.setText("" + appActivationManager.getAppActivationState());
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
//                        appActivationStateTV.setText("" + appActivationState);
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

        mActiveTrackOperator = MissionControl.getInstance().getActiveTrackOperator();
        if (mActiveTrackOperator == null) {
            return;
        }

        mActiveTrackOperator.addListener(this);
//        mActiveTrackOperator.getRetreatEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
//            @Override
//            public void onSuccess(final Boolean aBoolean) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
////                        mPushBackSw.setChecked(aBoolean);
//                    }
//                });
//            }
//
//            @Override
//            public void onFailure(DJIError error) {
//                setResultToToast("can't get retreat enable state " + error.getDescription());
//            }
//        });
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
//                    appActivationStateTV.setText("Unknown");
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
    private static final int COMPLETED = 0;
    private static final int LiveStre = 1;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == COMPLETED) {
                if(msg.obj.toString().equals("Login")) {
//                    appActivationStateTV.setText("已登录"); //UI更改操作
//                    loginBtn.setText("注销");
                }
                else {
//                    appActivationStateTV.setText("未登录");
//                    loginBtn.setText("登录");
                }
            }
            else if (msg.what == LiveStre){
                if(msg.obj.toString().equals("0")) {
                    Toast.makeText(getApplicationContext(),"开始直播!",Toast.LENGTH_LONG).show();
                }else {
                    Toast.makeText(getApplicationContext(),"开启直播失败!",Toast.LENGTH_LONG).show();
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
            SharedPreferences sharedPreferences =
                    getSharedPreferences("setting", Context.MODE_PRIVATE);
            liveShowUrl = (sharedPreferences.getString("url", "请输入直播地址!"));
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

    private static final int MAIN_CAMERA_INDEX = 0;
    private static final int INVAVID_INDEX = -1;
    private static final int MOVE_OFFSET = 20;
    private ActiveTrackOperator mActiveTrackOperator;
    private ActiveTrackMission mActiveTrackMission;
    private final DJIKey trackModeKey = FlightControllerKey.createFlightAssistantKey(FlightControllerKey.ACTIVE_TRACK_MODE);
    private ConcurrentHashMap<Integer, MultiTrackingView> targetViewHashMap = new ConcurrentHashMap<>();
    private int trackingIndex = INVAVID_INDEX;
    private boolean isAutoSensingSupported = false;
    private ActiveTrackMode startMode = ActiveTrackMode.TRACE;
    private QuickShotMode quickShotMode = QuickShotMode.UNKNOWN;
    private boolean isDrawingRect = false;




    float downX;
    float downY;
    private double calcManhattanDistance(double point1X, double point1Y, double point2X, double point2Y) {
        return Math.abs(point1X - point2X) + Math.abs(point1Y - point2Y);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDrawingRect = false;
                downX = event.getX();
                downY = event.getY();
                break;

            case MotionEvent.ACTION_MOVE:
                if (calcManhattanDistance(downX, downY, event.getX(), event.getY()) < MOVE_OFFSET && !isDrawingRect) {
                    trackingIndex = getTrackingIndex(downX, downY, targetViewHashMap);
                    if (targetViewHashMap.get(trackingIndex) != null) {
                        targetViewHashMap.get(trackingIndex).setBackgroundColor(Color.RED);
                    }
                    return true;
                }
                isDrawingRect = true;
                mSendRectIV.setVisibility(new Integer(0x00000000));
                int l = (int) (downX < event.getX() ? downX : event.getX());
                int t = (int) (downY < event.getY() ? downY : event.getY());
                int r = (int) (downX >= event.getX() ? downX : event.getX());
                int b = (int) (downY >= event.getY() ? downY : event.getY());
                mSendRectIV.setX(l);
                mSendRectIV.setY(t);
                mSendRectIV.getLayoutParams().width = r - l;
                mSendRectIV.getLayoutParams().height = b - t;
                mSendRectIV.requestLayout();
                break;

            case MotionEvent.ACTION_UP:
//                if (mGestureModeSw.isChecked()) {
//                    ToastUtils("Please try to start Gesture Mode!");
//                } else
                    if (!isDrawingRect) {
                    if (targetViewHashMap.get(trackingIndex) != null) {
                        ToastUtils("Selected Index: " + trackingIndex + ",Please Confirm it!");
                        targetViewHashMap.get(trackingIndex).setBackgroundColor(Color.TRANSPARENT);
                    }
                } else {
                    RectF rectF = getActiveTrackRect(mSendRectIV);
                    mActiveTrackMission = new ActiveTrackMission(rectF, startMode);
                    if (startMode == ActiveTrackMode.QUICK_SHOT) {
                        mActiveTrackMission.setQuickShotMode(quickShotMode);
                        checkStorageStates();
                    }
                    mActiveTrackOperator.startTracking(mActiveTrackMission, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError error) {
                            if (error == null) {
                                isDrawingRect = false;
                            }
                            ToastUtils("开始跟踪: " + (error == null
                                    ? "成功"
                                    : error.getDescription()));
                        }
                    });
                    mSendRectIV.setVisibility(View.INVISIBLE);
                    clearCurrentView();
                }
                break;

            default:
                break;
        }

        return true;
    }
    private int getTrackingIndex(final float x, final float y,
                                 final ConcurrentHashMap<Integer, MultiTrackingView> multiTrackinghMap) {
        if (multiTrackinghMap == null || multiTrackinghMap.isEmpty()) {
            return INVAVID_INDEX;
        }

        float l, t, r, b;
        for (Map.Entry<Integer, MultiTrackingView> vo : multiTrackinghMap.entrySet()) {
            int key = vo.getKey().intValue();
            MultiTrackingView view = vo.getValue();
            l = view.getX();
            t = view.getY();
            r = (view.getX() + (view.getWidth() / 2));
            b = (view.getY() + (view.getHeight() / 2));

            if (x >= l && y >= t && x <= r && y <= b) {
                return key;
            }
        }
        return INVAVID_INDEX;
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, final boolean isChecked) {
        if (mActiveTrackOperator == null) {
            return;
        }
//        switch (compoundButton.getId()) {
//            case R.id.set_multitracking_enabled:
//                startMode = ActiveTrackMode.TRACE;
//                quickShotMode = QuickShotMode.UNKNOWN;
//                setAutoSensingEnabled(isChecked);
//                break;
//            case R.id.set_multiquickshot_enabled:
//                startMode = ActiveTrackMode.QUICK_SHOT;
//                quickShotMode = QuickShotMode.CIRCLE;
//                checkStorageStates();
//                setAutoSensingForQuickShotEnabled(isChecked);
//                break;
//            case R.id.tracking_pull_back_tb:
//                mActiveTrackOperator.setRetreatEnabled(isChecked, new CommonCallbacks.CompletionCallback() {
//                    @Override
//                    public void onResult(DJIError error) {
//                        if (error != null) {
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    mPushBackSw.setChecked(!isChecked);
//                                }
//                            });
//                        }
//                        setResultToToast("Set Retreat Enabled: " + (error == null
//                                ? "Success"
//                                : error.getDescription()));
//                    }
//                });
//                break;
//            case R.id.tracking_in_gesture_mode:
//                mActiveTrackOperator.setGestureModeEnabled(isChecked, new CommonCallbacks.CompletionCallback() {
//                    @Override
//                    public void onResult(DJIError error) {
//                        if (error != null) {
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    mGestureModeSw.setChecked(!isChecked);
//                                }
//                            });
//                        }
//                        setResultToToast("Set GestureMode Enabled: " + (error == null
//                                ? "Success"
//                                : error.getDescription()));
//                    }
//                });
//                break;
//            default:
//                break;
//        }
    }


    @Override
    public void onUpdate(ActiveTrackMissionEvent event) {
        StringBuffer sb = new StringBuffer();
        String errorInformation = (event.getError() == null ? "null" : event.getError().getDescription()) + "\n";
        String currentState = event.getCurrentState() == null ? "null" : event.getCurrentState().getName();
        String previousState = event.getPreviousState() == null ? "null" : event.getPreviousState().getName();

        ActiveTrackTargetState targetState = ActiveTrackTargetState.UNKNOWN;
        if (event.getTrackingState() != null) {
            targetState = event.getTrackingState().getState();
        }
        Utils.addLineToSB(sb, "CurrentState: ", currentState);
        Utils.addLineToSB(sb, "PreviousState: ", previousState);
        Utils.addLineToSB(sb, "TargetState: ", targetState);
        Utils.addLineToSB(sb, "Error:", errorInformation);

        Object value = KeyManager.getInstance().getValue(trackModeKey);
        if (value instanceof ActiveTrackMode) {
            Utils.addLineToSB(sb, "TrackingMode:", value.toString());
        }

        ActiveTrackTrackingState trackingState = event.getTrackingState();
        if (trackingState != null) {
            final SubjectSensingState[] targetSensingInformations = trackingState.getAutoSensedSubjects();
            if (targetSensingInformations != null) {
                for (SubjectSensingState subjectSensingState : targetSensingInformations) {
                    RectF trackingRect = subjectSensingState.getTargetRect();
                    if (trackingRect != null) {
                        Utils.addLineToSB(sb, "Rect center x: ", trackingRect.centerX());
                        Utils.addLineToSB(sb, "Rect center y: ", trackingRect.centerY());
                        Utils.addLineToSB(sb, "Rect Width: ", trackingRect.width());
                        Utils.addLineToSB(sb, "Rect Height: ", trackingRect.height());
                        Utils.addLineToSB(sb, "Reason", trackingState.getReason().name());
                        Utils.addLineToSB(sb, "Target Index: ", subjectSensingState.getIndex());
                        Utils.addLineToSB(sb, "Target Type", subjectSensingState.getTargetType().name());
                        Utils.addLineToSB(sb, "Target State", subjectSensingState.getState().name());
                        isAutoSensingSupported = true;
                    }
                }
            } else {
                RectF trackingRect = trackingState.getTargetRect();
                if (trackingRect != null) {
                    Utils.addLineToSB(sb, "Rect center x: ", trackingRect.centerX());
                    Utils.addLineToSB(sb, "Rect center y: ", trackingRect.centerY());
                    Utils.addLineToSB(sb, "Rect Width: ", trackingRect.width());
                    Utils.addLineToSB(sb, "Rect Height: ", trackingRect.height());
                    Utils.addLineToSB(sb, "Reason", trackingState.getReason().name());
                    Utils.addLineToSB(sb, "Target Index: ", trackingState.getTargetIndex());
                    Utils.addLineToSB(sb, "Target Type", trackingState.getType().name());
                    Utils.addLineToSB(sb, "Target State", trackingState.getState().name());
                    isAutoSensingSupported = false;
                }
                clearCurrentView();
            }
        }

        setResultToText(sb.toString());
        updateActiveTrackRect(mTrackingImage, event);
        updateButtonVisibility(event);
    }

    /**
     * 将状态推到文本
     *
     * @param string
     */
    private void setResultToText(final String string) {
//        if (mPushInfoTv == null) {
//            setResultToToast("Push info tv has not be init...");
//        }
//        MainActivity.this.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                mPushInfoTv.setText(string);
//            }
//        });
    }
    /**
     * Update ActiveTrack Rect
     *
     * @param iv
     * @param event
     */
    private void updateActiveTrackRect(final ImageView iv, final ActiveTrackMissionEvent event) {
        if (iv == null || event == null) {
            return;
        }

        ActiveTrackTrackingState trackingState = event.getTrackingState();
        if (trackingState != null) {
            if (trackingState.getAutoSensedSubjects() != null) {
                final SubjectSensingState[] targetSensingInformations = trackingState.getAutoSensedSubjects();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateMultiTrackingView(targetSensingInformations);
                    }
                });
            } else {
                RectF trackingRect = trackingState.getTargetRect();
                ActiveTrackTargetState trackTargetState = trackingState.getState();
                postResultRect(iv, trackingRect, trackTargetState);
            }
        }

    }

    private void updateButtonVisibility(final ActiveTrackMissionEvent event) {
        ActiveTrackState state = event.getCurrentState();
        if (state == ActiveTrackState.AUTO_SENSING ||
                state == ActiveTrackState.AUTO_SENSING_FOR_QUICK_SHOT ||
                state == ActiveTrackState.WAITING_FOR_CONFIRMATION) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mStopBtn.setVisibility(View.VISIBLE);
                    mStopBtn.setClickable(true);
                    mConfirmBtn.setVisibility(View.VISIBLE);
                    mConfirmBtn.setClickable(true);
                    mRejectBtn.setVisibility(View.VISIBLE);
                    mRejectBtn.setClickable(true);
//                    mConfigBtn.setVisibility(View.GONE);
                }
            });
        } else if (state == ActiveTrackState.AIRCRAFT_FOLLOWING ||
                state == ActiveTrackState.ONLY_CAMERA_FOLLOWING ||
                state == ActiveTrackState.FINDING_TRACKED_TARGET ||
                state == ActiveTrackState.CANNOT_CONFIRM ||
                state == ActiveTrackState.PERFORMING_QUICK_SHOT) {
            MainActivity.this.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    mStopBtn.setVisibility(View.VISIBLE);
                    mStopBtn.setClickable(true);
                    mConfirmBtn.setVisibility(View.INVISIBLE);
                    mConfirmBtn.setClickable(false);
                    mRejectBtn.setVisibility(View.VISIBLE);
                    mRejectBtn.setClickable(true);
//                    mConfigBtn.setVisibility(View.GONE);
                }
            });
        } else {
            MainActivity.this.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    mStopBtn.setVisibility(View.INVISIBLE);
                    mStopBtn.setClickable(false);
                    mConfirmBtn.setVisibility(View.INVISIBLE);
                    mConfirmBtn.setClickable(false);
                    mRejectBtn.setVisibility(View.INVISIBLE);
                    mRejectBtn.setClickable(false);
                    mTrackingImage.setVisibility(View.INVISIBLE);
                }
            });
        }
    }


    /**
     * Get ActiveTrack RectF
     *
     * @param iv
     * @return
     */
    private RectF getActiveTrackRect(View iv) {
        View parent = (View) iv.getParent();
        return new RectF(
                ((float) iv.getLeft() + iv.getX()) / (float) parent.getWidth(),
                ((float) iv.getTop() + iv.getY()) / (float) parent.getHeight(),
                ((float) iv.getRight() + iv.getX()) / (float) parent.getWidth(),
                ((float) iv.getBottom() + iv.getY()) / (float) parent.getHeight());
    }

    /**
     * Post Result RectF
     *
     * @param iv
     * @param rectF
     * @param targetState
     */
    private void postResultRect(final ImageView iv, final RectF rectF,
                                final ActiveTrackTargetState targetState) {
        View parent = (View) iv.getParent();
        RectF trackingRect = rectF;

        final int l = (int) ((trackingRect.centerX() - trackingRect.width() / 2) * parent.getWidth());
        final int t = (int) ((trackingRect.centerY() - trackingRect.height() / 2) * parent.getHeight());
        final int r = (int) ((trackingRect.centerX() + trackingRect.width() / 2) * parent.getWidth());
        final int b = (int) ((trackingRect.centerY() + trackingRect.height() / 2) * parent.getHeight());

        MainActivity.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mTrackingImage.setVisibility(View.VISIBLE);
                if ((targetState == ActiveTrackTargetState.CANNOT_CONFIRM)
                        || (targetState == ActiveTrackTargetState.UNKNOWN)) {
                    iv.setImageResource(R.drawable.visual_track_cannotconfirm);
                } else if (targetState == ActiveTrackTargetState.WAITING_FOR_CONFIRMATION) {
                    iv.setImageResource(R.drawable.visual_track_needconfirm);
                } else if (targetState == ActiveTrackTargetState.TRACKING_WITH_LOW_CONFIDENCE) {
                    iv.setImageResource(R.drawable.visual_track_lowconfidence);
                } else if (targetState == ActiveTrackTargetState.TRACKING_WITH_HIGH_CONFIDENCE) {
                    iv.setImageResource(R.drawable.visual_track_highconfidence);
                }
                iv.setX(l);
                iv.setY(t);
                iv.getLayoutParams().width = r - l;
                iv.getLayoutParams().height = b - t;
                iv.requestLayout();
            }
        });
    }
    /**
     * PostMultiResult
     *
     * @param iv
     * @param rectF
     * @param information
     */
    private void postMultiResultRect(final MultiTrackingView iv, final RectF rectF,
                                     final SubjectSensingState information) {
        View parent = (View) iv.getParent();
        RectF trackingRect = rectF;

        final int l = (int) ((trackingRect.centerX() - trackingRect.width() / 2) * parent.getWidth());
        final int t = (int) ((trackingRect.centerY() - trackingRect.height() / 2) * parent.getHeight());
        final int r = (int) ((trackingRect.centerX() + trackingRect.width() / 2) * parent.getWidth());
        final int b = (int) ((trackingRect.centerY() + trackingRect.height() / 2) * parent.getHeight());

        MainActivity.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mTrackingImage.setVisibility(View.INVISIBLE);
                iv.setX(l);
                iv.setY(t);
                iv.getLayoutParams().width = r - l;
                iv.getLayoutParams().height = b - t;
                iv.requestLayout();
                iv.updateView(information);
            }
        });
    }

    /**
     * Update MultiTrackingView
     *
     * @param targetSensingInformations
     */
    private void updateMultiTrackingView(final SubjectSensingState[] targetSensingInformations) {
        ArrayList<Integer> indexs = new ArrayList<>();
        for (SubjectSensingState target : targetSensingInformations) {
            indexs.add(target.getIndex());
            if (targetViewHashMap.containsKey(target.getIndex())) {

                MultiTrackingView targetView = targetViewHashMap.get(target.getIndex());
                postMultiResultRect(targetView, target.getTargetRect(), target);
            } else {
                MultiTrackingView trackingView = new MultiTrackingView(MainActivity.this);
                mBgLayout.addView(trackingView, layoutParams);
                targetViewHashMap.put(target.getIndex(), trackingView);
            }
        }

        ArrayList<Integer> missingIndexs = new ArrayList<>();
        for (Integer key : targetViewHashMap.keySet()) {
            boolean isDisappeared = true;
            for (Integer index : indexs) {
                if (index.equals(key)) {
                    isDisappeared = false;
                    break;
                }
            }

            if (isDisappeared) {
                missingIndexs.add(key);
            }
        }

        for (Integer i : missingIndexs) {
            MultiTrackingView view = targetViewHashMap.remove(i);
            mBgLayout.removeView(view);
        }
    }


    /**
     * Enable MultiTracking
     *
     * @param isChecked
     */
    private void setAutoSensingEnabled(final boolean isChecked) {
        if (mActiveTrackOperator != null) {
            if (isChecked) {
                startMode = ActiveTrackMode.TRACE;
                mActiveTrackOperator.enableAutoSensing(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        if (error != null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
//                                    mAutoSensingSw.setChecked(!isChecked);
                                }
                            });
                        }
                        setResultToToast("Set AutoSensing Enabled " + (error == null ? "Success!" : error.getDescription()));
                    }
                });
            } else {
                disableAutoSensing();
            }
        }
    }

    /**
     * Enable QuickShotMode
     *
     * @param isChecked
     */
    private void setAutoSensingForQuickShotEnabled(final boolean isChecked) {
        if (mActiveTrackOperator != null) {
            if (isChecked) {
                mActiveTrackOperator.enableAutoSensingForQuickShot(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        if (error != null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
//                                    mQuickShotSw.setChecked(!isChecked);
                                }
                            });
                        }
                        setResultToToast("Set QuickShot Enabled " + (error == null ? "Success!" : error.getDescription()));
                    }
                });

            } else {
                disableAutoSensing();
            }

        }
    }

    /**
     * Disable AutoSensing
     */
    private void disableAutoSensing() {
        if (mActiveTrackOperator != null) {
            mActiveTrackOperator.disableAutoSensing(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mConfirmBtn.setVisibility(View.INVISIBLE);
                                mStopBtn.setVisibility(View.INVISIBLE);
                                mRejectBtn.setVisibility(View.INVISIBLE);
//                                mConfigBtn.setVisibility(View.VISIBLE);
                                isAutoSensingSupported = false;
                            }
                        });
                        clearCurrentView();
                    }
                    setResultToToast(error == null ? "Disable Auto Sensing Success!" : error.getDescription());
                }
            });
        }
    }


    /**
     * Confim Mission by Index
     */
    private void startAutoSensingMission() {
        if (trackingIndex != INVAVID_INDEX) {
            ActiveTrackMission mission = new ActiveTrackMission(null, startMode);
            mission.setQuickShotMode(quickShotMode);
            mission.setTargetIndex(trackingIndex);
            mActiveTrackOperator.startAutoSensingMission(mission, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        setResultToToast("Accept Confim index: " + trackingIndex + " Success!");
                        trackingIndex = INVAVID_INDEX;
                    } else {
                        setResultToToast(error.getDescription());
                    }
                }
            });
        }
    }


    /**
     * Change Storage Location
     */
    private void switchStorageLocation(final SettingsDefinitions.StorageLocation storageLocation) {
        KeyManager keyManager = KeyManager.getInstance();
        DJIKey storageLoactionkey = CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, MAIN_CAMERA_INDEX);

        if (storageLocation == SettingsDefinitions.StorageLocation.INTERNAL_STORAGE) {
            keyManager.setValue(storageLoactionkey, SettingsDefinitions.StorageLocation.SDCARD, new SetCallback() {
                @Override
                public void onSuccess() {
                    setResultToToast("Change to SD card Success!");
                }

                @Override
                public void onFailure(@NonNull DJIError error) {
                    setResultToToast(error.getDescription());
                }
            });
        } else {
            keyManager.setValue(storageLoactionkey, SettingsDefinitions.StorageLocation.INTERNAL_STORAGE, new SetCallback() {
                @Override
                public void onSuccess() {
                    setResultToToast("Change to Interal Storage Success!");
                }

                @Override
                public void onFailure(@NonNull DJIError error) {
                    setResultToToast(error.getDescription());
                }
            });
        }
    }

    /**
     * determine SD Card is or not Ready
     *
     * @param index
     * @return
     */
    private boolean isSDCardReady(int index) {
        KeyManager keyManager = KeyManager.getInstance();

        return ((Boolean) keyManager.getValue(CameraKey.create(CameraKey.SDCARD_IS_INSERTED, index))
                && !(Boolean) keyManager.getValue(CameraKey.create(CameraKey.SDCARD_IS_INITIALIZING, index))
                && !(Boolean) keyManager.getValue(CameraKey.create(CameraKey.SDCARD_IS_READ_ONLY, index))
                && !(Boolean) keyManager.getValue(CameraKey.create(CameraKey.SDCARD_HAS_ERROR, index))
                && !(Boolean) keyManager.getValue(CameraKey.create(CameraKey.SDCARD_IS_FULL, index))
                && !(Boolean) keyManager.getValue(CameraKey.create(CameraKey.SDCARD_IS_BUSY, index))
                && !(Boolean) keyManager.getValue(CameraKey.create(CameraKey.SDCARD_IS_FORMATTING, index))
                && !(Boolean) keyManager.getValue(CameraKey.create(CameraKey.SDCARD_IS_INVALID_FORMAT, index))
                && (Boolean) keyManager.getValue(CameraKey.create(CameraKey.SDCARD_IS_VERIFIED, index))
                && (Long) keyManager.getValue(CameraKey.create(CameraKey.SDCARD_AVAILABLE_CAPTURE_COUNT, index)) > 0L
                && (Integer) keyManager.getValue(CameraKey.create(CameraKey.SDCARD_AVAILABLE_RECORDING_TIME_IN_SECONDS, index)) > 0);
    }

    /**
     * determine Interal Storage is or not Ready
     *
     * @param index
     * @return
     */
    private boolean isInteralStorageReady(int index) {
        KeyManager keyManager = KeyManager.getInstance();

        boolean isInternalSupported = (boolean)
                keyManager.getValue(CameraKey.create(CameraKey.IS_INTERNAL_STORAGE_SUPPORTED, index));
        if (isInternalSupported) {
            return ((Boolean) keyManager.getValue(CameraKey.create(CameraKey.INNERSTORAGE_IS_INSERTED, index))
                    && !(Boolean) keyManager.getValue(CameraKey.create(CameraKey.INNERSTORAGE_IS_INITIALIZING, index))
                    && !(Boolean) keyManager.getValue(CameraKey.create(CameraKey.INNERSTORAGE_IS_READ_ONLY, index))
                    && !(Boolean) keyManager.getValue(CameraKey.create(CameraKey.INNERSTORAGE_HAS_ERROR, index))
                    && !(Boolean) keyManager.getValue(CameraKey.create(CameraKey.INNERSTORAGE_IS_FULL, index))
                    && !(Boolean) keyManager.getValue(CameraKey.create(CameraKey.INNERSTORAGE_IS_BUSY, index))
                    && !(Boolean) keyManager.getValue(CameraKey.create(CameraKey.INNERSTORAGE_IS_FORMATTING, index))
                    && !(Boolean) keyManager.getValue(CameraKey.create(CameraKey.INNERSTORAGE_IS_INVALID_FORMAT, index))
                    && (Boolean) keyManager.getValue(CameraKey.create(CameraKey.INNERSTORAGE_IS_VERIFIED, index))
                    && (Long) keyManager.getValue(CameraKey.create(CameraKey.INNERSTORAGE_AVAILABLE_CAPTURE_COUNT, index)) > 0L
                    && (Integer) keyManager.getValue(CameraKey.create(CameraKey.INNERSTORAGE_AVAILABLE_RECORDING_TIME_IN_SECONDS, index)) > 0);
        }
        return false;
    }

    /**
     * Check Storage States
     */
    private void checkStorageStates() {
        KeyManager keyManager = KeyManager.getInstance();
        DJIKey storageLocationkey = CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, MAIN_CAMERA_INDEX);
        Object storageLocationObj = keyManager.getValue(storageLocationkey);
        SettingsDefinitions.StorageLocation storageLocation = SettingsDefinitions.StorageLocation.INTERNAL_STORAGE;

        if (storageLocationObj instanceof SettingsDefinitions.StorageLocation){
            storageLocation = (SettingsDefinitions.StorageLocation) storageLocationObj;
        }

        if (storageLocation == SettingsDefinitions.StorageLocation.INTERNAL_STORAGE) {
            if (!isInteralStorageReady(MAIN_CAMERA_INDEX) && isSDCardReady(MAIN_CAMERA_INDEX)) {
                switchStorageLocation(SettingsDefinitions.StorageLocation.SDCARD);
            }
        }

        if (storageLocation == SettingsDefinitions.StorageLocation.SDCARD) {
            if (!isSDCardReady(MAIN_CAMERA_INDEX) && isInteralStorageReady(MAIN_CAMERA_INDEX)) {
                switchStorageLocation(SettingsDefinitions.StorageLocation.INTERNAL_STORAGE);
            }
        }

        DJIKey isRecordingKey = CameraKey.create(CameraKey.IS_RECORDING, MAIN_CAMERA_INDEX);
        Object isRecording = keyManager.getValue(isRecordingKey);
        if (isRecording instanceof Boolean) {
            if (((Boolean) isRecording).booleanValue()) {
                keyManager.performAction(CameraKey.create(CameraKey.STOP_RECORD_VIDEO, MAIN_CAMERA_INDEX), new ActionCallback() {
                    @Override
                    public void onSuccess() {
                        setResultToToast("Stop Recording Success!");
                    }

                    @Override
                    public void onFailure(@NonNull DJIError error) {
                        setResultToToast("Stop Recording Fail，Error " + error.getDescription());
                    }
                });
            }
        }
    }

    /**
     * Clear MultiTracking View
     */
    private void clearCurrentView() {
        if (targetViewHashMap != null && !targetViewHashMap.isEmpty()) {
            Iterator<Map.Entry<Integer, MultiTrackingView>> it = targetViewHashMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, MultiTrackingView> entry = it.next();
                final MultiTrackingView view = entry.getValue();
                it.remove();
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBgLayout.removeView(view);
                    }
                });
            }
        }
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        setUpListener();
        initFlightController();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
//        tearDownListener();

        isAutoSensingSupported = false;
        try {
            DJIVideoDataRecver.getInstance().setVideoDataListener(false, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mActiveTrackOperator != null) {
            mActiveTrackOperator.removeListener(this);
        }

        if (mCodecManager != null) {
            mCodecManager.destroyCodec();
        }

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
        setResultToToast(string);
    }
    public void  setResultToToast(String string){
        Toast.makeText(getApplicationContext(),string,Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {

    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        return false;
    }


}

