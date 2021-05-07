package com.cht.iot.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MqttBrokerTest {	
	
	@Test
	void test() throws Exception {
		try (MyBrokerImpl broker = new MyBrokerImpl()) {
			broker.setPort(1883);
			broker.setListener(new MyBroker.Listener() {
	
				@Override
				public boolean challenge(MqttSlave slave, String username, String password) {
					return true; // accept the anonymous
				}
	
				@Override
				public void onSlaveArrived(MqttSlave slave) {
					
				}
	
				@Override
				public void onSlaveExited(MqttSlave slave) {
				}
	
				@Override
				public void onSubscribe(MqttSlave slave, String topic) {
				}
	
				@Override
				public void onMessage(MqttSlave slave, String topic, byte[] payload) {
					// message from client
				}
				
			});
			broker.start();
			
			String topic = "test";
			
			try (MqttClient client = newMqttClient()) {
				client.subscribe(topic);
				
				client.publish(topic, new MqttMessage("Hello".getBytes()));
			}
			
			Thread.sleep(1000);
		}
	}
	
	MqttClient newMqttClient() throws MqttException {
		String serverURI = "tcp://localhost:1883";
		String clientId = MqttClient.generateClientId();
		MqttClientPersistence persistence = new MemoryPersistence();
		
		MqttClient client = new MqttClient(serverURI, clientId, persistence);
		client.setCallback(new MqttCallback() {
			
			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				log.info("topic: {}, message: {}", topic, new String(message.getPayload()));
			}
			
			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
			}
			
			@Override
			public void connectionLost(Throwable cause) {
			}
		});
		client.connect();
		
		return client;
	}
}
