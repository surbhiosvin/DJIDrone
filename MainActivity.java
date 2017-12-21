package com.senteksystems.survey.activities;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.senteksystems.survey.R;
import com.senteksystems.survey.util.FlightPlan;
import com.senteksystems.survey.util.Functions;
import com.senteksystems.survey.util.LocationFinder;
import com.senteksystems.survey.util.SharedPref;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import dji.sdk.AirLink.DJILBAirLink;
import dji.sdk.AirLink.DJIWiFiLink;
import dji.sdk.Battery.DJIBattery;
import dji.sdk.FlightController.DJIFlightController;
import dji.sdk.FlightController.DJIFlightControllerDataType;
import dji.sdk.FlightController.DJIFlightControllerDelegate;
import dji.sdk.MissionManager.DJIMission;
import dji.sdk.MissionManager.DJIMissionManager;
import dji.sdk.MissionManager.DJIWaypoint;
import dji.sdk.MissionManager.DJIWaypointMission;
import dji.sdk.Products.DJIAircraft;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIError;


public  class MainActivity extends Activity implements View.OnClickListener,View.OnTouchListener,DJIMissionManager.MissionProgressStatusCallback, DJIBaseComponent.DJICompletionCallback {
    private static final String TAG = MainActivity.class.getName();
    GoogleMap googleMap;
    ImageView img_menu, img_location, img_setting, img_drone, img_drawline, img_download, img_delete, img_pause, img_return_to_home, img_drone_land;
    TextView txt_centerMe, txt_centerDrone, txt_battery_level,txt_speed, txt_height, txt_range,txt_flight_time, txt_wifi_level ;
    LinearLayout line1, layout_battery_status, layout_aircraft_status, layout_wifi_status;

    int missionHeight = 50;
    int missionSpeed = 3;
    int MissionSideLap = 50;
    int MissionHatchAngle =1;
    int MissionVariant = 1;
    double MissionOverlap = 1;

    float currentAltitude = 1;

    SharedPref sp;
    Dialog mDialog;
    boolean missionPaused = false;
    boolean missionUploaded = false;
    public boolean isDroneFlying = false;

    private static final int GPS_SETTINGS = 102;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private Double mLatitude = 181.0, mLongitude = 181.0;
    private Double mHomeLatitude = 181.0, mHomeLongitude = 181.0;
    private LocationFinder newLocationFinder;
    NetworkInfo wifiCheck, intenetCheck;
    private View mMapShelterView;
    private GestureDetector mGestureDetector;
    private ArrayList<LatLng> mLatlngs = new ArrayList<LatLng>();
    ArrayList<LatLng> pathToFollow = new ArrayList<>();
    private PolylineOptions mPolylineOptions;
    private PolygonOptions mPolygonOptions;
    // flag to differentiate whether user is touching to draw or not
    private boolean mDrawFinished = false;

    private double droneLocationLat = 181.0, droneLocationLng = 181.0;
    private Marker droneMarker = null, HomeMarker = null;

    private String isAppRegistered = "";
    private boolean isDroneConnected = false;
    private LatLng centerOn = new LatLng(181.0, 181.0);
    private Handler mUIHandler = new Handler(Looper.getMainLooper());
    protected ProgressBar mPB;

    protected StringBuffer pushSB = null;
    private Animation mFadeoutAnimation;

    private DJIFlightControllerDataType.DJIFlightControllerFlightMode flightState = null;
    private DJIWaypointMission mWaypointMission;
    private DJIMissionManager mMissionManager;
    private DJIFlightController mFlightController;

    private DJIWaypointMission.DJIWaypointMissionFinishedAction mFinishedAction = DJIWaypointMission.DJIWaypointMissionFinishedAction.GoHome;
    private DJIWaypointMission.DJIWaypointMissionHeadingMode mHeadingMode = DJIWaypointMission.DJIWaypointMissionHeadingMode.Auto;
    float droneRotaion=0;
    DJIFlightController mController;

    String model = "";
    String name = "";
    String wifiName = "";
    String petName = "";
    String serialNumber="";
    ArrayList <DroneList> droneList = new ArrayList<>();
    DroneAdapter adapter;

    ArrayList <DJIWaypoint> DjiWaypointsFromCalculation = new ArrayList<>();
//    ArrayList<LatLng> alreadyExecutingMissionPath = new ArrayList<>();

    double speed =0.0;
    double height =0.0;

    private String wifi = "";

    protected PowerManager.WakeLock mWakeLock;
    ArrayList<LatLng> travelledPath= new ArrayList<>();
    int secondsTimerCount =0;
    int flightTimeInSeconds=0;

    @Override
    protected void onResume() {
        super.onResume();
        initFlightController();
        initMissionManager();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        this.mWakeLock.release();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // When the compile and target version is higher than 22, request the following permissions at runtime.
        if (!checkAllPermission()) {
            permissionsruntime();
        }
        setContentView(R.layout.activity_main);
        Initialize();

        IntentFilter filter = new IntentFilter();
        filter.addAction(SentekApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        CheckConnection();
        InitGooglemap();

        if(null!=SentekApplication.getProductInstance() || (null!=SentekApplication.getProductInstance() && SentekApplication.getProductInstance().isConnected())){
            getBatteryStatus();
        }

        checkAircraftCurrentStatus();

        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
        this.mWakeLock.acquire();

    }

    public void Initialize() {
        sp = new SharedPref(this);
        mDialog = new Dialog(new ContextThemeWrapper(this, R.style.dialog));
        mDialog.setCancelable(true);

        missionHeight = sp.getMissionHeight();
        missionSpeed = sp.getMissionSpeed();
        MissionSideLap = sp.getMissionSideLap();
        MissionHatchAngle =sp.getMissionHatchAngle();
        MissionVariant = sp.getMissionVariant();
        MissionOverlap = sp.getMissionOverlap();


        img_menu = (ImageView) findViewById(R.id.img_menu);
        img_location = (ImageView) findViewById(R.id.img_location);
        img_setting = (ImageView) findViewById(R.id.img_setting);
        img_drone = (ImageView) findViewById(R.id.img_drone);
        txt_centerMe = (TextView) findViewById(R.id.txt_centerMe);
        txt_centerDrone = (TextView) findViewById(R.id.txt_centerDrone);
        txt_battery_level = (TextView) findViewById(R.id.txt_battery_level);
        line1 = (LinearLayout) findViewById(R.id.line1);
        layout_battery_status = (LinearLayout) findViewById(R.id.layout_battery_status);
        img_drawline = (ImageView) findViewById(R.id.img_drawline);
        img_download = (ImageView) findViewById(R.id.img_download);
        img_delete = (ImageView) findViewById(R.id.img_delete);
        mMapShelterView = (View) findViewById(R.id.drawer_view);
        img_pause = (ImageView) findViewById(R.id.img_pause);
        img_drone_land = (ImageView) findViewById(R.id.img_drone_land);
        img_return_to_home = (ImageView) findViewById(R.id.img_return_to_home);
        mPB = (ProgressBar)findViewById(R.id.pb_mission);

        txt_height = (TextView) findViewById(R.id.txt_height);
        txt_speed = (TextView) findViewById(R.id.txt_speed);
        txt_range = (TextView) findViewById(R.id.txt_range);
        txt_wifi_level = (TextView) findViewById(R.id.txt_wifi_level);
        txt_flight_time = (TextView) findViewById(R.id.txt_flight_time);

        layout_wifi_status = (LinearLayout) findViewById(R.id.layout_wifi_status);
        layout_aircraft_status = (LinearLayout) findViewById(R.id.layout_aircraft_status);

        mFadeoutAnimation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_out);

        img_menu.setOnClickListener(this);
        img_location.setOnClickListener(this);
        img_setting.setOnClickListener(this);
        img_drone.setOnClickListener(this);
        img_drawline.setOnClickListener(this);
        img_download.setOnClickListener(this);
        txt_centerDrone.setOnClickListener(this);
        txt_centerMe.setOnClickListener(this);
        img_delete.setOnClickListener(this);
        img_pause.setOnClickListener(this);
        img_return_to_home.setOnClickListener(this);
        img_drone_land.setOnClickListener(this);

        mGestureDetector = new GestureDetector(this, new GestureListener());
        mMapShelterView.setOnTouchListener(this);
        img_menu.setVisibility(View.GONE);
        line1.setVisibility(View.VISIBLE);

    }

