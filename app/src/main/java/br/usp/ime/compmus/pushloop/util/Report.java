package br.usp.ime.compmus.pushloop.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import br.usp.ime.compmus.pushloop.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.os.Environment;
import android.os.SystemClock;

@SuppressLint("NewApi")
// elapsedRealTimeNanos() was added in API level 17
// Reference: http://developer.android.com/reference/android/os/SystemClock.html

public class Report {

	private File reportFolder;

	private File storageFolder = Environment.getExternalStorageDirectory();
	//	private File storageFolder = Environment.getDataDirectory();
//	private File storageFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

	private String reportFolderName = storageFolder.getAbsolutePath()+"/Reports";
	private String id;
	private File report;
	private FileWriter reportWriter;
	
	private boolean isStarted = false;
	private boolean isClosed = true;
	
	
	private ArrayList<Integer> reportErrors;
	
	private SimpleDateFormat dateFormater = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.ROOT);
	
	public Report() {
		
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
			id = dateFormater.format(SystemClock.elapsedRealtimeNanos());
		} else {
			id = dateFormater.format(SystemClock.elapsedRealtime());
		}
		id = id.concat(".txt");
		reportErrors = new ArrayList<Integer>();
	}
	
	private synchronized void setReportError(int errorId) {
		reportErrors.add(errorId);
	}
	
	public synchronized ArrayList<Integer> getReportErrors() {
		ArrayList<Integer> copyOfReportErrors = new ArrayList<Integer>();

		if (reportErrors == null) {
			
			reportErrors = new ArrayList<Integer>();
			return copyOfReportErrors;
		} else {
			
			copyOfReportErrors.addAll(reportErrors);
			reportErrors = new ArrayList<Integer>();
			return copyOfReportErrors;			
		}
	}
	
	public boolean isStarted() {
		
		return isStarted;
	}
	
	public boolean isClosed() {
		
		return isClosed;
	}
	
	public boolean init(Context context) {
				
		if (isStarted) {
			
			setReportError(R.string.toast_report_is_started_error);
			return false;
		}
		
		if (!createReportFile(context)) {
		
			return false;
		}
		
		isClosed = false;

		try {			
			addCommentToReport(context.getResources().getString(R.string.report_title));
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
		addCommentToReport("Mobile device build info:\n" + MobileDevice.getBuildInfo());
				
		return true;			
	}
	
	public boolean start(Context context) {
		
		if (isClosed) {
			
			setReportError(R.string.toast_report_is_closed);
			return false;
		} 
	
		if (isStarted) {
			
			setReportError(R.string.toast_report_is_started_error);
			return false;
		} else {
			isStarted = true;
			addCommentToReport(context.getResources().getString(R.string.report_start_message));
			return true;				
		}
	}
	
	public boolean finish(Context context) {
		
		if (isClosed) {
			
			setReportError(R.string.toast_report_is_closed);
			return false;
		} 

		if(!isStarted) {
		
			setReportError(R.string.toast_report_is_not_started);
			return false;
		} else {
			isStarted = false;
			addCommentToReport(context.getResources().getString(R.string.report_finished_successfully));
			return true;
		}
	}
	
	public boolean close(Context context) {
		
		
		if(isStarted) {
			
			finish(context);
		}
		isClosed = true;
		if (reportWriter != null) {
			
			try {
				reportWriter.flush();
				reportWriter.close();
			} catch (IOException e) {
				setReportError(R.string.toast_report_writer_close_error);
				e.printStackTrace();
				return false;
			} catch (NullPointerException e) {
				e.printStackTrace();
				return false;
			}
		}
		
		return true;
	}
	
	
	private boolean createReportFile(Context context) {
		
		if (!MobileDevice.verifyStorageStatus(context)) {
			
			setReportError(R.string.toast_report_storage_error);
			return false;
		}
		reportFolder = new File(reportFolderName);
		boolean reportCreated = reportFolder.mkdirs();

		// verify if the folder was created and really exists!
		if (!reportCreated && !reportFolder.exists()) {
			setReportError(R.string.toast_report_folder_error);
			return false;
		}		

		report = new File(reportFolder.getAbsolutePath()+"/"+id);

		try {
			reportWriter = new FileWriter(report);
		} catch (IOException e) {
			setReportError(R.string.toast_report_writer_error);
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * Add message to report with the most precise time stamp
	 * @param report
	 * @return true if added
	 */
	public boolean addToReport(String report) {
		
		return addToReport(MobileDevice.getTimestamp(), report);
	}
	
	/**
	 * Add message to report using a time stamp passed as argument
	 * @param timestamp
	 * @param report
	 * @return true if added
	 */
	public boolean addToReport(long timestamp, String report) {
		
		return addToReport(timestamp, report, false);
	}
	
	/**
	 * Add a comment to report
	 * @param report
	 * @return true if added
	 */
	public boolean addCommentToReport(String report) {
		
		return addToReport(MobileDevice.getTimestamp(), report, true);			
	}
	
	private synchronized boolean addToReport(long timestamp, String report, boolean isComment) {
		
		StringBuilder reportBuilder = new StringBuilder();
		
		if (isClosed) {
			
			return false;
		}
		
		if (isComment) {
			
			reportBuilder.append("# ");
		}
		
		try {
//			reportBuilder.append(dateFormater.format(timestamp));
			reportBuilder.append(timestamp);
		} catch (IllegalArgumentException e) {
			reportBuilder.append(timestamp);
		}
		reportBuilder.append(" ");
		reportBuilder.append(report);
		reportBuilder.append("\n");
		
		try {
			reportWriter.write(reportBuilder.toString());
			reportWriter.flush();
		} catch (IOException e) {
			setReportError(R.string.toast_report_writing_io_error);
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

//	public  boolean isStoragePermissionGranted() {
//		if (Build.VERSION.SDK_INT >= 23) {
//			if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
//					== PackageManager.PERMISSION_GRANTED) {
////				Log.v(TAG,"Permission is granted");
//				return true;
//			} else {
//
////				Log.v(TAG,"Permission is revoked");
//				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
//				return false;
//			}
//		}
//		else { //permission is automatically granted on sdk<23 upon installation
////			Log.v(TAG,"Permission is granted");
//			return true;
//		}
//	}
}
