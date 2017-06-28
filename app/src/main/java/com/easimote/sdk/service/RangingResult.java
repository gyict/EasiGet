package com.easimote.sdk.service;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.*;

import com.easimote.sdk.Preconditions;
import com.easimote.sdk.Region;
import com.easimote.sdk.beacon;
import com.easimote.sdk.basicObjType.Objects;

/**
 * Data object for results from ranging. 
 * Consists of range for which scanning was made and collection of found beacons in the range. 
 * Collection of beacons is sorted by its accuracy.
 * @author pk
 *
 */
public final class RangingResult implements Parcelable {

	public final Region region;//Region for which these results are for.
	public final List<beacon> beacons;//Collection of ranged beacons.
	
	@SuppressWarnings("unchecked")
	public RangingResult(Region region, Collection<beacon> collection) {
		this.region = (Region) Preconditions.checkNotNull(region, "region cannot be null");
		this.beacons = Collections.unmodifiableList(new ArrayList<beacon>((Collection<beacon>) Preconditions.checkNotNull(collection, "beacons cannot be null")));
	}

	@Override
	public boolean equals(Object mObject) {
		if (this == mObject)
			return true;
		if (mObject == null || getClass() != mObject.getClass())
			return false;
		RangingResult that = (RangingResult) mObject;
		if (!beacons.equals(that.beacons))
			return false;
		return region.equals(that.region);
	}

	@Override
	public int hashCode() {
		int result = region.hashCode();
		result = 31 * result + beacons.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this).add("region", region).add("beacons", beacons).toString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(region, flags);
		dest.writeList(beacons);
	}

	
	public static final Parcelable.Creator<RangingResult> CREATOR = new Parcelable.Creator<RangingResult>() {

		public RangingResult createFromParcel(Parcel source) {
			ClassLoader classLoader = getClass().getClassLoader();
			Region region = (Region) source.readParcelable(classLoader);
			List<beacon> beacons = new ArrayList<beacon>();
			source.readList(beacons, classLoader);
			return new RangingResult(region, beacons);
		}

		public RangingResult[] newArray(int size) {
			return new RangingResult[size];
		}

	};

}
