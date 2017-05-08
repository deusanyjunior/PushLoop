package br.usp.ime.compmus.pushloop;

import java.lang.ref.WeakReference;
import java.util.LinkedList;

import br.usp.ime.compmus.pushloop.connections.ConnectionInterface;
import br.usp.ime.compmus.pushloop.connections.Multicast;
import br.usp.ime.compmus.pushloop.connections.Packet;
import br.usp.ime.compmus.pushloop.connections.PubnubService;
import br.usp.ime.compmus.pushloop.connections.PusherService;
import br.usp.ime.compmus.pushloop.connections.UnicastIPv6;
import br.usp.ime.compmus.pushloop.connections.UnicastIPv4;
import br.usp.ime.compmus.pushloop.util.MobileDevice;
import br.usp.ime.compmus.pushloop.util.Report;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

public class PushActivity extends Activity {

	UnicastIPv4 unicastIPv4;
	UnicastIPv6 unicastIPv6;
	Multicast multicast;
	PusherService pusherFree;
	PusherService pusherPaid;
	PubnubService pubnubFree;
	PubnubService pubnubPaid;
	ConnectionInterface connectionOptions[];
	
	LinkedList<ConnectionInterface> connections;
	
	int initialPackets;
	int finalPackets;
	boolean geometricProgressionPackets;
	int progressionValuePackets;
	
	int initialDelay;
	int finalDelay;
	boolean geometricProgressionDelay;
	int progressionValueDelay;
	
	int initialExtraFloats;
	int finalExtraFloats;
	boolean geometricProgressionExtraFloats;
	int progressionValueExtraFloats;
	
	int repetitions;
	int standByTime;
	
	final Report report = new Report();
	final Messenger uiMessenger = new Messenger(new UiHandler(this));
	
	AsyncTaskPush asyncTaskTestConnections;
	
