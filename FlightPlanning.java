package com.senteksystems.survey.util;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import dji.sdk.MissionManager.DJIWaypoint;

/**
 * Contains the computed flight plan.
 * Flight plans can be created through CreateFlightPlan method.
 */
public class FlightPlan {
	private static final String TAG = "FlightPlan";

	private int mErrorCode;
	private ArrayList<DJIWaypoint> mPath;

	/**
	 * Returns the error code for this FlightPlan.
	 *
	 * @return The error code. Will return 0 when successful.
	 */
	public int getErrorCode() {
		return mErrorCode;
	}

	/**
	 * Gets the list of way points for the calculated flight.
	 *
	 * @return The list of way points for this flight.
	 */
	public List<DJIWaypoint> getPlan() {
		return mPath;
	}

	/**
	 * Create and return a new flight plan.
	 * Please check the flight plan error code.
	 *
	 * @param surveyRegion   The collection of points defining a closed path on the surface of the Earth.
	 * @param groundAlt      Altitude of the ground over WGS84 reference ellipsoid (m) - used to set height on mission waypoints.
	 * @param missionHeight  Height of mission above ground (m). Mission waypoint altitudes will be this much greater than groundAlt.
	 * @param sideLap        Value between 0.0 and 1.0. 0.0 means no overlap between rows. 0.75 means 75% of an image in one row overlaps with images in the next.
	 * @param hatchAngle     Value defining angle that vehicle passes make with the North vector (radians). 0 means passes are North-South, PI/2 means passes are East-West.
	 * @param gemsVariant    The GEMS variant. (Valid values are 0, 1, and 2.)
	 * @return               The flight plan for the specified region.
	 */
	public static FlightPlan CreateFlightPlan(LatLng home, ArrayList<LatLng> surveyRegion, double groundAlt, double missionHeight, double sideLap, double hatchAngle, int gemsVariant) {
		double[] jniData = new double[surveyRegion.size() * 2 + 2];
		int i = 0;
		jniData[i++] = Math.toRadians(home.latitude);
		jniData[i++] = Math.toRadians(home.longitude);
		Log.i(TAG, "Home Location: " + jniData[0] + "," + jniData[1]);

		for (LatLng ll : surveyRegion) {
			int cur = i;
			jniData[i++] = Math.toRadians(ll.latitude);
			jniData[i++] = Math.toRadians(ll.longitude);
			Log.i(TAG, String.valueOf(jniData[cur]) + "," + String.valueOf(jniData[cur + 1]));
		}

		Log.i(TAG, "Ground Alt: " + groundAlt);
		Log.i(TAG, "Mission Height: " + missionHeight);
		Log.i(TAG, "Side Lap: " + sideLap);
		Log.i(TAG, "Hatch Angle: " + hatchAngle);
		Log.i(TAG, "GEMS Variant: " + gemsVariant);

		Log.d(TAG, "Performing JNI call into native code to compute flight plan for " + surveyRegion.size() + " points");
		return new FlightPlan(CreateFlightPlanNative(jniData, groundAlt, missionHeight, sideLap, hatchAngle, gemsVariant));
	}

	/**
	 * Constructs a new FlightPlan instance.
	 *
	 * @param results The results from native flight plan calculation.
	 */
	private FlightPlan(double[] results) {
		if (results.length == 1) {
			// If only one element is in the array then there was an error and the value is the error code.
			mErrorCode = (int)(results[0] + 0.5);
			mPath = new ArrayList<>(0);
			Log.e(TAG, "Failed to create flight plan. Error Code " + mErrorCode);
		} else if (results.length < 3) {
			// We somehow got back an invalid size. This is in error.
			mErrorCode = -100;
			mPath = new ArrayList<>(0);
			Log.e(TAG, "Native code returned an invalid array.");
		} else {
			// The flight plan was successful - reconstruct data into waypoints
			mErrorCode = 0;

			// Allocate array
			int count = results.length / 3;
			mPath = new ArrayList<>(count);
			Log.d(TAG, "Successfully created flight plan. Waypoints = " + count);

			// Assign array
			for (int i = 0; i < results.length - 3; ) {
				double lat = Math.toDegrees(results[i++]);
				double lon = Math.toDegrees(results[i++]);
				float alt = (float)Math.toDegrees(results[i++]);
				mPath.add(new DJIWaypoint(lat, lon, alt));
			}
		}
	}

	/**
	 * JNI call into native code to create the flight plan.
	 * @param region        The array containing the home location and survey region (in lat, long)
	 * @param groundAlt     Altitude of the ground over WGS84 reference ellipsoid (m) - used to set height on mission waypoints.
	 * @param missionHeight Height of mission above ground (m). Mission waypoint altitudes will be this much greater than groundAlt.
	 * @param sideLap       Value between 0.0 and 1.0. 0.0 means no overlap between rows. 0.75 means 75% of an image in one row overlaps with images in the next.
	 * @param hatchAngle    Value defining angle that vehicle passes make with the North vector (radians). 0 means passes are North-South, PI/2 means passes are East-West.
	 * @param gemsVariant   The GEMS variant. (Valid values are 0, 1, and 2.)
	 * @return              The flight plan for the specified region packed into a double array.
	 *                      This is a way more efficient way than creating all the objects from within native code.
	 */
	private static native double[] CreateFlightPlanNative(double[] region, double groundAlt, double missionHeight, double sideLap, double hatchAngle, int gemsVariant);
}
