package com.easimote.sdk;
import com.easimote.sdk.basicObjType.Objects;
import com.easimote.sdk.util.utils;

import android.os.Parcel;
import android.os.Parcelable;

/***
 * Immutable representations of single beacon.
 * Two beacons are considered equal if their last 12 bytes of proximity UUID, major and minor are equal.
 * @author pk
 *
 */
public class beacon extends Object implements Parcelable{
	//beacon attributes
	private String proximityUUID;
    private String name;
    private String macAddress;
    private int major;
    private int minor;
    private int measuredPower;
    private int rssi;
    
	/***
	 * constructor, a beacon has parameters: proximity UUID, name, mac, major, minor, mPower and rssi
	 * @param proximityUUID
	 * @param name
	 * @param macAddress
	 * @param major
	 * @param minor
	 * @param measuredPower
	 * @param rssi
	 */
	public beacon(String proximityUUID, String name, String macAddress, int major, int minor, int measuredPower, int rssi)
    {
		this.proximityUUID = utils.normalizeProximityUUID(proximityUUID);
		this.name = name;
		this.macAddress = macAddress;
		this.major = major;
		this.minor = minor;
		this.measuredPower = measuredPower;
		this.rssi = rssi;
    }
	
	private beacon(Parcel parcel)
    {
    	proximityUUID = parcel.readString();
    	name = parcel.readString();
    	macAddress = parcel.readString();
    	major = parcel.readInt();
    	minor = parcel.readInt();
    	measuredPower = parcel.readInt();
    	rssi = parcel.readInt();
    }
	
	/***
	 * 
	 * @return Proximity UUID of the beacon in format XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX (all lower case).
	 */
	public String getProximityUUID()
    {
		return proximityUUID;
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

    public int getMajor()
    {
    	return major;
    }

    public int getMinor()
    {
    	return minor;
    }

    /***
     * 
     * @return Measured power of the beacon (in dBm).
     */
    public int getMeasuredPower()
    {
    	return measuredPower;
    }

    /***
     * 
     * @return Received Signal Strength Indication at the moment of scanning.
     */
    public int getRssi()
    {
    	return rssi;
    }

    
    
    @Override
    public String toString()
    {
    	return Objects.toStringHelper(this).add("macAddress", macAddress).
    			add("proximityUUID", proximityUUID).add("major", major).
    			add("minor", minor).add("measuredPower", measuredPower).
    			add("rssi", rssi).toString();
    }

    /**
     * we consider two beacons are same if they have the same major, minor and last 12 bytes proximity UUID.
     */
    @Override
    public boolean equals(Object another)
    {
    	if(this == another)
    		return true;
    	if(another == null || getClass() != another.getClass())
    		return false;
    	
    	beacon beacon = (beacon)another;
    	if(major != beacon.major)
    		return false;
    	if(minor != beacon.minor)
    		return false;
    	else
    		return (proximityUUID.substring(9, 36)).equals(beacon.proximityUUID.substring(9, 36));
    }

    @Override
    public int hashCode()
    {
    	int result = (proximityUUID.substring(9, 36)).hashCode();
    	result = 31 * result + major;
    	result = 31 * result + minor;
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
		dest.writeString(proximityUUID);
		dest.writeString(name);
		dest.writeString(macAddress);
		dest.writeInt(major);
		dest.writeInt(minor);
		dest.writeInt(measuredPower);
		dest.writeInt(rssi);
	}
	
	public static final Creator<beacon> CREATOR = new Creator<beacon>() {
		public beacon createFromParcel(Parcel source) {
			return new beacon(source);
		}

		public beacon[] newArray(int size) {
			return new beacon[size];
		}
	};

}
