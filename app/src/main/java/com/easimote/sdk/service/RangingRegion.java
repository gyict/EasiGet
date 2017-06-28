package com.easimote.sdk.service;

import android.os.Messenger;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.easimote.sdk.Region;
import com.easimote.sdk.beacon;
import com.easimote.sdk.util.LogService;
import com.easimote.sdk.util.utils;

public class RangingRegion {
	
	private final ConcurrentHashMap<beacon, Long> beacons = new ConcurrentHashMap<beacon, Long>();
	final Region region;
	final Messenger replyTo;
	
	RangingRegion(Region region, Messenger replyTo) {
		this.region = region;
		this.replyTo = replyTo;
	}

	public final Collection<beacon> getSortedBeacons() {
		ArrayList<beacon> sortedBeacons = new ArrayList<beacon>(beacons.keySet());
		Collections.sort(sortedBeacons, BEACON_ACCURACY_COMPARATOR);
		return sortedBeacons;
	}
	
	private static final Comparator<beacon> BEACON_ACCURACY_COMPARATOR = new Comparator<beacon>() {

		public int compare(beacon lhs, beacon rhs) {
			return Double.compare(utils.computeAccuracy(lhs), utils.computeAccuracy(rhs));
		}

	};

	public final void processFoundBeacons(Map<beacon, Long> beaconsFoundInScanCycle) {
		Iterator<Entry<beacon, Long>> it = beaconsFoundInScanCycle.entrySet().iterator();
		do {
			if (!it.hasNext())
				break;
			Map.Entry<beacon, Long> entry = (Map.Entry<beacon, Long>) it.next();
			if (utils.isBeaconInRegion((beacon) entry.getKey(), region)) {
				beacons.remove(entry.getKey());
				beacons.put(entry.getKey(), entry.getValue());
			}
		} while (true);
	}

	public final List<beacon> removeNotSeenBeacons(long currentTimeMillis) {
		List<beacon> toRemoveBeacon = new ArrayList<beacon>();
		Iterator<Entry<beacon, Long>> iterator = beacons.entrySet().iterator();
		do {
			if (!iterator.hasNext())
				break;
			Map.Entry<beacon, Long> entry = (Map.Entry<beacon, Long>) iterator.next();
			if (currentTimeMillis - ((Long) entry.getValue()).longValue() > BeaconService.EXPIRATION_MILLIS) {
				LogService.v((new StringBuilder()).append("Not seen lately: ").append(entry.getKey()).toString());
				toRemoveBeacon.add(entry.getKey());
				iterator.remove();
			}
		} while (true);
		return toRemoveBeacon;
	}

}
