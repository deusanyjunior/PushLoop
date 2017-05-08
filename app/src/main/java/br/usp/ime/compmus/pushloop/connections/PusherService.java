/**
 * 
 */
package br.usp.ime.compmus.pushloop.connections;

import org.json.JSONException;
import org.json.JSONObject;

import br.usp.ime.compmus.pushloop.R;
import br.usp.ime.compmus.pushloop.util.MobileDevice;
import br.usp.ime.compmus.pushloop.util.Report;

import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.PrivateChannel;
import com.pusher.client.channel.PrivateChannelEventListener;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;
import com.pusher.client.util.HttpAuthorizer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;

/**
 * @author dj
 *
 */
public class PusherService implements ConnectionInterface {
	
	private Report report;
	private Messenger uiMessenger;
	private StringBuilder status;
	
	private boolean paidService = false;
	private String name = "pusher";
	private String settingName = "pref_connectionPusher";
	private String key;
	private Pusher pusher;
	private PrivateChannel channel;	
	private HttpAuthorizer authorizer;
	private PusherOptions options;
	private String privateChannelName = "private-pushloop";
	
	/**
	 * 
	 */
	public PusherService(Report report, Messenger uiMessenger, boolean paidService) {
		
		this.report = report;
		this.uiMessenger = uiMessenger;
		this.paidService = paidService;
		
		if (this.paidService) {
			
			this.name = this.name.concat("Paid");
			this.settingName = this.settingName.concat("Paid");
			this.authorizer = new HttpAuthorizer("http://deusanyjunior.dj/a/temp/pusherPaid.php");
		} else {
			
			this.authorizer = new HttpAuthorizer("http://deusanyjunior.dj/a/temp/pusher.php");
		}
		
		this.options = new PusherOptions();
		this.options.setAuthorizer(authorizer);
//		this.options.setCluster("us");	
	}

	@Override
	public String getName() {

		return this.name;
	}

	@Override
	public String getSettingName() {

		return this.settingName;
	}

	@Override
	public void loadSettings(Context context) {

		SharedPreferences preferences = 
				PreferenceManager.getDefaultSharedPreferences(context);
		
		if (this.paidService) {
			
			this.key = preferences.getString("pref_pusherPaidKey",
					context.getResources().getString(
							R.string.pref_pusherPaidKeyDefault));
		} else {
			
			this.key = preferences.getString("pref_pusherKey",
					context.getResources().getString(
							R.string.pref_pusherKeyDefault));
		}
		setStatus("load settings ok");
	}

	@Override
	public boolean connect(boolean pushmode) {
		
		pusher = new Pusher(key, options);
		pusher.connect(connectionEventListener, ConnectionState.ALL);
		joinChannel(pushmode);
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (pusher.getConnection().getState() == ConnectionState.CONNECTED) {

			setStatus("connect ok");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return true;
		} else {
			
			setStatus("connect error");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return false;
		}
	}

	@Override
	public boolean disconnect() {

		if (pusher == null) {
			
			return true;
		}
		
		if (channel != null) {
			
			pusher.unsubscribe(channel.getName());
		}
		pusher.disconnect();
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (pusher.getConnection().getState() == ConnectionState.DISCONNECTED) {

			setStatus("disconnect ok");
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return true;
		} else {
			
			setStatus("disconnect error");
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return false;
		}
	}

	@Override
	public boolean send(String address, Packet packet) {
		
		return send(address, packet.getJsonContents());
	}
	
