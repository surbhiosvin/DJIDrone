package com.senteksystems.survey.activities;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.multidex.MultiDex;
import android.util.Log;
import android.widget.Toast;

import com.senteksystems.survey.util.SharedPref;

import dji.sdk.Camera.DJICamera;
import dji.sdk.Gimbal.DJIGimbal;
import dji.sdk.Products.DJIAircraft;
import dji.sdk.Products.DJIHandHeld;
import dji.sdk.SDKManager.DJISDKManager;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIError;
import dji.sdk.base.DJISDKError;

public class MyApplication extends Application {

    SharedPref sp;
    public static boolean isAppRegistered =false;
    private static final String TAG = SentekApplication.class.getName();

    public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";

    private static DJIBaseProduct mProduct;

    private Handler mHandler;

    public static synchronized DJIBaseProduct getProductInstance() {
        if (null == mProduct) {
            mProduct = DJISDKManager.getInstance().getDJIProduct();
        }
        return mProduct;
    }

    public static boolean isAircraftConnected() {
        return getProductInstance() != null && getProductInstance() instanceof DJIAircraft;
    }

    public static boolean isHandHeldConnected() {
        return getProductInstance() != null && getProductInstance() instanceof DJIHandHeld;
    }

    public static synchronized DJIAircraft getAircraftInstance() {
        if (!isAircraftConnected()) return null;
        return (DJIAircraft) getProductInstance();
    }

    public static synchronized DJIHandHeld getHandHeldInstance() {
        if (!isHandHeldConnected()) return null;
        return (DJIHandHeld) getProductInstance();
    }

    public static synchronized DJICamera getCameraInstance() {
        if (getProductInstance() == null) return null;
        Log.e(TAG, "getCameraInstance");
        return getProductInstance().getCamera();
    }

    public static synchronized DJIGimbal getGimbalInstance() {
        // -------- Log.e(TAG, "getGimbalInstance");
        if (getProductInstance() == null) return null;
        return getProductInstance().getGimbal();
    }



    protected void attachBaseContext(Context base){
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    private DJISDKManager.DJISDKManagerCallback mDJISDKManagerCallback = new DJISDKManager.DJISDKManagerCallback() {
        @Override
        public void onGetRegisteredResult(DJIError error) {
            Log.d(TAG, error == null ? "success" : error.getDescription());
            if(error == DJISDKError.REGISTRATION_SUCCESS) {
                DJISDKManager.getInstance().startConnectionToProduct();
                sp.setIsAppRegistered("yes");
                Log.d(TAG, "Register success");
            } else {
                sp.setIsAppRegistered("no");
                Handler handler = new Handler(Looper.getMainLooper());

                handler.post(new Runnable() {

                    @Override
                    public void run() {
//                        PhontomWifiDialog("App is not registered yet, Please connect your Phone to Internet for sometime for necessary Registrations");
                        Toast.makeText(getApplicationContext(), "The App needs to register to DJI, Check network is available and restart the app.", Toast.LENGTH_LONG).show();
                    }
                });
                Log.d(TAG, "Register failed");
            }
            Log.e(TAG, error == null ? "success" : error.getDescription());
        }

        @Override
        public void onProductChanged(DJIBaseProduct oldProduct, DJIBaseProduct newProduct) {
            mProduct = newProduct;
            if(mProduct != null) {
                mProduct.setDJIBaseProductListener(mDJIBaseProductListener);
            }
            notifyStatusChange();
        }
    };

    private DJIBaseProduct.DJIBaseProductListener mDJIBaseProductListener = new DJIBaseProduct.DJIBaseProductListener() {

        @Override
        public void onComponentChange(DJIBaseProduct.DJIComponentKey key, DJIBaseComponent oldComponent, DJIBaseComponent newComponent) {
            if(newComponent != null) {
                newComponent.setDJIComponentListener(mDJIComponentListener);
            }
            notifyStatusChange();
        }

        @Override
        public void onProductConnectivityChanged(boolean isConnected) {
            notifyStatusChange();
        }

    };

    private DJIBaseComponent.DJIComponentListener mDJIComponentListener = new DJIBaseComponent.DJIComponentListener() {

        @Override
        public void onComponentConnectivityChanged(boolean isConnected) {
            notifyStatusChange();
        }

    };

    @Override
    public void onCreate() {
        super.onCreate();

        mHandler = new Handler(Looper.getMainLooper());
        sp= new SharedPref(this);
        DJISDKManager.getInstance().initSDKManager(this, mDJISDKManagerCallback);
    }

    private void notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }

    private Runnable updateRunnable = new Runnable() {

        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            sendBroadcast(intent);
        }
    };
}
