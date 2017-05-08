package br.usp.ime.compmus.pushloop.util;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Random;

import br.usp.ime.compmus.pushloop.R;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

public class MobileDevice {
	
	private static int SYSTEM_BUILD = defineSystemBuild();
	
	private static boolean NANO_COMPATIBLE = defineNanoCompatibility();
	
	/**
	 * This length is defined in order to limit the size of messages that will include device Id.
	 */
	private static final int MAX_ID_LENGTH = 8;
	
	private static final String DEFAULT_ID = android.os.Build.ID;
	
	/**
	 * This list can be used with the method getResources().getString(ID) at an Activity.
	 * 
	 */
	private static ArrayList<Integer> lastStorageStatus = new ArrayList<Integer>();
	
	/**
	 * There will be only one user per application and this user might have a name.
	 */
	private static String id = DEFAULT_ID;
	
	/**
	 * When the user is interacting online, the other users can choose the same name,
	 * and thats the reason we have random Id to be used on server messages.
	 */
	private static String randomId;
	
	/**
	 * Returns the system build version
	 * @return int version
	 */
	public static int getSystemBuild() {
		
		return SYSTEM_BUILD;
	}
	
	private static boolean defineNanoCompatibility() {
		
		if (SYSTEM_BUILD >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
			return true;
		} else {
			return false;
		}
	}
	
