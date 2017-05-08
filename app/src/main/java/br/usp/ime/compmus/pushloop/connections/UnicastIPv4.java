/**
 * 
 */
package br.usp.ime.compmus.pushloop.connections;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;

import android.content.Context;
import android.content.SharedPreferences;
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
public class UnicastIPv4 implements ConnectionInterface {

	protected Report report;
	protected Messenger uiMessenger;
	protected StringBuilder status;
	protected OSCPortOut sender;
	protected OSCPortIn receiver;
	
	private String name = "unicastIPv4";
	private String settingName = "pref_connectionUnicastIPv4";
	private String host;
	private int port;
	
	/**
	 * 
	 */
	public UnicastIPv4(Report report, Messenger uiMessenger) {
		
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
							
		this.host = preferences.getString("pref_unicastIPv4Host",
				context.getResources().getString(
						R.string.pref_unicastIPv4HostDefault));
		
		this.port = Integer.parseInt(preferences.getString("pref_unicastIPv4Port",
				context.getResources().getString(
						R.string.pref_unicastIPv4PortDefault)));

		setStatus("load settings ok");
	}
	
	@Override
	public boolean connect(boolean pushmode) {
		
		boolean senderStarted;
		boolean receiverStarted;
		
		if (pushmode) {
			senderStarted = this.startSender();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			receiverStarted = this.startReceiver(pushmode);
		} else {
			receiverStarted = this.startReceiver(pushmode);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			senderStarted = this.startSender();
		}
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if (senderStarted && receiverStarted) {
			
			setStatus("connect ok");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return true;
		} else {
			
			if (senderStarted) {
				stopSender();
			} else {
				setStatus("connect error sender not started");
			}
			if (receiverStarted) {
				stopReceiver();
			} else {
				setStatus("connect error receiver not started");
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
		
		if (senderStopped && receiverStopped) {
			
			setStatus("disconnect ok");
			return true;
		} else {
			
			if (!senderStopped) {
				
				setStatus("disconnect error sender");
			}
			if (!stopReceiver()) {
				
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
		
		this.receiver.addListener("/" + LOOP, new OSCListener() {
			
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
			this.sender = new OSCPortOut(address, this.port);			
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
			this.receiver = new OSCPortIn(this.port);
			this.receiver.startListening();
			if (!pushmode) this.startPushListener();
			if (pushmode) this.startLoopListener();
			setStatus("startReceiver ok");
			return true;
		} catch (NullPointerException e) {
			e.printStackTrace();
			setStatus("startReceiver exception: " + e.getMessage());
		} catch (SocketException e) {
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
}
