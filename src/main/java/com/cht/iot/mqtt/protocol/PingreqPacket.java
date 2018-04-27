package com.cht.iot.mqtt.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PingreqPacket extends Packet {
	
	public PingreqPacket() {
		super(Packet.Type.PINGREQ);
	}
	
	// ======
	
	@Override
	public Packet from(ByteBuffer bytes) throws IOException {
		super.from(bytes);
		
		return this;
	}
	
	@Override
	protected ByteBuffer body() {
		throw new UnsupportedOperationException("not yet implemented");
	}
}
