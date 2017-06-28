package com.easimote.sdk.connect;
import android.bluetooth.*;
import android.content.Context;
import android.os.Handler;

import java.util.*;
import java.util.concurrent.TimeUnit;

import com.easimote.sdk.BlePeripheral;
import com.easimote.sdk.beacon;
import com.easimote.sdk.basicObjType.Hashcode;
import com.easimote.sdk.basicObjType.Objects;
import com.easimote.sdk.service.EasimoteService;
import com.easimote.sdk.util.LogService;
import com.easimote.sdk.util.utils;

/**
 * Establishes connection to the beacon and reads its characteristics.
 * Exposes methods to change beacon's UUID, major, minor, advertising interval and broadcasting power values. 
 * You can only change those values only if there is established connection to beacon.
 * @author pk
 *
 */
public class BeaconConnection {
	private final Context context;
	private final BluetoothDevice device;
	private final ConnectionCallback connectionCallback;
	private final Handler handler ;
	private final BluetoothGattCallback bluetoothGattCallback;
	private final Runnable timeoutHandler ;
	private final EasimoteService easinetService ;
	private final Map<UUID, BluetoothService> uuidToService;

	private boolean didReadCharacteristics;
	private LinkedList<BluetoothGattCharacteristic> toFetch;
	private BluetoothGatt mbluetoothGatt;
	
	public BeaconConnection(Context context, beacon beacon, ConnectionCallback connectionCallback) {
		this.context = context;
		this.device = deviceFromBeacon(beacon);
		this.toFetch = new LinkedList<BluetoothGattCharacteristic>();
		this.connectionCallback = connectionCallback;
		this.bluetoothGattCallback = createBluetoothGattCallback();
		this.timeoutHandler = createTimeoutHandler();
		this.easinetService = new EasimoteService();
		this.handler = new Handler();
		this.uuidToService = new HashMap<UUID, BluetoothService>() ;
		uuidToService.put(EasimoteUuid.EASIMOTE_SERVICE, easinetService);
	}
	
	public BeaconConnection(Context context, BlePeripheral blePeripheral, ConnectionCallback connectionCallback) {
		this.context = context;
		this.device = deviceFromBlePeripheral(blePeripheral);
		this.toFetch = new LinkedList<BluetoothGattCharacteristic>();
		this.connectionCallback = connectionCallback;
		this.bluetoothGattCallback = createBluetoothGattCallback();
		this.timeoutHandler = createTimeoutHandler();
		this.easinetService = new EasimoteService();
		this.handler = new Handler();
		this.uuidToService = new HashMap<UUID, BluetoothService>() ;
		uuidToService.put(EasimoteUuid.EASIMOTE_SERVICE, easinetService);
	}

	private BluetoothDevice deviceFromBeacon(beacon beacon) {
		BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService("bluetooth");
		BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		return bluetoothAdapter.getRemoteDevice(beacon.getMacAddress());
	}
	
	private BluetoothDevice deviceFromBlePeripheral(BlePeripheral blePeripheral) {
		BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService("bluetooth");
		BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		return bluetoothAdapter.getRemoteDevice(blePeripheral.getMacAddress());
	}

	/**
	 * Callback used to indicate status of updating beacon characteristic.
	 */
	public static interface WriteStateCallback {
		public abstract void onSuccess();
		public abstract void onError();
	}

	/**
	 * Callback used to indicate state of the connection to the beacon.
	 */
	public static interface ConnectionCallback {
		
		/**
		 *  Invoked when connection to beacon is established.
		 * @param beaconcharacteristics - Beacon's characteristics. 
		 *        gatt = GATT server
		 */
		public abstract void onConnected(BluetoothGatt gatt, EasimoteService easinetService);
		//@Deprecated
		//public abstract void onAuthenticated(BluetoothGatt gatt, EasinetService easinetService);//BeaconCharacteristics beaconcharacteristics);
		
		
		/**
		 * Connection was closed to the beacon. Can happen when beacon is out of range.
		 */
		public abstract void onDisconnected();
		
		
		/**
		 *  Invoked when there was a problem within authentication to the beacon.
		 */
		public abstract void onConnectError();

		
		/**
		 * Invoked when one characteristic is read.
		 */
		public abstract void onReadCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);
		
		/**
		 * Invoked when one characteristic value is updated.
		 * @param characteristic
		 */
		public abstract void onCharacteristicDataChange(BluetoothGattCharacteristic characteristic);
		
