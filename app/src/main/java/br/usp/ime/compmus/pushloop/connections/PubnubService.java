package br.usp.ime.compmus.pushloop.connections;

import org.json.JSONException;
import org.json.JSONObject;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;

import br.usp.ime.compmus.pushloop.R;
import br.usp.ime.compmus.pushloop.util.MobileDevice;
import br.usp.ime.compmus.pushloop.util.Report;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;

public class PubnubService implements ConnectionInterface {
	
	private Report report;
	private Messenger uiMessenger;
	private StringBuilder status;
	
	private boolean paidService = false;
	private String name = "pubnub";
	private String settingName = "pref_connectionPubNub";
	private String publish_key;
	private String subscribe_key;
	
	private Pubnub pubnub;

	public PubnubService(Report report, Messenger uiMessenger, boolean paidService) {
		
		this.report = report;
		this.uiMessenger = uiMessenger;
		this.paidService = paidService;
		
		if (this.paidService) {
			
			this.name = this.name.concat("Paid");
			this.settingName = this.settingName.concat("Paid");
		}
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
			
			this.publish_key = preferences.getString("pref_pubnubPaidPublishKey", 
					context.getResources().getString(
							R.string.pref_pubnubPublishKeyDefault));
			this.subscribe_key = preferences.getString("pref_pubnubPaidSubscribeKey",
					context.getResources().getString(
							R.string.pref_pubnubSubscribeKeyDefault));
		} else {
			
			this.publish_key = preferences.getString("pref_pubnubPublishKey", 
					context.getResources().getString(
							R.string.pref_pubnubPublishKeyDefault));
			this.subscribe_key = preferences.getString("pref_pubnubSubscribeKey",
					context.getResources().getString(
							R.string.pref_pubnubSubscribeKeyDefault));
		}
		
