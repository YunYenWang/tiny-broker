package com.cht.iot.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MqttBrokerTest {	
	
	String serverURI = "tcp://localhost:1883";
	
	@Test
	void test() throws Exception {
		try (MyBrokerImpl broker = new MyBrokerImpl()) {
			broker.setPort(1883);
			broker.setAuthenticationTimeout(5); // seconds
			broker.setIdleTimeout(90); // client's idle time-out in seconds
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
					log.info("[SERVER] '{}' is subscribed by {}", topic, slave.getConnection());
				}
	
				@Override
				public void onMessage(MqttSlave slave, String topic, byte[] payload) {
					log.info("[SERVER] got the message from {} : {}", slave.getConnection(), new String(payload));// message from client
					
				}
				
			});
			broker.start();
			
			String topic = "test";
			
			try (MqttClient client = newMqttClient()) {
				client.subscribe(topic);
				
				client.publish(topic, new MqttMessage("Hello".getBytes()));
				
				client.disconnect();
			}
			
			Thread.sleep(1_000L);
		}
	}
	
	MqttClient newMqttClient() throws MqttException {		
		String clientId = MqttClient.generateClientId();
		MqttClientPersistence persistence = new MemoryPersistence();
		
		MqttClient client = new MqttClient(serverURI, clientId, persistence);
		client.setCallback(new MqttCallback() {
			
			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				log.info("[CLIENT] got the message from SERVER: {}", new String(message.getPayload()));
			}
			
			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
			}
			
			@Override
			public void connectionLost(Throwable cause) {
			}
			
		});
		
		MqttConnectOptions opt = new MqttConnectOptions();		
		opt.setUserName("");
		opt.setPassword(new char[0]);
		opt.setConnectionTimeout(60);
		opt.setKeepAliveInterval(60);
		opt.setCleanSession(true);
		
		client.connect(opt);
		
		return client;
	}
}
