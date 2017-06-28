package com.easimote.sdk.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.easimote.sdk.BlePeripheral;
import com.easimote.sdk.Preconditions;

import android.os.Parcel;
import android.os.Parcelable;

public class BlePeripheralRangingResult implements Parcelable{
	
	public final List<BlePeripheral> blePeripheral;
	
	@SuppressWarnings("unchecked")
	public BlePeripheralRangingResult(Collection<BlePeripheral> mBlePeripheral) {
		this.blePeripheral = Collections.unmodifiableList(
				new ArrayList<BlePeripheral>((List<BlePeripheral>) Preconditions.checkNotNull(mBlePeripheral, "BlePeripheral cannot be null")));
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		dest.writeList(blePeripheral);
		
	}
	
	public static final Parcelable.Creator<BlePeripheralRangingResult> CREATOR = new Parcelable.Creator<BlePeripheralRangingResult>() {

		public BlePeripheralRangingResult createFromParcel(Parcel source) {
			ClassLoader classLoader = getClass().getClassLoader();
			List<BlePeripheral> blePeripheral = new ArrayList<BlePeripheral>();
			source.readList(blePeripheral, classLoader);
			return new BlePeripheralRangingResult(blePeripheral);
		}

		public BlePeripheralRangingResult[] newArray(int size) {
			return new BlePeripheralRangingResult[size];
		}

	};

}