	@SuppressLint("NewApi")
	public static long getTimestamp() {
		
		if (NANO_COMPATIBLE) {
			return SystemClock.elapsedRealtimeNanos();
		} else {
			return SystemClock.elapsedRealtime();
		}
	}
	
	
	/**
	 * The device Id is set considering a length limit.
	 * @param newId as a String
	 * @return true if the Id is not null and has the accepted length.
	 */
	public static boolean setId(String newId) {
		if (newId != null && newId.length() <= MAX_ID_LENGTH) {
			
			id = newId;
			randomId = setRandomId();
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * The device Id is set considering a length limit.
	 * @return true if the Id is not null and has the accepted length.
	 */
	public static void setId() {
		
		id = "id";
		randomId = setRandomId();
	}
	
	public static String getId() {
		if (id != null && id.length() <= MAX_ID_LENGTH) {
			return id;
		} else if (DEFAULT_ID.length() <= MAX_ID_LENGTH) {
				
			id = DEFAULT_ID;
			return id;
		} else { 
			id = DEFAULT_ID.substring(0,MAX_ID_LENGTH);
			return id;
		}
	}
	
	public static String getRandomId() {
		
		if (randomId == null) {
			
			randomId = setRandomId();
		}
		return randomId;
	}
	
	
	private static String setRandomId() {
		
		if (id == null) {
			id = "id";
		}
		Random random = new Random(System.currentTimeMillis());
		int randomNum = random.nextInt(90000000) + 10000000;
		String randomId = id.concat(Integer.toString(randomNum));
		return randomId;
	}

	public static String extractIdFromRandomId(String rand) {
		
		if (rand != null && rand.length() > 8) {
			
			return rand.substring(0, rand.length()-8);
		} else {
			return rand;
		}
		
	}
	
	/**
	 * Verify storage status and add the last status to a list in order to be retrieved afterward.
	 * 
	 * Reference:
	 * http://stackoverflow.com/questions/4580683/writing-text-file-to-sd-card-fails
	 * 
	 * @param mContext
	 * @return boolean Return true if the external storage can be used	
	 * 
	 */
	public static Boolean verifyStorageStatus(Context mContext) {
	    String auxSDCardStatus = Environment.getExternalStorageState();

	    if ( auxSDCardStatus.equals(Environment.MEDIA_MOUNTED) ) {
	    	lastStorageStatus.add(R.string.toast_storage_media_mounted);
	    	return true;
	    } else if ( auxSDCardStatus.equals(Environment.MEDIA_MOUNTED_READ_ONLY) ) {
	        lastStorageStatus.add(R.string.toast_storage_media_mounted_read_only);
	        return true;
	    } else if ( auxSDCardStatus.equals(Environment.MEDIA_NOFS) ) {
	        lastStorageStatus.add(R.string.toast_storage_media_nofs);
	        return false;
	    } else if ( auxSDCardStatus.equals(Environment.MEDIA_REMOVED) ) {
	        lastStorageStatus.add(R.string.toast_storage_media_removed);
	        return false;
	    } else if ( auxSDCardStatus.equals(Environment.MEDIA_SHARED) ) {
	        lastStorageStatus.add(R.string.toast_storage_media_shared);
	        return false;
	    } else if ( auxSDCardStatus.equals(Environment.MEDIA_UNMOUNTABLE) ) {
	        lastStorageStatus.add(R.string.toast_storage_media_unmountable);
	        return false;
	    } else if ( auxSDCardStatus.equals(Environment.MEDIA_UNMOUNTED) ) {
	        lastStorageStatus.add(R.string.toast_storage_media_unmounted);
	        return false;
	    }

	    return true;
	}
	
	/**
	 * Can be used to retrieve the last status from the external storage. You need
	 * to use the method getResources().getString(ID) at an Activity in order to 
	 * show the right status in a Toast, for example.
	 * 
	 * @return ArrayList<Integer> with the IDs regarding the resources with the status messages
	 */
	public static ArrayList<Integer> getLastStorageStatus() {
		
		ArrayList<Integer> tempLastStorageStatus = new ArrayList<Integer>();
		
		if (lastStorageStatus == null) {
			lastStorageStatus = new ArrayList<Integer>();
			return tempLastStorageStatus;
		}
		
		tempLastStorageStatus.addAll(lastStorageStatus);
		lastStorageStatus = new ArrayList<Integer>();
		return tempLastStorageStatus;
	}
	
	
	@SuppressWarnings("deprecation")
	private static int defineSystemBuild() {
		
		try {
			if (Integer.parseInt(android.os.Build.VERSION.SDK) < android.os.Build.VERSION_CODES.DONUT) {
				return Integer.parseInt(android.os.Build.VERSION.SDK);
			} else {
				return android.os.Build.VERSION.SDK_INT;
			}
		} catch (NumberFormatException e) {
			return 0;
		}
	}
	
	/**
	 * Retrieve all information available about the system build and return a string
	 * formated with this information depending on the API.
	 * 
	 * Reference:
	 * https://github.com/andrejb/DspBenchmarking
	 * 
	 * @return String containing all the information about the system build 
	 */
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public static String getBuildInfo() {
		StringBuffer info = new StringBuffer();
		
			info.append("# build: " + SYSTEM_BUILD + "\n");
			info.append("# board: " + android.os.Build.BOARD + "\n");									// API Level 1
		if (SYSTEM_BUILD >= android.os.Build.VERSION_CODES.FROYO)
			info.append("# bootloader: "+android.os.Build.BOOTLOADER+"\n"); 							// API Level 8
			info.append("# brand: " + android.os.Build.BRAND + "\n");									// API Level 1
		if (SYSTEM_BUILD < android.os.Build.VERSION_CODES.LOLLIPOP)
			info.append("# cpu_abi: " + android.os.Build.CPU_ABI + "\n");  								// API Level 4 - 21
		if (SYSTEM_BUILD >= android.os.Build.VERSION_CODES.FROYO && 
				SYSTEM_BUILD < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			info.append("# cpu_abi2: "+android.os.Build.CPU_ABI2+"\n");  								// API Level 8 - 21		
			info.append("# device: " + android.os.Build.DEVICE + "\n");									// API Level 1
		if (SYSTEM_BUILD >= android.os.Build.VERSION_CODES.CUPCAKE)
			info.append("# display: " + android.os.Build.DISPLAY + "\n");  								// API Level 3
			info.append("# fingerprint: " + android.os.Build.FINGERPRINT + "\n"); 						// API Level 1
		if (SYSTEM_BUILD >= android.os.Build.VERSION_CODES.FROYO)
			info.append("# hardware: "+android.os.Build.HARDWARE+"\n");  								// API Level 8
			info.append("# host: " + android.os.Build.HOST + "\n");										// API Level 1
			info.append("# id: " + android.os.Build.ID + "\n");											// API Level 1
		if (SYSTEM_BUILD >= android.os.Build.VERSION_CODES.DONUT)
			info.append("# manufacturer: " + android.os.Build.MANUFACTURER + "\n"); 					// API Level 4
			info.append("# model: " + android.os.Build.MODEL + "\n");									// API Level 1
			info.append("# product: " + android.os.Build.PRODUCT + "\n");								// API Level 1
		if (SYSTEM_BUILD >= android.os.Build.VERSION_CODES.FROYO && 
				SYSTEM_BUILD < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			info.append("# radio: " + android.os.Build.RADIO + "\n");									// API Level 8 - 14
		if (SYSTEM_BUILD >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			info.append("# radio: " + android.os.Build.getRadioVersion() + "\n");						// API Level 14
		if (SYSTEM_BUILD >= android.os.Build.VERSION_CODES.GINGERBREAD)
			info.append("# serial: "+android.os.Build.SERIAL+"\n");  									// API Level 9
		if (SYSTEM_BUILD >= android.os.Build.VERSION_CODES.LOLLIPOP)
			info.append("# supported_32_bit_abis: " + Arrays.toString(android.os.Build.SUPPORTED_32_BIT_ABIS) + "\n");	// API Level 21
		if (SYSTEM_BUILD >= android.os.Build.VERSION_CODES.LOLLIPOP)
			info.append("# supported_64_bit_abis: " + Arrays.toString(android.os.Build.SUPPORTED_64_BIT_ABIS) + "\n");	// API Level 21
		if (SYSTEM_BUILD >= android.os.Build.VERSION_CODES.LOLLIPOP)
			info.append("# supported_abis: " + Arrays.toString(android.os.Build.SUPPORTED_ABIS) + "\n");					// API Level 21
			info.append("# tags: " + android.os.Build.TAGS + "\n");										// API Level 1
			info.append("# time: " + android.os.Build.TIME + "\n");										// API Level 1
			info.append("# type: " + android.os.Build.TYPE + "\n");										// API Level 1
			info.append("# user: " + android.os.Build.USER + "\n");										// API Level 1
		if (SYSTEM_BUILD >= android.os.Build.VERSION_CODES.DONUT)
			info.append("# version codename: " + android.os.Build.VERSION.CODENAME + "\n");				// API Level 4
			info.append("# version incremental: " + android.os.Build.VERSION.INCREMENTAL + "\n");		// API Level 1
			info.append("# version release: " + android.os.Build.VERSION.RELEASE + "\n");				// API Level 1
		if (SYSTEM_BUILD < android.os.Build.VERSION_CODES.DONUT)
			info.append("# version sdk: " + android.os.Build.VERSION.SDK + "\n");						// API Level 1 - 4
		if (SYSTEM_BUILD >= android.os.Build.VERSION_CODES.DONUT)
			info.append("# version sdk_int: " + android.os.Build.VERSION.SDK_INT);						// API Level 4
			
		return info.toString();
	}
	
	
	/**
	 * Load Network Interfaces available on device
	 * @return the name of available interfaces.
	 */
	public static ArrayList<NetworkInterface> getNetworkInterfaces() {
		
		ArrayList<NetworkInterface> arrayInterfaces;
		try {
			Enumeration<NetworkInterface> enumerationInterfaces = NetworkInterface.getNetworkInterfaces();
			arrayInterfaces = Collections.list(enumerationInterfaces);
			return arrayInterfaces;
		} catch (SocketException e1) {
			e1.printStackTrace();
			arrayInterfaces = null;
			return arrayInterfaces;
		}
	}

	public static boolean isStoragePermissionGranted(android.app.Activity mActivity) {

		if (android.os.Build.VERSION.SDK_INT >= 23) {

			ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

			if (ContextCompat.checkSelfPermission(mActivity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
					== PackageManager.PERMISSION_GRANTED) {
				return true;
			} else {
				ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
			}
		}
		return false;
	}
}
