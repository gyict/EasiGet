package com.easimote.sdk.connect;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for
 * demonstration purposes.
 */
public class SampleGattAttributes {
	private static HashMap<String, String> attributes = new HashMap<String, String>();

	static {
		// Sample Services.
		attributes.put("0000180d-0000-1000-8000-00805f9b34fb","Heart Rate Service");

		// Basic Info Services.
		attributes.put("00001800-0000-1000-8000-00805f9b34fb","Basic Info service");
		attributes.put("00002a00-0000-1000-8000-00805f9b34fb", "Device Name");

		//
		
		
		attributes.put("00001801-0000-1000-8000-00805f9b34fb", "Generic Attribute");
		attributes.put("00002a01-0000-1000-8000-00805f9b34fb", "Appearance");
		attributes.put("00002a02-0000-1000-8000-00805f9b34fb", "Peripheral Privacy Flag");
		attributes.put("00002a03-0000-1000-8000-00805f9b34fb", "Appearance");
		attributes.put("00002a04-0000-1000-8000-00805f9b34fb", "Peripheral Preferred Connection Parameters");
		//attributes.put("00002a05-0000-1000-8000-00805f9b34fb", "");
		attributes.put("00002a06-0000-1000-8000-00805f9b34fb", "Alert Level");
		
		
		//
		attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
		attributes.put("00002a23-0000-1000-8000-00805f9b34fb", "System ID");
		attributes.put("00002a24-0000-1000-8000-00805f9b34fb", "Model Number");
		attributes.put("00002a25-0000-1000-8000-00805f9b34fb", "Serial Number");
		attributes.put("00002a26-0000-1000-8000-00805f9b34fb", "Firmware Rev");
		attributes.put("00002a27-0000-1000-8000-00805f9b34fb", "Hardware Rev");
		attributes.put("00002a28-0000-1000-8000-00805f9b34fb", "Software Rev");
		attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name");
		attributes.put("00002a2A-0000-1000-8000-00805f9b34fb", "Regulatory Certification Data List");
		attributes.put("00002a50-0000-1000-8000-00805f9b34fb", "PnP ID");

		// battery
		attributes.put("0000180f-0000-1000-8000-00805f9b34fb", "Battery Service");
		attributes.put("00002a19-0000-1000-8000-00805f9b34fb", "Battery Level");

		// easinet define services and characteristics
		attributes.put("0000ffe0-0000-1000-8000-00805f9b34fb", "Easimote Service");
		attributes.put("0000ffe1-0000-1000-8000-00805f9b34fb", "Transmit Power");
		attributes.put("0000ffe2-0000-1000-8000-00805f9b34fb", "Battery Level");
		attributes.put("0000ffe3-0000-1000-8000-00805f9b34fb", "Advertising Interval");
		attributes.put("0000ffe4-0000-1000-8000-00805f9b34fb", "EASI_IB1");
		attributes.put("0000ffe5-0000-1000-8000-00805f9b34fb", "EASI_IB2");
		attributes.put("0000ffe6-0000-1000-8000-00805f9b34fb", "EASI_IB3");
		attributes.put("0000ffe7-0000-1000-8000-00805f9b34fb", "EASI_IB4");
		attributes.put("0000ffe8-0000-1000-8000-00805f9b34fb", "Major");
		attributes.put("0000ffe9-0000-1000-8000-00805f9b34fb", "Minor");
		attributes.put("0000ffea-0000-1000-8000-00805f9b34fb", "Measured Power");
		attributes.put("0000ffeb-0000-1000-8000-00805f9b34fb", "Firmware version");
		attributes.put("0000ffec-0000-1000-8000-00805f9b34fb", "AES Enable");
		attributes.put("0000ffed-0000-1000-8000-00805f9b34fb", "Unknown");
		attributes.put("0000ffee-0000-1000-8000-00805f9b34fb", "Deploy The Device");
		

		// Sample Characteristics.
		attributes.put("00002a37-0000-1000-8000-00805f9b34fb", "Heart Rate Measurement");
		attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");

	}

	public static String lookup(String uuid, String defaultName) {
		String name = attributes.get(uuid);
		return name == null ? defaultName : name;
	}
}
