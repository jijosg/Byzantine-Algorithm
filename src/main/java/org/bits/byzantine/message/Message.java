package org.bits.byzantine.message;

import java.io.Serializable;

public class Message implements Serializable {
	private static final long serialVersionUID = -3189886797658116465L;
	private int id;
	private int sender;
	private int receiver;
	private long timestamp;
	
	public Message(int id, int sender, int receiver) {
		this.id = id;
		this.sender = sender;
		this.receiver = receiver;
		timestamp = System.currentTimeMillis();
	}

	public int getId() {
		return id;
	}

	public int getSender() {
		return sender;
	}

	public int getReceiver() {
		return receiver;
	}

	public long getTimestamp() {
		return timestamp;
	}

	
}
