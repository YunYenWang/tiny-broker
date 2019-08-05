package com.cht.iot.opt;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.cht.iot.mqtt.MqttSlave;
import com.cht.iot.mqtt.MyBroker;

@Component
@Profile("defence")
public class AntiAttackHandler implements MyBroker.Listener {

	@Override
	public boolean challenge(MqttSlave slave, String username, String password) {
		if ((password == null) || (password.isEmpty())) {
			return false;
		}
		
		return true;
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
	}
}
