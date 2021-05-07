package com.cht.iot.mqtt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TopicRoom {
	final String topic;
	final Set<MqttSlave> subscribers = Collections.synchronizedSet(new HashSet<>());
	
	public TopicRoom(String topic) {
		this.topic = topic;
	}
	
	public String getTopic() {
		return topic;
	}
	
	public void addSubscriber(MqttSlave subscriber) {
		subscribers.add(subscriber);
	}
	
	public void removeSubscriber(MqttSlave subscriber) {
		subscribers.remove(subscriber);
	}
	
	public List<MqttSlave> getSubscribers() {
		synchronized (subscribers) {		
			List<MqttSlave> slaves = new ArrayList<>(subscribers.size());
			slaves.addAll(subscribers);			
			
			return slaves; // don't lock 'subscribers'
		}
	}
	
	public boolean isEmpty() {
		return subscribers.isEmpty();
	}
	
	@Override
	public String toString() {
		return String.format("%s : %d", topic, subscribers.size());
	}
}