		/**
		 * Invoked when a descriptor in a characteristic is read.
		 * @param gatt
		 * @param descriptor
		 */
		public abstract void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor);
		
	}
	
	
	/**
	 * Starts connection flow to device with 10s timeout.
	 */
	public void establishConnection() {
		LogService.d("Trying to connect to GATT");
		didReadCharacteristics = false;
		mbluetoothGatt = device.connectGatt(context, false, bluetoothGattCallback);
		handler.postDelayed(timeoutHandler, TimeUnit.SECONDS.toMillis(15L));
	}

	/**
	 * Closes connection to beacon or cancels in-flight connection flow.
	 */
	public void closeConnection() {
		if (mbluetoothGatt != null) {
			mbluetoothGatt.disconnect();
			mbluetoothGatt.close();
		}
		handler.removeCallbacks(timeoutHandler);
	}

	/**
	 * Returns true if connection is successfully established with the beacon.
	 * @return
	 */
	public boolean isConnected() {
		BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService("bluetooth");
		int connectionState = bluetoothManager.getConnectionState(device, BluetoothProfile.GATT);
		return connectionState == BluetoothProfile.STATE_CONNECTED && didReadCharacteristics;
	}

	
	
	
	/**
	 * 
	 * @return
	 */
	private Runnable createTimeoutHandler() {
		return new Runnable() {			
			public void run() {
				LogService.d("Timeout while connecting");
				if (!didReadCharacteristics) {
					if (mbluetoothGatt != null) {
						mbluetoothGatt.disconnect();
						mbluetoothGatt.close();
						mbluetoothGatt = null;
					}
					LogService.d("Did not read characteristics");
					notifyConnectError();
				}
			}

		};
	}

	/**
	 * BluetoothGattCallbacks, the method callbacks when read\write\change happens
	 * BluetoothGattCallback.onDescriptorRead 
	 * @return
	 */
	private BluetoothGattCallback createBluetoothGattCallback() {
			
		return new BluetoothGattCallback() {
			@Override
			public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
				if (newState == BluetoothGatt.STATE_CONNECTED)
					LogService.d((new StringBuilder()).append("Connected to GATT server, discovering services: ").append(gatt.discoverServices()).toString());
				else if (newState == BluetoothGatt.STATE_DISCONNECTED && !didReadCharacteristics) 
				{
					LogService.e("Disconnected from GATT server, did not read characteristics");
					notifyConnectError();
				} 
				else if (newState == BluetoothGatt.STATE_DISCONNECTED) 
				{
					LogService.d("Disconnected from GATT server");
					notifyDisconnected();
				}
			}

			@Override
			public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
				LogService.v("setp into onCharacteristicRead");			
				if (status == BluetoothGatt.GATT_SUCCESS) 
				{
					//((BluetoothService) uuidToService.get(characteristic.getService().getUuid())).update(characteristic);
					if(didReadCharacteristics)
						notifyReadCharacteristic(gatt, characteristic);
					else
						//readEasiServiceCharacteristics(gatt);
						processGetEasinetServicesDetial(gatt);
				} 
				else 
				{
					LogService.e("Failed to read characteristic");
					toFetch.clear();
					notifyConnectError();
				}
			}

			@Override
			public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
				LogService.v("setp into onCharacteristicWrite");
				if (status == BluetoothGatt.GATT_SUCCESS) 
				{
					readNewCharacteristic(gatt, characteristic);
					if (EasimoteUuid.EASIMOTE_SERVICE.equals(characteristic.getService().getUuid()))
					{
						((BluetoothService) uuidToService.get(characteristic.getService().getUuid())).update(characteristic);
						easinetService.onCharacteristicWrite(characteristic,status);
					}
				}  
				else 
				{
					LogService.e("Characteristic write failed");
					notifyConnectError();
				}
			}

			@Override
			public void onServicesDiscovered(BluetoothGatt gatt, int status) {
				if (status == BluetoothGatt.GATT_SUCCESS) {
					LogService.v("Services discovered");
					processDiscoveredServices(gatt.getServices());
					processGetEasinetServicesDetial(gatt);
				} else {
					LogService.e((new StringBuilder()).append("Could not discover services, status: ").append(status).toString());
					notifyConnectError();
				}
			}
			
			@Override
			public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
				LogService.v("characteristic changed");
				connectionCallback.onCharacteristicDataChange(characteristic);
				}
			
			@Override
			public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
				LogService.v("descriptor updated");
				connectionCallback.onDescriptorRead(gatt, descriptor);
			}

		};
	}
	
	private void notifyConnectError() {
		handler.removeCallbacks(timeoutHandler);
		connectionCallback.onConnectError();
	}

	private void notifyDisconnected() {
		connectionCallback.onDisconnected();
	}
	
	private void notifyConnected(BluetoothGatt gatt) {
		LogService.d("Established connection to beacon");
		handler.removeCallbacks(timeoutHandler);
		didReadCharacteristics = true;
		connectionCallback.onConnected(gatt, easinetService);//BeaconCharacteristics(easinetService));//new BeaconCharacteristics(easinetService, versionService));
		//connectionCallback.onAuthenticated(new BeaconCharacteristics(easinetService, versionService));
	}
	
	private void notifyReadCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
		connectionCallback.onReadCharacteristic(gatt, characteristic);
	}

	private void processDiscoveredServices(final List<BluetoothGattService> services) {
		easinetService.processGattServices(services);
		toFetch.clear();
		toFetch.addAll(easinetService.getAvailableCharacteristics());
	}
	
	private void processGetEasinetServicesDetial(final BluetoothGatt gatt) {
//		handler.postDelayed(new Runnable() {
//			public void run() {
//				readEasiServiceCharacteristics(gatt);
//			}
//		}, 500L);
		readEasiServiceCharacteristics(gatt);
	}

	private void readNewCharacteristic(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
		gatt.readCharacteristic(characteristic);		
	}

	private void readEasiServiceCharacteristics(final BluetoothGatt gatt) {
		if (!toFetch.isEmpty())
		{
			final BluetoothGattCharacteristic mCharacteristic = toFetch.poll();
			LogService.d("read"+ mCharacteristic.getUuid().toString());
			gatt.readCharacteristic(mCharacteristic);				
//			handler.postDelayed(new Runnable() {
//				public void run() {
//					List<BluetoothGattDescriptor> mDescriptors = mCharacteristic.getDescriptors();
//					for(BluetoothGattDescriptor mDescriptor: mDescriptors)
//					{
//						Log.e("reading descriptor", mDescriptor.getUuid().toString());
//						gatt.readDescriptor(mDescriptor);
//					}
//				}
//			}, 500L);
			
		}
		else if (mbluetoothGatt != null)
		{
			notifyConnected(gatt);
		}
			
	}
	
	/**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
	public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if ( mbluetoothGatt == null) {
        	LogService.w("BluetoothAdapter not initialized");
            return;
        }
        mbluetoothGatt.readCharacteristic(characteristic);
    }


///***************read methods for easinet characteristics******************************************/
//	/**
//	 *   Value object for beacon's characteristics 
//	 *   (battery level, broadcasting power, advertising interval, software and hardware version).
//	 *   Those values can be read via BeaconConnection.connect() 
//	 *   and are delivered asynchronously in ConnectionCallback#onAuthenticated(BeaconCharacteristics).
//	 * @author pk
//	 *
//	 */
//	public static class BeaconCharacteristics {
//		private final Integer batteryPercent;
//		private final Byte broadcastingPower;
//		private final Integer advertisingIntervalMillis;
//
//		public BeaconCharacteristics(EasinetService easinetService) {
//			broadcastingPower = easinetService.getPowerDBM();
//			batteryPercent = easinetService.getBatteryPercent();
//			advertisingIntervalMillis = easinetService.getAdvertisingIntervalMillis();
//		}
//		
//		public BeaconCharacteristics(EasinetService easinetService, VersionService versionService) {
//			broadcastingPower = easinetService.getPowerDBM();
//			batteryPercent = easinetService.getBatteryPercent();
//			advertisingIntervalMillis = easinetService.getAdvertisingIntervalMillis();
//		}
//		
//		public Integer getBatteryPercent() {
//			return batteryPercent;
//		}
//
//		/**
//		 * @return Broadcasting power. @see BeaconConnection#ALLOWED_POWER_LEVELS
//		 */
//		public Byte getBroadcastingPower() {
//			return broadcastingPower;
//		}
//
//		public Integer getAdvertisingIntervalMillis() {
//			return advertisingIntervalMillis;
//		}
//
//		/**
//		 * Human readable description of the characteristics.
//		 */
//		public String toString() {
//			return Objects.toStringHelper(this)
//					.add("batteryPercent", batteryPercent)
//					.add("broadcastingPower", broadcastingPower)
//					.add("advertisingIntervalMillis", advertisingIntervalMillis).toString();
//		}
//	}
//	
///***************************************************************************************************/
	

