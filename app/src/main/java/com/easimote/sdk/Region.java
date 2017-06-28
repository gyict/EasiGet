package com.easimote.sdk;

import com.easimote.sdk.basicObjType.Objects;
import com.easimote.sdk.util.utils;

import android.os.Parcel;
import android.os.Parcelable;

/***
 * Defines a region based on device's proximity to a beacon. 
 * Region is defined by proximity UUID, major and minor numbers of targeting beacons. 
 * All of those can be nulls which means wildcard on that field.
 * @author pk
 *
 */
public class Region extends Object implements Parcelable {
	
	private final String identifier;
	private final String proximityUUID;
	private final Integer major;
	private final Integer minor;
	
	//Describes current location relationship with region bounds
	public enum State
    {
    	INSIDE(0),
        OUTSIDE(1)
        {
    		@Override
    		public boolean isRest(){
    			return true;
    		}
    	};
    	private int value;
   	 
	    private State(int value) {
	        this.value = value;
	    }
	 
	    public int getValue() {
	        return value;
	    }
	 
	    public boolean isRest() {
	        return false;
	    }
    }
	
	//constructors
	private Region(Parcel parcel) {
		identifier = parcel.readString();
		proximityUUID = parcel.readString();
		Integer majorTemp = Integer.valueOf(parcel.readInt());
		if (majorTemp.intValue() == -1)
			majorTemp = null;
		major = majorTemp;
		Integer minorTemp = Integer.valueOf(parcel.readInt());
		if (minorTemp.intValue() == -1)
			minorTemp = null;
		minor = minorTemp;
	}
	
	/***
	 * 
	 * @param identifier:  A unique identifier for a region. Cannot be null.
	 * @param proximityUUID: Proximity UUID of beacons. Can be null. Null indicates all proximity UUIDs.
	 * @param major: Major version of the beacons. Can be null. Null indicates all major versions. 
	 * @param minor: Minor version of the beacons. Can be null. Null indicates all minor versions.
	 */
	public Region(String identifier, String proximityUUID, Integer major, Integer minor) {
		this.identifier = (String) Preconditions.checkNotNull(identifier);
		this.proximityUUID = proximityUUID == null ? proximityUUID : utils.normalizeProximityUUID(proximityUUID);
		this.major = major;
		this.minor = minor;
	}

	/***
	 * 
	 * @return Region's unique identifier. Cannot be null.
	 */
	public String getIdentifier() {
		return identifier;
	}

	/***
	 * 
	 * @return Proximity UUID of targeting beacons.
	 */
	public String getProximityUUID() {
		return proximityUUID;
	}

	/***
	 * 
	 * @return Major version of targeting beacons.
	 */
	public Integer getMajor() {
		return major;
	}

	/***
	 * 
	 * @return Minor version of targeting beacons.
	 */
	public Integer getMinor() {
		return minor;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this).add("identifier", identifier)
				.add("proximityUUID", proximityUUID).add("major", major)
				.add("minor", minor).toString();
	}

	/**
	 * We consider two regions are same if they have the same major, minor and last 12 bytes of Proximity UUID
	 * If one corresponding parameter is null, we consider them equal.  
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Region region = (Region) o;
		if (major == null ? region.major != null : !major.equals(region.major))
			return false;
		if (minor == null ? region.minor != null : !minor.equals(region.minor))
			return false;
		return proximityUUID == null ? region.proximityUUID == null
				: (proximityUUID.substring(9, 36)).equals(region.proximityUUID.substring(9, 36));
	}

	@Override
	public int hashCode() {
		int result = proximityUUID == null ? 0 : (proximityUUID.substring(9, 36)).hashCode();
		result = 31 * result + (major == null ? 0 : major.hashCode());
		result = 31 * result + (minor == null ? 0 : minor.hashCode());
		return result;
	}	

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(identifier);
		dest.writeString(proximityUUID);
		dest.writeInt(major != null ? major.intValue() : -1);
		dest.writeInt(minor != null ? minor.intValue() : -1);
	}

	//creator interface
	public static final Parcelable.Creator<Region> CREATOR = new Creator<Region>() {
		public Region createFromParcel(Parcel source) {
			return new Region(source);
		}
		public Region[] newArray(int size) {
			return new Region[size];
		}

	};

}
