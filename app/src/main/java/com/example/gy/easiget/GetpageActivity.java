
package com.example.gy.easiget;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.easimote.sdk.BeaconManager;
import com.easimote.sdk.BlePeripheral;
import com.easimote.sdk.Region;
import com.easimote.sdk.beacon;
import com.easimote.sdk.service.BeaconService;
import com.easimote.sdk.util.LogService;
import com.easimote.sdk.util.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class GetpageActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1234;
    private static final int REQUEST_ENABLE_LOC = 5678;

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION=0;
    private static final String TAG = GetpageActivity.class.getSimpleName();
    public static final String URLSTRING = "URLSTRING";
    public static final Region ALL_BEACONS_REGION = new Region("rid", null, null, null);
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;


    public static BeaconManager beaconManager;
    public static NotificationManager notificationManager;

    private static final int TIME_INTERVAL = 2000; // # milliseconds, desired time passed between two back presses.
    private long mBackPressed;

    /**
     * regions for test
     */
    //for modified value
    public static Region web2Region = new Region("金瓯永固杯", "fda50693a4e24fb1afcfc6eb07647825", 8487, 30947);
    public static Region web1Region = new Region("金编钟", "fda50693a4e24fb1afcfc6eb07647825", 8487, 30691);
    //for default value
    public static Region web22Region = new Region("金瓯永固杯", "49435457534e455a183ca57ca4b4b5c6", 43520, 43520);
    public static Region web11Region = new Region("金编钟", "49435457534e455a183cc219cd9786dd", 43520, 43520);

    public static List<String> webs = new ArrayList<String>();

    /**
     * regions saved users interest entry
     */
    public static List<Region> interestedRegion = new ArrayList<Region>();
    public static HashMap<Region,String> ruhash = new HashMap<Region,String>();


    private BeaconListAdapter beaconListAdapter;
    private ListView blelv;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_getpage);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Intent intent = new Intent(GetpageActivity.this, BeaconService.class);
        stopService(intent);

