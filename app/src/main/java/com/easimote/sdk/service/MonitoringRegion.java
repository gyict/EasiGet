package com.easimote.sdk.service;

import com.easimote.sdk.Region;

import android.os.Messenger;

class MonitoringRegion extends RangingRegion {
	
	private static final int NOT_SEEN = -1;
	private long lastSeenTimeMillis;
	private boolean wasInside;

	public MonitoringRegion(Region region, Messenger replyTo) {
		super(region, replyTo);
		lastSeenTimeMillis = NOT_SEEN;
	}

	public boolean markAsSeen(long currentTimeMillis) {
		lastSeenTimeMillis = currentTimeMillis;
		if (!wasInside) {
			wasInside = true;
			return true;
		} else {
			return false;
		}
	}

	public boolean isInside(long currentTimeMillis) {
		return lastSeenTimeMillis != NOT_SEEN
				&& currentTimeMillis - lastSeenTimeMillis < BeaconService.EXPIRATION_MILLIS;
	}

	public boolean didJustExit(long currentTimeMillis) {
		if (wasInside && !isInside(currentTimeMillis)) {
			lastSeenTimeMillis = NOT_SEEN;
			wasInside = false;
			return true;
		} else {
			return false;
		}
	}


}
