package com.easimote.sdk.util;

import com.easimote.sdk.beacon;

/**
 * Only legal beacons can pass the filter.
 * We haven't set the filter rules for easimote yet.
 * @author PK
 */
public class BeaconFilter {

	public static final String IOS_IBEACON_PROXIMITY_UUID = "8492E75F-4FD6-469D-B132-043FE94921D8";
	
	public BeaconFilter() {}
	
	public static String AESdecode(beacon beacon){
		return null;
	}
		
	public static boolean isIOSBeacon(beacon beacon) {
		return IOS_IBEACON_PROXIMITY_UUID.equalsIgnoreCase(beacon.getProximityUUID());
	}

	public static boolean isEasimote(beacon beacon) {
		return true;
	}
	
	public static boolean isValidName(String name) {
		return true;
	}
}
