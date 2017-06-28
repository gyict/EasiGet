package com.easimote.sdk.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.easimote.sdk.BlePeripheral;
import com.easimote.sdk.Preconditions;
import com.easimote.sdk.Region;
import com.easimote.sdk.beacon;
import com.easimote.sdk.basicObjType.Hashcode;

import android.bluetooth.*;
import android.content.*;
import android.text.TextUtils;
import android.util.SparseArray;

public class utils extends Object{

	
	/**
	 * the distance types
	 * Proximity :
	 *	   UNKNOWN,
     *     IMMEDIATE,
     *     NEAR,
     *     FAR;
	 */
    public enum Proximity
    {
    	UNKNOWN(0),
        IMMEDIATE(1),
        NEAR(2),
        FAR(3){
    		@Override
    		public boolean isRest(){
    			return true;
    		}
    	};
    	private int value;
   	 
	    private Proximity(int value) {
	        this.value = value;
	    }
	 
	    public int getValue() {
	        return value;
	    }
	 
	    public boolean isRest() {
	        return false;
	    }
    }
    
	//Interface used to notify called that Bluetooth stack has been restarted.
    public static interface RestartCompletedListener
    {
    	public abstract void onRestartCompleted();
    }   
    
    /****
     * decide what type the ble device is:
     * DEVICE_TYPE_BEACON == 1;
     * DEVICE_TYPE_BLEPERIPHERAL == 2;
     * 
     * what makes a ble device a beacon?
     * it is decided by broadcast message:
     * in PDU, it contains up to 31bytes, if it is a beacon device, it should be like this:
     * 02 01 06 1A FF 4C 00 02 15: iBeacon prefix (fixed) 
     * B9 40 7F 30 F5 F8 46 6E AF F9 25 55 6B 57 FE 6D: proximity UUID 
     * 00 49: major 
     * 00 0A: minor 
     * C5: 2s complement of measured TX power
     * 
     * and if this device is not a beacon device, we classify it as a BlePeripheral
     * 
     * see: Easimote SDK User's Guide
     */
    
    public static int DEVICE_TYPE_BEACON = 1;
    public static int DEVICE_TYPE_BLEPERIPHERAL = 2;
    public static int checkDeviceType(byte scanRecord[])
    {
    	if(unsignedByteToInt(scanRecord[0]) == 0x02 
				&& unsignedByteToInt(scanRecord[1]) == 0x01
				&& unsignedByteToInt(scanRecord[2]) == 0x06
				&& unsignedByteToInt(scanRecord[3]) == 0x1a
				&& unsignedByteToInt(scanRecord[4]) == 0xff
				//&& unsignedByteToInt(scanRecord[5]) == 0x4c// this byte means the company type
				//&& unsignedByteToInt(scanRecord[6]) == 0x00
				&& unsignedByteToInt(scanRecord[7]) == 0x02
				&& unsignedByteToInt(scanRecord[8]) == 0x15)
    		return DEVICE_TYPE_BEACON;
    	else
    		return DEVICE_TYPE_BLEPERIPHERAL;
    	
    }
    
    /***
     * parse scanRecord to construct blePeripheral
     * 
     */
    public static BlePeripheral BlePeripheralFromLeScan(BluetoothDevice device, int rssi, byte scanRecord[])
    {   	
    	String name = device.getName();
    	String macAddress = device.getAddress();
    	int type = device.getType();
    	return new BlePeripheral(name, macAddress, type, rssi, scanRecord);
    }
    
