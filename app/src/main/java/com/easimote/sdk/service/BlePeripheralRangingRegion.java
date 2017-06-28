package com.easimote.sdk.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.easimote.sdk.BlePeripheral;
import com.easimote.sdk.util.LogService;

import android.os.Messenger;

public class BlePeripheralRangingRegion {
	
	private final ConcurrentHashMap<BlePeripheral, Long> blePeripheral = new ConcurrentHashMap<BlePeripheral, Long>();
	final Messenger replyTo;
	
	BlePeripheralRangingRegion(Messenger replyTo) {
		this.replyTo = replyTo;
	}
	
	public final Collection<BlePeripheral> getFoundBlePeriapherals() {
		ArrayList<BlePeripheral> foundBlePeripherals = new ArrayList<BlePeripheral>(blePeripheral.keySet());
		return foundBlePeripherals;
	}
	
	public final void processFoundBlePeripherals(Map<BlePeripheral, Long> blePeripheralFoundInScanCycle) {
		Iterator<Entry<BlePeripheral, Long>> it = blePeripheralFoundInScanCycle.entrySet().iterator();
		do {
			if (!it.hasNext())
				break;
			Map.Entry<BlePeripheral, Long> entry = (Map.Entry<BlePeripheral, Long>) it.next();
			blePeripheral.remove(entry.getKey());
			blePeripheral.put(entry.getKey(), entry.getValue());
		} while (true);
	}

	public final void removeNotSeenBlePeripherals(long currentTimeMillis) {
		Iterator<Entry<BlePeripheral, Long>> iterator = blePeripheral.entrySet().iterator();
		do {
			if (!iterator.hasNext())
				break;
			Map.Entry<BlePeripheral, Long> entry = (Map.Entry<BlePeripheral, Long>) iterator.next();
			if (currentTimeMillis - ((Long) entry.getValue()).longValue() > BeaconService.EXPIRATION_MILLIS) {
				LogService.v((new StringBuilder()).append("Not seen lately: ").append(entry.getKey()).toString());
				iterator.remove();
			}
		} while (true);
	}

}
