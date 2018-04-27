package com.cht.iot.mqtt;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.mina.core.session.IoSession;

public class TopicRoom {
	final String topic;
	List<WeakReference<IoSession>> subscribers = Collections.synchronizedList(new ArrayList<WeakReference<IoSession>>());
	
	public TopicRoom(String topic) {
		this.topic = topic;
	}
	
	public String getTopic() {
		return topic;
	}
	
	public void addSubscriber(IoSession subscriber) {
		this.subscribers.add(new WeakReference<IoSession>(subscriber));
	}
	
	public List<WeakReference<IoSession>> getSubscribers() {
		return this.subscribers;
	}
	
	@Override
	public String toString() {
		return String.format("%s : %d", topic, subscribers.size());
	}
}
