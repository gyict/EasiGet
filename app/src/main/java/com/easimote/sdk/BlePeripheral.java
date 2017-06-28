package com.easimote.sdk;

import com.easimote.sdk.basicObjType.Objects;

import android.os.Parcel;
import android.os.Parcelable;

/***
 * some other beacon/ble devices.
 * @author pk
 *
 */
public class BlePeripheral extends Object implements Parcelable{
	//BlePeripheral attributes
    private String name;
    private String macAddress;
    private int type;
    private int rssi;
    private byte[] broadcastMsg;
    
	//constructors
	public BlePeripheral(String name, String macAddress, int type, int rssi, byte[] broadcastMsg)
    {
		this.name = name;
		this.macAddress = macAddress;
		this.type = type;
		this.rssi = rssi;
		this.broadcastMsg = broadcastMsg;
    }
	
	private BlePeripheral(Parcel parcel)
    {
    	name = parcel.readString();
    	macAddress = parcel.readString();
    	type = parcel.readInt();
    	rssi = parcel.readInt();
    	broadcastMsg = parcel.createByteArray();
    }

	/***
	 * 
	 * @return Device friendly name (this name is advertised by the beacon).
	 */
    public String getName()
    {
    	return name;
    }

    /***
     * 
     * @return MAC address of the beacon.
     */
    public String getMacAddress()
    {
    	return macAddress;
    }

    public int getType()
    {
    	return type;
    }

    /***
     * 
     * @return Received Signal Strength Indication at the moment of scanning.
     */
    public int getRssi()
    {
    	return rssi;
    }

    /**
     * 
     * @return broadcast payload(message)
     */
    public byte[] getBroadcastMsg()
    {
    	return broadcastMsg;
    }
    
    //basic functions
    @Override
    public String toString()
    {
		return Objects.toStringHelper(this).add("name", name).add("macAddress", macAddress).add("type", type)
				.add("rssi", rssi).toString();
    }

    @Override
    public boolean equals(Object o)
    {
    	if(this == o)
    		return true;
    	if(o == null || getClass() != o.getClass())
    		return false;
    	BlePeripheral mBlePeripheral = (BlePeripheral)o;
    	
    	return macAddress.equalsIgnoreCase(mBlePeripheral.macAddress);
    }

    @Override
    public int hashCode()
    {
    	int result = macAddress.hashCode();
    	return result;
    }

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		dest.writeString(name);
		dest.writeString(macAddress);
		dest.writeInt(type);
		dest.writeInt(rssi);
		dest.writeByteArray(broadcastMsg, 0, broadcastMsg.length);
	}
	
	public static final Creator<BlePeripheral> CREATOR = new Creator<BlePeripheral>() {
		public BlePeripheral createFromParcel(Parcel source) {
			return new BlePeripheral(source);
		}

		public BlePeripheral[] newArray(int size) {
			return new BlePeripheral[size];
		}
	};

}

