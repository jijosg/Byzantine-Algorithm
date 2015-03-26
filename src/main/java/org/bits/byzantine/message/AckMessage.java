package org.bits.byzantine.message;

public class AckMessage extends Message {
	private static final long serialVersionUID = -3290844120612315033L;
	private int ackId;
	
	public AckMessage(int id, int sender, int receiver){
		super(id, sender, receiver);
	}

	public int getAckId() {
		return ackId;
	}

	public void setAckId(int ackId) {
		this.ackId = ackId;
	}

}
