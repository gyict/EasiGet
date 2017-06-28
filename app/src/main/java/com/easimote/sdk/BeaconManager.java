package com.easimote.sdk;

import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

import com.easimote.sdk.service.BeaconService;
import com.easimote.sdk.service.BlePeripheralRangingResult;
import com.easimote.sdk.service.MonitoringResult;
import com.easimote.sdk.service.RangingResult;
import com.easimote.sdk.service.ScanPeriodData;
import com.easimote.sdk.util.LogService;

/***
 * This is a class that clients should use to interact with Beacons
 * It allows to:
 *   range beacons (scan beacons and optionally filter them by their values)
 *   monitor beacons (track beacons in order to get events that device has entered or leaved regions of interest)
 * @author pk
 *
 */
public class BeaconManager {

	//private static final String ANDROID_MANIFEST_CONDITIONS_MSG = "AndroidManifest.xml does not contain android.permission.BLUETOOTH or android.permission.BLUETOOTH_ADMIN permissions. BeaconService may be also not declared in AndroidManifest.xml.";
	private final Context context;
	private final InternalServiceConnection serviceConnection = new InternalServiceConnection();
	private final Messenger incomingMessenger = new Messenger(new IncomingHandler());
	private final Set<String> rangedRegionIds = new HashSet<String>();
	private final Set<String> monitoredRegionIds = new HashSet<String>();
	private Messenger serviceMessenger;
	private RangingListener rangingListener;
	private BlePeripheralRangingListener blePeripheralRangingListener;
	private MonitoringListener monitoringListener;
	private ErrorListener errorListener;
	private onNewBeaconFoundListener mOnNewBeaconFoundListener;
	private onBeaconNotSeenListener mOnBeaconNotSeenListener;
	private ServiceReadyCallback callback;
	private ScanPeriodData foregroundScanPeriod;
	private ScanPeriodData backgroundScanPeriod;


	/**
	 * Constructs BeaconManager instance.
	 * It's good practice to have only one per application stored in your Application subclass.
	 * @param context
	 */
	public BeaconManager(Context context) {
		this.context = (Context) Preconditions.checkNotNull(context);
	}

	/***
	 * Returns true if device supports Bluetooth Low Energy.
	 * @return
	 */
	public boolean hasBluetooth() {
		return context.getPackageManager().hasSystemFeature("android.hardware.bluetooth_le");
	}

	public boolean checkPermissionsAndService() {
		PackageManager pm = context.getPackageManager();
		int bluetoothPermission = pm.checkPermission("android.permission.BLUETOOTH", context.getPackageName());
		int bluetoothAdminPermission = pm.checkPermission("android.permission.BLUETOOTH_ADMIN", context.getPackageName());
		//int locationPermission = pm.checkPermission("android.permission.ACCESS_COARSE_LOCATION", context.getPackageName());
		int locationPermission = 0;
		Intent intent = new Intent(context, BeaconService.class);
		List<ResolveInfo> resolveInfo = pm.queryIntentServices(intent, 65536);
		LogService.e("bat"+bluetoothAdminPermission+"bt"+bluetoothPermission+"lp"+locationPermission+"rs"+resolveInfo.size());
		return bluetoothPermission == 0 && bluetoothAdminPermission == 0 && locationPermission == 0 && resolveInfo.size() > 0;
	}

	/***
	 *  Returns true if Bluetooth is enabled on the device
	 * @return
	 */
	public boolean isBluetoothEnabled() {
		if (!checkPermissionsAndService()) {
			LogService.e("AndroidManifest.xml does not contain android.permission.BLUETOOTH or android.permission.BLUETOOTH_ADMIN permissions. BeaconService may be also not declared in AndroidManifest.xml.");
			return false;
		} else {
			BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService("bluetooth");
			BluetoothAdapter adapter = bluetoothManager.getAdapter();
			return adapter != null && adapter.isEnabled();
		}
	}

