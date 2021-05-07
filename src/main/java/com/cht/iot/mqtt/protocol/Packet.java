package com.cht.iot.mqtt.protocol;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public abstract class Packet {
	public static final Type[] TYPES = new Type[] {
		Type.RESERVED,
		Type.CONNECT, Type.CONNACK,
		Type.PUBLISH, Type.PUBACK, Type.PUBREC, Type.PUBREL, Type.PUBCOMP,
		Type.SUBSCRIBE, Type.SUBACK, Type.UNSUBSCRIBE, Type.UNSUBACK,
		Type.PINGREQ, Type.PINGRESP,
		Type.DISCONNECT,
		Type.RESERVED
	};
	
	public static final String UTF8 = "UTF-8";
	
	int type; // 4 bits
	int flags; // 4 bits
	int length;
	
	public Packet(int type) {
		this.type = type;
	}
	
	public Packet(Type type) {
		this.type = type.getId();
	}
	
	// ======
	
	public int getType() {
		return type;
	}
	
	public void setType(int type) {
		this.type = type;
	}
	
	public void setType(Type type) {
		this.type = type.getId();
	}
	
	public int getFlags() {
		return flags;
	}
	
	public void setFlags(int flags) {
		this.flags = flags;
	}
	
	public boolean isDuplicated() {
		return ((flags & 0x08) != 0);
	}
	
	public int getQoS() {
		return ((flags & 0x06) >> 1);
	}
	
	public boolean isRetain() {
		return ((flags & 0x01) != 0);
	}
	
	public int getLength() {
		return length;
	}
	
	public void setLength(int length) {
		this.length = length;
	}
	
	// ======
	
	public Packet from(ByteBuffer bytes) throws IOException { // the sub-class should read the other fields
		byte h = bytes.get();
		
		type = (h & 0x0F0) >> 4;
		flags = h & 0x00F;		
		
		length = Packet.readRemainingLength(bytes);
		
		return this;
	}
	
	abstract ByteBuffer body() throws IOException;
	
	public final ByteBuffer toByteBuffer() throws IOException {
		ByteBuffer body = body();
		length = body.remaining();
		
		ByteBuffer size = Packet.toRemainingLength(length);
		ByteBuffer bytes = ByteBuffer.allocate(1 + size.remaining() + length);		
		
		bytes.put((byte) (((type & 0x0FF) << 4) | (flags)));
		bytes.put(size);
		bytes.put(body);
		
		bytes.flip();
		
		return bytes;
	}
	
	// ======
	
	public static final ByteBuffer toRemainingLength(int length) {
		ByteBuffer bytes = ByteBuffer.allocate(4);
		do {
			int b = length % 128;
			length = length / 128;
			if (length > 0) {
				b = b | 0x080;
			}
			
			bytes.put((byte) b);
			
		} while (length > 0);
		
		bytes.flip();
		
		return bytes;
	}
	
	public static final int readRemainingLength(ByteBuffer bytes) {
		int m = 1;
		int v = 0;
		int b = 0;
		do {
			b = bytes.get() & 0x0FF;
			v += ((b & 0x07F) * m);
			m *= 0x080;
			
		} while ((b & 0x080) != 0);
		
		return v;
	}	
	
	// ======
	
	public static final String readString(ByteBuffer bytes) throws IOException {
		int s = bytes.getShort();
		byte[] d = new byte[s];
		bytes.get(d);
		
		return new String(d, UTF8);
	}
	
	public static final void writeString(ByteBuffer bytes, String string) throws IOException {
		byte[] d = string.getBytes(UTF8);
		bytes.putShort((short) d.length);
		bytes.put(d);
	}
	
	public static final byte[] toStringBytes(String string) throws UnsupportedEncodingException {
		byte[] d = string.getBytes(UTF8);
		byte[] bytes = new byte[2 + d.length];
		
		bytes[0] = (byte) ((d.length & 0x0FF00) >> 8);
		bytes[1] = (byte) (d.length & 0x000FF);
		
		System.arraycopy(d, 0, bytes, 2, d.length);
		
		return bytes;
	}
	
	// ======
	
	public static enum Type {
		RESERVED(0),
		CONNECT(1), CONNACK(2),
		PUBLISH(3), PUBACK(4), PUBREC(5), PUBREL(6), PUBCOMP(7),
		SUBSCRIBE(8), SUBACK(9), UNSUBSCRIBE(10), UNSUBACK(11),
		PINGREQ(12), PINGRESP(13),
		DISCONNECT(14);
		
		final int id;
		
		private Type(int id) {
			this.id = id;
		}
		
		public int getId() {
			return id;
		}
	}
}
