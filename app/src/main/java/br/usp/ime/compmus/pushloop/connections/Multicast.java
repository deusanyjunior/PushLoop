/**
 * 
 */
package br.usp.ime.compmus.pushloop.connections;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import br.usp.ime.compmus.pushloop.R;
import br.usp.ime.compmus.pushloop.util.MobileDevice;
import br.usp.ime.compmus.pushloop.util.Report;

import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;

/**
 * @author dj
 *
 */
public class Multicast implements ConnectionInterface {

	protected Report report;
	protected Messenger uiMessenger;
	protected StringBuilder status;
	protected OSCPortOut sender;
	protected OSCPortIn receiver;
	
	private String name = "multicast";
	private String settingName = "pref_connectionMulticast";
	private String host;
	private int port;
	private int ttl;
	private static MulticastLock multicastLock;
	private int networkInterfaceID;
	private NetworkInterface networkInterface;

	
	/**
	 * 
	 */
	public Multicast(Report report, Messenger uiMessenger) {
		this.report = report;
		this.uiMessenger = uiMessenger;
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
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		
		this.host = preferences.getString("pref_multicastHost", 
				context.getResources().getString(
						R.string.pref_multicastHostDefault));
		
		this.port = Integer.parseInt(preferences.getString("pref_multicastPort", 
				context.getResources().getString(
						R.string.pref_multicastPortDefault)));
		
		this.ttl = Integer.parseInt(preferences.getString("pref_multicastTTL", 
				context.getResources().getString(
						R.string.pref_multicastTTLDefault)));
		
		this.networkInterfaceID = Integer.parseInt(preferences.getString("pref_multicastInterface", 
				context.getResources().getString(
						R.string.pref_multicastInterfaceDefault)));
		
		this.networkInterface = MobileDevice.getNetworkInterfaces().
				get(this.networkInterfaceID);
		
		this.acquireMulticastLock(context);
		
		setStatus("load settings ok");
	}
	
