/*
 * Copyright (C) 2016 The Josh Tool Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mumu.pokemongogo.location;

import android.content.Context;
import android.util.Log;

import com.mumu.pokemongogo.R;

public class FakeLocationManager {
    private final static String TAG = "PokemonGoGo";
    private Context mContext;
    private double mCurrentLat = 25.0335;
    private double mCurrentLong = 121.5642;
    private double mCurrentAlt = 10.2;
    private double mCurrentAccuracy = 6.91;
    private double mAutoLat = -1;
    private double mAutoLong = -1;
    private static final double mPaceLat = 0.000038;
    private static final double mPaceLong = 0.000039;
    private static double mPaceLatShift = 0.000001;
    private static double mPaceLongShift = 0.000001;
    private static double mSpeed = 1;
    private static boolean mIsEnabled;
    private static boolean mIsAutoPilot = false;
    private static boolean mIsAutoPilotInterruptible = true;
    public FakeLocation mDefaultLocation;

    public FakeLocationManager(Context context, FakeLocation defaultLoc) {
        mContext = context;

        // Start fetch information from framework hacking
        boolean shouldUseLastLocation = initLastLocation();

        if (defaultLoc != null) {
            mDefaultLocation = defaultLoc;
        } else if (shouldUseLastLocation) {
            mDefaultLocation = new FakeLocation(mCurrentLat, mCurrentLong, mCurrentAlt, mCurrentAccuracy);
        } else {
            mDefaultLocation = new FakeLocation(25.0335, 121.5642, 10.2, 6.91); //this is the location of Taipei 101
        }

        // Override location now, maybe in the future we should restore actual location
        setDefaultLocation(mDefaultLocation);
    }

    public boolean initLastLocation() {
        boolean oldDataConsist = true;
        String enabled = PropertyService.getSystemProperty(mContext.getString(R.string.property_fake_enable));
        String currentLat = PropertyService.getSystemProperty(mContext.getString(R.string.property_fake_lat));
        String currentLong = PropertyService.getSystemProperty(mContext.getString(R.string.property_fake_long));
        String currentAlt = PropertyService.getSystemProperty(mContext.getString(R.string.property_fake_alt));

        mIsEnabled = enabled.equals("1");

        if (!currentLat.equals("")) {
            mCurrentLat = Double.parseDouble(currentLat);
        } else {
            oldDataConsist = false;
        }

        if (!currentLong.equals("")) {
            mCurrentLong = Double.parseDouble(currentLong);
        } else {
            oldDataConsist = false;
        }

        if (!currentAlt.equals("")) {
            mCurrentAlt = Double.parseDouble(currentAlt);
        }

        Log.d(TAG, "default: lat = " + mCurrentLat + ", long = " + mCurrentLong + ", alt = " + mCurrentAlt);
        return oldDataConsist;
    }

    public void setEnable(boolean enable) {
        if (enable) {
            PropertyService.setSystemProperty(mContext.getString(R.string.property_fake_enable), "1");
        } else {
            PropertyService.setSystemProperty(mContext.getString(R.string.property_fake_enable), "0");
        }
    }

    public boolean getEnable() {
        return mIsEnabled;
    }

    public FakeLocation getCurrentLocation() {
        return new FakeLocation(mCurrentLat, mCurrentLong, mCurrentAlt, mCurrentAccuracy);
    }

    private void setDefaultLocation(FakeLocation loc) {
        if (loc == null) {
            Log.e(TAG, "Cannot set default location null");
        } else {
            setLocation(loc);
        }
    }

    public void setLocation(FakeLocation loc) {
        Log.d(TAG, "Set location " + loc.toString());
        setLatitude(loc.latitude);
        setLongitude(loc.longitude);
        setAltitude(loc.altitude);
        setAccuracy(loc.accuracy);
    }

    public void setSpeed(double speed) {
        if (speed > 0.0)
            mSpeed = speed;
        else
            Log.w(TAG, "Unsupported speed");
    }

    public void autoPilotTo(double targetLat, double targetLong, boolean interruptible) {
        mIsAutoPilot = true;
        mIsAutoPilotInterruptible = interruptible;
        mAutoLat = targetLat;
        mAutoLong = targetLong;
        new AutoPilotThread().start();
    }

    public void walkPace(int direction, double ratio) {
        double coordinateChange;
        // must introduce random variable
        controlRandomShift();

        // ratio must be within 0.0 ~ 1.0
        if (ratio > 1.0 || ratio < 0.0) {
            Log.e(TAG, "Unacceptable ratio " + ratio + " set to 1.0");
            ratio = 1.0;
        }

        // if auto pilot is on going, cancel it if interruptible
        if (mIsAutoPilot && mIsAutoPilotInterruptible) {
            Log.w(TAG, "Auto pilot is in progress, cancel auto pilot first");
            mIsAutoPilot = false;
        }

        switch (direction) {
            case FakeLocation.EAST:
                coordinateChange = (mPaceLong + mPaceLongShift) * mSpeed;
                mCurrentLong += coordinateChange * ratio;
                mCurrentLat += coordinateChange * (1 - ratio);
                break;
            case FakeLocation.WEST:
                coordinateChange = (mPaceLong + mPaceLongShift) * mSpeed;
                mCurrentLong -= coordinateChange * ratio;
                mCurrentLat -= coordinateChange * (1 - ratio);
                break;
            case FakeLocation.NORTH:
                coordinateChange = (mPaceLat + mPaceLatShift) * mSpeed;
                mCurrentLat += coordinateChange * ratio;
                mCurrentLong += coordinateChange * (1 - ratio);
                break;
            case FakeLocation.SOUTH:
                coordinateChange = (mPaceLat + mPaceLatShift) * mSpeed;
                mCurrentLat -= coordinateChange * ratio;
                mCurrentLong -= coordinateChange * (1 - ratio);
                break;
            case FakeLocation.NORTHWEST:
                coordinateChange = (mPaceLat + mPaceLongShift) * mSpeed;
                mCurrentLong -= coordinateChange * 0.5;
                mCurrentLat += coordinateChange * 0.5;
                break;
            case FakeLocation.WESTSOUTH:
                coordinateChange = (mPaceLong + mPaceLongShift) * mSpeed;
                mCurrentLong -= coordinateChange * 0.5;
                mCurrentLat -= coordinateChange * 0.5;
                break;
            case FakeLocation.SOUTHEAST:
                coordinateChange = (mPaceLat + mPaceLatShift) * mSpeed;
                mCurrentLat -= coordinateChange * 0.5;
                mCurrentLong += coordinateChange * 0.5;
                break;
            case FakeLocation.EASTNORTH:
                coordinateChange = (mPaceLong + mPaceLongShift) * mSpeed;
                mCurrentLong += coordinateChange * 0.5;
                mCurrentLat += coordinateChange * 0.5;
                break;
            case FakeLocation.STAY:
                setAccuracy(mCurrentAccuracy);
                return;
            default:
                Log.d(TAG, "Not supported direction");
                return;
        }

        setLongitude(mCurrentLong);
        setLatitude(mCurrentLat);
        setAccuracy(mCurrentAccuracy);
    }

    private void controlRandomShift() {
        // shift is controlled to be within -0.000001 ~ 0.000001
        double shift = Math.random() / 1000000 - 0.0000005;
        double accShift = Math.random() * 2 - 1;
        mPaceLatShift = mPaceLatShift + shift;
        mPaceLongShift = mPaceLongShift + shift;
        mCurrentAccuracy += accShift;

        if (mPaceLatShift > 0.000002 || mPaceLatShift < -0.000002)
            mPaceLatShift = 0.000001;

        if (mPaceLongShift > 0.000002 || mPaceLongShift < -0.000002)
            mPaceLongShift = 0.000001;

        if (mCurrentAccuracy > 9.9 || mCurrentAccuracy < 1.5)
            mCurrentAccuracy = 5.2;

        Log.d(TAG, "RANDOM lat = " + mPaceLatShift + ", long = " + mPaceLongShift);
    }

    // Setter
    private void setLatitude(double lat) {
        PropertyService.setSystemProperty(mContext.getString(R.string.property_fake_lat), ""+lat);
    }

    private void setLongitude(double lon) {
        PropertyService.setSystemProperty(mContext.getString(R.string.property_fake_long), ""+lon);
    }

    private void setAltitude(double alt) {
        PropertyService.setSystemProperty(mContext.getString(R.string.property_fake_alt), ""+alt);
    }

    private void setAccuracy(double acc) {
        PropertyService.setSystemProperty(mContext.getString(R.string.property_fake_acc), ""+acc);
    }

    // Threading runnable
    private class AutoPilotThread extends Thread {
        @Override
        public void run() {
            Log.d(TAG, "Start auto piloting .. ");
            double diffLat, diffLong, incrementLat, incrementLong;
            while(mIsAutoPilot
                    && !(Math.abs(mCurrentLat - mAutoLat) < mPaceLat)
                    && !(Math.abs(mCurrentLong - mAutoLong) < mPaceLong)) {

                controlRandomShift();
                diffLat = mAutoLat - mCurrentLat;
                diffLong = mAutoLong - mCurrentLong;
                incrementLat = (diffLat / Math.abs(diffLong + diffLat)) * (mPaceLat + mPaceLatShift) * mSpeed;
                incrementLong = (diffLong / Math.abs(diffLong + diffLat)) * (mPaceLong + mPaceLongShift) * mSpeed;

                mCurrentLong += incrementLong;
                mCurrentLat += incrementLat;

                setLongitude(mCurrentLong);
                setLatitude(mCurrentLat);
                setAccuracy(mCurrentAccuracy);

                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mIsAutoPilot = false;
        }
    };
}
