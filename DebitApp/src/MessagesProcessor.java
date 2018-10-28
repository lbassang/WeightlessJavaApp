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
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;

import javax.xml.bind.DatatypeConverter;

import ubiik.wp.model.messages.PSMessage;
import ubiik.wp.model.messages.PSMessageFactory;

/**
 * Sample Message Processor that creates messages from bytes, 
 * prints the received data to the console and in case the 
 * data contains a PSMessage, it also prints it to the console
 *
 */
public class MessagesProcessor implements Runnable {

	private BlockingQueue<byte[]> inQueue;
	
	public MessagesProcessor(BlockingQueue<byte[]> inMessagesQueue) {
		super();
		this.inQueue = inMessagesQueue;
	}
	
	@Override
	public void run() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss");
		Date date = new Date();
		PSMessageFactory factory = new PSMessageFactory();
		byte[] messageData = null;
		FileWriter fw = null;
		BufferedWriter bw = null;
			try {
				fw = new FileWriter("receivedLog_"+dateFormat.format(date)+".txt",true);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			bw = new BufferedWriter(fw);
			PrintWriter out = new PrintWriter(bw);
		
		try {
			while (true) {
				messageData = this.inQueue.take();
				System.out.println("Received Data: " + DatatypeConverter.printHexBinary(messageData));
				System.out.println("Message size : "+messageData.length);
				if(messageData.length > 45) {
					byte[] payload = new byte[messageData.length-45];
					/* Data with payload sent by ED */
					for(int i = 0; i<payload.length;i++) {
						payload[i]=messageData[45+i];
					}
					System.out.println("Payload : "+DatatypeConverter.printHexBinary(payload));
					System.out.println("Payload Size : "+payload.length);
					//bw.write(DatatypeConverter.printHexBinary(payload));
					date = new Date();
					out.println("Received data @"+dateFormat.format(date)+" "+DatatypeConverter.printHexBinary(payload));
					out.println("With Message size : "+messageData.length + " and Payload length : "+payload.length);
					out.println("*******************************");
				}

				// create Message
				PSMessage message = factory.createMessageFromBytes(messageData);
				// if message is invalid, ignore it
				if(message != null) {
					System.out.println("Data contains message: "+message.getClass().getName()
							+ " with data: "+message.getDataString());
				}
			}
		} catch (InterruptedException e) {
			// do nothing
		} finally {
			try {
				if(out != null)
					out.close();
				if (bw != null)
					bw.close();
				if (fw != null)
					fw.close();
			}catch(IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				
			}

		}
	}

}