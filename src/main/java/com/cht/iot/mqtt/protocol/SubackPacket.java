package com.cht.iot.mqtt.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;

public class SubackPacket extends Packet {
	protected int packetIdentifier;
	protected int returnCode;
	
	public SubackPacket() {
		super(Packet.Type.SUBACK);
	}
	
	public int getPacketIdentifier() {
		return packetIdentifier;
	}
	
	public void setPacketIdentifier(int packetIdentifier) {
		this.packetIdentifier = packetIdentifier;
	}
	
	public int getReturnCode() {
		return returnCode;
	}
	
	public void setReturnCode(int returnCode) {
		this.returnCode = returnCode;
	}
	
	public void setReturnCode(ReturnCode returnCode) {
		this.returnCode = returnCode.getCode();
	}
	
	// ======
	
	@Override
	public Packet from(ByteBuffer bytes) throws IOException {
		super.from(bytes);
		
		this.packetIdentifier = bytes.getShort();
		this.returnCode = bytes.get();
		
		return this;
	}
	
	@Override
	protected ByteBuffer body() {
		ByteBuffer bytes = ByteBuffer.allocate(3);
		
		bytes.putShort((short) this.packetIdentifier);
		bytes.put((byte) this.returnCode);
		
		bytes.flip();
		
		return bytes;
	}
	
	// ======
	
	public static enum ReturnCode {
		QOS0(0x00),
		QOS1(0x01),
		QOS2(0x02),
		FAILURE(0x80);
		
		protected final int code;
		
		ReturnCode(int code) {
			this.code = code;
		}
		
		public int getCode() {
			return this.code;
		}
	}
}
