package com.dji.uxsdkdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import android.os.Bundle;
import android.view.View;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;

import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;

public class MapActivity extends FragmentActivity implements View.OnClickListener, AMap.OnMapClickListener {

    private MapView mapView;
    private AMap aMap;

    private double droneLocationLat = 181, droneLocationLng = 181;
    private Marker droneMarker = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        //Create MarkerOptions object
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

        float zoomlevel = (float) 18.0;
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, zoomlevel);
        aMap.moveCamera(cu);

        MapView mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);// 此方法必须重写
        AMap aMap = mapView.getMap();

        aMap.setTrafficEnabled(true);// 显示实时交通状况
        //地图模式可选类型：MAP_TYPE_NORMAL,MAP_TYPE_SATELLITE,MAP_TYPE_NIGHT
        aMap.setMapType(AMap.MAP_TYPE_SATELLITE);// 卫星地图模式
    }

    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onMapClick(LatLng latLng) {
//        if (isAdd == true){
//            markWaypoint(point);
//            Waypoint mWaypoint = new Waypoint(point.latitude, point.longitude, altitude);
//            //Add Waypoints to Waypoint arraylist;
//            if (waypointMissionBuilder != null) {
//                waypointList.add(mWaypoint);
//                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
//            }else
//            {
//                waypointMissionBuilder = new WaypointMission.Builder();
//                waypointList.add(mWaypoint);
//                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
//            }
//        }else{
//            setResultToToast("Cannot Add Waypoint");
//        }
    }
}