//        web1Region = new Region("web1", null, 65535, 65535);
//        web2Region = new Region("web2", null, 43520, 43520);
        webs.add("http://test.zhongketianhe.com.cn:8080/EasiWeb/jinbianzhong.jsp");
        webs.add("http://test.zhongketianhe.com.cn:8080/EasiWeb/jinouyonggubei.jsp");
        interestedRegion.add(web1Region);
        interestedRegion.add(web2Region);
        interestedRegion.add(web11Region);
        interestedRegion.add(web22Region);
        ruhash.put(web1Region,webs.get(0));
        ruhash.put(web2Region,webs.get(1));
        ruhash.put(web11Region,webs.get(0));
        ruhash.put(web22Region,webs.get(1));
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Contact to Us by xxx@xxx.xx", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }

        // Configure verbose debug logging.
        LogService.enableDebugLogging(true);

        if(!CheckWifiandBLEandLocate()){
            AlertDialog.Builder builder = new AlertDialog.Builder(GetpageActivity.this);
            builder.setMessage("请开启手机网络、蓝牙及定位服务，否则将无法beacon搜索");
            builder.setTitle("提示");
            builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    // GetpageActivity.this.finish();
                }

            });
            builder.setNegativeButton("不想打开", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    GetpageActivity.this.finish();
                }

            });
            builder.create().show();
        }

        //ArrayList<beacon> test = new ArrayList<beacon>();
        blelv = (ListView) findViewById(R.id.blelistview);
        beaconListAdapter = new BeaconListAdapter(this);
        blelv.setAdapter(this.beaconListAdapter);

        blelv.setOnItemClickListener(beaconListClickListener());


        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);


        // Configure BeaconManager.
        beaconManager = new BeaconManager(this);

        beaconManager.setRangingListener(new BeaconManager.RangingListener() {

            @Override
            public void onBeaconsDiscovered(Region region, final List<beacon> list) {
                //do nothing
                runOnUiThread(new Runnable() {
                    public void run() {
                        //ListBeaconsActivity.this.getActionBar().setSubtitle("已搜寻beacon:" + paramAnonymousList.size());

                        //Toast.makeText(GetpageActivity.this,"beacondiscovered",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        beaconManager.setMonitoringListener(new BeaconManager.MonitoringListener() {
            @Override
            public void onEnteredRegion(Region region, List<beacon> beacons) {
                //Toast.makeText(GetpageActivity.this,"enterregion",Toast.LENGTH_SHORT).show();
                beaconListAdapter.addsome(beacons);
                //Region region1 = checkIsInInterestRegion(beacons);
                postNotification("发现展品:"+region.getIdentifier(), region.getIdentifier(), 1);
                Log.e(region.getIdentifier(),beacons.get(0).getName());
                //Log.e("enterregion",""+beacons.size());
            }

            @Override
            public void onExitedRegion(Region region) {
                // Toast.makeText(GetpageActivity.this,"exitregion",Toast.LENGTH_SHORT).show();
               // postNotification("Exit Region", region.getIdentifier(), 4);
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
//                Region region = checkIsInInterestRegion(newComingBeacon);
                Log.e("nbf",newComingBeacon.getName()+" "+newComingBeacon.getMajor()+" "+newComingBeacon.getMinor()+" "+newComingBeacon.getProximityUUID());
//                if(region!=null){
//                    beaconListAdapter.addone(newComingBeacon);
//                    postNotification("发现展品"+region.getIdentifier(), region.getIdentifier(), 1);
//                }
                //Region region1 = checkIsInInterestRegion(beacons);

            }
        });

        beaconManager.setOnBeaconNotSeenListener(new BeaconManager.onBeaconNotSeenListener() {

            @Override
            public void onBeaconNotSeen(List<beacon> list) {
                //do nothing
                /**
                 * when a beacon event happens, we should request data
                 * from server using: "beacon Proximity UUID"
                 * "beacon major" "beacon minor" the event it makes
                 * contain: eventType:"Beacon_leave"
                 * eventRelateBeaconUUID: the beacon's Proximity UUID
                 * eventMessage: Abstract which server send
                 */
                //Toast.makeText(GetpageActivity.this,"beaconnotseen",Toast.LENGTH_SHORT).show();
                //postNotification("beacon监控:离开", list);

                List<beacon> beacons = new ArrayList<beacon>() ;
                for(beacon b:list){
                    if(checkIsInInterestRegion(b)!=null){
                        beacons.add(b);
                    }
                }
                //Log.e("not seen",""+beacons.size());
                if(beacons.size()>0){
                    beaconListAdapter.removesome(beacons);
                }

               // postNotification("beacons not seen", "in monitered region", 2);
            }
        });

        beaconManager.setBlePeripheralRangingListener(new BeaconManager.BlePeripheralRangingListener()
        {
            public void onBlePeripheralDiscovered(final List<BlePeripheral> paramAnonymousList)
            {
                GetpageActivity.this.runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        //Log.e("blefound",""+paramAnonymousList.size());
                    }
                });
            }
        });
    }

    /**
     * Check if the network,BLE and location is opened
     * @return
     */
    private boolean CheckWifiandBLEandLocate() {

        ConnectivityManager connectivity = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            // 获取网络连接管理的对象
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info == null || !info.isAvailable())
                return false;

            if (info != null && info.getState() != NetworkInfo.State.CONNECTED)
                return false;

        }

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())
            return false;
        //TODO add location request and check
        LocationManager locationManager
                = (LocationManager) GetpageActivity.this.getSystemService(Context.LOCATION_SERVICE);
        // 通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）
        if (Build.VERSION.SDK_INT >= 23) {
            int checkCallPhonePermission = ContextCompat.checkSelfPermission(GetpageActivity.this,Manifest.permission.ACCESS_COARSE_LOCATION);
            if(checkCallPhonePermission != PackageManager.PERMISSION_GRANTED){
                //判断是否需要 向用户解释，为什么要申请该权限
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION))
                    Toast.makeText(GetpageActivity.this,"必须打开定位服务才能正确接收到结果", Toast.LENGTH_SHORT).show();

                ActivityCompat.requestPermissions(this ,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

            }else{

            }
        } else {

        }