    /***
     * Parses scanRecord to look for manufacturer specific data, verifies that and constructs Beacon
     * @param device: Bluetooth device found by BluetoothAdapter.
     * @param rssi: Current RSSI between devices.
     * @param scanRecord: received Bluetooth advertisement data.
     * @param isEncryped: Whether broadcast data has encrypted.
     * @return Constructed Beacon or null if scanRecord could not have been parsed
     */
    public static beacon beaconFromLeScan(BluetoothDevice device, int rssi, byte scanRecord[], boolean isEncrypted)
    {   	
    	
    	String scanRecordAsHex = Hashcode.fromBytes(scanRecord).toString();
    	//Log.e(device.getName(), scanRecordAsHex);
    	int i = 0;
    	do
    	{
    		if(i >= scanRecord.length)
    			break;
    		int payloadLength = unsignedByteToInt(scanRecord[i]);
    		if(payloadLength == 0 || i + 1 >= scanRecord.length)
    			break;
    		if(unsignedByteToInt(scanRecord[i + 1]) != 255)
    		{
    			i += payloadLength;
    		}

    		else if(payloadLength == 26)
    		{
    			if(/*unsignedByteToInt(scanRecord[i + 2]) == 76 && unsignedByteToInt(scanRecord[i + 3]) == 0 
    					&&*/ unsignedByteToInt(scanRecord[i + 4]) == 2 && unsignedByteToInt(scanRecord[i + 5]) == 21)
    			{
    				String proximityUUID = null;
    				if(!isEncrypted)
    				{
        				proximityUUID = String.format("%s-%s-%s-%s-%s", new Object[] {
        				scanRecordAsHex.substring(18, 26), 
        				scanRecordAsHex.substring(26, 30), 
        				scanRecordAsHex.substring(30, 34), 
        				scanRecordAsHex.substring(34, 38), 
        				scanRecordAsHex.substring(38, 50)
        				});
    				}
    				else
    				{
    					byte[] proximityUUIDAsByte = new byte[16];
        				for(int k = 0; k<16; k++)
        					proximityUUIDAsByte[k] = scanRecord[k+9];
        				
        				String password = "0123456789abcdef";
        				byte[] Decrypt_proximityUUID = AESMath.decrypt(proximityUUIDAsByte, password);
    	
        				String tmp = Hashcode.fromBytes(Decrypt_proximityUUID).toString();
        				
        				proximityUUID = String.format("%s-%s-%s-%s-%s", new Object[] {
        						tmp.substring(0, 8),
        						tmp.substring(8, 12),
        						tmp.substring(12, 16),
        						tmp.substring(16, 20),
        						tmp.substring(20, 32),
        						});
    				}
		
    				//see BluetoothGattCharacteristic.setValue(int value, int formatType, int offset)
    				//mValue is set as little-endian : the first 8bits is lower
    				int major = unsignedByteToInt(scanRecord[i + 22])  + unsignedByteToInt(scanRecord[i + 23])* 256;
    				int minor = unsignedByteToInt(scanRecord[i + 24])  + unsignedByteToInt(scanRecord[i + 25])* 256;
    				int measuredPower = scanRecord[i + 26];
    				return new beacon(proximityUUID, device.getName(), device.getAddress(), major, minor, measuredPower, rssi);
    			} 
    			else
    				return null;
    		} 
    		else
    			return null;
    		i++;
    	} while(true);
    	return null;
    }
    
    /***
     * 
     * @param beacon
     * @param region
     * @return  Returns true if beacon matches the region.
     */
    public static boolean isBeaconInRegion(beacon beacon, Region region)
    {
    	return (region.getProximityUUID() == null || (beacon.getProximityUUID().substring(9, 36)).toLowerCase().equals(region.getProximityUUID().substring(9, 36).toLowerCase()))
    			&& (region.getMajor() == null || beacon.getMajor() == region.getMajor().intValue())
    			&& (region.getMinor() == null || beacon.getMinor() == region.getMinor().intValue());
    }
    
    /***
     * compute the reference for distance measurements
     * the method can be described as two linera formula
     * @param beacon
     * @return distance in meters based on beacon's RSSI and measured power
     */
    public static double computeAccuracy(beacon beacon)
    {
    	if(beacon.getRssi() == 0)
    		return -1D;
    	double ratio = (double)beacon.getRssi() / (double)beacon.getMeasuredPower();
    	double rssiCorrection = 0.95999999999999996D + (Math.pow(Math.abs(beacon.getRssi()), 3D) % 10D) / 150D;
    	if(ratio <= 1.0D)
    		return Math.pow(ratio, 9.9800000000000004D) * rssiCorrection;
    	else
    		return (0.10299999999999999D + 0.89978000000000002D * Math.pow(ratio, 7.71D)) * rssiCorrection;
    }
    