	/***
	 *  Returns true if Bluetooth is enabled on the device
	 * @return
	 */
	public boolean isLocationEnabled() {
//		PackageManager pm = context.getPackageManager();
//		int locationPermission = pm.checkPermission("android.permission.ACCESS_COARSE_LOCATION", context.getPackageName());
//		if (locationPermission!=0) {
//			LogService.e("Don't have the permission of location");
//			//return false;
//		} else {
			LocationManager locationManager
					= (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
			boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
			// 通过WLAN或移动网络(3G/2G)确定的位置（也称作AGPS，辅助GPS定位。主要用于在室内或遮盖物（建筑群或茂密的深林等）密集的地方定位）
			boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
			if (gps || network) {
				return true;
			}

			return false;
		//}
	}

	/**
	 *  Connects to BeaconService
	 * @param callback Callback to be invoked when connection is made to service.
	 */
	public void connect(ServiceReadyCallback callback) {
		if (!checkPermissionsAndService())
			LogService.e("AndroidManifest.xml does not contain android.permission.BLUETOOTH or android.permission.BLUETOOTH_ADMIN permissions. BeaconService may be also not declared in AndroidManifest.xml.");
		this.callback = (ServiceReadyCallback) Preconditions.checkNotNull(callback, "callback cannot be null");
		if (isConnectedToService())
			callback.onServiceReady();
		boolean bound = context.bindService(new Intent(context, BeaconService.class), serviceConnection, 1);
		if (!bound)
			;// L.w("Could not bind service: make sure that sdk.service.BeaconService is declared in AndroidManifest.xml");
	}

	/**
	 * Disconnects from BeaconService. If there were any ranging or monitoring in progress,
	 * they will be stopped. This should be typically called in onDestroy method. 
	 */
	public void disconnect() {
		if (!isConnectedToService()) {
			LogService.i("Not disconnecting because was not connected to service");
			return;
		}
		CopyOnWriteArraySet<String> tempRangedRegionIds = new CopyOnWriteArraySet<String>(rangedRegionIds);
		for (Iterator<String> it = tempRangedRegionIds.iterator(); it.hasNext();) {
			String regionId = (String) it.next();
			try {
				internalStopRanging(regionId);
			} catch (RemoteException e) {
				LogService.e("Swallowing error while disconnect/stopRanging", e);
			}
		}

		CopyOnWriteArraySet<String> tempMonitoredRegionIds = new CopyOnWriteArraySet<String>(monitoredRegionIds);
		for (Iterator<String> it = tempMonitoredRegionIds.iterator(); it.hasNext();) {
			String regionId = (String) it.next();
			try {
				internalStopMonitoring(regionId);
			} catch (RemoteException e) {
				LogService.e("Swallowing error while disconnect/stopMonitoring", e);
			}
		}

		context.unbindService(serviceConnection);
		serviceMessenger = null;
	}

	/**
	 * Sets new ranging listener. Old one will be discarded.
	 * @param listener New listener.
	 */ 
	public void setRangingListener(RangingListener listener) {
		rangingListener = (RangingListener) Preconditions.checkNotNull(listener, "listener cannot be null");
	}
	
	public void setBlePeripheralRangingListener(BlePeripheralRangingListener listener){
		blePeripheralRangingListener = (BlePeripheralRangingListener)Preconditions.checkNotNull(listener, "listener cannot be null");
	}

	/**
	 * Sets new monitoring listener. Old one will be discarded.
	 * @param listener New listener.
	 */
	public void setMonitoringListener(MonitoringListener listener) {
		monitoringListener = (MonitoringListener) Preconditions.checkNotNull(listener, "listener cannot be null");
	}

	/**
	 * Sets new error listener. Old one will be discarded.
	 * @param listener
	 */ 
	public void setErrorListener(ErrorListener listener) {
		errorListener = listener;
		if (isConnectedToService() && listener != null)
			registerErrorListenerInService();
	}
	
	public void setOnNewBeaconFoundListener(onNewBeaconFoundListener listener) {
		mOnNewBeaconFoundListener = listener;
	}
	
	public void setOnBeaconNotSeenListener(onBeaconNotSeenListener listener){
		mOnBeaconNotSeenListener = listener;
	}

	/**
	 * Changes defaults scanning periods when ranging is performed. Default
	 * values: scanPeriod=1s, waitTime=0s
	 * @param scanPeriodMillis  How long to perform Bluetooth Low Energy scanning?
	 * @param waitTimeMillis How long to wait until performing next scanning?
	 */ 
	public void setForegroundScanPeriod(long scanPeriodMillis, long waitTimeMillis) {
		if (isConnectedToService())
			setScanPeriod(new ScanPeriodData(scanPeriodMillis, waitTimeMillis), BeaconService.MSG_SET_FOREGROUND_SCAN_PERIOD);
		else
			foregroundScanPeriod = new ScanPeriodData(scanPeriodMillis, waitTimeMillis);
	}

	/**
	 * Changes defaults scanning periods when monitoring is performed. Default
	 * values: scanPeriod=5s, waitTime=25s
	 * @param scanPeriodMillis  How long to perform Bluetooth Low Energy scanning?
	 * @param waitTimeMillis How long to wait until performing next scanning?
	 */
	public void setBackgroundScanPeriod(long scanPeriodMillis, long waitTimeMillis) {
		if (isConnectedToService())
			setScanPeriod(new ScanPeriodData(scanPeriodMillis, waitTimeMillis), BeaconService.MSG_SET_BACKGROUND_SCAN_PERIOD);
		else
			backgroundScanPeriod = new ScanPeriodData(scanPeriodMillis, waitTimeMillis);
	}

	//
	private void setScanPeriod(ScanPeriodData scanPeriodData, int msgId) {
		Message scanPeriodMsg = Message.obtain(null, msgId);
		scanPeriodMsg.obj = scanPeriodData;
		try {
			serviceMessenger.send(scanPeriodMsg);
		} catch (RemoteException e) {
			LogService.e((new StringBuilder()).append("Error while setting scan periods: ").append(msgId).toString());
		}
	}

	/**
	 * Starts ranging given range. Ranging results will be delivered to listener registered via setRangingListener(RangingListener). 
	 * If given region is already ranged, this is no-op.
	 * @param region Region to range.
	 * @throws RemoteException If communication with service failed.
	 */
	public void startRanging(Region region) throws RemoteException {
		if (!isConnectedToService()) {
			
			LogService.i("Not starting ranging, not connected to service");
			return;
		}
		Preconditions.checkNotNull(region, "region cannot be null");
		if (rangedRegionIds.contains(region.getIdentifier()))
			LogService.i((new StringBuilder()).append("Region already ranged but that's OK: ").append(region).toString());
		rangedRegionIds.add(region.getIdentifier());
		
		Message startRangingMsg = Message.obtain(null, BeaconService.MSG_START_RANGING);
		startRangingMsg.obj = region;
		startRangingMsg.replyTo = incomingMessenger;
		try {
			serviceMessenger.send(startRangingMsg);
		} catch (RemoteException e) {
			LogService.e("Error while start ranging", e);
			throw e;
		} 
		
		if(mOnNewBeaconFoundListener != null)
		{
			Message newBeaconComingMsg = Message.obtain(null, BeaconService.MSG_NEW_BEACON_COMING);
			newBeaconComingMsg.replyTo = incomingMessenger;
			try {
				serviceMessenger.send(newBeaconComingMsg);
			} catch (RemoteException e) {
				LogService.e("Error while looking for new coming beacons", e);
				//throw e;
			}
		}
		
		//MSG_BEACON_NOT_SEEN
		if(mOnBeaconNotSeenListener != null)
		{
			Message beaconNotSeenMsg = Message.obtain(null, BeaconService.MSG_BEACON_NOT_SEEN);
			beaconNotSeenMsg.replyTo = incomingMessenger;
			try {
				serviceMessenger.send(beaconNotSeenMsg);
			} catch (RemoteException e) {
				LogService.e("Error while starting looking for not seen beacons", e);
				//throw e;
			}
		}
	}

	public void stopRanging(Region region) throws RemoteException {
		if (!isConnectedToService()) {
			LogService.i("Not stopping ranging, not connected to service");
			return;
		} else {
			Preconditions.checkNotNull(region, "region cannot be null");
			internalStopRanging(region.getIdentifier());
			return;
		}
	}

	private void internalStopRanging(String regionId) throws RemoteException {
		rangedRegionIds.remove(regionId);
		Message stopRangingMsg = Message.obtain(null, BeaconService.MSG_STOP_RANGING);
		stopRangingMsg.obj = regionId;
		try {
			serviceMessenger.send(stopRangingMsg);
		} catch (RemoteException e) {
			LogService.e("Error while stopping ranging", e);
			throw e;
		}
	}
	
	
	public void startBlePeripheralRanging() throws RemoteException {
		if (!isConnectedToService()) {
			LogService.i("Not starting ranging, not connected to service");
			return;
		}
		
		Message startBlePeripheralRangingMsg = Message.obtain(null, BeaconService.MSG_START_BLEPERIPHERAL_RANGING);
		//startBlePeripheralRangingMsg.obj = region;
		startBlePeripheralRangingMsg.replyTo = incomingMessenger;
		try {
			serviceMessenger.send(startBlePeripheralRangingMsg);
		} catch (RemoteException e) {
			LogService.e("Error while starting blePeripheralRanging", e);
			throw e;
		}
	}
	
	
	public void stopBlePeripheralRanging() throws RemoteException{
		if (!isConnectedToService()) {
			LogService.i("Not stopping blePeripheralRanging, not connected to service");
			return;
		} else {
			internalStopBlePeripheralRanging();
			return;
		}
	}
	
	private void internalStopBlePeripheralRanging() throws RemoteException {
		Message stopBlePeripheralRangingMsg = Message.obtain(null, BeaconService.MSG_STOP_BLEPERIPHERAL_RANGING);
		try {
			serviceMessenger.send(stopBlePeripheralRangingMsg);
		} catch (RemoteException e) {
			LogService.e("Error while stopping blePeripheralRanging", e);
			throw e;
		}
	}
	

	public void startMonitoring(Region region) throws RemoteException {
		if (!isConnectedToService()) {
			LogService.e("Not starting monitoring, not connected to service");
			return;
		}
		Preconditions.checkNotNull(region, "region cannot be null");
		if (monitoredRegionIds.contains(region.getIdentifier()))
			LogService.e((new StringBuilder()).append("Region already monitored but that's OK: ").append(region).toString());
		monitoredRegionIds.add(region.getIdentifier());
		Message startMonitoringMsg = Message.obtain(null, BeaconService.MSG_START_MONITORING);
		startMonitoringMsg.obj = region;
		startMonitoringMsg.replyTo = incomingMessenger;
		try {
			serviceMessenger.send(startMonitoringMsg);
		} catch (RemoteException e) {
			LogService.e("Error while starting monitoring", e);
			throw e;
		}
	}

	public void stopMonitoring(Region region) throws RemoteException {
		if (!isConnectedToService()) {
			LogService.i("Not stopping monitoring, not connected to service");
			return;
		} else {
			Preconditions.checkNotNull(region, "region cannot be null");
			internalStopMonitoring(region.getIdentifier());
			return;
		}
	}

	private void internalStopMonitoring(String regionId) throws RemoteException {
		monitoredRegionIds.remove(regionId);
		Message stopMonitoringMsg = Message.obtain(null, BeaconService.MSG_STOP_MONITORING);
		stopMonitoringMsg.obj = regionId;
		try {
			serviceMessenger.send(stopMonitoringMsg);
		} catch (RemoteException e) {
			LogService.e("Error while stopping ranging");
			throw e;
		}
	}

	@SuppressLint("HandlerLeak")
	private class IncomingHandler extends Handler {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case BeaconService.MSG_RANGING_RESPONSE: // '3'
				if (rangingListener != null) {
					RangingResult rangingResult = (RangingResult) msg.obj;
					rangingListener.onBeaconsDiscovered(rangingResult.region, rangingResult.beacons);
				}
				break;

			case BeaconService.MSG_MONITORING_RESPONSE: // '6'
				if (monitoringListener == null)
					break;
				MonitoringResult monitoringResult = (MonitoringResult) msg.obj;
				if (monitoringResult.state == Region.State.INSIDE)
					monitoringListener.onEnteredRegion(monitoringResult.region, monitoringResult.beacons);
				else
					monitoringListener.onExitedRegion(monitoringResult.region);
				break;

			case BeaconService.MSG_ERROR_RESPONSE: // '8'
				if (errorListener != null) {
					Integer errorId = (Integer) msg.obj;
					errorListener.onError(errorId);
				}
				break;
				
			case BeaconService.MSG_BLEPERIPHERAL_RANGING_RESPONSE:
				if(blePeripheralRangingListener != null)
				{
					BlePeripheralRangingResult blePeripheralRangingResult = (BlePeripheralRangingResult) msg.obj;
					blePeripheralRangingListener.onBlePeripheralDiscovered(blePeripheralRangingResult.blePeripheral);
				}
				break;

			case BeaconService.MSG_NEW_BEACON_COMING:
				beacon newBeaconInScan = (beacon) msg.obj;
				LogService.d("find a new beacon, name: " + newBeaconInScan.getName());
				mOnNewBeaconFoundListener.onNewBeaconFound(newBeaconInScan);
				break;
				
			case BeaconService.MSG_BEACON_NOT_SEEN:
				@SuppressWarnings("unchecked")
				List<beacon> beaconNotSeen = (List<beacon>) msg.obj;
				mOnBeaconNotSeenListener.onBeaconNotSeen(beaconNotSeen);
				break;
				
			default:
				LogService.d((new StringBuilder()).append("Unknown message: ").append(msg).toString());
				break;
			}
		}

	}

	private class InternalServiceConnection implements ServiceConnection {

		public void onServiceConnected(ComponentName name, IBinder service) {
			serviceMessenger = new Messenger(service);
			if (errorListener != null)
				registerErrorListenerInService();
			if (foregroundScanPeriod != null) {
				setScanPeriod(foregroundScanPeriod, BeaconService.MSG_SET_FOREGROUND_SCAN_PERIOD);
				foregroundScanPeriod = null;
			}
			if (backgroundScanPeriod != null) {
				setScanPeriod(backgroundScanPeriod, BeaconService.MSG_SET_BACKGROUND_SCAN_PERIOD);
				backgroundScanPeriod = null;
			}
			if (callback != null) {
				callback.onServiceReady();
				callback = null;
			}
		}

		public void onServiceDisconnected(ComponentName name) {
			LogService.e((new StringBuilder()).append("Service disconnected, crashed? ").append(name).toString());
			serviceMessenger = null;
		}

		private InternalServiceConnection() {
			super();
		}

	}

	public static interface ErrorListener {
		public abstract void onError(Integer integer);
	}

	public static interface MonitoringListener {
		public abstract void onEnteredRegion(Region region, List<beacon> list);

		public abstract void onExitedRegion(Region region);
	}

	public static interface RangingListener {
		public abstract void onBeaconsDiscovered(Region region, List<beacon> list);
	}

	public static interface BlePeripheralRangingListener{
		public abstract void onBlePeripheralDiscovered(List<BlePeripheral> list);
	}
	
	public static interface ServiceReadyCallback {
		public abstract void onServiceReady();
	}
	
	public static interface onNewBeaconFoundListener {
		public abstract void onNewBeaconFound(beacon newComingBeacon);
	}
	
	public static interface onBeaconNotSeenListener{
		public abstract void onBeaconNotSeen(List<beacon> list);
	}

	private void registerErrorListenerInService() {
		Message registerMsg = Message.obtain(null, BeaconService.MSG_REGISTER_ERROR_LISTENER);
		registerMsg.replyTo = incomingMessenger;
		try {
			serviceMessenger.send(registerMsg);
		} catch (RemoteException e) {
			LogService.e("Error while registering error listener");
		}
	}

	private boolean isConnectedToService() {
		return serviceMessenger != null;
	}

}