    public void InitGooglemap() {
        if (googleMap == null) {
            googleMap = ((MapFragment) getFragmentManager().findFragmentById(
                    R.id.map_fragment)).getMap();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            getLatLong();
            googleMap.setMyLocationEnabled(true);
            googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            googleMap.getUiSettings().setZoomGesturesEnabled(true);
            googleMap.getUiSettings().setAllGesturesEnabled(true);
            googleMap.getUiSettings().setScrollGesturesEnabled(true);
            googleMap.getUiSettings().setMapToolbarEnabled(false);
            googleMap.getUiSettings().setCompassEnabled(false);
            googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                @Override
                public void onMapLongClick(LatLng latLng) {
                    img_location.setVisibility(View.VISIBLE);
                    txt_centerDrone.setVisibility(View.VISIBLE);
                    txt_centerMe.setVisibility(View.VISIBLE);

                }
            });
            googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    img_location.setVisibility(View.GONE);
                    txt_centerDrone.setVisibility(View.GONE);
                    txt_centerMe.setVisibility(View.GONE);
                }
            });
            googleMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
                @Override
                public void onMyLocationChange(Location arg0) {
                    // TODO Auto-generated method stub
//                    googleMap.addMarker(new MarkerOptions().position(new LatLng(arg0.getLatitude(), arg0.getLongitude())));
                    mLatitude = arg0.getLatitude();
                    mLongitude=arg0.getLongitude();
                }
            });
        }
        mMapShelterView.setVisibility(View.GONE);
    }

    private void centerOnMe() {
        if (mLatitude != 181.0 && mLongitude != 181.0 && mLatitude != 0.0 && mLongitude != 0.0) {
            LatLng latLng = new LatLng(mLatitude, mLongitude);
            Log.e("Laitude", "----------" + mLatitude);
            Log.e("Longitude", "---------" + mLongitude);
            CameraPosition cameraPosition = new CameraPosition.Builder().target(latLng).zoom(18).build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
        img_location.setVisibility(View.GONE);
        txt_centerDrone.setVisibility(View.GONE);
        txt_centerMe.setVisibility(View.GONE);
    }

    private void centerOnDrone() {
        if (droneLocationLat != 181.0 && droneLocationLng != 181.0 && droneLocationLat != 0.0 && droneLocationLng != 0.0) {
            LatLng latLng = new LatLng(droneLocationLat, droneLocationLng);
            Log.e("Laitude", "----------" + droneLocationLat);
            Log.e("Longitude", "---------" + droneLocationLng);
            CameraPosition cameraPosition = new CameraPosition.Builder().target(latLng).zoom(18).build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    private void getBatteryStatus() {
        try {
            SentekApplication.getProductInstance().getBattery().setBatteryStateUpdateCallback(
                    new DJIBattery.DJIBatteryStateUpdateCallback() {
                        @Override
                        public void onResult(final DJIBattery.DJIBatteryState djiBatteryState) {
                            secondsTimerCount = secondsTimerCount + 1;
                            flightTimeInSeconds=flightTimeInSeconds+1;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    txt_battery_level.setText(djiBatteryState.getBatteryEnergyRemainingPercent() + "%");
                                    if (SentekApplication.getAircraftInstance().getFlightController().getCurrentState().isFlying() || SentekApplication.getAircraftInstance().getFlightController().getCurrentState().areMotorsOn()) {
                                        hideAndShowFlightControlButtons(true);
                                    } else {
                                        hideAndShowFlightControlButtons(false);
                                    }

                                    txt_flight_time.setText("Flight time : " + secondsToMinutesAndSeconds(flightTimeInSeconds)+ " min");
//                                    getSpeedStatus();
                                    getWIFIStatus();
                                    if(pathToFollow.size()>0){
                                        img_delete.setEnabled(true);
                                        setBW(img_delete);
                                        img_delete.setColorFilter(0);
                                    }else {
                                        img_delete.setEnabled(false);
                                        setBW(img_delete);
                                    }
                                    travelledPath.add(new LatLng(droneLocationLat, droneLocationLng));
                                    if(travelledPath.size()>10){
                                        int i=travelledPath.size()-2;
                                        googleMap.addPolyline(new PolylineOptions()
                                                .add(new LatLng(travelledPath.get(i).latitude, travelledPath.get(i).longitude),
                                                        new LatLng(travelledPath.get(i + 1).latitude, travelledPath.get(i + 1).longitude))
                                                .width(5).color(ContextCompat.getColor(MainActivity.this, R.color.white)));
                                    }
                                }
                            });
                        }
                    }
            );
        } catch (Exception exception) {
        }
    }

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            return false;
        }
    }

    public void CheckConnection() {
        ConnectivityManager connectionManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        wifiCheck = connectionManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiCheck.isConnected()) {
            // Do whatever here
            String a = wifiCheck.getTypeName();
            String b = wifiCheck.getExtraInfo();
            String c = wifiCheck.getReason();
            String d = wifiCheck.getSubtypeName();
            String e = wifiCheck.getSubtype() + "";
            String f = wifiCheck.getType() + "";
            String g = wifiCheck.getDetailedState() + "";
            wifiName= b;
            Log.e("ConnectionWifi", a + "\n" + b + "\n" + c + "\n" + d + "\n" + e + "\n" + f + "\n" + g);
        } else {
            Log.e("ConnectionWifi", "WiFi is not Connected");
        }
    }

    @Override
    public void onClick(View v) {
        img_location.setVisibility(View.GONE);
        txt_centerMe.setVisibility(View.GONE);
        txt_centerDrone.setVisibility(View.GONE);

        switch (v.getId()) {
/*            case R.id.img_menu:
                if (line1.getVisibility() == View.VISIBLE) {
                    line1.setVisibility(View.GONE);
                } else {
                    line1.setVisibility(View.VISIBLE);
                }
                break;*/
            case R.id.img_location:
                break;
            case R.id.img_setting:
                settingDialog();
                break;
            case R.id.img_drone:
                if(pathToFollow.size()>0) {
                    DialogStartMission();
                }else{
                    showAlertDialog(mDialog,"Oops, You have not planned a mission yet, Please plan a mission before flying the drone.");
                }
                break;
            case R.id.img_drawline:
                missionUploaded = false;
                pathToFollow.clear();
                mLatlngs.clear();
                googleMap.clear();
                mPolylineOptions = null;
                mPolygonOptions = null;
                mDrawFinished = !mDrawFinished;
                mMapShelterView.setVisibility(View.VISIBLE);
                break;
            case R.id.img_download:
                Log.e("inside","img_download");
                if(null!=SentekApplication.getProductInstance()|| (null!=SentekApplication.getProductInstance() && SentekApplication.getProductInstance().isConnected())) {
                    Log.e("inside", "img_download not null");
                    downloadMission(1);
                    if(SentekApplication.getProductInstance().getMissionManager().isMissionReadyToExecute){
                    }else{
                        Log.e("inside", "img_download else");
//                        showAlertDialog(mDialog,"Sorry, there is no mission uploaded to the drone right now.");
                    }
                }
                break;
            case R.id.img_delete:
                String msg = "This will clear the existing mission. Are you sure?";
                showDeletMissionDialog(msg);
                break;

            case R.id.img_pause:
                if (!missionPaused) {
                    pauseWaypointMission();
                } else {
                    resumeWaypointMission();
                }
                break;

            case R.id.img_return_to_home:
                String returnHome = "Return To Home";
                DialogReturnToHome(returnHome, 1);
                break;

            case R.id.img_drone_land:
                String landImmidiately = "Land Immediately";
                DialogReturnToHome(landImmidiately, 2);
                break;

            case R.id.txt_centerMe:
                centerOnMe();
                break;

            case R.id.txt_centerDrone:
                centerOnDrone();
                break;
        }
    }

    public void getLatLong() {
        if (!checkAllPermission()) {
            requestPermission();
        } else {
            if (checkPlayServices()) {
                LocationManager locationManagerresume = (LocationManager) getSystemService(LOCATION_SERVICE);
                if (locationManagerresume.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManagerresume.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    newLocationFinder = new LocationFinder();
                    newLocationFinder.getLocation(MainActivity.this, mLocationResult);
                } else {
                    showGPSDisabledAlertToUser();
                }
            }
        }
    }

    private void showGPSDisabledAlertToUser() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder
                .setMessage(
                        "GPS is disabled in your device. Would you like to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                Intent callGPSSettingIntent = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivityForResult(callGPSSettingIntent, GPS_SETTINGS);
                            }
                        });
        alertDialogBuilder.setNegativeButton("No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        LocationManager locationManagerresume = (LocationManager) getSystemService(LOCATION_SERVICE);
                        if (locationManagerresume.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManagerresume.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                            newLocationFinder = new LocationFinder();
                            newLocationFinder.getLocation(MainActivity.this, mLocationResult);
                        } else {
                            showAlertDialog(mDialog, "Can't fetch current location");
                        }
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        }
    }

    private boolean checkAllPermission() {
        int readMediaAccess = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int writeMediaAccess = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int fineLocationAccess = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int courseLocationAccess = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        int photStateAccess = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);
        int internet = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
