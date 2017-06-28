package com.easimote.sdk.service;

import com.easimote.sdk.basicObjType.Objects;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Data object containing values for setting scan periods.
 * @author pk
 *
 */
public final class ScanPeriodData implements Parcelable {

	/**
	 * How long we perform low energy scanning and restarting the scan.
	 */
	public final long scanPeriodMillis;

	/**
	 * How long we wait before starting a new low energy scan.
	 */
	public final long waitTimeMillis;

	/**
	 * constructor
	 * @param scanPeriodMillis
	 * @param waitTimeMillis
	 */
	public ScanPeriodData(long scanPeriodMillis, long waitTimeMillis) {
		this.scanPeriodMillis = scanPeriodMillis;
		this.waitTimeMillis = waitTimeMillis;
	}

	@Override
	public boolean equals(Object mObject) {
		if (this == mObject)
			return true;
		if (mObject == null || getClass() != mObject.getClass())
			return false;
		ScanPeriodData that = (ScanPeriodData) mObject;
		if (scanPeriodMillis != that.scanPeriodMillis)
			return false;
		return waitTimeMillis == that.waitTimeMillis;
	}

	@Override
	public int hashCode() {
		int result = (int) (scanPeriodMillis ^ scanPeriodMillis >>> 32);
		result = 31 * result + (int) (waitTimeMillis ^ waitTimeMillis >>> 32);
		return result;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("scanPeriodMillis", scanPeriodMillis)
				.add("waitTimeMillis", waitTimeMillis).toString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(scanPeriodMillis);
		dest.writeLong(waitTimeMillis);
	}

	
	public static final Parcelable.Creator<ScanPeriodData> CREATOR = new Parcelable.Creator<ScanPeriodData>() {

		public ScanPeriodData createFromParcel(Parcel source) {
			long scanPeriodMillis = source.readLong();
			long waitTimeMillis = source.readLong();
			return new ScanPeriodData(scanPeriodMillis, waitTimeMillis);
		}

		public ScanPeriodData[] newArray(int size) {
			return new ScanPeriodData[size];
		}


	};

}
