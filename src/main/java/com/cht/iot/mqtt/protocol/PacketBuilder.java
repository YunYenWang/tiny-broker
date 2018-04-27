package com.cht.iot.mqtt.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class PacketBuilder {	
	protected Buffer buffer;
	protected ByteBuffer header;

	public PacketBuilder(int packetBufferInitialSize) {
		this.buffer = new Buffer(packetBufferInitialSize);
		this.header = ByteBuffer.allocate(4); // maximum 4 bytes remaining length
	}
	
	public Packet transform(ByteBuffer bytes) throws IOException {
		Packet.Type type = Packet.TYPES[(bytes.get(0) & 0x0F0) >> 4];
		if (type == Packet.Type.PUBLISH) {
			return new PublishPacket().from(bytes);

		} else if (type == Packet.Type.PUBACK) {
			return new PubackPacket().from(bytes);
			
		} else if (type == Packet.Type.PUBREC) {
			return new PubrecPacket().from(bytes);
			
		} else if (type == Packet.Type.PUBCOMP) {
			return new PubcompPacket().from(bytes);			
			
		} else if (type == Packet.Type.CONNECT) {
			return new ConnectPacket().from(bytes);
			
		} else if (type == Packet.Type.CONNACK) {
			return new ConnackPacket().from(bytes);
			
		} else if (type == Packet.Type.SUBSCRIBE) {
			return new SubscribePacket().from(bytes);
			
		} else if (type == Packet.Type.SUBACK) {
			return new SubackPacket().from(bytes);
			
		} else if (type == Packet.Type.UNSUBSCRIBE) {
			return new UnsubscribePacket().from(bytes);
			
		} else if (type == Packet.Type.PINGREQ) {
			return new PingreqPacket().from(bytes);
			
		} else if (type == Packet.Type.DISCONNECT) {
			return new DisconnectPacket().from(bytes);
			
		} else {		
			return new UnknownPacket().from(bytes);
		}
	}
	
	public synchronized List<Packet> build(ByteBuffer bytes) throws IOException {
		this.buffer.write(bytes); // eat it
		
		List<Packet> packets = new ArrayList<Packet>();
		
		int p;		
		while ((p = this.buffer.size()) >= 2) { // current size
			int s = (p > 5)? 5 : p; // 1 type + maximum 4 bytes remaining length
		
			this.header.clear();
			
			int i = 1;			
			for (;i < s;i++) {			
				byte b = this.buffer.get(i);				
				this.header.put(b);				
				if ((b & 128) == 0) {					
					break;
				}
			}
			
			if (i >= 5) { // the last remaining bytes should not be large than 127
				throw new IllegalProtocolException();
				
			} else if (i >= s) { // not enough bytes, to be continued ...
				break;
			}
			
			this.header.flip();
			s = i + 1 + Packet.readRemainingLength(this.header);
			
			if (p >= s) {
				ByteBuffer bb = ByteBuffer.allocate(s);
				this.buffer.read(bb); // 'position' is moved
				bb.flip();
				
				Packet packet = this.transform(bb); // I just want the 'part' of data
				packets.add(packet);
				
			} else { // not enough bytes, to be continued
				break;
			}
		}
		
		return packets;
	}
}
