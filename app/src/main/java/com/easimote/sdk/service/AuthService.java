package com.easimote.sdk.service;

import android.bluetooth.BluetoothGattCharacteristic;
//import android.bluetooth.BluetoothGattService;

import java.util.*;

import com.easimote.sdk.connect.BluetoothService;

public class AuthService implements BluetoothService {

	private final HashMap<UUID, BluetoothGattCharacteristic> characteristics = new HashMap<UUID, BluetoothGattCharacteristic>();

	@Override
	public void update(BluetoothGattCharacteristic bluetoothgattcharacteristic) {
		// TODO Auto-generated method stub
		characteristics.put(bluetoothgattcharacteristic.getUuid(), bluetoothgattcharacteristic);
	}
	
//	private final HashMap<UUID, BluetoothGattCharacteristic> characteristics = new HashMap<UUID, BluetoothGattCharacteristic>();
//	
//	public AuthService() {
//	}
//
//	public void processGattServices(List<BluetoothGattService> services) {
//		Iterator<BluetoothGattService> it = services.iterator();
//		do {
//			if (!it.hasNext())
//				break;
//			BluetoothGattService service = (BluetoothGattService) it.next();
//			if (EasinetUuid.AUTH_SERVICE.equals(service.getUuid())) {
//				characteristics.put(EasinetUuid.AUTH_SEED_CHAR, service.getCharacteristic(EasinetUuid.AUTH_SEED_CHAR));
//				characteristics.put(EasinetUuid.AUTH_VECTOR_CHAR, service.getCharacteristic(EasinetUuid.AUTH_VECTOR_CHAR));
//			}
//		} while (true);
//	}
//
//	public void update(BluetoothGattCharacteristic characteristic) {
//		characteristics.put(characteristic.getUuid(), characteristic);
//	}
//
//	public boolean isAvailable() {
//		return true;//characteristics.size() == 2;
//	}
//
//	public boolean isAuthSeedCharacteristic(BluetoothGattCharacteristic characteristic) {
//		return true;//characteristic.getUuid().equals(EasinetUuid.AUTH_SEED_CHAR);
//	}
//
//	public boolean isAuthVectorCharacteristic(BluetoothGattCharacteristic characteristic) {
//		return true;// characteristic.getUuid().equals(EasinetUuid.AUTH_VECTOR_CHAR);
//	}
//
//	public BluetoothGattCharacteristic getAuthSeedCharacteristic() {
//		return (BluetoothGattCharacteristic) characteristics.get(EasinetUuid.AUTH_SEED_CHAR);
//	}
//
//	public BluetoothGattCharacteristic getAuthVectorCharacteristic() {
//		return (BluetoothGattCharacteristic) characteristics.get(EasinetUuid.AUTH_VECTOR_CHAR);
//	}

}