	private boolean created = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_push);

		if (!MobileDevice.isStoragePermissionGranted(this))
		{
			this.onDestroy();
		}

		if (!created) {
			
			this.loadSettings();
			this.report.init(this);
			this.created = true;
		}
	}
	
	@Override
	public void onBackPressed() {
		
		if (this.asyncTaskTestConnections != null) {
    		this.asyncTaskTestConnections.cancel(true);
    	}
		this.report.close(this);
		if (this.connections != null) {
		
			for (ConnectionInterface connection: connections) {
				
				connection.disconnect();
			}
		}
		this.created = false;
		finish();
		super.onBackPressed();
	}
	
	private void defineConnections() {

		this.unicastIPv4 = new UnicastIPv4(report, uiMessenger);
		this.unicastIPv6 = new UnicastIPv6(report, uiMessenger);
		this.multicast = new Multicast(report, uiMessenger);
		this.pusherFree = new PusherService(report, uiMessenger, false);
		this.pusherPaid = new PusherService(report, uiMessenger, true);
		this.pubnubFree = new PubnubService(report, uiMessenger, false);
		this.pubnubPaid = new PubnubService(report, uiMessenger, true);
		
		this.connectionOptions = new ConnectionInterface[]{
				this.unicastIPv4,
				this.unicastIPv6,
				this.multicast,
				this.pusherFree,
				this.pusherPaid,
				this.pubnubFree,
				this.pubnubPaid};
	}
	
	private void loadSettings() {
		
		
		SharedPreferences preferences = 
				PreferenceManager.getDefaultSharedPreferences(this);
		
		this.defineConnections();
		this.connections = new LinkedList<ConnectionInterface>();
		for (ConnectionInterface connection: connectionOptions) {
			
			if (preferences.getBoolean(connection.getSettingName(), false)) {
				connection.loadSettings(this);
				this.connections.add(connection);
			}
		}
				
		this.initialPackets = Integer.parseInt(
				preferences.getString("pref_initialPackets", 
						this.getResources().getString(R.string.pref_initialPacketsDefault)));
		this.finalPackets = Integer.parseInt(
				preferences.getString("pref_finalPackets", 
						this.getResources().getString(R.string.pref_finalPacketsDefault)));
		this.geometricProgressionPackets = 
				preferences.getBoolean("pref_geometricProgressionPackets", 
						this.getResources().getBoolean(R.bool.pref_geometricProgressionPacketsDefault));
		this.progressionValuePackets = Integer.parseInt(
				preferences.getString("pref_progressionValuePackets", 
						this.getResources().getString(R.string.pref_progressionValuePacketsDefault)));
		
		this.initialDelay = Integer.parseInt(
				preferences.getString("pref_initialDelay", 
						this.getResources().getString(R.string.pref_initialDelayDefault)));
		this.finalDelay = Integer.parseInt(
				preferences.getString("pref_finalDelay", 
						this.getResources().getString(R.string.pref_finalDelayDefault)));
		this.geometricProgressionDelay = 
				preferences.getBoolean("pref_geometricProgressionDelay", 
						this.getResources().getBoolean(R.bool.pref_geometricProgressionDelayDefault));
		this.progressionValueDelay = Integer.parseInt(
				preferences.getString("pref_progressionValueDelay", 
						this.getResources().getString(R.string.pref_progressionValueDelayDefault)));
		
		this.initialExtraFloats = Integer.parseInt(
				preferences.getString("pref_initialExtraFloats", 
						this.getResources().getString(R.string.pref_initialExtraFloatsDefault)));
		this.finalExtraFloats = Integer.parseInt(
				preferences.getString("pref_finalExtraFloats", 
						this.getResources().getString(R.string.pref_finalExtraFloatsDefault)));
		this.geometricProgressionExtraFloats = 
				preferences.getBoolean("pref_geometricProgressionExtraFloats", 
						this.getResources().getBoolean(R.bool.pref_geometricProgressionExtraFloatsDefault));
		this.progressionValueExtraFloats = Integer.parseInt(
				preferences.getString("pref_progressionValueExtraFloats", 
						this.getResources().getString(R.string.pref_progressionValueExtraFloatsDefault)));
		
		this.repetitions = Integer.parseInt(
				preferences.getString("pref_repetitions", 
						this.getResources().getString(R.string.pref_repeatitionsDefault)));
		this.standByTime = Integer.parseInt(
				preferences.getString("pref_standByTime", 
						this.getResources().getString(R.string.pref_standByTimeDefault)));
	}
	
	public void onToggleClicked(View view) {
	    // Is the toggle on?
	    boolean on = ((ToggleButton) view).isChecked();
	    
	    if (on) {
	    	// Start tests
	    	report.start(this);
	    	if (this.asyncTaskTestConnections == null) {
	    		
	    		this.asyncTaskTestConnections = new AsyncTaskPush();
	    	} else {
	    		this.asyncTaskTestConnections.cancel(true);
	    		this.asyncTaskTestConnections = new AsyncTaskPush();
	    	}
			this.asyncTaskTestConnections.execute(
					connections.toArray(new ConnectionInterface[connections.size()]));
	    } else {
	        // Stop tests
	    	report.finish(this);
	    	if (this.asyncTaskTestConnections != null) {
	    		this.asyncTaskTestConnections.cancel(true);
	    	}
	    	if (connections != null) {
	    		
	    		for (ConnectionInterface connection: connections) {
					connection.disconnect();
				}
	    	}
	    }
	}
	
	static class UiHandler extends Handler {
		
		public final WeakReference<PushActivity> parent;
		
		public UiHandler(PushActivity activity) {
			
			this.parent = new WeakReference<PushActivity>(activity);
		}
		
		@Override
		public void handleMessage(Message msg) {
			
			PushActivity activity = parent.get();
			if (activity != null) {
				activity.handleUiMessage(msg);
			}
		}
		
	}
	
	public void handleUiMessage(Message msg) {
		
		String[] message;
		try {
			message = (String[]) msg.obj;
		} catch (ClassCastException e) {
			message = new String[3];
		}
		
		if (message.length < 4) {
			return;
		}

		TextView textViewPushConnection = (TextView) findViewById(R.id.textViewPushConnection);
		if (message[0].length() > 0 && textViewPushConnection != null) {
			
			textViewPushConnection.setText(message[0]);
		}
		
		TextView textViewPushPacket = (TextView) findViewById(R.id.textViewPushPacket);
		if (message[1].length() > 0 && textViewPushPacket != null) {
			
			textViewPushPacket.setText("sent: " + message[1]);
		}
		
		TextView textViewPushPacketReceived = (TextView) findViewById(R.id.textViewPushPacketReceived);
		if (message[2].length() > 0 && textViewPushPacketReceived != null) {
			
			textViewPushPacketReceived.setText("received: " + message[2]);
		}
		
		TextView textViewPushStatus = (TextView) findViewById(R.id.textViewPushStatus);
		if (message[3].length() > 0 && textViewPushStatus != null) {
				
			textViewPushStatus.setText(message[3]);
		}
	}
	
	private class AsyncTaskPush extends AsyncTask<ConnectionInterface, String, Integer> {

		ConnectionInterface[] paramsConnection;
		
		@Override
		protected void onPreExecute() {
			
			super.onPreExecute();
		}
		
		@Override
		protected Integer doInBackground(ConnectionInterface... params) {
			
			this.paramsConnection = params;
			boolean increasingExtraFloats = finalExtraFloats >= initialExtraFloats ? true : false;
			boolean increasingPackets = finalPackets >= initialPackets ? true : false;
			boolean increasingDelay = finalDelay >= initialDelay ? true : false;
			
			for (ConnectionInterface connection: paramsConnection) {
				
				connection.connect(true);
			}
				
			for (int extraFloats = initialExtraFloats; 
					increasingExtraFloats == true ? 
							(extraFloats <= finalExtraFloats) :	
								(extraFloats >= finalExtraFloats); 
					extraFloats = (geometricProgressionExtraFloats == true ? 
							(extraFloats * progressionValueExtraFloats) : 
								(extraFloats + progressionValueExtraFloats)) ) {
					
				Packet packet = new Packet(extraFloats);
					
				for (int delay = initialDelay;
						(increasingDelay == true) ?
								(delay <= finalDelay) :
									(delay >= finalDelay);
						delay = (geometricProgressionDelay == true ? 
								(delay * progressionValueDelay) :
									(delay + progressionValueDelay)) ) {
						
					for (int totalPackets = initialPackets; 
							(increasingPackets == true ?
									(totalPackets <= finalPackets) :
										(totalPackets >= finalPackets));
							totalPackets = (geometricProgressionPackets == true ?
									(totalPackets * progressionValuePackets) :
										(totalPackets + progressionValuePackets)) ) {
							
						for (ConnectionInterface connection: paramsConnection) {
							
							for (int repetition = 1; repetition <= repetitions; repetition++) {
								
								String testDetails = extraFloats + "/" + finalExtraFloats +
										  " " + delay + "/" + finalDelay +
										  " " + totalPackets + "/" + finalPackets + 
										  " " + repetition + "/" + repetitions;
								
								report.addToReport(connection.getName() + " " + testDetails);	
								
								for (int packetNumber = 1; packetNumber <= totalPackets; packetNumber++) {
									
									try {
										connection.send(ConnectionInterface.PUSH, packet);
									} catch (Exception e) {
										e.printStackTrace();
										// how many exceptions are expected here?!
										report.addCommentToReport(connection.getName() + " exception " + e.getMessage());
									}
									
									onProgressUpdate(connection.getName(),
											testDetails.concat(" " + packetNumber + "/" + totalPackets),
											"",
											connection.getStatus());
									
									if (isCancelled()) {
										return null;
									}
									
									try {
										Thread.sleep(delay);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
								} //packet							
								
								try {
									Thread.sleep(standByTime);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								
							} //repetition
						} //connection						
					} //packets
				} //delay
			} // extraFloats
			
			return null;
		}
		
		@Override
		protected void onProgressUpdate(String... values) {
			
			super.onProgressUpdate(values);
			
			try {
				Message msg = new Message();
				msg.obj = values;
				uiMessenger.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			
			// wait for remaining packets
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			super.onPostExecute(result);
			
			for (ConnectionInterface connection: paramsConnection) {
				connection.disconnect();
			}
			
			ToggleButton toggleButtonPush = (ToggleButton) findViewById(R.id.toggleButtonPush);
			if (toggleButtonPush != null) {
				toggleButtonPush.setChecked(false);
			}
		}
		
		@Override
		protected void onCancelled() {

			super.onCancelled();
			
			if (paramsConnection != null) {
				
				for (ConnectionInterface connection: paramsConnection) {
					connection.disconnect();
				}
			}
		}
	}
}