	private boolean send(String address, String jsonString) {
		
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("message", jsonString);
		} catch (JSONException e) {
			e.printStackTrace();
			setStatus("send exception: " + e.getMessage());
		}
		return send(address, jsonObject);
	}
	
	private boolean send(String address, JSONObject jsonObject) {
		
		int packetSize = 0;
		long timestamp = MobileDevice.getTimestamp();		
		try {
			this.channel.trigger(address, jsonObject.toString());			
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			setStatus("send exception: " + e.getMessage());
			return false;
		} catch (IllegalStateException e) {
			e.printStackTrace();
			setStatus("send exception: " + e.getMessage());
			return false;
		}
		
		String jsonString = jsonObject.optString("message", jsonObject.toString());
		packetSize = jsonObject.toString().getBytes().length;
		
		report.addToReport(timestamp, getName() + " " + address + " " + jsonString + " " + packetSize);
		return true;
	}

	@Override
	synchronized public String getStatus() {

		if (this.status == null) {
			
			this.status = new StringBuilder();
		}
		
		return this.status.toString();
	}
	
	synchronized private void setStatus(String status) {
		
		report.addCommentToReport(getName() + " " + status);
		
		if (this.status == null) {
			this.status = new StringBuilder(); 
		}
		this.status.insert(0, status + "\n"); 
		
		try {
			Message message = new Message();
			message.obj = new String[]{getName(), "", "", getStatus()};
			this.uiMessenger.send(message);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	synchronized private void setReceived(String packet) {
		
		try {
			Message message = new Message();
			message.obj = new String[]{getName(), "", packet, getStatus()};
			uiMessenger.send(message);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	private ConnectionEventListener connectionEventListener = new ConnectionEventListener() {
		
		@Override
		public void onError(String message, String code, Exception e) {
			
			try {
				
				setStatus("connectionEventListener error message: " + message
						+ "; code: " + code + "; exception: " + e.getMessage());
			} catch (NullPointerException ex) {
				
				ex.printStackTrace();
			}
		}
		
		@Override
		public void onConnectionStateChange(ConnectionStateChange change) {
			
			setStatus("connectionEventListener state: " + change.getCurrentState());
		}
	};
	
	private void joinChannel(boolean pushmode) { 
		
		if (pusher != null) {
			
			if (channel != null) {
				
				pusher.unsubscribe(channel.getName());
			}
			channel = pusher.subscribePrivate(privateChannelName);
			
			if (!pushmode) {
				
				channel.bind(PUSH, privateChannelEventListenerPush);
			} else {
				
				channel.bind(LOOP, privateChannelEventListenerLoop);
			}
		}
	}
	
	private PrivateChannelEventListener privateChannelEventListenerPush = new PrivateChannelEventListener() {
		
		@Override
		public void onEvent(String channelName, String eventName, String data) {
			
			JSONObject jsonObject;
			try {
				jsonObject = new JSONObject(data); 
				send(LOOP, jsonObject);
			} catch (JSONException e) {
				send(LOOP, data);
				jsonObject = new JSONObject();
				e.printStackTrace();
			}

			long timestamp = MobileDevice.getTimestamp();
			String jsonString = jsonObject.optString("message", jsonObject.toString());
			int packetSize = jsonObject.toString().getBytes().length;
			
			report.addToReport(timestamp, getName() + " " + PUSH + " " + jsonString + " " +  packetSize);
			try {
				setReceived(jsonString.substring(0, 5).concat("..."));
			} catch (IndexOutOfBoundsException e) {
				e.printStackTrace();
				setReceived("...");
			}
		}

		@Override
		public void onSubscriptionSucceeded(String channelName) {
		
			setStatus("eventListenerPush subscription succeeded on " + channelName);
		}
		
		@Override
		public void onAuthenticationFailure(String message, Exception e) {

			setStatus("eventListenerPush authentication failure: " + message
					+ "; exception: " + e.getMessage());
		}
	};
	
	private PrivateChannelEventListener privateChannelEventListenerLoop = new PrivateChannelEventListener() {
		
		@Override
		public void onEvent(String channelName, String eventName, String data) {
			
			long timestamp = MobileDevice.getTimestamp();
			
			JSONObject jsonObject;
			try {
				jsonObject = new JSONObject(data);
			} catch (JSONException e) {
				jsonObject = new JSONObject();
				e.printStackTrace();
			}
			
			String jsonString = jsonObject.optString("message", jsonObject.toString());
			int packetSize = data.getBytes().length;
			
			report.addToReport(timestamp, getName() + " " + LOOP + " " + jsonString + " " + packetSize);
			try {
				setReceived(jsonString.substring(0, 5).concat("..."));
			} catch (IndexOutOfBoundsException e) {
				e.printStackTrace();
				setReceived("...");
			}
		}
		
		@Override
		public void onSubscriptionSucceeded(String channelName) {
			
			setStatus("eventListenerLoop subscription succeeded on " + channelName);
		}
		
		@Override
		public void onAuthenticationFailure(String message, Exception e) {
			
			setStatus("eventListenerLoop authentication failure: " + message
					+ "; exception: " + e.getMessage());
		}
	};

}