//        int writeMediaAccess = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (readMediaAccess == PackageManager.PERMISSION_GRANTED &&
                writeMediaAccess == PackageManager.PERMISSION_GRANTED &&
                fineLocationAccess == PackageManager.PERMISSION_GRANTED &&
                courseLocationAccess == PackageManager.PERMISSION_GRANTED &&
                internet == PackageManager.PERMISSION_GRANTED &&
                photStateAccess == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    public boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        100).show();
            } else {
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLatLong();
                } else {
                }
                break;
        }
    }

    LocationFinder.LocationResult mLocationResult = new LocationFinder.LocationResult() {
        public void gotLocation(final double latitude, final double longitude) {
            if (latitude == 0.0 || longitude == 0.0) {
                return;
            } else {
                runOnUiThread(new Runnable() {
                    public void run() {
                        mLatitude = latitude;
                        mLongitude = longitude;
                        zoomCurrentLocation();
                    }
                });
            }
        }
    };

    private void zoomCurrentLocation() {
        if (mLatitude != 0.0 && mLongitude != 0.0 && mLatitude != 181.0 && mLongitude != 181.0) {
            LatLng latLng = new LatLng(mLatitude, mLongitude);
            Log.e("Laitude", "----------" + mLatitude);
            Log.e("Longitude", "---------" + mLongitude);
            CameraPosition cameraPosition = new CameraPosition.Builder().target(latLng).zoom(18).build();
            //googleMap.addMarker(new MarkerOptions().position(latLng).title(""));
            //googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GPS_SETTINGS) {
            if (checkPlayServices()) {
                LocationManager locationManagerresume = (LocationManager) getSystemService(LOCATION_SERVICE);
                if (locationManagerresume
                        .isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManagerresume.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    newLocationFinder = new LocationFinder();
                    newLocationFinder.getLocation(MainActivity.this, mLocationResult);

                    zoomCurrentLocation();
                } else {
                    showGPSDisabledAlertToUser();
                }
            }
        } else {

        }
    }
    protected void DialogStartMission() {
        final Dialog dialog;
        dialog = new Dialog(MainActivity.this);
        dialog.setCancelable(true);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setFormat(PixelFormat.TRANSLUCENT);
        Drawable d = new ColorDrawable(Color.BLACK);
        d.setAlpha(0);
        dialog.getWindow().setBackgroundDrawable(d);
        dialog.setContentView(R.layout.dialog_start_mission);
        final SeekBar sb;
        final TextView txt_cancel_mission, txt_start_mission;
        final LinearLayout linearlayout_start_mission;


        sb = (SeekBar) dialog.findViewById(R.id.seek_start_mission);
        txt_cancel_mission = (TextView) dialog.findViewById(R.id.txt_cancel_mission);
        txt_start_mission = (TextView) dialog.findViewById(R.id.txt_start_mission);
        linearlayout_start_mission = (LinearLayout) dialog.findViewById(R.id.linearlayout_start_mission);

        if (!missionUploaded) {
            txt_start_mission.setText("Slide to upload and takeoff");
        } else {

        }

        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                if (arg1 > 95) {
                    arg0.setThumb(getResources().getDrawable(R.drawable.ic_slid));
                    Log.e("Start Mission Seekbar", "onProgressChanged");
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {
                Log.d("onStopTrackingTouch", "onStopTrackingTouch");
                if (arg0.getProgress() < 80) {
                    arg0.setProgress(0);

                } else {
                    arg0.setProgress(100);
                    Log.e("Start Mission Seekbar", "onStopTrackingTouch");
                    configWayPointMission();
              /*      if (missionUploaded) {
                        startWaypointMission();
                    } else {
                        configWayPointMission();
                    }*/
                    dialog.dismiss();
                }
            }
        });

        linearlayout_start_mission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sb.setVisibility(View.VISIBLE);
                sb.setProgress(0);
            }
        });
        txt_cancel_mission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        dialog.show();
    }

    protected void DialogReturnToHome(String msg, final int code) {
        final Dialog dialog;
        dialog = new Dialog(MainActivity.this);
        dialog.setCancelable(true);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setFormat(PixelFormat.TRANSLUCENT);
        Drawable d = new ColorDrawable(Color.BLACK);
        d.setAlpha(0);
        dialog.getWindow().setBackgroundDrawable(d);
        dialog.setContentView(R.layout.dialog_return_home);

        final SeekBar sb;
        final TextView header_text, slider_text;
        final LinearLayout page_background;


        sb = (SeekBar) dialog.findViewById(R.id.myseek);
        header_text = (TextView) dialog.findViewById(R.id.header_text);
        slider_text = (TextView) dialog.findViewById(R.id.slider_text);
        page_background = (LinearLayout) dialog.findViewById(R.id.full_page_layout);

        if (code == 1) {
            slider_text.setText("Slide the button to Right to Return to home and Land");
        } else if (code == 2) {
            slider_text.setText("Slide the button to Right to Land immediately.");
        }
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                if (arg1 > 80) {
                    arg0.setThumb(getResources().getDrawable(R.drawable.ic_slid));
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
                ///  djiSimulatorInitializationData=new DJISimulator.DJISimulatorInitializationData(mLatitude,mLongitude,35,48);
            }

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {
                Log.d("onStopTrackingTouch", "onStopTrackingTouch");
                if (arg0.getProgress() < 80) {
                    arg0.setProgress(0);
                } else {
                    arg0.setProgress(100);
                    if (code == 1) {
                        retunToHomeWayPointMission();
//                        stopWaypointMission();

                    } else if (code == 2) {
                        immidiateLandingMission();
                    }
                    dialog.dismiss();
                }
            }
        });
        page_background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sb.setVisibility(View.VISIBLE);
                sb.setProgress(0);
            }
        });
        header_text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });
        dialog.show();
    }

    public void permissionsruntime() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }

    }
    protected void showDeletMissionDialog(String msg) {
        final Dialog dialog;
        dialog = new Dialog(MainActivity.this);
        dialog.setCancelable(true);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setFormat(PixelFormat.TRANSLUCENT);

        Drawable d = new ColorDrawable(Color.BLACK);
        d.setAlpha(0);
        dialog.getWindow().setBackgroundDrawable(d);

        TextView yes;
        TextView cancel;
        TextView message;

        dialog.setContentView(R.layout.dialog_clear_mission);
        yes = (TextView) dialog.findViewById(R.id.yes);
        cancel = (TextView) dialog.findViewById(R.id.cancel);
        message = (TextView) dialog.findViewById(R.id.message);
        message.setText(msg);
        yes.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                pathToFollow.clear();
                mLatlngs.clear();
                googleMap.clear();
                dialog.dismiss();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    protected void settingDialog() {

        final Dialog dialog;
        dialog = new Dialog(MainActivity.this);
        dialog.setCancelable(true);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setFormat(PixelFormat.TRANSLUCENT);

        Drawable d = new ColorDrawable(Color.BLACK);
        d.setAlpha(0);
        dialog.getWindow().setBackgroundDrawable(d);
        final TextView vehicle,  height, mission, overlap, sidelap, hatchangle, wizard,auto_speed;
        final SeekBar seekbar_height, seekbar_mission, seekbar_sidelap, seekbar_hatchangle;

        dialog.setContentView(R.layout.dialog_setting);
        wizard = (TextView) dialog.findViewById(R.id.wizard);
        auto_speed = (TextView) dialog.findViewById(R.id.auto_speed);
        vehicle = (TextView) dialog.findViewById(R.id.vehicle);

        final Spinner varint ;
        varint = (Spinner) dialog.findViewById(R.id.varint);

        List<String> varintList;
        varintList = new ArrayList<String> ();
        varintList.add("V0 (35\u00B0HFOV)");
        varintList.add("V1 (48\u00B0HFOV)");
        varintList.add("V2 (60\u00B0HFOV)");

        ArrayAdapter varintAdapter =new ArrayAdapter<String>
                (this,android.R.layout.simple_dropdown_item_1line, varintList);
        varint.setAdapter(varintAdapter);
        varint.setSelection(MissionVariant);

        height = (TextView) dialog.findViewById(R.id.height);
        mission = (TextView) dialog.findViewById(R.id.mission);
        overlap = (TextView) dialog.findViewById(R.id.overlap);
        sidelap = (TextView) dialog.findViewById(R.id.sidelap);
        hatchangle = (TextView) dialog.findViewById(R.id.hatchangle);
        seekbar_height = (SeekBar) dialog.findViewById(R.id.seekbar_height);
        seekbar_mission = (SeekBar) dialog.findViewById(R.id.seekbar_mission);
        seekbar_sidelap = (SeekBar) dialog.findViewById(R.id.seekbar_sidelap);
        seekbar_hatchangle = (SeekBar) dialog.findViewById(R.id.seekbar_hatchangle);

        missionHeight = sp.getMissionHeight();
        missionSpeed = sp.getMissionSpeed();
        MissionSideLap = sp.getMissionSideLap();
        MissionHatchAngle =sp.getMissionHatchAngle();
        MissionVariant = sp.getMissionVariant();
        MissionOverlap = sp.getMissionOverlap();

        final int missionHeightStep = 1;
        final int missionHeightMax = 400;
        final int missionHeightMin = 50;
//        final int missionHeightMin = 10;
        seekbar_height.setMax( (missionHeightMax - missionHeightMin) / missionHeightStep );

        final int missionSpeedStep = 1;
        final int missionSpeedMax = 35;
        final int missionSpeedMin = 3;
        seekbar_mission.setMax( (missionSpeedMax - missionSpeedMin) / missionSpeedStep );

        final int missionSidelapStep = 1;
        final int missionSidelapMax = 95;
        final int missionSidelapMin = 50;
        seekbar_sidelap.setMax( (missionSidelapMax - missionSidelapMin) / missionSidelapStep );

        final int missionHatchangleStep = 1;
        final int missionHatchangleMax = 90;
        final int missionHatchangleMin = -90;
        seekbar_hatchangle.setMax( (missionHatchangleMax - missionHatchangleMin) / missionHatchangleStep );


// Ex :
// If you want values from 3 to 5 with a step of 0.1 (3, 3.1, 3.2, ..., 5)
// this means that you have 21 possible values in the seekbar.
// So the range of the seek bar will be [0 ; (5-3)/0.1 = 20].



        {
//            double value = missionHeightMin + (missionHeight * missionHeightStep);
            seekbar_height.setProgress(missionHeight - missionHeightMin);
            height.setText(missionHeight + " ft");
        }

        {
//            double value = missionSpeedMin + (missionSpeed * missionSpeedStep);
            seekbar_mission.setProgress(missionSpeed - missionSpeedMin);
            mission.setText(missionSpeed + " mph");

        }

        {
//            double value = missionSidelapMin + (MissionSideLap * missionSidelapStep);
            seekbar_sidelap.setProgress(MissionSideLap - missionSidelapMin);
            sidelap.setText(MissionSideLap + " %");
        }

        {
//            double value = missionHatchangleMin + (MissionHatchAngle * missionHatchangleStep);
            seekbar_hatchangle.setProgress(MissionHatchAngle - missionHatchangleMin);
            hatchangle.setText(MissionHatchAngle + "\u00B0");

        }

        if(!Double.isNaN(getCalculatedOverlap(MissionVariant, missionSpeed, missionHeight))){
            MissionOverlap = getCalculatedOverlap(MissionVariant, missionSpeed, missionHeight);
            overlap.setText(MissionOverlap + "%");
            if(MissionOverlap<60){
                overlap.setTextColor(getResources().getColor(R.color.colorPrimary));
            } else if(MissionOverlap>=60 && MissionOverlap<70){
                overlap.setTextColor(getResources().getColor(R.color.coloryellow));
            }else if(MissionOverlap>=70 ){
                overlap.setTextColor(getResources().getColor(R.color.golorgreen));
            }
        }


        if(!Double.isNaN(MissionSideLap)){
            if(MissionSideLap<60){
                sidelap.setTextColor(getResources().getColor(R.color.colorPrimary));
            } else if(MissionSideLap>=60 && MissionSideLap<70){
                sidelap.setTextColor(getResources().getColor(R.color.coloryellow));
            }else if(MissionSideLap>=70 ){
                sidelap.setTextColor(getResources().getColor(R.color.golorgreen));
            }
        }


        varint.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                MissionVariant = position;
                sp.setMissionVariant(MissionVariant);
                if(!Double.isNaN(getCalculatedOverlap(MissionVariant, missionSpeed, missionHeight))){
                    MissionOverlap = getCalculatedOverlap(MissionVariant, missionSpeed, missionHeight);
                    sp.setMissionOverlap((int)MissionOverlap);
                    overlap.setText(MissionOverlap + "%");
                    if(MissionOverlap<60){
                        overlap.setTextColor(getResources().getColor(R.color.colorPrimary));
                    } else if(MissionOverlap>=60 && MissionOverlap<70){
                        overlap.setTextColor(getResources().getColor(R.color.coloryellow));
                    }else if(MissionOverlap>=70 ){
                        overlap.setTextColor(getResources().getColor(R.color.golorgreen));
                    }
                    drawTheMissionOnMap();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                MissionVariant = 0;
            }
        });

        seekbar_height.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double value = missionHeightMin + (progress * missionHeightStep);
                missionHeight = (int) value;
                sp.setMissionHeight(missionHeight);
                height.setText(missionHeight + " ft");
                if (!Double.isNaN(getCalculatedOverlap(MissionVariant, missionSpeed, missionHeight))) {
                    MissionOverlap = getCalculatedOverlap(MissionVariant, missionSpeed, missionHeight);
                    overlap.setText(MissionOverlap + "%");
                    sp.setMissionOverlap((int) MissionOverlap);
                    if (MissionOverlap < 60) {
                        overlap.setTextColor(getResources().getColor(R.color.colorPrimary));
                    } else if (MissionOverlap >= 60 && MissionOverlap < 70) {
                        overlap.setTextColor(getResources().getColor(R.color.coloryellow));
                    } else if (MissionOverlap >= 70) {
                        overlap.setTextColor(getResources().getColor(R.color.golorgreen));
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                drawTheMissionOnMap();
            }
        });

        seekbar_mission.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double value = missionSpeedMin + (progress * missionSpeedStep);
                missionSpeed = (int) value;
                sp.setMissionSpeed(missionSpeed);
                mission.setText(missionSpeed + " mph");
                if(!Double.isNaN(getCalculatedOverlap(MissionVariant, missionSpeed, missionHeight))){
                    MissionOverlap = getCalculatedOverlap(MissionVariant, missionSpeed, missionHeight);
                    overlap.setText(MissionOverlap + "%");
                    sp.setMissionOverlap((int) MissionOverlap);
                    if(MissionOverlap<60){
                        overlap.setTextColor(getResources().getColor(R.color.colorPrimary));
                    } else if(MissionOverlap>=60 && MissionOverlap<70){
                        overlap.setTextColor(getResources().getColor(R.color.coloryellow));
                    }else if(MissionOverlap>=70 ){
                        overlap.setTextColor(getResources().getColor(R.color.golorgreen));
                    }
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                drawTheMissionOnMap();
            }
        });
        seekbar_sidelap.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double value = missionSidelapMin + (progress * missionSidelapStep);
                MissionSideLap = (int) value;
                sp.setMissionSideLap(MissionSideLap);
                sidelap.setText(MissionSideLap + " %");
                if(!Double.isNaN(getCalculatedOverlap(MissionVariant, missionSpeed, missionHeight))){
                    MissionOverlap = getCalculatedOverlap(MissionVariant, missionSpeed, missionHeight);
                    overlap.setText(MissionOverlap + "%");
                    sp.setMissionOverlap((int) MissionOverlap);
                    if(MissionOverlap<60){
                        overlap.setTextColor(getResources().getColor(R.color.colorPrimary));
                    } else if(MissionOverlap>=60 && MissionOverlap<70){
                        overlap.setTextColor(getResources().getColor(R.color.coloryellow));
                    }else if(MissionOverlap>=70 ){
                        overlap.setTextColor(getResources().getColor(R.color.golorgreen));
                    }
                }

                if(!Double.isNaN(MissionSideLap)){
                    if(MissionSideLap<60){
                        sidelap.setTextColor(getResources().getColor(R.color.colorPrimary));
                    } else if(MissionOverlap>=60 && MissionSideLap<70){
                        sidelap.setTextColor(getResources().getColor(R.color.coloryellow));
                    }else if(MissionSideLap>=70 ){
                        sidelap.setTextColor(getResources().getColor(R.color.golorgreen));
                    }
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                drawTheMissionOnMap();
            }
        });

        seekbar_hatchangle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double value = missionHatchangleMin + (progress * missionHatchangleStep);
                MissionHatchAngle = (int) value;
                sp.setMissionHatchAngle(MissionHatchAngle);
                hatchangle.setText(MissionHatchAngle + "\u00B0");
                if(!Double.isNaN(getCalculatedOverlap(MissionVariant, missionSpeed, missionHeight))){
                    MissionOverlap = getCalculatedOverlap(MissionVariant, missionSpeed, missionHeight);
                    overlap.setText(MissionOverlap + "%");
                    sp.setMissionOverlap((int) MissionOverlap);
                    if(MissionOverlap<60){
                        overlap.setTextColor(getResources().getColor(R.color.colorRed));
                    } else if(MissionOverlap>=60 && MissionOverlap<70){
                        overlap.setTextColor(getResources().getColor(R.color.colorYellow));
                    }else if(MissionOverlap>=70 ){
                        overlap.setTextColor(getResources().getColor(R.color.golorgreen));
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                drawTheMissionOnMap();
            }
        });

        wizard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = "To change the connected drone, turn on the drone and connect it to Mobile Device. " +
                        "\n\n1). If the drone connects to mobile device using USB cable, connect the USB cable. Please ensure that USB debugging is enabled on your mobile device." +
                        "\n\n2). If the drone connects to mobile device via WIFI, Open wifi settings and select the name of the Drone's WIFI." +
                        "\n\nPlease select which settings to open.";
                HowToConnectDialog(mDialog, msg);
            /*    if (mDialog != null && !mDialog.isShowing()) {
                    HowToConnectDialog(mDialog, msg);
                }*/
