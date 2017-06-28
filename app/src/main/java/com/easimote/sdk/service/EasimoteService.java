package com.easimote.sdk.service;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import com.easimote.sdk.connect.BeaconConnection;
import com.easimote.sdk.connect.BluetoothService;
import com.easimote.sdk.connect.EasimoteUuid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class EasimoteService implements BluetoothService {
 
	private final HashMap<UUID, BluetoothGattCharacteristic> characteristics = new HashMap<UUID, BluetoothGattCharacteristic>();
	private final HashMap<UUID, BeaconConnection.WriteStateCallback> writeCallbacks = new HashMap<UUID, BeaconConnection.WriteStateCallback>();

	public void processGattServices(List<BluetoothGattService> services) {
		Iterator<BluetoothGattService> it = services.iterator();
		while(it.hasNext()){
			BluetoothGattService service = (BluetoothGattService) it.next();
			if(EasimoteUuid.EASIMOTE_SERVICE.equals(service.getUuid()))
			{
				characteristics.put(EasimoteUuid.TRANSMIT_POWER, service.getCharacteristic(EasimoteUuid.TRANSMIT_POWER));
				characteristics.put(EasimoteUuid.BATTERY_CHAR, service.getCharacteristic(EasimoteUuid.BATTERY_CHAR));
				characteristics.put(EasimoteUuid.ADVERTISING_INTERVAL_CHAR, service.getCharacteristic(EasimoteUuid.ADVERTISING_INTERVAL_CHAR));
				characteristics.put(EasimoteUuid.EASI_IB1, service.getCharacteristic(EasimoteUuid.EASI_IB1));
				characteristics.put(EasimoteUuid.EASI_IB2, service.getCharacteristic(EasimoteUuid.EASI_IB2));
				characteristics.put(EasimoteUuid.EASI_IB3, service.getCharacteristic(EasimoteUuid.EASI_IB3));
				characteristics.put(EasimoteUuid.EASI_IB4, service.getCharacteristic(EasimoteUuid.EASI_IB4));
				characteristics.put(EasimoteUuid.MAJOR_CHAR, service.getCharacteristic(EasimoteUuid.MAJOR_CHAR));
				characteristics.put(EasimoteUuid.MINOR_CHAR, service.getCharacteristic(EasimoteUuid.MINOR_CHAR));
				characteristics.put(EasimoteUuid.MEASURED_POWER, service.getCharacteristic(EasimoteUuid.MEASURED_POWER));
				characteristics.put(EasimoteUuid.FIRMWARE_VERSION, service.getCharacteristic(EasimoteUuid.FIRMWARE_VERSION));
				characteristics.put(EasimoteUuid.AES_ENABLE, service.getCharacteristic(EasimoteUuid.AES_ENABLE));
				characteristics.put(EasimoteUuid.DEPLOY_THE_DEVICE, service.getCharacteristic(EasimoteUuid.DEPLOY_THE_DEVICE));
			}
			//here we only put the service we need, if you need all, loop as below
//			List<BluetoothGattCharacteristic> mBluetoothGattCharacteristic = service.getCharacteristics();
//			for(BluetoothGattCharacteristic mit: mBluetoothGattCharacteristic){
//				characteristics.put(mit.getUuid(), mit);
//			}
		}
	}

	public boolean hasCharacteristic(UUID uuid) {
		return characteristics.containsKey(uuid);
	}

	/***
	 * get BatteryPercent value
	 * @return
	 */
	public Integer getBatteryPercent() {
		return characteristics.containsKey(EasimoteUuid.BATTERY_CHAR) ? Integer
				.valueOf(getUnsignedByte(((BluetoothGattCharacteristic) characteristics.get(EasimoteUuid.BATTERY_CHAR)).getValue()))
				: null;
	}

	/***
	 * get powerDBM value
	 * @return
	 */
	public Byte getPowerDBM() {
		return characteristics.containsKey(EasimoteUuid.TRANSMIT_POWER) ? Byte
				.valueOf(((BluetoothGattCharacteristic) characteristics.get(EasimoteUuid.TRANSMIT_POWER)).getValue()[0]) 
				: null;
	}

	/***
	 * get ad interview
	 * @return
	 */
	public Integer getAdvertisingIntervalMillis() {
		return characteristics.containsKey(EasimoteUuid.ADVERTISING_INTERVAL_CHAR) ? Integer
				.valueOf(getUnsignedByte(((BluetoothGattCharacteristic) characteristics.get(EasimoteUuid.ADVERTISING_INTERVAL_CHAR)).getValue()))
				: null;
//		return characteristics
//				.containsKey(EasinetUuid.ADVERTISING_INTERVAL_CHAR) ? Integer
//				.valueOf(Math.round((float) getUnsignedInt16(((BluetoothGattCharacteristic) characteristics.get(EasinetUuid.ADVERTISING_INTERVAL_CHAR))
//								.getValue()) * 0.625F))
//				: null;
	}

	/***
	 * get a characteristic value which our Easinet beacon provide
	 * @param uuid
	 * @return byte[]
	 */
	public byte[] getOneCharacteristicValue(UUID uuid){
		if(hasCharacteristic(uuid))
		{
			return characteristics.get(uuid).getValue();
		}
		else
			return null;
	}
	
	/***
	 * get a characteristic value which our Easinet beacon provide
	 * @param uuid
	 * @return String
	 */
	public String getOneCharacteristicValueString(UUID uuid){
		if(hasCharacteristic(uuid))
		{
			byte[] value = characteristics.get(uuid).getValue();
			if (value != null && value.length > 0)
		    {
		        final StringBuilder stringBuilder = new StringBuilder(value.length);
		        for(byte byteChar : value)
		            stringBuilder.append(String.format("%02X ", byteChar));
		        String valueString = stringBuilder.toString();
		        return valueString;
		    }
			return null;
		}
		else
			return null;
	}	   
    
	public void update(BluetoothGattCharacteristic characteristic) {
		characteristics.put(characteristic.getUuid(), characteristic);
	}

	public List<BluetoothGattCharacteristic> getAvailableCharacteristics() {
		List<BluetoothGattCharacteristic> chars = new ArrayList<BluetoothGattCharacteristic>(characteristics.values());
		chars.removeAll(Collections.singleton(null));
		return chars;
	}

	private static int getUnsignedByte(byte bytes[]) {
		return unsignedByteToInt(bytes[0]);
	}

//	private static int getUnsignedInt16(byte bytes[]) {
//		return unsignedByteToInt(bytes[0]) + (unsignedByteToInt(bytes[1]) << 8);
//	}

	public BluetoothGattCharacteristic beforeCharacteristicWrite(UUID uuid, BeaconConnection.WriteStateCallback callback) {
		writeCallbacks.put(uuid, callback);
		return (BluetoothGattCharacteristic) characteristics.get(uuid);
	}

	public void onCharacteristicWrite(BluetoothGattCharacteristic characteristic, int status) {
		BeaconConnection.WriteStateCallback writeCallback = (BeaconConnection.WriteStateCallback) writeCallbacks.remove(characteristic.getUuid());
		if (status == BluetoothGatt.GATT_SUCCESS)
			writeCallback.onSuccess();
		else
			writeCallback.onError();
	}

	private static int unsignedByteToInt(byte value) {
		return value & 255;
	}

}