package com.cht.iot.mqtt;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.mina.core.session.IoSession;

import com.cht.iot.mqtt.protocol.PacketBuilder;

public class MqttSlave {
	final String uid; // session id
	final IoSession session;
	final String connection;
	final long birthday;
	
	Account account;
	String clientId;
	
	final PacketBuilder builder;
	
	Map<String, Object> attributes = new HashMap<String, Object>(); 
	
	BlockingQueue<Payload> payloads = new LinkedBlockingQueue<Payload>();
	
	public MqttSlave(IoSession session, int packetBufferInitialSize) {
		this.session = session;
		
		uid = String.format("slave-%d", session.getId());
		connection = SessionUtils.toString(session);
		birthday = System.currentTimeMillis();
		
		builder = new PacketBuilder(packetBufferInitialSize);
	}
	
	public IoSession getSession() {
		return session;
	}
	
	public String getConnection() {
		return connection;
	}
	
	public long getBirthday() {
		return birthday;
	}
	
	public Account getAccount() {
		return account;
	}
	
	public void setAccount(Account account) {
		this.account = account;
	}
	
	public String getClientId() {
		return clientId;
	}
	
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	
	public PacketBuilder getPacketBuilder() {
		return builder;
	}
	
	public void putAttribute(String key, Object o) {
		attributes.put(key, o);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getAttribute(String key) {
		return (T) attributes.get(key);
	}
	
	public BlockingQueue<Payload> getPayloads() {
		return payloads;
	}
	
	public void close() {
		session.closeOnFlush();
	}
	
	public void write(byte[] bytes) {
		session.write(ByteBuffer.wrap(bytes));
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MqttSlave) {
			MqttSlave s = (MqttSlave) obj;			
			return uid.equals(s.uid);
		}
		
		return false;
	}
	
	@Override
	public String toString() {			
		return String.format("account: %s, clientId: %s, from: %s",
				((account != null) && (account.getUsername() != null))? account.getUsername() : "",
				(clientId != null)? clientId : "",
				connection);
	}
	
	public static class Payload {
		public final String topic;
		public final byte[] payload;
		
		public Payload(String topic, byte[] payload) {
			this.topic = topic;
			this.payload = payload;
		}
	}
}
