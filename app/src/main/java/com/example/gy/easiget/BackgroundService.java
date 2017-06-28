package com.example.gy.easiget;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.easimote.sdk.BeaconManager;
import com.easimote.sdk.BlePeripheral;
import com.easimote.sdk.Region;
import com.easimote.sdk.beacon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by lenovo on 2017/6/23.
 */

public class BackgroundService extends Service {

    private static List<String> webs = new ArrayList<String>();

    /**
     * regions saved users interest entry
     */
    public static List<Region> interestedRegion = GetpageActivity.interestedRegion;
    public static HashMap<Region,String> ruhash = GetpageActivity.ruhash;
    public BeaconManager beaconManager = GetpageActivity.beaconManager;
    public NotificationManager notificationManager = GetpageActivity.notificationManager;
    public static final String URLSTRING = "URLSTRING";

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(BackgroundService.this,"backgst",Toast.LENGTH_SHORT).show();
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {

            @Override
            public void onBeaconsDiscovered(Region region, final List<beacon> list) {
                //do nothing
//                runOnUiThread(new Runnable() {
//                    public void run() {
//                        //ListBeaconsActivity.this.getActionBar().setSubtitle("已搜寻beacon:" + paramAnonymousList.size());
//
//                        //Toast.makeText(GetpageActivity.this,"beacondiscovered",Toast.LENGTH_SHORT).show();
//                    }
//                });
            }
        });

        beaconManager.setMonitoringListener(new BeaconManager.MonitoringListener() {
            @Override
            public void onEnteredRegion(Region region, List<beacon> beacons) {
                //Toast.makeText(GetpageActivity.this,"enterregion",Toast.LENGTH_SHORT).show();

                postNotification("Enter Region: "+region.getIdentifier(), region.getIdentifier(), 1);
                Log.e(region.getIdentifier(),beacons.get(0).getName());
                //Log.e("enterregion",""+beacons.size());
            }

            @Override
            public void onExitedRegion(Region region) {
                // Toast.makeText(GetpageActivity.this,"exitregion",Toast.LENGTH_SHORT).show();
                postNotification("Exit Region", region.getIdentifier(), 4);
            }
        });

        beaconManager.setOnNewBeaconFoundListener(new BeaconManager.onNewBeaconFoundListener() {

            @Override
            public void onNewBeaconFound(beacon newComingBeacon) {
                /**
                 * when a beacon event happens, we should request data
                 * from server using: "beacon Proximity UUID"
                 * "beacon major" "beacon minor" the event it makes
                 * contain: eventType:"Beacon_into"
                 * eventRelateBeaconUUID: the beacon's Proximity UUID
                 * eventMessage: Abstract which server send
                 */


                //Toast.makeText(GetpageActivity.this,"newbeaconfound",Toast.LENGTH_SHORT).show();
                // Log.e("newbeacon",newComingBeacon.getName());
                Log.e("nbf",newComingBeacon.getName()+" "+newComingBeacon.getMajor()+" "+newComingBeacon.getMinor()+" "+newComingBeacon.getProximityUUID());

            }
        });



        beaconManager.setBlePeripheralRangingListener(new BeaconManager.BlePeripheralRangingListener()
        {
            public void onBlePeripheralDiscovered(final List<BlePeripheral> paramAnonymousList)
            {

            }
        });
    }

    private void connectToService() {
        beaconManager.connect(new BeaconManager.ServiceReadyCallback()
        {
            public void onServiceReady()
            {
                try
                {
                    beaconManager.startRanging(GetpageActivity.ALL_BEACONS_REGION);
                    beaconManager.startBlePeripheralRanging();

                    beaconManager.startMonitoring(GetpageActivity.web1Region);
                    beaconManager.startMonitoring(GetpageActivity.web2Region);
                    //beaconManager.startMonitoring(web1region);

                    //beaconManager.startMonitoring(ALL_BEACONS_REGION);

                }
                catch (RemoteException localRemoteException)
                {
                    Toast.makeText(BackgroundService.this, "无法搜寻", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }


    private void postNotification(String msg, String msg2, int NOTIFICATION_ID) {
        Intent notifyIntent = new Intent(BackgroundService.this, WebViewActivity.class);
        if(msg2.contains("web2")){
            notifyIntent.putExtra(URLSTRING,webs.get(1));
        }else if(msg2.contains("web1")){
            notifyIntent.putExtra(URLSTRING,webs.get(0));
        }
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivities(
                BackgroundService.this, 0, new Intent[] { notifyIntent },
                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(BackgroundService.this)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        (NOTIFICATION_ID == 1 || NOTIFICATION_ID == 3) ? R.drawable.in: R.drawable.out))
                .setSmallIcon(R.drawable.smallicon).setContentTitle(msg)
                .setContentText(msg2).setAutoCancel(true)
                .setContentIntent(pendingIntent).build();
        if ((NOTIFICATION_ID == 1 || NOTIFICATION_ID == 3))
        {
            notification.sound = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.in);
            notification.ledARGB = 0xff00ff00;
        }
        else
        {
            notification.sound = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.out);
            notification.ledARGB = 0xff0000;
        }

        // notification.defaults |= Notification.DEFAULT_LIGHTS;
        notification.ledOnMS = 1000;
        notification.ledOffMS = 1000;
        notification.flags |= Notification.FLAG_SHOW_LIGHTS;

        notificationManager.notify(NOTIFICATION_ID, notification);

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
