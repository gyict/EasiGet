package com.easimote.sdk.service;

import android.bluetooth.BluetoothGattCharacteristic;
//import android.bluetooth.BluetoothGattService;
import java.util.*;

import com.easimote.sdk.connect.BluetoothService;


public class VersionService implements BluetoothService {

	private final HashMap<UUID, BluetoothGattCharacteristic> characteristics = new HashMap<UUID, BluetoothGattCharacteristic>();
	
	@Override
	public void update(BluetoothGattCharacteristic bluetoothgattcharacteristic) {
		// TODO Auto-generated method stub
		characteristics.put(bluetoothgattcharacteristic.getUuid(), bluetoothgattcharacteristic);
	}
//	private final HashMap<UUID, BluetoothGattCharacteristic> characteristics = new HashMap<UUID, BluetoothGattCharacteristic>();
//
//	public void processGattServices(List services) {
//		Iterator it = services.iterator();
//		do {
//			if (!it.hasNext())
//				break;
//			BluetoothGattService service = (BluetoothGattService) it.next();
//			if (EasinetUuid.VERSION_SERVICE.equals(service.getUuid())) {
//				characteristics.put(EasinetUuid.HARDWARE_VERSION_CHAR, service.getCharacteristic(EasinetUuid.HARDWARE_VERSION_CHAR));
//				characteristics.put(EasinetUuid.SOFTWARE_VERSION_CHAR, service.getCharacteristic(EasinetUuid.SOFTWARE_VERSION_CHAR));
//			}
//		} while (true);
//	}
//
//	public String getSoftwareVersion() {
//		return characteristics.containsKey(EasinetUuid.SOFTWARE_VERSION_CHAR) ? getStringValue(((BluetoothGattCharacteristic) characteristics
//				.get(EasinetUuid.SOFTWARE_VERSION_CHAR)).getValue()) : null;
//	}
//
//	public String getHardwareVersion() {
//		return characteristics.containsKey(EasinetUuid.HARDWARE_VERSION_CHAR) ? getStringValue(((BluetoothGattCharacteristic) characteristics
//				.get(EasinetUuid.HARDWARE_VERSION_CHAR)).getValue()) : null;
//	}
//
//	public void update(BluetoothGattCharacteristic characteristic) {
//		characteristics.put(characteristic.getUuid(), characteristic);
//	}
//
//	public List<BluetoothGattCharacteristic> getAvailableCharacteristics() {
//		List<BluetoothGattCharacteristic> chars = new ArrayList<BluetoothGattCharacteristic>(characteristics.values());
//		chars.removeAll(Collections.singleton(null));
//		return chars;
//	}
//
//	private static String getStringValue(byte bytes[]) {
//		int indexOfFirstZeroByte;
//		for (indexOfFirstZeroByte = 0; bytes[indexOfFirstZeroByte] != 0; indexOfFirstZeroByte++)
//			;
//		byte strBytes[] = new byte[indexOfFirstZeroByte];
//		for (int i = 0; i != indexOfFirstZeroByte; i++)
//			strBytes[i] = bytes[i];
//
//		return new String(strBytes);
//	}

}
