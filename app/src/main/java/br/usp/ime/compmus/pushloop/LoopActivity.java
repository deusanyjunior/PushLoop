package br.usp.ime.compmus.pushloop;

import java.lang.ref.WeakReference;
import java.util.LinkedList;

import br.usp.ime.compmus.pushloop.connections.ConnectionInterface;
import br.usp.ime.compmus.pushloop.connections.Multicast;
import br.usp.ime.compmus.pushloop.connections.PubnubService;
import br.usp.ime.compmus.pushloop.connections.PusherService;
import br.usp.ime.compmus.pushloop.connections.UnicastIPv6;
import br.usp.ime.compmus.pushloop.connections.UnicastIPv4;
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

public class LoopActivity extends Activity {

	UnicastIPv4 unicastIPv4;
	UnicastIPv6 unicastIPv6;
	Multicast multicast;
	PusherService pusherFree;
	PusherService pusherPaid;
	PubnubService pubnubFree;
	PubnubService pubnubPaid;
	ConnectionInterface connectionOptions[];
	
	LinkedList<ConnectionInterface> connections;
	
	AsyncTaskLoop asyncTaskLoopback;
	
	final Report report = new Report();
	final Messenger uiMessenger = new Messenger(new UiHandler(this));
	
	private boolean created = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_loop);
		
		if (!created) {
			
			this.loadSettings();
			this.report.init(this);
			this.created = true;
		}
	}
	
	@Override
	public void onBackPressed() {
		
		if (this.asyncTaskLoopback != null) {
			this.asyncTaskLoopback.cancel(true);
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
		
		connectionOptions = new ConnectionInterface[]{
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
		for (ConnectionInterface connection : connectionOptions) {
			
			if (preferences.getBoolean(connection.getSettingName(), false)) {
				
				connection.loadSettings(this);
				this.connections.add(connection);
			}
		}
	}
	
	public void onToggleClicked(View view) {
	    // Is the toggle on?
	    boolean on = ((ToggleButton) view).isChecked();
	    
	    if (on) {
	        // Start tests
	    	if (this.asyncTaskLoopback == null) {
	    		
	    		this.asyncTaskLoopback = new AsyncTaskLoop();
	    	} else {
	    		
	    		this.asyncTaskLoopback.cancel(true);
	    		this.asyncTaskLoopback = new AsyncTaskLoop();
	    	}
			this.asyncTaskLoopback.execute(
					connections.toArray(new ConnectionInterface[connections.size()]));
	    } else {
	        // Stop tests
	    	report.finish(this);
	    	if (this.asyncTaskLoopback != null) {
	    		
	    		this.asyncTaskLoopback.cancel(true);
	    	}
	    	if (connections != null) {
	    		
	    		for (ConnectionInterface connection: connections) {
					connection.disconnect();
				}
	    	}
	    }
	}
	
	static class UiHandler extends Handler {
		
		public final WeakReference<LoopActivity> parent;
		
		public UiHandler(LoopActivity activity) {
			
			this.parent = new WeakReference<LoopActivity>(activity);
		}
		
		@Override
		public void handleMessage(Message msg) {
			
			LoopActivity activity = parent.get();
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

		TextView textViewLoopConnection = (TextView) findViewById(R.id.textViewLoopConnection);
		if (message[0].length() > 0 && textViewLoopConnection != null) {
			
			textViewLoopConnection.setText(message[0]);
		}
		
		TextView textViewLoopPacket = (TextView) findViewById(R.id.textViewLoopPacket);
		if (message[1].length() > 0 && textViewLoopPacket != null) {
			
			textViewLoopPacket.setText("sent: " + message[1]);
		}
		
		TextView textViewLoopPacketReceived = (TextView) findViewById(R.id.textViewLoopPacketReceived);
		if (message[2].length() > 0 && textViewLoopPacketReceived != null) {
			
			textViewLoopPacketReceived.setText("received: " + message[2]);
		}
		
		TextView textViewLoopStatus = (TextView) findViewById(R.id.textViewLoopStatus);
		if (message[3].length() > 0 && textViewLoopStatus != null) {
				
			textViewLoopStatus.setText(message[3]);
		}
	}
	
	private class AsyncTaskLoop extends AsyncTask<ConnectionInterface, String, Integer> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}
		
		@Override
		protected Integer doInBackground(ConnectionInterface... params) {
			
			for (ConnectionInterface connection: params) {
				connection.connect(false);
				onProgressUpdate(connection.getName(), "", "Connected");
			}
			
			return null;
		}
		
		@Override
		protected void onProgressUpdate(String... values) {
			
			super.onProgressUpdate(values);
			
			try {
				Message message = new Message();
				message.obj = values;
				uiMessenger.send(message);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		protected void onPostExecute(Integer result) {

			super.onPostExecute(result);
		}
		
		@Override
		protected void onCancelled() {
			
			super.onCancelled();
		}		
	}	
}