/************write methods*****************************************/
	/**
	 * Changes proximity UUID of the beacon.
	 * @param proximityUuid - String representation of the UUID (16 bytes encoded as hex with or without dashes).
	 * @param writeCallback - Callback to be invoked when write is completed.
	 */
	public void writeProximityUuid(String proximityUuid, final WriteStateCallback writeCallback) {
		if (!isConnected() || !easinetService.hasCharacteristic(EasimoteUuid.EASI_IB1)
				|| !easinetService.hasCharacteristic(EasimoteUuid.EASI_IB2)
				|| !easinetService.hasCharacteristic(EasimoteUuid.EASI_IB3)
				|| !easinetService.hasCharacteristic(EasimoteUuid.EASI_IB4)) 
		{
			LogService.e("Not connected to beacon or no corresponding uuid. Discarding change proximity UUID.");
			writeCallback.onError();
			return;
		} 
		else 
		{
			final byte uuidAsBytes[] = Hashcode.fromString(proximityUuid.replaceAll("-", "").toLowerCase(Locale.US)).asBytes();
			
			handler.post(new Runnable() {
				public void run() {
					byte easiIB1[] = {uuidAsBytes[0], uuidAsBytes[1], uuidAsBytes[2], uuidAsBytes[3] };
					BluetoothGattCharacteristic uuidChar = easinetService.beforeCharacteristicWrite(EasimoteUuid.EASI_IB1, writeCallback);
					uuidChar.setValue(easiIB1);
					mbluetoothGatt.writeCharacteristic(uuidChar);
				}
			});
			
			handler.postDelayed(new Runnable() {
				public void run() {
					byte easiIB2[] = {uuidAsBytes[4], uuidAsBytes[5], uuidAsBytes[6], uuidAsBytes[7] };
					BluetoothGattCharacteristic uuidChar = easinetService.beforeCharacteristicWrite(EasimoteUuid.EASI_IB2, writeCallback);
					uuidChar.setValue(easiIB2);
					mbluetoothGatt.writeCharacteristic(uuidChar);
				}
			},600L);
			
			handler.postDelayed(new Runnable() {
				public void run() {
					byte easiIB3[] = {uuidAsBytes[8], uuidAsBytes[9], uuidAsBytes[10], uuidAsBytes[11] };
					BluetoothGattCharacteristic uuidChar = easinetService.beforeCharacteristicWrite(EasimoteUuid.EASI_IB3, writeCallback);
					uuidChar.setValue(easiIB3);
					mbluetoothGatt.writeCharacteristic(uuidChar);
				}
			}, 1200L);
			
			handler.postDelayed(new Runnable() {
				public void run() {
					byte easiIB4[] = {uuidAsBytes[12], uuidAsBytes[13], uuidAsBytes[14], uuidAsBytes[15] };
					BluetoothGattCharacteristic uuidChar = easinetService.beforeCharacteristicWrite(EasimoteUuid.EASI_IB4, writeCallback);
					uuidChar.setValue(easiIB4);
					mbluetoothGatt.writeCharacteristic(uuidChar);
				}
			}, 1800L);
			
			return;
		}
	}

	/**
	 * Changes advertising interval of the beacon. 
	 * Interval value will be normalized to fit the range (50 - 2000 ms).
	 * @param intervalMillis - Advertising interval in milliseconds.
	 * @param writeCallback -  Callback to be invoked when write is completed.
	 */
	public void writeAdvertisingInterval(int intervalMillis, WriteStateCallback writeCallback) {
		if (!isConnected() || !easinetService.hasCharacteristic(EasimoteUuid.ADVERTISING_INTERVAL_CHAR)) {
			LogService.e("Not connected to beacon. Discarding changing advertising interval.");
			writeCallback.onError();
			return;
		} else {
			intervalMillis = Math.max(0, Math.min(2000, intervalMillis));
			//int correctedInterval = (int) ((double) intervalMillis / 0.625D);
			int correctedInterval = intervalMillis;
			BluetoothGattCharacteristic intervalChar = easinetService.beforeCharacteristicWrite(EasimoteUuid.ADVERTISING_INTERVAL_CHAR, writeCallback);
			intervalChar.setValue(correctedInterval, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
			mbluetoothGatt.writeCharacteristic(intervalChar);
			return;
		}
	}

	/**
	 * Changes broadcasting power of the beacon. 
	 * Allowed values: -30, -20, -16, -12, -8, -4, 0, 4. 
	 * Larger value means stronger power.
	 * @param powerDBM - Broadcasting power to be set.
	 * @param writeCallback - Callback to be invoked when write is completed.
	 */
	public static Set<Integer> ALLOWED_POWER_LEVELS = Collections.unmodifiableSet(
			new HashSet<Integer>(Arrays.asList(new Integer[] {
					Integer.valueOf(-30), Integer.valueOf(-20),
					Integer.valueOf(-16), Integer.valueOf(-12),
					Integer.valueOf(-8), Integer.valueOf(-4),
					Integer.valueOf(0), Integer.valueOf(4) })));
	
	public void writeBroadcastingPower(int powerDBM, WriteStateCallback writeCallback) {
		if (!isConnected() || !easinetService.hasCharacteristic(EasimoteUuid.TRANSMIT_POWER)) {
			LogService.w("Not connected to beacon. Discarding changing broadcasting power.");
			writeCallback.onError();
			return;
		}
		if (!ALLOWED_POWER_LEVELS.contains(Integer.valueOf(powerDBM))) {
			LogService.w("Not allowed power level. Discarding changing broadcasting power.");
			writeCallback.onError();
			return;
		} else {
			BluetoothGattCharacteristic powerChar = easinetService.beforeCharacteristicWrite(EasimoteUuid.TRANSMIT_POWER, writeCallback);
			powerChar.setValue(powerDBM, BluetoothGattCharacteristic.FORMAT_SINT8, 0);
			mbluetoothGatt.writeCharacteristic(powerChar);
			return;
		}
	}

	/**
	 * Changes major value of the beacon. 
	 * Major value will be normalized to fit the range (1, 65535).
	 * @param major -  Major value to be set.
	 * @param writeCallback - Callback to be invoked when write is completed.
	 */
	public void writeMajor(int major, WriteStateCallback writeCallback) {
		if (!isConnected()) {
			LogService.w("Not connected to beacon. Discarding changing major.");
			writeCallback.onError();
			return;
		} else {
			major = utils.normalize16BitUnsignedInt(major);
			BluetoothGattCharacteristic majorChar = easinetService.beforeCharacteristicWrite(EasimoteUuid.MAJOR_CHAR, writeCallback);
			majorChar.setValue(major, BluetoothGattCharacteristic.FORMAT_UINT16, 0);
			mbluetoothGatt.writeCharacteristic(majorChar);
			return;
		}
	}

	/**
	 * Changes major value of the beacon. 
	 * Major value will be normalized to fit the range (1, 65535).
	 * @param minor - Major value to be set.
	 * @param writeCallback - Callback to be invoked when write is completed.
	 */
	public void writeMinor(int minor, WriteStateCallback writeCallback) {
		if (!isConnected()) {
			LogService.w("Not connected to beacon. Discarding changing minor.");
			writeCallback.onError();
			return;
		} else {
			minor = utils.normalize16BitUnsignedInt(minor);
			BluetoothGattCharacteristic minorChar = easinetService.beforeCharacteristicWrite(EasimoteUuid.MINOR_CHAR, writeCallback);
			minorChar.setValue(minor, BluetoothGattCharacteristic.FORMAT_UINT16, 0);
			mbluetoothGatt.writeCharacteristic(minorChar);
			return;
		}
	}
	
	/**
	 * Change value of a selected characteristic
	 * @param value - the input data should parse as byte[]
	 * @param characteristic - characteristic to change
	 * @param writeCallback - Callback to be invoked when write is completed.
	 */
	public void characteristicValueWriteByte(byte[] value, final BluetoothGattCharacteristic characteristic, WriteStateCallback writeCallback){
		LogService.d("Write value to characteristic");
		characteristic.setValue(value);	
		if(mbluetoothGatt.writeCharacteristic(characteristic))
			writeCallback.onSuccess();
		else
			writeCallback.onError();
	}
/******************************************************************************************************/
}