package com.cht.iot.mqtt.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PubackPacket extends Packet {
	protected int packetIdentifier;
	
	public PubackPacket() {
		super(Packet.Type.PUBACK);
	}
	
	public int getPacketIdentifier() {
		return packetIdentifier;
	}
	
	public void setPacketIdentifier(int packetIdentifier) {
		this.packetIdentifier = packetIdentifier;
	}
	
	// ======	
	
	@Override
	public Packet from(ByteBuffer bytes) throws IOException {
		super.from(bytes);
		
		this.packetIdentifier = bytes.getShort();
		
		return this;
	}
	
	@Override
	protected ByteBuffer body() {
		ByteBuffer bytes = ByteBuffer.allocate(2);
		
		bytes.putShort((short) this.packetIdentifier);
		
		bytes.flip();
		
		return bytes;
	}	
}
