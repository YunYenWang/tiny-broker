package com.cht.iot.mqtt.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ConnackPacket extends Packet {
	int acknowledgeFlags;
	int returnCode;
	
	public ConnackPacket() {
		super(Packet.Type.CONNACK);
	}
	
	public void setSessionPresent(boolean present) {
		acknowledgeFlags |= (present? 1 : 0);
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
		
		acknowledgeFlags = bytes.get();
		returnCode = bytes.get();
		
		return this;
	}	
	
	ByteBuffer body() {
		ByteBuffer bytes = ByteBuffer.allocate(2);
		
		bytes.put((byte) acknowledgeFlags);
		bytes.put((byte) returnCode);
		
		bytes.flip();
		
		return bytes;
	}
	
	// ======
	
	public static enum ReturnCode {
		ACCEPTED(0),
		UNACCEPTABLE(1),
		IDENTIFIER_REJECTED(2),
		UNAVAILABLE(3),
		UNAUTHENTICATED(4),
		UNAUTHORIZED(5);
		
		final int code;
		
		ReturnCode(int code) {
			this.code = code;
		}
		
		public int getCode() {
			return code;
		}
	}
}