	@Override
	public boolean connect(boolean pushmode) {
		
		boolean senderStarted = this.startSender();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		boolean receiverStarted = this.startReceiver(pushmode);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (senderStarted && receiverStarted) {
			
			setStatus("connect ok");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		} else {
			
			if (senderStarted) {
				stopSender();
			} else {
				setStatus("connect error: sender not started");
			}
			if (receiverStarted) {
				stopReceiver();
			} else {
				setStatus("connect error: receiver not started");
			}
		}
		setStatus("connect error");
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean disconnect() {
		
		boolean senderStopped = stopSender(); 
		boolean receiverStopped = stopReceiver();
		
		releaseMulticastLock();
		
		if (senderStopped && receiverStopped) {
			
			setStatus("disconnect ok");
			return true;
		} else {
			
			if (!senderStopped) {
				
				setStatus("disconnect error sender");
			}
			if (!receiverStopped) {
				
				setStatus("disconnect error receiver");
			}
		}
		setStatus("disconnect error");
		return false;
	}
	
	@Override
	public boolean send(String address, Packet packet) {
		
		OSCMessage message = new OSCMessage("/" + address, packet.getListContents());
		
		return send(message);
	}
	
	private boolean send(OSCMessage message) {
		
		int packetSize = 0;
		long timestamp = MobileDevice.getTimestamp();
		try {
			packetSize = this.sender.send(message);
			report.addToReport(timestamp, getName() + " " + message.getAddress().substring(1) + " " + message.getArguments().toString() + " " + packetSize);
			return true;
		} catch (SocketException e) {
			e.printStackTrace();
			setStatus("send exception: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			setStatus("send exception: " + e.getMessage());
		} catch (NullPointerException e) {
			e.printStackTrace();
			setStatus("send exception: " + e.getMessage());
		}
		setStatus("send error");
		return false;
	}
	
	@Override
	synchronized public String getStatus() {
		
		if (status == null) {
			status = new StringBuilder(); 
		}
		
		return status.toString();
	}	
	
	synchronized public void setStatus(String status) {
		
		report.addCommentToReport(getName() + " " + status);
		
		if (this.status == null) {
			this.status = new StringBuilder(); 
		}
		this.status.insert(0, status+"\n"); 
		
		try {
			Message message = new Message();
			message.obj = new String[]{getName(), "", "", getStatus()};
			uiMessenger.send(message);
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
		
	public void startPushListener() {
		
		this.receiver.addListener("/" + PUSH, new OSCListener() {
			
			@Override
			public void acceptMessage(Date time, OSCMessage message) {
				
				message.setAddress("/" + LOOP);
				send(message);
				long timestamp = MobileDevice.getTimestamp();
				report.addToReport(timestamp, getName() + " " + PUSH + " " + message.getArguments().toString());
				try {
					setReceived(message.getArguments().toString().substring(0, 5).concat("..."));
				} catch (IndexOutOfBoundsException e) {
					e.printStackTrace();
					setReceived("...");
				}
			}
		});
	}
	
	public void startLoopListener() {
		
		this.receiver.addListener("/" +  LOOP, new OSCListener() {
			
			@Override
			public void acceptMessage(Date time, OSCMessage message) {
				
				long timestamp = MobileDevice.getTimestamp();
				report.addToReport(timestamp, getName() + " " + LOOP + " " + message.getArguments().toString());
				try {
					setReceived(message.getArguments().toString().substring(0, 5).concat("..."));
				} catch (IndexOutOfBoundsException e) {
					e.printStackTrace();
					setReceived("...");
				}
			}
		});
	}
	
	private boolean startSender() {
		
		try {
			InetAddress address = InetAddress.getByName(this.host);
			MulticastSocket multicastSocketSender = new MulticastSocket(this.port);
			multicastSocketSender.setReuseAddress(true);
			multicastSocketSender.setTimeToLive(this.ttl);
			multicastSocketSender.joinGroup(new InetSocketAddress(address, this.port), networkInterface);
			this.sender = new OSCPortOut(address, this.port, multicastSocketSender);
			setStatus("startSender ok");
			return true;
		} catch (BindException e) {
			e.printStackTrace();
			setStatus("startSender exception: " + e.getMessage());
		} catch (NullPointerException e) {
			e.printStackTrace();
			setStatus("startSender exception: " + e.getMessage());
		} catch (UnknownHostException e) {
			e.printStackTrace();
			setStatus("startSender exception: " + e.getMessage());
		} catch (SocketException e) {
			e.printStackTrace();
			setStatus("startSender exception: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			setStatus("startSender exception: " + e.getMessage());
		}
		
		this.sender = null;
		setStatus("startSender error");
		return false;
	}
	
	private boolean stopSender() {
		
		try {
			if (this.sender != null) {
				
				this.sender.close();
				this.sender = null;
			}
			setStatus("stopSender ok");
			return true;
		} catch (IOException e) {
			this.sender = null;
			e.printStackTrace();
			setStatus("stopSender exception: " + e.getMessage());
		}
		
		this.sender = null;
		setStatus("stopSender error");
		return false;
	}
	
	private boolean startReceiver(boolean pushmode) {
		
		try {
			InetAddress address = InetAddress.getByName(this.host);
			MulticastSocket multicastSocketReceiver = new MulticastSocket(this.port);
			multicastSocketReceiver.setReuseAddress(true);
			multicastSocketReceiver.setTimeToLive(this.ttl);
			multicastSocketReceiver.joinGroup(new InetSocketAddress(address, this.port), this.networkInterface);
			this.receiver = new OSCPortIn(multicastSocketReceiver);
			this.receiver.startListening();
			if(!pushmode) this.startPushListener();
			if(pushmode) this.startLoopListener();
			setStatus("startReceiver ok");
			return true;
		} catch (NullPointerException e) {
			e.printStackTrace();
			setStatus("startReceiver exception: " + e.getMessage());
		} catch (SocketException e) {
			e.printStackTrace();
			setStatus("startReceiver exception: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			setStatus("startReceiver exception: " + e.getMessage());
		}
		
		this.receiver = null;
		setStatus("startReceiver error");
		return false;
	}
	
	private boolean stopReceiver() {
		
		try {
			if (this.receiver != null) {
				this.receiver.removeListener(PUSH);
				this.receiver.removeListener(LOOP);
				this.receiver.stopListening();
				this.receiver.close();
				this.receiver = null;
			}
			setStatus("stopReceiver ok");
			return true;
		} catch (IOException e) {
			this.receiver = null;
			e.printStackTrace();
			setStatus("stopReceiver exception: " + e.getMessage());
		}
		
		this.receiver = null;
		setStatus("stopReceiver error");
		return false;
	}
	
	/**
	 * 
	 * @param ctx
	 * @return
	 */
	public boolean acquireMulticastLock(Context ctx) {
		
		WifiManager wifiManager = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		if (wifiManager != null) {
			
			multicastLock = wifiManager.createMulticastLock(this.getName());
			multicastLock.setReferenceCounted(false);
			multicastLock.acquire();
			return true;
		} else {
			
			return releaseMulticastLock();
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public static boolean releaseMulticastLock() {
		
		if (multicastLock != null && multicastLock.isHeld()) {
			
			multicastLock.release();
			return true;
		}
		return false;
	}
}