//        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
//        if(gps==false){
//            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//            GetpageActivity.this.startActivityForResult(intent,1);
//        }
//        gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
//        if(gps==false)return false;
        return true;
    }

    /**
     * check if a new found beacon is in the interest list
     */
    public static Region checkIsInInterestRegion(beacon newFoundBeacon){
        for(Region mRegion: interestedRegion){
            if(utils.isBeaconInRegion(newFoundBeacon, mRegion))
                return mRegion;
        }
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // TODO request success
                }
                break;
        }
    }


    @Override
    protected void onDestroy() {
        try {
            beaconManager.stopRanging(ALL_BEACONS_REGION);
            beaconManager.stopMonitoring(web1Region);
            beaconManager.stopMonitoring(web2Region);
        } catch (RemoteException e) {
            Log.d("onDestroy", "Error while stopping ranging");
        }

        for(int i = 0; i<4; i++){
            notificationManager.cancel(i);
        }
        beaconManager.disconnect();

        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if device supports Bluetooth Low Energy.
        if (!beaconManager.hasBluetooth()) {
            //Toast.makeText(this, "Device does not have Bluetooth Low Energy", Toast.LENGTH_LONG).show();
            return;
        }

        // If Bluetooth is not enabled, let user enable it.
        if (!beaconManager.isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        if(!beaconManager.isLocationEnabled()){
            Log.e("locate",""+beaconManager.isLocationEnabled());
            Intent GPSIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            //Intent GPSIntent = new Intent()
            startActivityForResult(GPSIntent, REQUEST_ENABLE_LOC);

        }
        //connectToService();
        if(this.beaconListAdapter.isEmpty()){
            connectToService();
        }else{
            Timer timer = new Timer();// 实例化Timer类
            timer.schedule(new TimerTask() {
                public void run() {
                    connectToService();
                    Log.e(TAG,"start again");
                    this.cancel();
                }
            }, 30000);
        }


    }


    @Override
    public void onBackPressed()
    {

        if (mBackPressed + TIME_INTERVAL > System.currentTimeMillis())
        {
            super.onBackPressed();
            return;
        }
        else {
            Toast.makeText(getBaseContext(), "再次点击以退出，如果退出则无法接收推送消息！可点击home键，以继续接收推送消息", Toast.LENGTH_SHORT).show();
        }

        mBackPressed = System.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
//        try {
//            beaconManager.stopRanging(ALL_BEACONS_REGION);
//            beaconManager.stopMonitoring(web1Region);
//            beaconManager.stopMonitoring(web2Region);
//        } catch (RemoteException e) {
//            Log.d(TAG, "Error while stopping ranging", e);
//        }
        Intent intent = new Intent(GetpageActivity.this, BeaconService.class);
        startService(intent);

        super.onStop();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                connectToService();
            } else {
                Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_LONG).show();
                //getActionBar().setSubtitle("Bluetooth not enabled");
            }
        }else if(requestCode == REQUEST_ENABLE_LOC){
            Log.e("requestCode",""+resultCode+" "+Activity.RESULT_OK);
            if(resultCode == Activity.RESULT_OK){
                connectToService();
            }else{
                //Toast.makeText(this, "Location not enabled", Toast.LENGTH_LONG).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void connectToService() {
//        Runnable   runnableUi=new  Runnable(){
//            @Override
//            public void run() {
//                //更新界面
//                beaconListAdapter.replaceWith(Collections.<beacon> emptyList());
//            }
//
//        };

        //if(beaconListAdapter.isEmpty())beaconListAdapter.replaceWith(Collections.<beacon> emptyList());
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
                    beaconManager.startMonitoring(GetpageActivity.web11Region);
                    beaconManager.startMonitoring(GetpageActivity.web22Region);
                    //beaconManager.startMonitoring(web1region);

                    //beaconManager.startMonitoring(ALL_BEACONS_REGION);

                }
                catch (RemoteException localRemoteException)
                {
                    Toast.makeText(GetpageActivity.this, "无法搜寻", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private AdapterView.OnItemClickListener  beaconListClickListener(){
        return new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Region rg = checkIsInInterestRegion(beaconListAdapter.getItem(position));
                //Log.e(beaconListAdapter.getItem(position).getMacAddress(),beaconListAdapter.getItem(position).getMajor()+" "+beaconListAdapter.getItem(position).getMinor()+" "+rg);
                if(rg!=null){
                    //String UrlMsg = "http://test.zhongketianhe.com.cn:8080/EasiWeb/jinbianzhong.jsp";
                    String UrlMsg = ruhash.get(rg);
                    //Toast.makeText(GetpageActivity.this,"isin",Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(GetpageActivity.this,WebViewActivity.class);
                    intent.putExtra(URLSTRING,UrlMsg);
                    startActivity(intent);
                }

            }
        };
    }


    private void postNotification(String msg, String msg2, int NOTIFICATION_ID) {
        Intent notifyIntent = new Intent(GetpageActivity.this, WebViewActivity.class);
        if(msg2.contains("金瓯永固杯")){
            notifyIntent.putExtra(URLSTRING,webs.get(1));
        }else if(msg2.contains("金编钟")){
            notifyIntent.putExtra(URLSTRING,webs.get(0));
        }
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivities(
                GetpageActivity.this, 0, new Intent[] { notifyIntent },
                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(GetpageActivity.this)
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


}
