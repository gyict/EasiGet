package com.easimote.sdk.service;

import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.os.*;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.easimote.sdk.BlePeripheral;
import com.easimote.sdk.Preconditions;
import com.easimote.sdk.Region;
import com.easimote.sdk.beacon;
import com.easimote.sdk.Region.State;
import com.easimote.sdk.util.BeaconFilter;
import com.easimote.sdk.util.LogService;
import com.easimote.sdk.util.utils;


public class BeaconService extends Service {
	
	/**
	 * use for classify Ble devices
	 */
	public static final int BEACONSCAN = 1;
	public static final int BLEPERIPHERALSCAN = 2;
	
	/**
	 * use for handler message
	 */
	public static final int MSG_START_RANGING = 1;
	public static final int MSG_STOP_RANGING = 2;
	public static final int MSG_RANGING_RESPONSE = 3;
	public static final int MSG_START_MONITORING = 4;
	public static final int MSG_STOP_MONITORING = 5;
	public static final int MSG_MONITORING_RESPONSE = 6;
	public static final int MSG_REGISTER_ERROR_LISTENER = 7;
	public static final int MSG_ERROR_RESPONSE = 8;
	public static final int MSG_SET_FOREGROUND_SCAN_PERIOD = 9;
	public static final int MSG_SET_BACKGROUND_SCAN_PERIOD = 10;
	public static final int MSG_START_BLEPERIPHERAL_RANGING = 11;
	public static final int MSG_STOP_BLEPERIPHERAL_RANGING = 12;
	public static final int MSG_BLEPERIPHERAL_RANGING_RESPONSE = 13;
	public static final int MSG_NEW_BEACON_COMING = 14;
	public static final int MSG_BEACON_NOT_SEEN = 15;
	public static final int ERROR_COULD_NOT_START_LOW_ENERGY_SCANNING = -1;

	/**
	 * if Easimote's data has encrypted, we should set true;
	 */
	private static boolean isEncrypted = false;


	//10 at first,change to 30 by gy
	public static final long EXPIRATION_MILLIS = TimeUnit.SECONDS.toMillis(30L);
	
	private static final Intent SCAN_START_INTENT = new Intent("startScan");
	private static final Intent AFTER_SCAN_INTENT = new Intent("afterScan");
	
	private final Messenger messenger = new Messenger(new IncomingHandler());
	
	private final BluetoothAdapter.LeScanCallback leScanCallback = new InternalLeScanCallback();
	
	private final ConcurrentHashMap<beacon, Long> beaconsFoundInScanCycle = new ConcurrentHashMap<beacon, Long>();
	private final ConcurrentHashMap<BlePeripheral, Long> BlePeripheralFoundInScanCycle = new ConcurrentHashMap<BlePeripheral, Long>();
	
	private final List<RangingRegion> rangedRegions = new ArrayList<RangingRegion>();
	private final List<MonitoringRegion> monitoredRegions = new ArrayList<MonitoringRegion>();

	private List<beacon> toRemoveNotSeenBeacons = new ArrayList<beacon>();
	
	private static BlePeripheralRangingRegion mBlePeripheralRangingRegion;
	
	private BluetoothAdapter adapter;
	private AlarmManager alarmManager;
	private HandlerThread handlerThread;
	private Handler handler;
	private Runnable afterScanCycleTask;
	private boolean scanning;
	private Messenger errorReplyTo;
	private Messenger newBeaconMsg;
	private Messenger beaconNotSeenMsg;
	private BroadcastReceiver bluetoothBroadcastReceiver;
	private BroadcastReceiver scanStartBroadcastReceiver;
	private PendingIntent scanStartBroadcastPendingIntent;
	private BroadcastReceiver afterScanBroadcastReceiver;
	private PendingIntent afterScanBroadcastPendingIntent;
	private ScanPeriodData foregroundScanPeriod;
	private ScanPeriodData backgroundScanPeriod;
	
	public BeaconService() {
		foregroundScanPeriod = new ScanPeriodData(TimeUnit.SECONDS.toMillis(1L), TimeUnit.SECONDS.toMillis(0L));
		backgroundScanPeriod = new ScanPeriodData(TimeUnit.SECONDS.toMillis(5L), TimeUnit.SECONDS.toMillis(30L));
	}

