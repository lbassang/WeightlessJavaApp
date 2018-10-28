/****************************************************************
* Copyright (c) Ubiik Inc., 2016-2017
*
* This unpublished material is proprietary to Ubiik Inc.
* All rights reserved. The methods and
* techniques described herein are considered trade secrets
* and/or confidential. Reproduction or distribution, in whole
* or in part, is forbidden except by express written permission
* of Ubiik Inc.
****************************************************************/

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ubiik.wp.model.messages.PSDownlink;
import ubiik.wp.model.messages.pscommands.PSCommandGetBaseChannel;
import ubiik.wp.psconnection.PSConnectionErrorCode;
import ubiik.wp.psconnection.PSConnectionException;
import ubiik.wp.psconnection.PSConnectionManager;
import ubiik.wp.psconnection.PSConnectionManagerDelegate;

public class ConnectionSample implements PSConnectionManagerDelegate, Runnable {
	// time to run sample. 10 minute
	//public static final int SAMPLE_TIMEOUT = 600000;
	private int SAMPLE_TIMEOUT;
	private Thread msgProcessorThread;
	private PSConnectionManager connectionManager;
	private MessagesProcessor msgProcessor;
	private String host;
	private int port;
	
	public ConnectionSample(String host, int port, int sampleTimeout) {
		super();
		this.host = host;
		this.port = port;
		this.SAMPLE_TIMEOUT = sampleTimeout;
	}
	
	/**
	 * This sample connects to the protocol stack in the given IP Port.
	 * After being authenticated, sends a Downlink and a command as an example
	 * A command response is expected to be received.
	 * It runs for SAMPLE_TIMEOUT and prints out any received messages.
	 */
	public void runSample() {
		/* Queue used by the PSConnectionManager to store the messages as they come in. 
		 * Bytes contain only the Payload. Message Length and CRC are removed 
		 * from the message. 
		 */ 
		BlockingQueue<byte[]> inMessagesQueue = new LinkedBlockingQueue<byte[]>();
		this.connectionManager = new PSConnectionManager(host, port, this, inMessagesQueue);
		this.msgProcessor = new MessagesProcessor(inMessagesQueue);
		
		this.msgProcessorThread = new Thread(this.msgProcessor);
		this.msgProcessorThread.start();
		
		// Connect
		try {
			// The application will connect and authenticate.
			// We will be notified when the authentication has finished
			this.connectionManager.connect();
		} catch(PSConnectionException e) {
			this.authenticationFinished(false);
		}
				
	}

	@Override
	/**
	 * From PSConnectionManagerDelegate
	 * Receive notification when the authentication has finished
	 * @param autheticated indicates if the authentication was successful 
	 */
	public void authenticationFinished(boolean autheticated) {
		if(autheticated) {
			System.out.println("Authenticated");
			
			// Send a Downlink
			byte[] data = {(byte) 0x84,0x05}; // Downlink Data
			byte[] target = {0,1,2,3,4,5,6,7,8,0,0,0,0,0,0,0}; // End Device UUID
			PSDownlink downlink = new PSDownlink(data, target);
			System.out.println("Send " + downlink.getClass().getName() +" with Data: " + downlink.getDataString());
			this.connectionManager.sendBytes(downlink.toBytes());
			
			// Send a command
			PSCommandGetBaseChannel command = new PSCommandGetBaseChannel(null); // No command delegate
			System.out.println("Send " + command.getClass().getName() +" with Data: " + command.getDataString());
			this.connectionManager.sendBytes(command.toBytes());
			
			// Keep the the sample running for 1 minute 
			TimerTask authTimeoutTask = new TimerTask() {
				@Override
				public void run() {
					disconnect();
				}
			};
			Timer timer = new Timer(true);
			timer.schedule(authTimeoutTask, SAMPLE_TIMEOUT);
		} else {
			// stop
			System.out.println("Failed to Authenticate. Please check that you have configured the correct IP address (BS_IP in Main.java)");
			this.disconnect();
		}
	}

	@Override
	/**
	 * From PSConnectionManagerDelegate
	 * Called when the connection fails
	 */
	public void connectionFailed(PSConnectionErrorCode errorCode) {
		this.disconnect();
	}

	@Override
	public void run() {
		this.runSample();
		
		try {
			if(this.msgProcessorThread != null) {
				this.msgProcessorThread.join();	
			}
		} catch (InterruptedException e) {
			
		}
	}
	
	public void disconnect() {
		if(this.connectionManager != null) {
			this.connectionManager.disconnect();
			this.connectionManager = null;
			System.out.println("Disconnect");
		}

		if(this.msgProcessorThread != null) {
			this.msgProcessorThread.interrupt();
			this.msgProcessorThread = null;			
		}
	}

}