		setStatus("load settings ok");
	}

	@Override
	public boolean connect(boolean pushmode) {

		pubnub = new Pubnub(publish_key, subscribe_key, false);
		
		if (!pushmode) {
		
			try {
				pubnub.subscribe(PUSH, callbackPush);

				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				setStatus("connect ok");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return true;
			} catch (Exception e) {
				
				e.printStackTrace();
				setStatus("connect exception push: " + e.getMessage());
			}
		} else {
			
			try {
				pubnub.subscribe(LOOP, callbackLoop);
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				setStatus("connect ok");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return true;
			} catch (Exception e) {
				
				e.printStackTrace();
				setStatus("connect exception loop: " + e.getMessage());
			}
		}
		
		setStatus("connect error");
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean disconnect() {
		
		if (pubnub == null) {
			
			return true;
		}
		
		pubnub.unsubscribeAll();
		pubnub.shutdown();
		setStatus("disconnect ok");
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return true;
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
		this.pubnub.publish(address, jsonObject, callbackSend);

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
	
	Callback callbackPush = new Callback() {
		
		@Override
		public void connectCallback(String channel, Object message) {
			
			super.connectCallback(channel, message);
			setStatus("callbackPush connect " + channel + ": "
					+ message.toString());
		}
		
		@Override
		public void disconnectCallback(String channel, Object message) {
			
			super.disconnectCallback(channel, message);
			setStatus("callbackPush disconnect " + channel + ": "
					+ message.toString());
		}
		
		@SuppressWarnings("deprecation")
		@Override
		public void errorCallback(String channel, Object message) {

			super.errorCallback(channel, message);
			setStatus("callbackPush error* " + channel + ": "
					+ message.toString());
		}
		
		@Override
		public void errorCallback(String channel, PubnubError error) {

			super.errorCallback(channel, error);
			setStatus("callbackPush error " + channel + ": "
					+ error.getErrorString());
		}
		
		@Override
		public void reconnectCallback(String channel, Object message) {

			super.reconnectCallback(channel, message);
			setStatus("callbackPush reconnect " + channel + ": "
					+ message.toString());
		}
		
		@Override
		public void successCallback(String channel, Object message) {
			
//			super.successCallback(channel, message);
//			setStatus("callbackPush success* " + channel + ": " 
//					+ message.toString());
			JSONObject jsonObject;
			try {
				jsonObject = new JSONObject(message.toString());
				send(LOOP, jsonObject);
			} catch (JSONException e) {
				send(LOOP, message.toString());
				jsonObject = new JSONObject();
				e.printStackTrace();
			}
			
			long timestamp = MobileDevice.getTimestamp();
			String jsonString = jsonObject.optString("message", message.toString());
			int packetSize = jsonObject.toString().getBytes().length;
			
			report.addToReport(timestamp, getName() + " " + PUSH + " " + jsonString + " " + packetSize);
			try {
				setReceived(jsonString.substring(0, 5).concat("..."));
			} catch (IndexOutOfBoundsException e) {
				e.printStackTrace();
				setReceived("...");
			}
		}
		
//		@Override
//		public void successCallback(String channel, Object message,
//				String timetoken) {
//
//			super.successCallback(channel, message, timetoken);
//			setStatus("callbackPush success " + channel + ": " 
//					+ message.toString() + " timetoken: " + timetoken);
//		}
	};
	
	Callback callbackLoop = new Callback() {
		
		@Override
		public void connectCallback(String channel, Object message) {
			
			super.connectCallback(channel, message);
			setStatus("callbackLoop connect " + channel + ": "
					+ message.toString());
		}
		
		@Override
		public void disconnectCallback(String channel, Object message) {
			
			super.disconnectCallback(channel, message);
			setStatus("callbackLoop disconnect " + channel + ": "
					+ message.toString());
		}
		
		@SuppressWarnings("deprecation")
		@Override
		public void errorCallback(String channel, Object message) {

			super.errorCallback(channel, message);
			setStatus("callbackLoop error* " + channel + ": "
					+ message.toString());
		}
		
		@Override
		public void errorCallback(String channel, PubnubError error) {

			super.errorCallback(channel, error);
			setStatus("callbackLoop error " + channel + ": "
					+ error.getErrorString());
		}
		
		@Override
		public void reconnectCallback(String channel, Object message) {

			super.reconnectCallback(channel, message);
			setStatus("callbackLoop reconnect " + channel + ": "
					+ message.toString());
		}
		
		@Override
		public void successCallback(String channel, Object message) {
			
//			super.successCallback(channel, message);
//			setStatus("callbackLoop success* " + channel + ": " 
//					+ message.toString());
			
			long timestamp = MobileDevice.getTimestamp();
			
			JSONObject jsonObject;
			try {
				jsonObject = new JSONObject(message.toString());
			} catch (JSONException e) {
				jsonObject = new JSONObject();
				e.printStackTrace();
			}
			
			String jsonString = jsonObject.optString("message", message.toString());
			int packetSize = jsonObject.toString().getBytes().length;
			
			
			report.addToReport(timestamp, getName() + " " + LOOP + " " + jsonString + " " + packetSize);
			try {
				setReceived(jsonString.substring(0, 5).concat("..."));
			} catch (IndexOutOfBoundsException e) {
				e.printStackTrace();
				setReceived("...");
			}
		}
		
//		@Override
//		public void successCallback(String channel, Object message,
//				String timetoken) {
//
//			super.successCallback(channel, message, timetoken);
//			setStatus("callbackLoop success " + channel + ": " 
//					+ message.toString() + " timetoken: " + timetoken);
//		}
	};
	
	Callback callbackSend = new Callback() {
		
		@Override
		public void connectCallback(String channel, Object message) {
			
			super.connectCallback(channel, message);
			setStatus("callbackSend connect " + channel + ": "
					+ message.toString());
		}
		
		@Override
		public void disconnectCallback(String channel, Object message) {
			
			super.disconnectCallback(channel, message);
			setStatus("callbackSend disconnect " + channel + ": "
					+ message.toString());
		}
		
		@SuppressWarnings("deprecation")
		@Override
		public void errorCallback(String channel, Object message) {

			super.errorCallback(channel, message);
			setStatus("callbackSend error* " + channel + ": "
					+ message.toString());
		}
		
		@Override
		public void errorCallback(String channel, PubnubError error) {

			super.errorCallback(channel, error);
			setStatus("callbackSend error " + channel + ": "
					+ error.getErrorString());
		}
		
		@Override
		public void reconnectCallback(String channel, Object message) {

			super.reconnectCallback(channel, message);
			setStatus("callbackSend reconnect " + channel + ": "
					+ message.toString());
		}
		
//		@Override
//		public void successCallback(String channel, Object message) {
//			
//			super.successCallback(channel, message);
//			setStatus("callbackSend success* " + channel + ": " 
//					+ message.toString());
//		}
		
//		@Override
//		public void successCallback(String channel, Object message,
//				String timetoken) {
//
//			super.successCallback(channel, message, timetoken);
//			setStatus("callbackSend success " + channel + ": " 
//					+ message.toString() + " timetoken: " + timetoken);
//		}
	};




}