	@Override
	public void onCreate() {
		super.onCreate();
		LogService.i("Creating service");
		alarmManager = (AlarmManager) getSystemService("alarm");
		BluetoothManager bluetoothManager = (BluetoothManager) getSystemService("bluetooth");
		adapter = bluetoothManager.getAdapter();
		afterScanCycleTask = new AfterScanCycleTask();
		handlerThread = new HandlerThread("BeaconServiceThread", Thread.MAX_PRIORITY);
		handlerThread.start();
		handler = new Handler(handlerThread.getLooper());
		bluetoothBroadcastReceiver = createBluetoothBroadcastReceiver();
		scanStartBroadcastReceiver = createScanStartBroadcastReceiver();
		afterScanBroadcastReceiver = createAfterScanBroadcastReceiver();
		registerReceiver(bluetoothBroadcastReceiver, new IntentFilter("android.bluetooth.adapter.action.STATE_CHANGED"));
		registerReceiver(scanStartBroadcastReceiver, new IntentFilter("startScan"));
		registerReceiver(afterScanBroadcastReceiver, new IntentFilter("afterScan"));
		afterScanBroadcastPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, AFTER_SCAN_INTENT, 0);
		scanStartBroadcastPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, SCAN_START_INTENT, 0);
	}
	
	@Override
	public void onDestroy() {
		LogService.i("Service destroyed");
		unregisterReceiver(bluetoothBroadcastReceiver);
		unregisterReceiver(scanStartBroadcastReceiver);
		unregisterReceiver(afterScanBroadcastReceiver);
		if (adapter != null)
			stopScanning();
		removeAfterScanCycleCallback();
		handlerThread.quit();
		super.onDestroy();
	}

	public IBinder onBind(Intent intent) {
		return messenger.getBinder();
	}

	private void startRanging(RangingRegion rangingRegion) {
		checkNotOnUiThread();
		LogService.v((new StringBuilder()).append("Start ranging: ").append(rangingRegion.region).toString());
		Preconditions.checkNotNull(adapter, "Bluetooth adapter cannot be null");
		rangedRegions.add(rangingRegion);
		startScanning(BEACONSCAN);
	}

	private void stopRanging(String regionId) {
		LogService.v((new StringBuilder()).append("Stopping ranging: ").append(regionId).toString());
		checkNotOnUiThread();
		Iterator<RangingRegion> iterator = rangedRegions.iterator();
		do {
			if (!iterator.hasNext())
				break;
			RangingRegion rangingRegion = (RangingRegion) iterator.next();
			if (regionId.equals(rangingRegion.region.getIdentifier()))
				iterator.remove();
		} while (true);
		if (rangedRegions.isEmpty() && monitoredRegions.isEmpty()) {
			removeAfterScanCycleCallback();
			stopScanning();
			beaconsFoundInScanCycle.clear();
		}
	}
	
	private void startBlePeripheralRanging(BlePeripheralRangingRegion blePeripheralRangingRegion){
		checkNotOnUiThread();
		Preconditions.checkNotNull(adapter, "Bluetooth adapter cannot be null");
		mBlePeripheralRangingRegion = blePeripheralRangingRegion;
		startScanning(BLEPERIPHERALSCAN);
	}
	
	private void stopBlePeripheralRanging(){
		checkNotOnUiThread();
		BlePeripheralFoundInScanCycle.clear();
		stopRanging(null);
	}

	public void startMonitoring(MonitoringRegion monitoringRegion) {
		checkNotOnUiThread();
		LogService.v((new StringBuilder()).append("Starting monitoring: ").append(monitoringRegion.region).toString());
		Preconditions.checkNotNull(adapter, "Bluetooth adapter cannot be null");
		monitoredRegions.add(monitoringRegion);
		startScanning(BEACONSCAN);
	}

	public void stopMonitoring(String regionId) {
		LogService.v((new StringBuilder()).append("Stopping monitoring: ").append(regionId).toString());
		checkNotOnUiThread();
		Iterator<MonitoringRegion> iterator = monitoredRegions.iterator();
		do {
			if (!iterator.hasNext())
				break;
			MonitoringRegion monitoringRegion = (MonitoringRegion) iterator.next();
			if (regionId.equals(monitoringRegion.region.getIdentifier()))
				iterator.remove();
		} while (true);
		if (monitoredRegions.isEmpty() && rangedRegions.isEmpty()) {
			removeAfterScanCycleCallback();
			stopScanning();
			beaconsFoundInScanCycle.clear();
		}
	}

	private void startScanning(int ScanType) {
		if (scanning) {
			LogService.d("Scanning already in progress, not starting one more");
			return;
		}
		if (ScanType == BEACONSCAN && monitoredRegions.isEmpty() && rangedRegions.isEmpty()) {
			LogService.d("Not starting scanning, no monitored on ranged regions");
			return;
		}
		if (!adapter.isEnabled()) {
			LogService.d("Bluetooth is disabled, not starting scanning");
			return;
		}
		if (!adapter.startLeScan(leScanCallback)) {
			LogService.d("Bluetooth adapter did not start le scan");
			sendError(Integer.valueOf(-1));
			return;
		} else {
			scanning = true;
			removeAfterScanCycleCallback();
			setAlarm(afterScanBroadcastPendingIntent, scanPeriodTimeMillis());
			return;
		}
	}

	private void stopScanning() {
		try {
			scanning = false;
			adapter.stopLeScan(leScanCallback);
		} catch (Exception e) {
			LogService.e("BluetoothAdapter throws unexpected exception");
		}
	}
	
	
	private class InternalLeScanCallback implements android.bluetooth.BluetoothAdapter.LeScanCallback {
		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte scanRecord[]) {
			checkNotOnUiThread();
			int deviceType = utils.checkDeviceType(scanRecord);
			if(deviceType == utils.DEVICE_TYPE_BEACON)
			{
				beacon beacon = utils.beaconFromLeScan(device, rssi, scanRecord, isEncrypted);
				if (beacon == null || !BeaconFilter.isEasimote(beacon)) {
					return;
				}
				else 
				{
					if(newBeaconMsg != null)
					{
						boolean isBeaconScaned = false;
						for (Iterator<RangingRegion> it = rangedRegions.iterator(); it.hasNext();) 
						{
							RangingRegion rangingRegion = (RangingRegion) it.next();
							if(rangingRegion.getSortedBeacons().contains(beacon))
								isBeaconScaned = true;	
						}
						if(!isBeaconScaned)
						{
							Message newComingBeaconMsg = Message.obtain(null, MSG_NEW_BEACON_COMING);
							newComingBeaconMsg.obj = beacon;
							try {
								newBeaconMsg.send(newComingBeaconMsg);
							} catch (RemoteException e) {
								LogService.e("Error while delivering responses", e);
							}
						}
						
					}
					
					beaconsFoundInScanCycle.put(beacon, Long.valueOf(System.currentTimeMillis()));
					return;
				}
			}
			
			else if(deviceType == utils.DEVICE_TYPE_BLEPERIPHERAL)
			{
				BlePeripheral mBlePeripheral = utils.BlePeripheralFromLeScan(device, rssi, scanRecord);
				LogService.v("peripheral" + ("" + mBlePeripheral.getRssi()) + ":" + device.getName());
				BlePeripheralFoundInScanCycle.put(mBlePeripheral, Long.valueOf(System.currentTimeMillis()));
				return;
			}
			
		}

	}

	private class IncomingHandler extends Handler {		
		//final BeaconService mBeaconService;

		private IncomingHandler() {
			super();
		//	mBeaconService = BeaconService.this;	
		}
		@Override
		public void handleMessage(Message msg) {
			final int what = msg.what;
			final Object obj = msg.obj;
			final Messenger replyTo = msg.replyTo;
			handler.post(new Runnable() {
				@Override
				public void run() {
					switch (what) {
					case MSG_START_RANGING: 
						RangingRegion rangingRegion = new RangingRegion((Region) obj, replyTo);
						startRanging(rangingRegion);
						break;

					case MSG_STOP_RANGING: 
						String rangingRegionId = (String) obj;
						stopRanging(rangingRegionId);
						break;

					case MSG_START_MONITORING: 
						MonitoringRegion monitoringRegion = new MonitoringRegion((Region) obj, replyTo);
						startMonitoring(monitoringRegion);
						break;

					case MSG_STOP_MONITORING: 
						String monitoredRegionId = (String) obj;
						stopMonitoring(monitoredRegionId);
						break;

					case MSG_REGISTER_ERROR_LISTENER: 
						errorReplyTo = replyTo;
						break;

					case MSG_SET_FOREGROUND_SCAN_PERIOD: 
						LogService.d((new StringBuilder()).append("Setting foreground scan period: ").append(foregroundScanPeriod).toString());
						foregroundScanPeriod = (ScanPeriodData) obj;
						break;

					case MSG_SET_BACKGROUND_SCAN_PERIOD: 
						LogService.d((new StringBuilder()).append("Setting background scan period: ").append(backgroundScanPeriod).toString());
						backgroundScanPeriod = (ScanPeriodData) obj;
						break;
						
					case MSG_START_BLEPERIPHERAL_RANGING:
						BlePeripheralRangingRegion blePeripheralRangingRegion = new BlePeripheralRangingRegion(replyTo);
						startBlePeripheralRanging(blePeripheralRangingRegion);
						break;
						
					case MSG_STOP_BLEPERIPHERAL_RANGING:
						stopBlePeripheralRanging();
						break;
						
					case MSG_NEW_BEACON_COMING:
						newBeaconMsg = replyTo;
						break;
						
					case MSG_BEACON_NOT_SEEN:
						beaconNotSeenMsg = replyTo;
						break;

					case MSG_RANGING_RESPONSE: 
					case MSG_MONITORING_RESPONSE: 
					case MSG_ERROR_RESPONSE:
					default:
						LogService.d((new StringBuilder()).append("Unknown message: what=").append(what).append(" obj=").append(obj).toString());
						break;
					}
				}

				
			});
		}

	}

	private class AfterScanCycleTask implements Runnable {
		
		private AfterScanCycleTask() {
			super();
			//mBeaconService = BeaconService.this;
		}
		
		public void run() {
			LogService.v("BeaconService start to run");
			checkNotOnUiThread();
			long now = System.currentTimeMillis();
			stopScanning();
			processRanging();
			
			List<MonitoringRegion> enteredRegions = findEnteredRegions(now);
			List<MonitoringRegion> exitedRegions = findExitedRegions(now);
			
			toRemoveNotSeenBeacons.clear();
			toRemoveNotSeenBeacons = removeNotSeenBeacons(now);
			
			if(!toRemoveNotSeenBeacons.isEmpty())
				notifyNotSeenBeacon(toRemoveNotSeenBeacons);
			
			mBlePeripheralRangingRegion.removeNotSeenBlePeripherals(now);
			beaconsFoundInScanCycle.clear();
			BlePeripheralFoundInScanCycle.clear();
			
			invokeCallbacks(enteredRegions, exitedRegions);
			blePeripheralInvokeCallback();
			
			if (scanWaitTimeMillis() == 0L)
				startScanning(BEACONSCAN);
			else
				setAlarm(scanStartBroadcastPendingIntent, scanWaitTimeMillis());
		}

		private void notifyNotSeenBeacon(List<beacon> notSeenBeaconList){
			if(beaconNotSeenMsg != null)
			{
				for(beacon it:toRemoveNotSeenBeacons)
					LogService.d("not seen beacon: " + it.getName());
				Message beaconNotSeenMsg = Message.obtain(null, MSG_BEACON_NOT_SEEN);
				beaconNotSeenMsg.obj = notSeenBeaconList;
				try {
					newBeaconMsg.send(beaconNotSeenMsg);
				} catch (RemoteException e) {
					LogService.e("Error while delivering responses", e);
				}
			}
		}
		
		private void processRanging() {
			RangingRegion rangedRegion;
			for (Iterator<RangingRegion> it = rangedRegions.iterator(); it.hasNext(); rangedRegion.processFoundBeacons(beaconsFoundInScanCycle))
				rangedRegion = (RangingRegion) it.next();
			mBlePeripheralRangingRegion.processFoundBlePeripherals(BlePeripheralFoundInScanCycle);

		}

		private List<MonitoringRegion> findEnteredRegions(long currentTimeMillis) {
			List<MonitoringRegion> didEnterRegions = new ArrayList<MonitoringRegion>();
			for (Iterator<Entry<beacon, Long>> it = beaconsFoundInScanCycle.entrySet().iterator(); it.hasNext();) {
				Entry<beacon, Long> entry = (Entry<beacon, Long>) it.next();
				Iterator<MonitoringRegion> it2 = matchingMonitoredRegions((beacon) entry.getKey()).iterator();
				while (it2.hasNext()) {
					MonitoringRegion monitoringRegion = (MonitoringRegion) it2.next();
					monitoringRegion.processFoundBeacons(beaconsFoundInScanCycle);
					if (monitoringRegion.markAsSeen(currentTimeMillis))
						didEnterRegions.add(monitoringRegion);
				}
			}

			for(Iterator<MonitoringRegion> it = didEnterRegions.iterator(); it.hasNext();)
			{
				MonitoringRegion tmp = it.next();
				LogService.d("enter monitoring region: " + tmp.toString());
			}
			return didEnterRegions;
		}

		private List<MonitoringRegion> matchingMonitoredRegions(beacon beacon) {
			List<MonitoringRegion> results = new ArrayList<MonitoringRegion>();
			Iterator<MonitoringRegion> it = monitoredRegions.iterator();
			do {
				if (!it.hasNext())
					break;
				MonitoringRegion monitoredRegion = (MonitoringRegion) it.next();
				if (utils.isBeaconInRegion(beacon, monitoredRegion.region))
					results.add(monitoredRegion);
			} while (true);
			return results;
		}

		private List<beacon> removeNotSeenBeacons(long currentTimeMillis) {
			List<beacon> RemoveBeaconsList = new ArrayList<beacon>();
			RangingRegion rangedRegion;
			for (Iterator<RangingRegion> it = rangedRegions.iterator(); it.hasNext(); RemoveBeaconsList.addAll(rangedRegion.removeNotSeenBeacons(currentTimeMillis)))
				rangedRegion = (RangingRegion) it.next();

			MonitoringRegion monitoredRegion;
			for (Iterator<MonitoringRegion> it = monitoredRegions.iterator(); it.hasNext(); monitoredRegion.removeNotSeenBeacons(currentTimeMillis))
				monitoredRegion = (MonitoringRegion) it.next();

			return RemoveBeaconsList;
		}

		private List<MonitoringRegion> findExitedRegions(long currentTimeMillis) {
			List<MonitoringRegion> didExitMonitors = new ArrayList<MonitoringRegion>();
			Iterator<MonitoringRegion> it = monitoredRegions.iterator();
			do {
				if (!it.hasNext())
					break;
				MonitoringRegion monitoredRegion = (MonitoringRegion) it.next();
				if (monitoredRegion.didJustExit(currentTimeMillis))
					didExitMonitors.add(monitoredRegion);
			} while (true);
			return didExitMonitors;
		}

		private void invokeCallbacks(List<MonitoringRegion> enteredMonitors, List<MonitoringRegion> exitedMonitors) {
			for (Iterator<RangingRegion> it = rangedRegions.iterator(); it.hasNext();) {
				RangingRegion rangingRegion = (RangingRegion) it.next();
				try {
					Message rangingResponseMsg = Message.obtain(null, MSG_RANGING_RESPONSE);
					rangingResponseMsg.obj = new RangingResult(rangingRegion.region, rangingRegion.getSortedBeacons());
					rangingRegion.replyTo.send(rangingResponseMsg);
				} catch (RemoteException e) {
					LogService.e("Error while delivering responses", e);
				}
			}

			for (Iterator<MonitoringRegion> it = enteredMonitors.iterator(); it.hasNext();) {
				MonitoringRegion didEnterMonitor = (MonitoringRegion) it.next();
				Message monitoringResponseMsg = Message.obtain(null, MSG_MONITORING_RESPONSE);
				monitoringResponseMsg.obj = new MonitoringResult(didEnterMonitor.region, Region.State.INSIDE, didEnterMonitor.getSortedBeacons());
				try {
					didEnterMonitor.replyTo.send(monitoringResponseMsg);
				} catch (RemoteException e) {
					LogService.e("Error while delivering responses", e);
				}
			}

			for (Iterator<MonitoringRegion> it = exitedMonitors.iterator(); it.hasNext();) {
				MonitoringRegion didEnterMonitor = (MonitoringRegion) it.next();
				Message monitoringResponseMsg = Message.obtain(null, MSG_MONITORING_RESPONSE);
				monitoringResponseMsg.obj = new MonitoringResult(didEnterMonitor.region, Region.State.OUTSIDE, Collections.<beacon>emptyList());
				try {
					didEnterMonitor.replyTo.send(monitoringResponseMsg);
				} catch (RemoteException e) {
					LogService.e("Error while delivering responses", e);
				}
			}

		}
		
		private void blePeripheralInvokeCallback(){
			try {
				Message BlePeripheralScanResultMsg = Message.obtain(null, MSG_BLEPERIPHERAL_RANGING_RESPONSE);
				BlePeripheralScanResultMsg.obj = new BlePeripheralRangingResult(mBlePeripheralRangingRegion.getFoundBlePeriapherals());
				//BlePeripheralScanResultMsg.obj = new BlePeripheralRangingResult(new ArrayList<BlePeripheral>(BlePeripheralFoundInScanCycle.keySet()));
				mBlePeripheralRangingRegion.replyTo.send(BlePeripheralScanResultMsg);
			} catch (RemoteException e) {
				LogService.e("Error while delivering responses", e);
			}
		}
	}


	private void sendError(Integer errorId) {
		if (errorReplyTo != null) {
			Message errorMsg = Message.obtain(null, MSG_ERROR_RESPONSE);
			errorMsg.obj = errorId;
			try {
				errorReplyTo.send(errorMsg);
			} catch (RemoteException e) {
				LogService.e("Error while reporting message, funny right?", e);
			}
		}
	}

	private long scanPeriodTimeMillis() {
		if (!rangedRegions.isEmpty())
			return foregroundScanPeriod.scanPeriodMillis;
		else
			return backgroundScanPeriod.scanPeriodMillis;
	}

	private long scanWaitTimeMillis() {
		if (!rangedRegions.isEmpty())
			return foregroundScanPeriod.waitTimeMillis;
		else
			return backgroundScanPeriod.waitTimeMillis;
	}

	private void setAlarm(PendingIntent pendingIntent, long delayMillis) {
		alarmManager.set(2, SystemClock.elapsedRealtime() + delayMillis, pendingIntent);
	}

	private void checkNotOnUiThread() {
		//Preconditions.checkArgument(Looper.getMainLooper().getThread() != Thread.currentThread(),
		//				"This cannot be run on UI thread, starting BLE scan can be expensive");
		//Preconditions.checkNotNull(Boolean.valueOf(handlerThread.getLooper() == Looper.myLooper()),
		//				"It must be executed on service's handlerThread");
	}

	private BroadcastReceiver createBluetoothBroadcastReceiver() {
		return new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				if ("android.bluetooth.adapter.action.STATE_CHANGED".equals(intent.getAction())) {
					int state = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", -1);
					if (state == 10)
						handler.post(new Runnable() {
							public void run() {
								LogService.i("Bluetooth is OFF: stopping scanning");
								removeAfterScanCycleCallback();
								stopScanning();
								beaconsFoundInScanCycle.clear();
							}

							/*final _cls1 this$1;

							{
								super();
								this$1 = _cls1.this;
								
							}*/
						});
					else if (state == 12)
						handler.post(new Runnable() {

							public void run() {
								if (!monitoredRegions.isEmpty()|| !rangedRegions.isEmpty()) {
									LogService.i(String.format("Bluetooth is ON: resuming scanning (monitoring: %d ranging:%d)", new Object[] {
															Integer.valueOf(monitoredRegions.size()),
															Integer.valueOf(rangedRegions.size()) }));
									startScanning(BEACONSCAN);
								}
							}

							/*final _cls1 this$1;

							{
								this$1 = _cls1.this;
								super();
							}*/
						});
				}
			}

			/*final BeaconService this$0;

			{
				this$0 = BeaconService.this;
				super();
			}*/
		};
	}

	private void removeAfterScanCycleCallback() {
		handler.removeCallbacks(afterScanCycleTask);
		alarmManager.cancel(afterScanBroadcastPendingIntent);
		alarmManager.cancel(scanStartBroadcastPendingIntent);
	}

	private BroadcastReceiver createAfterScanBroadcastReceiver() {
		return new BroadcastReceiver() {

			public void onReceive(Context context, Intent intent) {
				handler.post(afterScanCycleTask);
			}

			/*final BeaconService this$0;

			{
				this$0 = BeaconService.this;
				super();
			}*/
		};
	}

	private BroadcastReceiver createScanStartBroadcastReceiver() {
		return new BroadcastReceiver() {

			public void onReceive(Context context, Intent intent) {
				handler.post(new Runnable() {

					public void run() {
						startScanning(BEACONSCAN);
					}

					/*final _cls3 this$1;

					{
						this$1 = _cls3.this;
						super();
					}*/
				});
			}

			/*final BeaconService this$0;

			{
				this$0 = BeaconService.this;
				super();
			}*/
		};
	}



}