    /***
     * use accuracy to convert to proximity
     * @param accuracy
     * @return enum :near, far, unknow, immediate
     */
    public static Proximity proximityFromAccuracy(double accuracy)
    {
    	if(accuracy < 0.0D)
    		return Proximity.UNKNOWN;
    	if(accuracy < 0.5D)
    		return Proximity.IMMEDIATE;
    	if(accuracy <= 3D)
    		return Proximity.NEAR;
    	else
    		return Proximity.FAR;
    }
    
    public static Proximity computeProximity(beacon beacon)
    {
    	return proximityFromAccuracy(computeAccuracy(beacon));
    }
    
    /***
     * Restarts Bluetooth stack on the device.
     * Never invoke this method without user's explicit consent that Bluetooth on the device is going to be restarted.
     * @param context: context
     * @param listener: Listener to be invoked when Bluetooth stack is restarted
     */
    public static void restartBluetooth(Context context, final RestartCompletedListener listener)
    {
    	BluetoothManager bluetoothManager = (BluetoothManager)context.getSystemService("bluetooth");
    	final BluetoothAdapter adapter = bluetoothManager.getAdapter();
    	IntentFilter intentFilter = new IntentFilter("android.bluetooth.adapter.action.STATE_CHANGED");
    	
    	BroadcastReceiver receiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				// TODO Auto-generated method stub
				if ("android.bluetooth.adapter.action.STATE_CHANGED".equals(intent.getAction())) 
				{
					int state = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", -1);
					if (state == BluetoothAdapter.STATE_OFF)
						adapter.enable();
					else if (state == BluetoothAdapter.STATE_ON) 
					{
						context.unregisterReceiver(this);
						listener.onRestartCompleted();
					}
				}		
			}
    		
    	};
    	
    	context.registerReceiver(receiver, intentFilter);
    	adapter.disable();
    }
    

    private static int unsignedByteToInt(byte value)
    {
    	return value & 0xff;
    }
    
    /***
     * Parses string to integer
     * @param numberAsString
     * @return
     */ 
    public static int parseInt(String numberAsString)
    {
    	try
    	{
    		return Integer.parseInt(numberAsString);
    	}
    	catch(NumberFormatException e)
    	{
    		return 0;
    	}
    }
    
    /***
     * Normalizes integer to be unsigned 16-bit integer from range (1, 65535).
     * @param value
     * @return
     */
    public static int normalize16BitUnsignedInt(int value)
    {
    	return Math.max(1, Math.min(value, 0xffff));
    }
    
    /***
     * Normalizes proximity UUID by making it lowercase and in format XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX.
     * @param proximityUUID
     * @return
     */
    public static String normalizeProximityUUID(String proximityUUID)
    {
    	String withoutDashes = proximityUUID.replace("-", "").toLowerCase(Locale.US);
    	Preconditions.checkArgument(withoutDashes.length() == 32, "Proximity UUID must be 32 characters without dashes");
    	return String.format("%s-%s-%s-%s-%s", new Object[]{
    			withoutDashes.substring(0, 8), withoutDashes.substring(8, 12), withoutDashes.substring(12, 16), withoutDashes.substring(16, 20), withoutDashes.substring(20, 32)
                });
    }
    
    /***
     * transform values to meaning
     */
    //private static HashMap<Integer, String> charPermissions = new HashMap<Integer, String>();
    private static SparseArray<String> charPermissions = new SparseArray<String>();
    static {
    	charPermissions.put(0, "UNKNOW");
    	charPermissions.put(BluetoothGattCharacteristic.PERMISSION_READ, "READ");
    	charPermissions.put(BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED, "READ_ENCRYPTED");
    	charPermissions.put(BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM, "READ_ENCRYPTED_MITM");
    	charPermissions.put(BluetoothGattCharacteristic.PERMISSION_WRITE, "WRITE");
    	charPermissions.put(BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED, "WRITE_ENCRYPTED");
    	charPermissions.put(BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM, "WRITE_ENCRYPTED_MITM");
    	charPermissions.put(BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED, "WRITE_SIGNED");
    	charPermissions.put(BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM, "WRITE_SIGNED_MITM");	
    }
    
    public static String getCharPermission(int permission){
    	return getHashMapValue(charPermissions, permission);
    }
    //-------------------------------------------    
    //private static HashMap<Integer, String> charProperties = new HashMap<Integer, String>();
    private static SparseArray<String> charProperties = new SparseArray<String>();
    static {
    	
    	charProperties.put(BluetoothGattCharacteristic.PROPERTY_BROADCAST, "BROADCAST");
    	charProperties.put(BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS, "EXTENDED_PROPS");
    	charProperties.put(BluetoothGattCharacteristic.PROPERTY_INDICATE, "INDICATE");
    	charProperties.put(BluetoothGattCharacteristic.PROPERTY_NOTIFY, "NOTIFY");
    	charProperties.put(BluetoothGattCharacteristic.PROPERTY_READ, "READ");
    	charProperties.put(BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE, "SIGNED_WRITE");
    	charProperties.put(BluetoothGattCharacteristic.PROPERTY_WRITE, "WRITE");
    	charProperties.put(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, "WRITE_NO_RESPONSE");
    }
    
    public static String getCharPropertie(int property){
    	return getHashMapValue(charProperties,property);
    }
    
    //--------------------------------------------------------------------------
    //private static HashMap<Integer, String> descPermissions = new HashMap<Integer, String>();
    private static SparseArray<String> descPermissions = new SparseArray<String>();
    static {
    	descPermissions.put(0, "UNKNOW");
    	descPermissions.put(BluetoothGattDescriptor.PERMISSION_READ, "READ");
    	descPermissions.put(BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED, "READ_ENCRYPTED");
    	descPermissions.put(BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM, "READ_ENCRYPTED_MITM");
    	descPermissions.put(BluetoothGattDescriptor.PERMISSION_WRITE, "WRITE");
    	descPermissions.put(BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED, "WRITE_ENCRYPTED");
    	descPermissions.put(BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM, "WRITE_ENCRYPTED_MITM");
    	descPermissions.put(BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED, "WRITE_SIGNED");
    	descPermissions.put(BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM, "WRITE_SIGNED_MITM");
    }
    
    public static String getDescPermission(int property){
    	return getHashMapValue(descPermissions,property);
    }
    
    
    private static String getHashMapValue(SparseArray<String> hashMap, int number){
    	String result =hashMap.get(number);
    	if(TextUtils.isEmpty(result)){
    		List<Integer> numbers = getElement(number);
    		result="";
    		for(int i=0;i<numbers.size();i++){
    			result+=hashMap.get(numbers.get(i))+"|";
    		}
    	}
    	return result;
    }
    
    static private List<Integer> getElement(int number){
    	List<Integer> result = new ArrayList<Integer>();
        for (int i = 0; i < 32; i++){
            int b = 1 << i;
            if ((number & b) > 0) 
            	result.add(b);
        }
        
        return result;
    }
    
    
    public static String bytesToHexString(byte[] src){  
        StringBuilder stringBuilder = new StringBuilder("");  
        if (src == null || src.length <= 0) {  
            return null;  
        }  
        for (int i = 0; i < src.length; i++) {  
            int v = src[i] & 0xFF;  
            String hv = Integer.toHexString(v);  
            if (hv.length() < 2) {  
                stringBuilder.append(0);  
            }  
            stringBuilder.append(hv);  
        }  
        return stringBuilder.toString();  
    }
	
	public static byte[] hexStrToBytes(String src) {  
	    int m = 0, n = 0;  
	    int l = src.length() / 2;  
	    System.out.println(l);  
	    byte[] ret = new byte[l];  
	    for (int i = 0; i < l; i++) {  
	        m = i * 2 + 1;  
	        n = m + 1;  
	        ret[i] = uniteBytes(src.substring(i * 2, m), src.substring(m, n));  
	    }  
	    return ret;  
	}
	
	private static byte uniteBytes(String src0, String src1) {  
	    byte b0 = Byte.decode("0x" + src0).byteValue();  
	    b0 = (byte) (b0 << 4);  
	    byte b1 = Byte.decode("0x" + src1).byteValue();  
	    byte ret = (byte) (b0 | b1);  
	    return ret;  
	}  

}
