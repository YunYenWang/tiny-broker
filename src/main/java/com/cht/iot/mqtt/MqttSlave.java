package com.cht.iot.mqtt;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.mina.core.session.IoSession;

public class MqttSlave {
	final String uid; // session id
	final IoSession session;
	final String connection;
	final long birthday;
	
	String id = "";
	
	Map<String, Object> attributes = new HashMap<String, Object>(); 
	
	// fair lock
//	ReentrantLock lock = new ReentrantLock(true);
	// fairness lock
	ReentrantLock lock = new ReentrantLock();
	BlockingQueue<Payload> payloads = new LinkedBlockingQueue<Payload>();
	
	public MqttSlave(IoSession session) {
		this.uid = String.format("slave-%d", session.getId());		
		this.session = session;			
		this.connection = toString(session);
		this.birthday = System.currentTimeMillis();
	}
	
	public String getUid() {
		return uid;
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
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public void putAttribute(String key, Object o) {
		attributes.put(key, o);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getAttribute(String key) {
		return (T) attributes.get(key);
	}
	
	public ReentrantLock getLock() {
		return lock;
	}
	
	public BlockingQueue<Payload> getPayloads() {
		return payloads;
	}
	
	public void close() {
		session.closeOnFlush();
	}
	
	public synchronized void write(byte[] bytes) {
		session.write(ByteBuffer.wrap(bytes));
	}
	
	public static final String toString(IoSession session) {
		String from = (String) session.getAttribute("from");
		if (from != null) {
			return from;
			
		} else {
			return session.toString();
		}		
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
		return String.format("[%s] %s", id, connection);
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
