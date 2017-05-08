/*
 * Copyright (C) 2003-2014, C. Ramakrishnan / Illposed Software.
 * All rights reserved.
 *
 * This code is licensed under the BSD 3-Clause license.
 * See file LICENSE (or LICENSE.html) for more information.
 */

package com.illposed.osc;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * OSCPort is an abstract superclass, to send OSC messages,
 * use {@link OSCPortOut}.
 * To listen for OSC messages, use {@link OSCPortIn}.
 *
 * @author Chandrasekhar Ramakrishnan
 */
public class OSCPort {

	private final Socket tcpSocket;
	private final DatagramSocket udpSocket;
	private final int port;
	private final boolean isUDP;

	public static final int DEFAULT_SC_OSC_PORT = 57110;
	public static final int DEFAULT_SC_LANG_OSC_PORT = 57120;

	protected OSCPort(DatagramSocket socket, int port) throws SocketException {
		this.tcpSocket = null;
		this.udpSocket = socket;
		this.port = port;
		this.isUDP = true;
		
		this.udpSocket.setReuseAddress(true);
	}

	protected OSCPort(Socket socket, int port) throws SocketException {
		this.tcpSocket = socket;
		this.udpSocket = null;
		this.port = port;
		this.isUDP = false;
		
		this.tcpSocket.setReuseAddress(true);
	}
	
	/**
	 * The port that the SuperCollider <b>synth</b> engine
	 * usually listens to.
	 * @return default SuperCollider <b>synth</b> UDP port
	 * @see #DEFAULT_SC_OSC_PORT
	 */
	public static int defaultSCOSCPort() {
		return DEFAULT_SC_OSC_PORT;
	}

	/**
	 * The port that the SuperCollider <b>language</b> engine
	 * usually listens to.
	 * @return default SuperCollider <b>language</b> UDP port
	 * @see #DEFAULT_SC_LANG_OSC_PORT
	 */
	public static int defaultSCLangOSCPort() {
		return DEFAULT_SC_LANG_OSC_PORT;
	}
	
	protected boolean isUDPPort() {
		
		return isUDP;
	}

	/**
	 * Returns the socket associated with this port.
	 * @return this ports socket
	 */
	protected DatagramSocket getUDPSocket() {

		return udpSocket;
	}

	/**
	 * Returns the socket associated with this port.
	 * @return this ports socket
	 */
	protected Socket getTCPSocket() {

		return tcpSocket;
	}
	
	/**
	 * Returns the port number associated with this port.
	 * @return this ports number
	 */
	protected int getPort() {
		return port;
	}

	/**
	 * Close the socket and free-up resources.
	 * It is recommended that clients call this when they are done with the
	 * port.
	 * @throws IOException 
	 */
	public void close() throws IOException {
		if (udpSocket != null) {
			
			udpSocket.close();
		}
		if (tcpSocket != null) {
			
			tcpSocket.close();
		}
	}
	
}