//                wizardDialog();

            }
        });
        auto_speed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mission.setText(getAutoSpeed(MissionVariant, missionSpeed,missionHeight) + " mph");
                missionSpeed=(int) getAutoSpeed(MissionVariant, missionSpeed,missionHeight);
                sp.setMissionSpeed(missionSpeed);
                double progress;
                progress = ( missionSpeed - missionSpeedMin ) / missionSpeedStep  ;
                seekbar_mission.setProgress((int)progress);
                drawTheMissionOnMap();
            }
        });
        dialog.show();
    }

    protected void wizardDialog() {

        final Dialog dialog;
        dialog = new Dialog(MainActivity.this);
        dialog.setCancelable(false);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setFormat(PixelFormat.TRANSLUCENT);
        Drawable d = new ColorDrawable(Color.BLACK);
        d.setAlpha(0);
        dialog.getWindow().setBackgroundDrawable(d);
        dialog.setContentView(R.layout.dialog_wizard);

        final Button cancel_btn = (Button) dialog.findViewById(R.id.cancel_btn);
        final Button ok_btn = (Button) dialog.findViewById(R.id.ok_btn);
        ok_btn.setText("OK");

        final TextView vehicle_name= (TextView) dialog.findViewById(R.id.vehicle_name);
        final EditText edt_vehicle_pet_name = (EditText) dialog.findViewById(R.id.edt_vehicle_pet_name);

        final ListView previousDroneslist = (ListView) dialog.findViewById(R.id.list);

        if(isDroneConnected){
            vehicle_name.setText(SentekApplication.getProductInstance().getModel()+"");
            if((SentekApplication.getProductInstance().getModel()+"").equalsIgnoreCase(sp.getModel())){
                edt_vehicle_pet_name.setText(sp.getPetName());
            }
        }else{
            vehicle_name.setText("");
        }

        DroneList drone2 =new DroneList();
        // Fill items id drone2
        drone2.setModel(sp.getModel());
        drone2.setName(sp.getName());
        drone2.setPetName(sp.getPetName());
        drone2.setWifiName(sp.getWifiName());
        drone2.setSerialNumber(sp.getSerialNumber());

        droneList.clear();
        droneList.add(drone2);

/*
        ArrayList<DroneList> arrlistDrones= new ArrayList<DroneList>();
        //  fiil items in arrlist
        if(arrlistDrones.size()>0) {
            for (int i=0;i<arrlistDrones.size();i++) {
                DroneList drone2 =new DroneList();
                // Fill items id drone2
                drone2.setModel(sp.getModel());
                drone2.setName(sp.getName());
                drone2.setPetName(sp.getPetName());
                drone2.setWifiName(sp.getWifiName());
                drone2.setSerialNumber(sp.getSerialNumber());
                droneList.add(drone2);
            }
        }
*/

        adapter =  new DroneAdapter( MainActivity.this, droneList);
        previousDroneslist.setAdapter(adapter);

        ok_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DroneList drone= new DroneList();
                if(isDroneConnected) {
                    model = SentekApplication.getProductInstance().getModel().toString();
                    name = SentekApplication.getProductInstance().getModel().getDisplayName();
                    petName = edt_vehicle_pet_name.getText().toString().trim();

                    drone.setModel(model);
                    drone.setName(name);
                    drone.setPetName(petName);
                    drone.setModel(wifiName);

                    if( !(model.equalsIgnoreCase(sp.getModel()) && name.equalsIgnoreCase(sp.getName()) && petName.equalsIgnoreCase(sp.getPetName())) ) {
                        sp.setModel(model);
                        sp.setName(name);
                        sp.setPetName(petName);
                        sp.setWifiName(wifiName);
                        sp.setSerialNumber(serialNumber);
                    }
                    droneList.add(drone);
                    dialog.dismiss();
                }else{
                    ok_btn.setText("Drone is not Connected");
                }
            }
        });

        cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();

    }


    void HowToConnectDialog(final Dialog dialog, String msg) {
        dialog.getWindow().setFormat(PixelFormat.TRANSLUCENT);
        Drawable d = new ColorDrawable(Color.BLACK);
        d.setAlpha(0);
        dialog.getWindow().setBackgroundDrawable(d);
        TextView txt_yes, txt_usb_setting, message;
        dialog.setContentView(R.layout.dialog_drone_connection);
        txt_yes = (TextView) dialog.findViewById(R.id.txt_yes);

        message = (TextView) dialog.findViewById(R.id.message);
        txt_usb_setting = (TextView) dialog.findViewById(R.id.txt_usb_setting);
        message.setText(msg);
        txt_yes.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                startActivity(intent);
                dialog.dismiss();
            }
        });
        txt_usb_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                startActivity(intent);
                dialog.dismiss();
            }
        });

        dialog.show();
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int X1 = (int) event.getX();
        int Y1 = (int) event.getY();
        Point point = new Point();
        point.x = X1;
        point.y = Y1;
        LatLng firstGeoPoint = googleMap.getProjection().fromScreenLocation(
                point);
        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                mPolylineOptions = new PolylineOptions();
                mPolylineOptions.color(Color.RED);
                mPolylineOptions.width(5);
                break;

            case MotionEvent.ACTION_MOVE:
                if (mDrawFinished) {
                    X1 = (int) event.getX();
                    Y1 = (int) event.getY();
                    point = new Point();
                    point.x = X1;
                    point.y = Y1;
                    LatLng geoPoint = googleMap.getProjection()
                            .fromScreenLocation(point);
                    mLatlngs.add(geoPoint);
                    mPolylineOptions.add(geoPoint);
                    googleMap.addPolyline(mPolylineOptions);
                }
                break;

            case MotionEvent.ACTION_UP:
                if (mLatlngs.size() > 0) {
                    Log.d(TAG, "Points array size " + mLatlngs.size());
                    mLatlngs.add(firstGeoPoint);
                    googleMap.clear();
                    mMapShelterView.setVisibility(View.GONE);
                    mDrawFinished = false;
                    mLatlngs=isTooClose(mLatlngs,0.5,0.05);
                    drawTheMissionOnMap();
                }
                break;
        }
        return mGestureDetector.onTouchEvent(event);
    }

    private void drawTheMissionOnMap() {
        if (mLatlngs.size() > 0) {
            double sideLap = MissionSideLap / 100.0;
            double hatchAngle = Math.toRadians(MissionHatchAngle);
            LatLng homeLat=new LatLng(mLatitude, mLongitude);
            float alt  = (float)googleMap.getMyLocation().getAltitude();
            Log.e("alt","" +alt);
            Log.e("curr alt","" +currentAltitude);

            FlightPlan plan = FlightPlan.CreateFlightPlan(homeLat, mLatlngs, currentAltitude, missionHeight*0.3048, sideLap, hatchAngle, MissionVariant);
            if (plan.getErrorCode() != 0) {
                // TODO: Process/handle error
                showAlertDialog(mDialog,"Sorry, couldn't draw this survey region, Please draw again.");
                return;
            }else{
                List<DJIWaypoint> flightPlan = plan.getPlan();
                flightPlan.remove(0);
                flightPlan.remove(flightPlan.size()-1);
                flightPlan.remove(flightPlan.size()-1);

                DjiWaypointsFromCalculation.clear();
                DjiWaypointsFromCalculation.addAll(flightPlan);
                pathToFollow.clear();
                googleMap.clear();

                mPolylineOptions = null;
                mPolygonOptions = new PolygonOptions();
                mPolygonOptions.strokeColor(Color.BLACK);
                mPolygonOptions.strokeWidth(5);
                mPolygonOptions.addAll(mLatlngs);
                googleMap.addPolygon(mPolygonOptions);

                for(int i=0;i<flightPlan.size();i++){
                    pathToFollow.add(new LatLng(flightPlan.get(i).latitude, flightPlan.get(i).longitude));
                    Log.e("Adding", "pathToFollow"+i );
                }

                if(pathToFollow.size()>3){
                    for (int i = 0; i < pathToFollow.size() - 1; i++) {
                        googleMap.addPolyline(new PolylineOptions()
                                .add(new LatLng(pathToFollow.get(i).latitude, pathToFollow.get(i).longitude),
                                        new LatLng(pathToFollow.get(i + 1).latitude, pathToFollow.get(i + 1).longitude))
                                .width(3).color(Color.WHITE).geodesic(true));
                    }
                }else{
                    showAlertDialog(mDialog,"The path is too small for the drone to move properly, please draw a larger area.");
                }
            }
        }
    }

    double getCalculatedOverlap(int GEMS, float speed, float height){
        double overlap=0.0;
        double f=0.0;
        double s=0.0;
        double deltaT=0.0;
        double h=0.0;
        double pitch=0.0;

        if(GEMS==0){
            f=0.0077;
        }else if(GEMS==1){
            f=0.0054;
        } else if(GEMS==2){
            f=0.00414;
        }

        s= speed*0.44704;
        h =height*0.3048;

/*        s= speed;
        h =height;*/

        deltaT=0.9;
        pitch=3.75*Math.pow(10,-6);
        overlap = 100 *(1-((f*s*deltaT)/(960*h*pitch)));
        Log.e("Overlap is-",overlap+"   s-"+s+"  h-"+h+" dT-"+deltaT+"  pitch="+pitch);

        if(overlap<0){
            overlap = 0;
        }else if(overlap>100){
            overlap = 100;
        }
        return Double.parseDouble(new DecimalFormat("#.##").format(Math.abs(overlap)));
    }

    double getAutoSpeed(int GEMS, float speed, float height){
        double autoSpeed=0.0;
        double f=0.0;
        double s=0.0;
        double deltaT=0.0;
        double h=0.0;
        double pitch=0.0;

        if(GEMS==0){
            f=0.0077;
        }else if(GEMS==1){
            f=0.0054;
        } else if(GEMS==2){
            f=0.00414;
        }

        s= speed*0.44704;
        h =height*0.3048;

/*        s= speed;
        h =height;*/

        deltaT=0.9;
        pitch=3.75*Math.pow(10,-6);

        autoSpeed= (644.24*h*pitch) / (f * deltaT)  ;

        if(autoSpeed<3){
            autoSpeed = 3.0;
        }else if(autoSpeed>35){
            autoSpeed = 35;
        }
        return Double.parseDouble(new DecimalFormat("#.#").format(Math.abs(autoSpeed)));
    }


    /*
        +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        +
        +    Code For Hatch Lines  start
        +
        +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    */

    public ArrayList<LatLng> isTooClose(ArrayList<LatLng> allPoints, Double offSetDistance, Double errorDistance) {
        ArrayList<LatLng> pointsToRemove = new ArrayList<>();
        ArrayList<Double> selectedInterPointDistance = new ArrayList<>();
        for (int i = 0; i < allPoints.size() - 2; i++) {
            selectedInterPointDistance.add(findDistanceBetween(allPoints.get(i).latitude, allPoints.get(i).longitude, allPoints.get(i + 1).latitude, allPoints.get(i + 1).longitude));
        }

        for (int i = 0; i < selectedInterPointDistance.size() - 4; i++) {
            if (selectedInterPointDistance.get(i) < offSetDistance + errorDistance && selectedInterPointDistance.get(i + 1) < offSetDistance + errorDistance && selectedInterPointDistance.get(i + 2) < offSetDistance + errorDistance) {
                pointsToRemove.add(allPoints.get(i + 1));
            }
        }
        allPoints.removeAll(pointsToRemove);
        return allPoints;
    }

    private double findDistanceBetween(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        // multiply by 1000 for distance in meters
        return (dist * 1000);
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }
    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    private ArrayList<LatLng> filterSamePoints(ArrayList<LatLng> allPoints) {
        ArrayList<LatLng> pointsToRemove = new ArrayList<>();
        for (int i = 0; i < allPoints.size() - 1; i++) {
            if (allPoints.get(i).equals(allPoints.get(i + 1))) {
                pointsToRemove.add(allPoints.get(i + 1));
            }
        }
        allPoints.removeAll(pointsToRemove);
        return allPoints;
    }


    /*
    +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    +
    +    Code For Hatch Lines  end
    +
    +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    */


    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };

    private void onProductConnectionChange() {
        initMissionManager();
        initFlightController();
    }

    private void initMissionManager() {
        DJIBaseProduct product = SentekApplication.getProductInstance();
        if (product == null ) {
            final String msg = "No vehicle connected. Make sure you have your phone/tablet connected to your vehicle using WiFi or USB. \n\n Follow the instructions provided by DJI for connecting your device to your model of drone.";
            if (mDialog != null && !mDialog.isShowing()) {
                enableDisableAllButtons(false);
                runOnUiThread(new Runnable() {
                    public void run() {
                        showAlertDialog(mDialog, msg);
                    }
                });
            }
            mMissionManager = null;
            return;
        } else {
            if(null!=SentekApplication.getProductInstance() || (null!=SentekApplication.getProductInstance() && SentekApplication.getProductInstance().isConnected())){
                getBatteryStatus();
            }

            if (mDialog != null) {
                mDialog.dismiss();
                if (mDialog != null) {
                    mDialog.dismiss();
                }
            }
            String name = product.getModel().getDisplayName();
            setResultToToast("App connected to : " + name);
            mMissionManager = product.getMissionManager();
            mMissionManager.setMissionProgressStatusCallback(this);
            mMissionManager.setMissionExecutionFinishedCallback(this);
            enableDisableAllButtons(true);
        }
        mWaypointMission = new DJIWaypointMission();
    }

    private void setResultToToast(final String s) {
        try{
            runOnUiThread(new Runnable() {
                              @Override
                              public void run() {
                                  Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
                              }
                          }
            );

        }catch (Exception e){e.printStackTrace();}
    }

    private void initFlightController() {
        DJIBaseProduct product = SentekApplication.getProductInstance();
        if (product != null || (null!=SentekApplication.getProductInstance() && SentekApplication.getProductInstance().isConnected())) {
            if (product instanceof DJIAircraft) {
                mFlightController = ((DJIAircraft) product).getFlightController();
            }
        }

        if (mFlightController != null) {
            mFlightController.setUpdateSystemStateCallback(new DJIFlightControllerDelegate.FlightControllerUpdateSystemStateCallback() {

                @Override
                public void onResult(final DJIFlightControllerDataType.DJIFlightControllerCurrentState state) {
                    droneLocationLat = state.getAircraftLocation().getLatitude();
                    droneLocationLng = state.getAircraftLocation().getLongitude();
                    updateDroneLocation();
                    mHomeLatitude = state.getHomeLocation().getLatitude();
                    mHomeLongitude = state.getHomeLocation().getLongitude();
                    flightState = state.getFlightMode();
                    droneRotaion = state.getAircraftHeadDirection();
                    currentAltitude = state.getAircraftLocation().getAltitude();
                    isDroneConnected = true;
                    updateHomeLocation();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            float altitude = state.getAircraftLocation().getAltitude();
                            float speedX = state.getVelocityX();
                            float VelocityX = state.getVelocityX();
                            float speedY = state.getVelocityY();
                            float VelocityY = state.getVelocityY();
                            float speedZ = state.getVelocityZ();
                            float VelocityZ = state.getVelocityZ();

                            float speedHighest = 00;

                            double realSpeed = Math.abs(Math.sqrt(Math.pow(Math.abs(speedX), 2) + Math.pow(Math.abs(speedY), 2)));

                            if (speedX > speedY) {
                                speedHighest = speedX;
                            } else if (speedX > speedZ) {
                                speedHighest = speedX;
                            } else if (speedY > speedX) {
                                speedHighest = speedY;
                            } else if (speedY > speedZ) {
                                speedHighest = speedY;
                            } else if (speedZ > speedZ) {
                                speedHighest = speedZ;
                            } else if (speedZ > speedZ) {
                                speedHighest = speedZ;
                            } else {
                                speedHighest = speedX;
                            }

                            double speedInMph = speedHighest * 2.23694;
                            double realSpeedInMph = realSpeed * 2.23694;
                            double heightInFeet = altitude * 3.28084;

                            speed = Double.parseDouble(new DecimalFormat("#.##").format(Math.abs(realSpeedInMph)));
                            height = Double.parseDouble(new DecimalFormat("#.##").format(Math.abs(heightInFeet)));
//                          Log.e("getSpeedStatus", "-------------------" + "speed "+speedInMph+"  height "+heightInFeet);
//                          float flightTime = SentekApplication.getAircraftInstance().getFlightController().getCurrentState().getFlyTime();
                            double rangeDist = Double.parseDouble(new DecimalFormat("#.##").format(Math.abs(findDistanceBetweenNew(mHomeLatitude, mHomeLongitude, droneLocationLat, droneLocationLng) * 1000 * 3.28084)));

                            txt_speed.setText("Speed : " + speed + " mph");
                            txt_height.setText("Height : " + height + " ft");
                            txt_range.setText("Distance from launch \npoint : " + rangeDist + "ft");
                        }
                    });
                }
            });
        }
    }

    private String secondsToMinutesAndSeconds(float seconds){
        float input=seconds;
        int numberOfDays;
        int numberOfHours;
        int numberOfMinutes;
        int numberOfSeconds;

        numberOfDays = (int) (input / 86400);
        numberOfHours = (int) ((input % 86400 ) / 3600) ;
        numberOfMinutes =(int) (((input % 86400 ) % 3600 ) / 60);
        numberOfSeconds = (int) (((input % 86400 ) % 3600 ) % 60  );

        String flightTime = numberOfMinutes +":"+numberOfSeconds;
        return  flightTime;
    }


    /**
     * DJIMissionManager Delegate Methods
     */
    @Override
    public void missionProgressStatus(DJIMission.DJIMissionProgressStatus progressStatus) {

    }

    /**
     * DJIMissionManager Delegate Methods
     */
    @Override
    public void onResult(final DJIError error) {
        runOnUiThread(new Runnable() {
            public void run() {
//                showAlertDialog(mDialog, "Execution finished: " + (error == null ? "Success!" : error.getDescription()));
            }
        });
    }


    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    // Update the drone location based on states from MCU.
    private void updateDroneLocation() {
        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);

        //Create MarkerOptions object
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(pos);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft)).anchor(0.5f,0.5f).rotation(droneRotaion);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (droneMarker != null) {
                    droneMarker.remove();
                }
                if (checkGpsCoordination(droneLocationLat, droneLocationLng)) {
                    droneMarker = googleMap.addMarker(markerOptions);
                }
            }
        });
    }

    private void updateHomeLocation() {
        LatLng pos = new LatLng(mHomeLatitude, mHomeLongitude);
        //Create MarkerOptions object
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(pos);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_return_home));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (HomeMarker != null) {
                    HomeMarker.remove();
                }
                if (checkGpsCoordination(mHomeLatitude, mHomeLongitude)) {
                    HomeMarker = googleMap.addMarker(markerOptions);
                }
            }
        });
    }

    private void downloadMissionAndDrawMission() {
        googleMap.clear();
        SentekApplication.getProductInstance().
                getMissionManager().
                downloadMission(new DJIMission.DJIMissionProgressHandler() {
                    @Override
                    public void onProgress(DJIMission.DJIProgressType type, float progress) {
                    }
                }, new DJIBaseComponent.DJICompletionCallbackWith<DJIMission>() {
                    @Override
                    public void onSuccess(DJIMission mission) {
                        final StringBuffer sb = new StringBuffer();
                        if (mission instanceof DJIWaypointMission) {
                            Functions.addLineToSB(sb, "Current Mission", "");
                            Functions.addLineToSB(sb, "Flight path mode", ((DJIWaypointMission) mission).flightPathMode);
                            Functions.addLineToSB(sb, "Max flight speed", ((DJIWaypointMission) mission).maxFlightSpeed);
                            Functions.addLineToSB(sb, "Auto flight speed", ((DJIWaypointMission) mission).autoFlightSpeed);
                            for (int i = 0; i < ((DJIWaypointMission) mission).getWaypointCount(); i++) {
                                pathToFollow.add(new LatLng(((DJIWaypointMission) mission).getWaypointAtIndex(i).latitude, ((DJIWaypointMission) mission).getWaypointAtIndex(i).longitude));
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    PolygonOptions polygonOptionsToSend1 = new PolygonOptions();
                                    polygonOptionsToSend1.strokeColor(Color.WHITE);
                                    polygonOptionsToSend1.strokeWidth(3);
                                    polygonOptionsToSend1.addAll(pathToFollow);

                                    Log.e("Adding Polygon", polygonOptionsToSend1.getPoints().toString());
                                    googleMap.addPolygon(polygonOptionsToSend1);
                                    centerOnDrone();

                                }
                            });
                        }
                    }

                    public void onFailure(final DJIError error) {
                    }
                });
    }


    boolean isIntValue(String val) {
        try {
            val = val.replace(" ", "");
            Integer.parseInt(val);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private void configWayPointMission() {
        mWaypointMission = new DJIWaypointMission();
        mWaypointMission.finishedAction = mFinishedAction;
        mWaypointMission.headingMode = mHeadingMode;
/*        mWaypointMission.autoFlightSpeed = missionSpeed * 0.44704f;
        mWaypointMission.maxFlightSpeed = 50 * 0.44704f;*/

        if(missionSpeed * 0.44704f>14){
            mWaypointMission.autoFlightSpeed =14;
        }else{
            mWaypointMission.autoFlightSpeed =missionSpeed * 0.44704f;
        }

        mWaypointMission.maxFlightSpeed = 14;

        for(int i=1;i<DjiWaypointsFromCalculation.size()-1;i++){
            mWaypointMission.addWaypoint(new DJIWaypoint(DjiWaypointsFromCalculation.get(i).latitude, DjiWaypointsFromCalculation.get(i).longitude, (float)(missionHeight * 0.3048)));
            Log.e("Current waypoint", "" + DjiWaypointsFromCalculation.get(i).latitude + " , " + DjiWaypointsFromCalculation.get(i).longitude + " , " + DjiWaypointsFromCalculation.get(i).altitude);
        }
/*        for(int i=0;i<DjiWaypointsFromCalculation.size();i++){
            mWaypointMission.addWaypoint(DjiWaypointsFromCalculation.get(i));
            Log.e("Current waypoint", "" + DjiWaypointsFromCalculation.get(i).latitude + " , " + DjiWaypointsFromCalculation.get(i).longitude + " , " + DjiWaypointsFromCalculation.get(i).altitude);
        }*/

//        mWaypointMission.addWaypoints(DjiWaypointsFromCalculation);
        Log.e("Total Distance f", "" + findTotalDistancebetween(pathToFollow));
        prepareWayPointMission();
    }

    private void prepareWayPointMission() {
        Log.e("Inside", "prepareWayPointMission");
        if (mMissionManager != null && mWaypointMission != null) {
            DJIMission.DJIMissionProgressHandler progressHandler = new DJIMission.DJIMissionProgressHandler() {
                @Override
                public void onProgress(DJIMission.DJIProgressType type, float progress) {
                    setProgressBar((int) (progress * 100f));
                }
            };
            mMissionManager.prepareMission(mWaypointMission, progressHandler, new DJIBaseComponent.DJICompletionCallback() {
                @Override
                public void onResult(final DJIError error) {
                    if (error != null) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                showAlertDialog(mDialog, "" + (error == null ? "" : error.getDescription()));
//                                downloadMissionAndDrawMission();
                            }
                        });
                    } else {
                        startWaypointMission();
                    }
                }
            });
        }
    }

    public void hideAndShowFlightControlButtons(boolean show) {
        if (show) {
            img_drone.setVisibility(View.GONE);
            line1.setVisibility(View.GONE);
            img_menu.setVisibility(View.GONE);
            img_setting.setVisibility(View.GONE);

            img_pause.setVisibility(View.VISIBLE);
            img_return_to_home.setVisibility(View.VISIBLE);
            img_drone_land.setVisibility(View.VISIBLE);

            layout_battery_status.setVisibility(View.VISIBLE);
            layout_wifi_status.setVisibility(View.VISIBLE);
            layout_aircraft_status.setVisibility(View.VISIBLE);


        } else if (!show) {
            img_drone.setVisibility(View.VISIBLE);
            line1.setVisibility(View.VISIBLE);
//            img_menu.setVisibility(View.VISIBLE);
            img_setting.setVisibility(View.VISIBLE);

            img_pause.setVisibility(View.GONE);
            img_return_to_home.setVisibility(View.GONE);
            img_drone_land.setVisibility(View.GONE);

            layout_battery_status.setVisibility(View.GONE);
            layout_wifi_status.setVisibility(View.GONE);
            layout_aircraft_status.setVisibility(View.GONE);

        }
    }

    public void enableDisableFlightControlButtons(boolean enabled) {
        if (enabled) {
            Log.e("FlightControlButtons", "enabled");
            img_drone.setEnabled(false);
            img_pause.setEnabled(true);
            img_return_to_home.setEnabled(true);
            img_drone_land.setEnabled(true);
        } else if (!enabled) {
            Log.e("FlightControlButtons", "Not enabled");
            img_drone.setEnabled(true);
            img_pause.setEnabled(false);
            img_return_to_home.setEnabled(false);
            img_drone_land.setEnabled(false);
        }
    }


    public void enableDisableAllButtons(boolean enabled) {
        if (enabled) {
            Log.e("AllButtons", "enabled");
            img_drone.setColorFilter(0);
            img_setting.setColorFilter(0);
            img_menu.setColorFilter(0);
            img_delete.setColorFilter(0);
            img_download.setColorFilter(0);
            img_drawline.setColorFilter(0);
            img_pause.setColorFilter(0);

            img_drone.setEnabled(true);
            img_setting.setEnabled(true);
            img_menu.setEnabled(true);
            img_delete.setEnabled(true);
            img_download.setEnabled(true);
            img_drawline.setEnabled(true);
            img_pause.setEnabled(true);

            img_pause.setEnabled(true);
            img_return_to_home.setEnabled(true);
            img_drone_land.setEnabled(true);
            img_drone.setEnabled(true);

        } else if (!enabled) {
            Log.e("AllButtons", "Desabled");
            setBW(img_drone);
            setBW(img_setting);
            setBW(img_menu);
            setBW(img_delete);
            setBW(img_download);
            setBW(img_drawline);
            setBW(img_pause);

            img_drone.setEnabled(false);
            img_setting.setEnabled(false);
            img_menu.setEnabled(false);
            img_delete.setEnabled(false);
            img_download.setEnabled(false);
            img_drawline.setEnabled(false);
            img_pause.setEnabled(false);

            img_pause.setEnabled(false);
            img_return_to_home.setEnabled(false);
            img_drone_land.setEnabled(false);
            img_drone.setEnabled(false);
        }
    }

    private void startWaypointMission() {
        Log.e("Inside", "startWaypointMission");
        if (mMissionManager != null) {
            mMissionManager.startMissionExecution(new DJIBaseComponent.DJICompletionCallback() {
                @Override
                public void onResult(final DJIError error) {
                    if (error == null) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                isDroneFlying = true;
                                flightTimeInSeconds=0;
                                missionPaused=false;
                                hideAndShowFlightControlButtons(true);
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                showAlertDialog(mDialog, "" + (error == null ? "" : error.getDescription()));
                                isDroneFlying = false;
//                                hideAndShowFlightControlButtons(false);
                            }
                        });
                    }
                    Log.e("Mission Start:", "------------" + (error == null ? "Successfully" : error.getDescription()));
                }
            });
        }
    }

    private void stopWaypointMission() {
        Log.e("Inside", "stopWaypointMission 1");
        if (mMissionManager != null) {
            mMissionManager.stopMissionExecution(new DJIBaseComponent.DJICompletionCallback() {
                @Override
                public void onResult(final DJIError error) {
                    Log.e("Inside", "stopWaypointMission 2");
                    if (error==null) {
                        Log.e("Inside", "stopWaypointMission 2.1");
                        Log.e("Mission Stop: ", "--------------" + "Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));
                        missionUploaded = !missionUploaded;
                        DJIMission.DJIMissionProgressHandler progressHandler = new DJIMission.DJIMissionProgressHandler() {
                            @Override
                            public void onProgress(DJIMission.DJIProgressType type, float progress) {
                            }
                        };

                        if (mWaypointMission != null) {
                            mWaypointMission.removeAllWaypoints();
                            Log.e("Removeall", "--------------");

                        }

                        DJIWaypointMission goHomeMission = new DJIWaypointMission();
                        goHomeMission.finishedAction = mFinishedAction;
                        goHomeMission.headingMode = mHeadingMode;
                        goHomeMission.autoFlightSpeed = 5;
                        goHomeMission.maxFlightSpeed = 10;
                        goHomeMission.addWaypoint(new DJIWaypoint(SentekApplication.getAircraftInstance().getFlightController().getCurrentState().getHomeLocation().getLatitude(), SentekApplication.getAircraftInstance().getFlightController().getCurrentState().getHomeLocation().getLongitude(), missionHeight * 0.3048f));
                        Log.e("Inside", "stopWaypointMission 3");
                        Log.e("Mission Stop: ", "--------------" + "Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));
                        mMissionManager.prepareMission(goHomeMission, progressHandler, new DJIBaseComponent.DJICompletionCallback() {
                            @Override
                            public void onResult(final DJIError error) {
                                Log.e("Inside", "stopWaypointMission 4");
                                Log.e("Mission Stop: ", "--------------" + "Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));

                                if (error == null) {
                                    if (mMissionManager != null) {
                                        Log.e("Inside", "stopWaypointMission 5");
                                        Log.e("Mission Stop: ", "--------------" + "Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));

                                        mMissionManager.startMissionExecution(new DJIBaseComponent.DJICompletionCallback() {
                                            @Override
                                            public void onResult(final DJIError error) {
                                                Log.e("Inside", "stopWaypointMission 6");
                                                Log.e("Mission Stop: ", "--------------" + "Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));

                                                if (error == null) {
                                                    Log.e("Inside", "stopWaypointMission 7");
                                                    Log.e("Mission Stop: ", "--------------" + "Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));

                                                    runOnUiThread(new Runnable() {
                                                        public void run() {
                                                            Log.e("Inside", "stopWaypointMission 8");
                                                            Log.e("Mission Stop: ", "--------------" + "Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));

                                                            showAlertDialog(mDialog, "Returning to Home" + (error == null ? "" : error.getDescription()));
                                                            isDroneFlying = true;
                                                            hideAndShowFlightControlButtons(true);
                                                        }
                                                    });
                                                } else {
                                                    Log.e("Inside", "stopWaypointMission 9");
                                                    Log.e("Mission Stop: ", "--------------" + "Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));

                                                    runOnUiThread(new Runnable() {
                                                        public void run() {
                                                            Log.e("Inside", "stopWaypointMission 10");
                                                            Log.e("Mission Stop: ", "--------------" + "Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));

                                                            showAlertDialog(mDialog, "Unable to return to home " + (error == null ? "" : error.getDescription()));
                                                            isDroneFlying = false;
//                                hideAndShowFlightControlButtons(false);
                                                        }
                                                    });
                                                }
                                                Log.e("Inside", "stopWaypointMission 11");
                                                Log.e("Mission Start:", "------------" + (error == null ? "Successfully" : error.getDescription()));

                                            }
                                        });
                                        Log.e("Inside", "stopWaypointMission 12");
                                        Log.e("Mission Stop: ", "--------------" + "Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));

                                    }
                                } else {
                                    Log.e("Inside", "stopWaypointMission 13");
                                    Log.e("Mission Stop: ", "--------------" + "Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));

                                }
                            }
                        });
                        Log.e("Inside", "stopWaypointMission 14");
                        Log.e("Mission Stop: ", "--------------" + "Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));
                    } else {
                        Log.e("Inside", "stopWaypointMission 15");
                        Log.e("Mission Stop: ", "--------------" + "Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));

                    }
                }
            });
            Log.e("Inside", "stopWaypointMission 16");

            if (mWaypointMission != null) {
                mWaypointMission.removeAllWaypoints();
                Log.e("Removeall", "--------------");
            }
        }
    }

    private void pauseWaypointMission() {
        Log.e("Inside", "stopWaypointMission");
        if (mMissionManager != null) {
            mMissionManager.pauseMissionExecution(new DJIBaseComponent.DJICompletionCallback() {
                @Override
                public void onResult(final DJIError error) {
                    if(error==null){
                        runOnUiThread(new Runnable() {
                            public void run() {
                                missionUploaded = !missionUploaded;
                                missionPaused=true;
                                img_pause.setImageResource(R.drawable.selector_resume);
                            }
                        });
                    }
                    Log.e("Mission Pause: ", "--------------" + (error == null ? "Successfully" : error.getDescription()));
                }
            });
        }
    }

    private void resumeWaypointMission() {
        Log.e("Inside", "stopWaypointMission");
        if (mMissionManager != null) {
            mMissionManager.resumeMissionExecution(new DJIBaseComponent.DJICompletionCallback() {
                @Override
                public void onResult(final DJIError error) {
                    if(error==null){
                        runOnUiThread(new Runnable() {
                            public void run() {
                                missionPaused=false;
                                missionUploaded = !missionUploaded;
                                img_pause.setImageResource(R.drawable.selector_pause);
                            }
                        });
                    }
                    Log.e("Mission Resume: ", "--------------" + (error == null ? "Successfully" : error.getDescription()));
                }
            });
        }
    }

    private void immidiateLandingMission() {
        Log.e("Inside", "stopWaypointMission");
        if (mFlightController != null){
            mFlightController.autoLanding(new DJIBaseComponent.DJICompletionCallback() {
                @Override
                public void onResult(final DJIError error) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            missionUploaded = !missionUploaded;
                            missionPaused=false;
                            hideAndShowFlightControlButtons(false);
                            showAlertDialog(mDialog, "" + (error == null ? "Aircraft Landing Immediately" : error.getDescription()));
                        }
                    });
                }
            });
            if (mWaypointMission != null) {
                mWaypointMission.removeAllWaypoints();
                Log.e("Removeall", "--------------");

            }
        }
    }

    private void retunToHomeWayPointMission() {
        mFlightController.goHome(new DJIBaseComponent.DJICompletionCallback() {
            @Override
            public void onResult(final DJIError error) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (error != null) {
                            missionUploaded = !missionUploaded;
                            showAlertDialog(mDialog, "" + (error == null ? "Aircraft Returning to Home " : error.getDescription()));
                        } else {
                            if (mFlightController != null){
                                mFlightController.autoLanding(new DJIBaseComponent.DJICompletionCallback() {
                                    @Override
                                    public void onResult(final DJIError error) {
                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                missionUploaded = !missionUploaded;
                                                missionPaused=false;
                                                hideAndShowFlightControlButtons(false);
                                                showAlertDialog(mDialog, "" + (error == null ? "Aircraft Returning to Home " : error.getDescription()));
                                            }
                                        });
                                    }
                                });

                            }
                        }
                    }
                });
                if (mWaypointMission != null) {
                    mWaypointMission.removeAllWaypoints();
                    Log.e("Removeall", "--------------");

                }
            }
        });
        Log.e("Inside", "stopWaypointMission");
    }

    private void checkAircraftCurrentStatus(){
        if(null!=SentekApplication.getProductInstance()|| (null!=SentekApplication.getProductInstance() && SentekApplication.getProductInstance().isConnected())){
            downloadMission(0);
        }
    }

    private void downloadMission(final int code) {
        pathToFollow.clear();
        mLatlngs.clear();
        googleMap.clear();
        Log.e("inside","downloadMission");
        try {
            SentekApplication.getProductInstance().
                    getMissionManager().
                    downloadMission(new DJIMission.DJIMissionProgressHandler() {
                                        @Override
                                        public void onProgress(DJIMission.DJIProgressType type, float progress) {
                                        }
                                    }, new DJIBaseComponent.DJICompletionCallbackWith<DJIMission>() {
                                        @Override
                                        public void onSuccess(DJIMission mission) {
                                            Log.e("inside", "downloadMission onSuccess");
                                            if (mission instanceof DJIWaypointMission) {
                                                Log.e("inside", "downloadMission mission");
                                                for (int i = 0; i < ((DJIWaypointMission) mission).getWaypointCount(); i++) {
                                                    pathToFollow.add(new LatLng(((DJIWaypointMission) mission).getWaypointAtIndex(i).latitude, ((DJIWaypointMission) mission).getWaypointAtIndex(i).longitude));
                                                    Log.e("inside", "downloadMission for");
                                                }
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        PolygonOptions polygonOptionsToSend1 = new PolygonOptions();
                                                        polygonOptionsToSend1.strokeColor(Color.WHITE);
                                                        polygonOptionsToSend1.strokeWidth(3);
                                                        polygonOptionsToSend1.addAll(pathToFollow);
                                                        Log.e("Adding Polygon", polygonOptionsToSend1.getPoints().toString());
                                                        if(pathToFollow.size()>1){
                                                            googleMap.addPolygon(polygonOptionsToSend1);
                                                            centerOnDrone();
                                                        }
                                                    }
                                                });
                                            } else {
                                                Log.e("inside", "downloadMission else");
                                                if (code == 1) {
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            showAlertDialog(mDialog, "There is no downloadable mission onboard.");
                                                        }
                                                    });
                                                }
                                            }
                                        }

                                        public void onFailure(final DJIError error) {
                                            Log.e("inside", "downloadMission onFailure");
                                            DJIMission mission = SentekApplication.getProductInstance().getMissionManager().getCurrentExecutingMission();
                                            if (mission instanceof DJIWaypointMission) {
                                                Log.e("inside", "downloadMission mission");
                                                for (int i = 0; i < ((DJIWaypointMission) mission).getWaypointCount(); i++) {
                                                    pathToFollow.add(new LatLng(((DJIWaypointMission) mission).getWaypointAtIndex(i).latitude, ((DJIWaypointMission) mission).getWaypointAtIndex(i).longitude));
                                                    Log.e("inside", "downloadMission for");
                                                }
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        PolygonOptions polygonOptionsToSend1 = new PolygonOptions();
                                                        polygonOptionsToSend1.strokeColor(Color.WHITE);
                                                        polygonOptionsToSend1.strokeWidth(3);
                                                        polygonOptionsToSend1.addAll(pathToFollow);
                                                        Log.e("Adding Polygon", polygonOptionsToSend1.getPoints().toString());
                                                        googleMap.addPolygon(polygonOptionsToSend1);
                                                        centerOnDrone();
                                                    }
                                                });
                                            } else {
                                                Log.e("inside", "downloadMission else");
                                                if (code == 1) {
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            showAlertDialog(mDialog, "There is no downloadable mission onboard.");
                                                        }
                                                    });
                                                }
                                            }
                                        }
                                    }
                    );
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void setProgressBar(final int progress) {
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                if (progress > 0 && progress < 100) {
                    mPB.setVisibility(View.VISIBLE);
                    mPB.setProgress(100);
                    mFadeoutAnimation.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }
                        @Override
                        public void onAnimationEnd(Animation animation) {
                            mPB.setVisibility(View.INVISIBLE);
                        }
                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                    });
                    mUIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mPB.startAnimation(mFadeoutAnimation);
                        }
                    });
                } else if (progress < 0) {
                    mPB.setVisibility(View.INVISIBLE);
                    mPB.setProgress(0);
                } else if (progress >= 100) {
                    mPB.setVisibility(View.INVISIBLE);
                    mPB.setProgress(0);
                } else {
                    mPB.setVisibility(View.INVISIBLE);
                    mPB.setProgress(0);
                }
            }
        });
    }

    void showAlertDialog(final Dialog dialog, String msg) {
        dialog.getWindow().setFormat(PixelFormat.TRANSLUCENT);
        Drawable d = new ColorDrawable(Color.BLACK);
        d.setAlpha(0);

        dialog.getWindow().setBackgroundDrawable(d);
        TextView txt_yes, message;
        dialog.setContentView(R.layout.dialog_connect_drone);
        txt_yes = (TextView) dialog.findViewById(R.id.txt_yes);
        message = (TextView) dialog.findViewById(R.id.message);
        message.setText(msg);
        txt_yes.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void setBW(ImageView iv) {
        float brightness = 10; // change values to suite your need
        float[] colorMatrix = {
                0.33f, 0.33f, 0.33f, 0, brightness,
                0.33f, 0.33f, 0.33f, 0, brightness,
                0.33f, 0.33f, 0.33f, 0, brightness,
                0, 0, 0, 1, 0
        };
        ColorFilter colorFilter = new ColorMatrixColorFilter(colorMatrix);
        iv.setColorFilter(colorFilter);
    }


    private double findTotalDistancebetween(ArrayList<LatLng> allPoints) {
        double totalDist=0.0;
        for (int i = 0; i < allPoints.size() - 1; i++) {
            double tempdist =  Double.parseDouble(new DecimalFormat("##.######").format(Math.abs(findDistanceBetweenNew(allPoints.get(i).latitude, allPoints.get(i).longitude, allPoints.get(i + 1).latitude, allPoints.get(i + 1).longitude))));
            if(tempdist>0 && !Double.isNaN(tempdist) ){
                totalDist = totalDist +tempdist;
            }
            Log.e("Total Distance", totalDist+"");
        }
        return totalDist *1000 ;
    }

    private double findDistanceBetweenNew(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.15150000000000;
        // multiply by 1000 for distance in meters
        return (dist);
    }


    // Speed & altitude status drone
    private int getSpeedStatus(){
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    float altitude = SentekApplication.getAircraftInstance().getFlightController().getCurrentState().getAircraftLocation().getAltitude();
                    float speedX = SentekApplication.getAircraftInstance().getFlightController().getCurrentState().getVelocityX();
                    float VelocityX = SentekApplication.getAircraftInstance().getFlightController().getCurrentState().getVelocityX();
                    float speedY = SentekApplication.getAircraftInstance().getFlightController().getCurrentState().getVelocityY();
                    float VelocityY = SentekApplication.getAircraftInstance().getFlightController().getCurrentState().getVelocityY();
                    float speedZ = SentekApplication.getAircraftInstance().getFlightController().getCurrentState().getVelocityZ();
                    float VelocityZ = SentekApplication.getAircraftInstance().getFlightController().getCurrentState().getVelocityZ();

                    float speedHighest = 00;

                    double realSpeed = Math.abs(Math.sqrt(Math.pow(Math.abs(speedX), 2) + Math.pow(Math.abs(speedY), 2)));

                    if (speedX > speedY) {
                        speedHighest = speedX;
                    } else if (speedX > speedZ) {
                        speedHighest = speedX;
                    } else if (speedY > speedX) {
                        speedHighest = speedY;
                    } else if (speedY > speedZ) {
                        speedHighest = speedY;
                    } else if (speedZ > speedZ) {
                        speedHighest = speedZ;
                    } else if (speedZ > speedZ) {
                        speedHighest = speedZ;
                    } else {
                        speedHighest = speedX;
                    }

                    double speedInMph = speedHighest * 2.23694;
                    double realSpeedInMph = realSpeed * 2.23694;
                    double heightInFeet = altitude * 3.28084;

                    speed = Double.parseDouble(new DecimalFormat("#.##").format(Math.abs(realSpeedInMph)));
                    height = Double.parseDouble(new DecimalFormat("#.##").format(Math.abs(heightInFeet)));
//                            Log.e("getSpeedStatus", "-------------------" + "speed "+speedInMph+"  height "+heightInFeet);
//                    float flightTime = SentekApplication.getAircraftInstance().getFlightController().getCurrentState().getFlyTime();
                    double rangeDist = Double.parseDouble(new DecimalFormat("#.##").format(Math.abs(findDistanceBetweenNew(mHomeLatitude, mHomeLongitude, droneLocationLat, droneLocationLng)*1000 *3.28084)));

                    txt_speed.setText("Speed : " + speed + " mph");
                    txt_height.setText("Height : " + height + " ft");

                }
            });
        }catch (Exception ex){
            Log.e("Exception",ex.getStackTrace()+"");
        }
        return 1;
    }
    // Wifi status drone
    private String getWIFIStatus(){
        try {
            if (SentekApplication.getProductInstance().getAirLink().isWiFiLinkSupported()){
                SentekApplication.getProductInstance().getAirLink().getWiFiLink().setDJIWiFiSignalQualityChangedCallback(new DJIWiFiLink.DJIWiFiSignalQualityChangedCallback() {
                    @Override
                    public void onResult(final DJIWiFiLink.DJIWiFiSignalQuality djiWiFiSignalQuality) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                wifi = djiWiFiSignalQuality.toString();
                                txt_wifi_level.setText("" + wifi);
//                                Log.e("wifi", wifi + "");

                            }
                        });
                    }
                });
            }else if(SentekApplication.getProductInstance().getAirLink().isLBAirLinkSupported()){
                SentekApplication.getProductInstance().getAirLink().getLBAirLink().setDJILBAirLinkFPVBandwidthPercentChangedCallback(new DJILBAirLink.DJILBAirLinkFPVBandwidthPercentChangedCallback() {
                    @Override
                    public void onResult(final float v) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                float value = 100 - v;
                                wifi = value +"";
                                txt_wifi_level.setText("" + wifi+" %");
                            }
                        });
                    }
                });

            }
        }catch (Exception ex){
            Log.e("Exception",ex.getStackTrace()+"");
        }
        return wifi;
    }


      /*
    +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    +
    +    Code For C++ Libraries
    +
    +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    */

    static {
        Log.d(TAG, "Before Load Library");
        System.loadLibrary("PlanFlight");
        Log.d(TAG, "After Load Library");
    }
}
