package com.cht.iot.mqtt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.mina.core.session.IoSession;

public class TopicRoom {
	final String topic;
	Set<IoSession> subscribers = new HashSet<IoSession>();
	
	public TopicRoom(String topic) {
		this.topic = topic;
	}
	
	public String getTopic() {
		return topic;
	}
	
	public synchronized void addSubscriber(IoSession subscriber) {
		subscribers.add(subscriber);
	}
	
	public synchronized void removeSubscriber(IoSession subscriber) {
		subscribers.remove(subscriber);
	}
	
	public synchronized List<IoSession> getSubscribers() {
		List<IoSession> sessions = new ArrayList<>(subscribers.size());
		sessions.addAll(subscribers);			
		
		return sessions;
	}
	
	public synchronized boolean isEmpty() {
		return subscribers.isEmpty();
	}
	
	@Override
	public String toString() {
		return String.format("%s : %d", topic, subscribers.size());
	}
}
