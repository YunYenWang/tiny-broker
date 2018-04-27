package com.cht.iot.mqtt.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ConnectPacket extends Packet {
	protected String protocolName; // 'MQTT'
	protected int protocolLevel; // 4
	protected int connectionFlags;
	protected int keepAlive; // in second
	
	// payload
	protected String clientId;
	protected String willTopic;
	protected String willMessage;
	
	protected String username;
	protected String password;
	
	public ConnectPacket() {
		super(Packet.Type.CONNECT);
	}

	public String getProtocolName() {
		return protocolName;
	}

	public void setProtocolName(String protocolName) {
		this.protocolName = protocolName;
	}

	public int getProtocolLevel() {
		return protocolLevel;
	}

	public void setProtocolLevel(int protocolLevel) {
		this.protocolLevel = protocolLevel;
	}

	public int getConnectionFlags() {
		return connectionFlags;
	}

	public void setConnectionFlags(int connectionFlags) {
		this.connectionFlags = connectionFlags;
	}
	
	public int getKeepAlive() {
		return keepAlive;
	}
	
	public void setKeepAlive(int keepAlive) {
		this.keepAlive = keepAlive;
	}
	
	public String getClientId() {
		return clientId;
	}
	
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	
	public String getWillTopic() {
		return willTopic;
	}
	
	public String getWillMessage() {
		return willMessage;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public boolean hasUsername() {
		return (this.connectionFlags & 0x0080) != 0;
	}
	
	public boolean hasPassword() {
		return (this.connectionFlags & 0x0040) != 0;
	}
	
	public boolean hasWillRetain() {
		return (this.connectionFlags & 0x0020) != 0;
	}
	
	public int getWillQos() {
		return (this.connectionFlags & 0x0018) >> 3;
	}
	
	public boolean hasCleanSession() {
		return (this.connectionFlags & 0x0002) != 0;
	}
	
	// ======
	
	@Override
	public Packet from(ByteBuffer bytes) throws IOException {
		super.from(bytes);
		
		this.protocolName = Packet.readString(bytes);
		
		this.protocolLevel = bytes.get() & 0x00FF;
		
		this.connectionFlags = bytes.get() & 0x00FF;
		
		this.keepAlive = bytes.getShort();
		
		this.clientId = Packet.readString(bytes);
		
		if (this.hasWillRetain()) {
			this.willTopic = Packet.readString(bytes);
			this.willMessage = Packet.readString(bytes);
		}
		
		if (this.hasUsername()) {
			this.username = Packet.readString(bytes);
		}
		
		if (this.hasPassword()) {
			this.password = Packet.readString(bytes);
		}
		
		return this;
	}
	
	@Override
	protected ByteBuffer body() {
		ByteBuffer bytes = ByteBuffer.allocate(64);
		
		bytes.put(Packet.toStringBytes(this.protocolName));
		bytes.put((byte) this.protocolLevel);
		bytes.put((byte) this.connectionFlags);
		bytes.putShort((short) this.keepAlive);
		bytes.put(Packet.toStringBytes(this.clientId));
		
		if (this.hasWillRetain()) {
			bytes.put(Packet.toStringBytes(this.willTopic));
			bytes.put(Packet.toStringBytes(this.willMessage));
		}
		
		if (this.hasUsername()) {
			bytes.put(Packet.toStringBytes(this.username));
		}
		
		if (this.hasPassword()) {
			bytes.put(Packet.toStringBytes(this.password));
		}
		
		bytes.flip();
		
		return bytes;
		
//		throw new UnsupportedOperationException("not yet implemented");
	}
}