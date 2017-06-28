package com.easimote.sdk.service;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.*;

import com.easimote.sdk.Preconditions;
import com.easimote.sdk.Region;
import com.easimote.sdk.beacon;
import com.easimote.sdk.Region.State;
import com.easimote.sdk.basicObjType.Objects;

/**
 * Data object for results from ranging.
 * @author pk
 *
 */
public class MonitoringResult implements Parcelable {
	public final Region region;
	public final Region.State state;
	public final List<beacon> beacons;//Collection of beacons that triggered ENTER state

	public MonitoringResult(Region region, Region.State state, Collection<beacon> beacons) {
		this.region = (Region) Preconditions.checkNotNull(region, "region cannot be null");
		this.state = (Region.State) Preconditions.checkNotNull(state, "state cannot be null");
		this.beacons = new ArrayList<beacon>(beacons);
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		MonitoringResult that = (MonitoringResult) o;
		if (state != that.state)
			return false;
		return region == null ? that.region == null : region.equals(that.region);
	}

	public int hashCode() {
		int result = region == null ? 0 : region.hashCode();
		result = 31 * result + (state == null ? 0 : state.hashCode());
		return result;
	}

	public String toString() {
		return Objects.toStringHelper(this).add("region", region)
				.add("state", state.name()).add("beacons", beacons).toString();
	}

	/**
	 * describeContents in interface Parcelable
	 */
	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(region, flags);
		dest.writeInt(state.ordinal());
		dest.writeList(beacons);
	}

	
	public static final Parcelable.Creator<MonitoringResult> CREATOR = new Parcelable.Creator<MonitoringResult>() {

		public MonitoringResult createFromParcel(Parcel source) {
			ClassLoader classLoader = getClass().getClassLoader();
			Region region = (Region) source.readParcelable(classLoader);
			Region.State event = Region.State.values()[source.readInt()];
			List<beacon> beacons = new ArrayList<beacon>();
			source.readList(beacons, classLoader);
			return new MonitoringResult(region, event, beacons);
		}

		public MonitoringResult[] newArray(int size) {
			return new MonitoringResult[size];
		}

		/*public volatile Object[] newArray(int x0) {
			return newArray(x0);
		}

		public volatile Object createFromParcel(Parcel x0) {
			return createFromParcel(x0);
		}*/

	};

}
