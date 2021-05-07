package com.cht.iot;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cht.iot.mqtt.MyBroker;
import com.cht.iot.mqtt.MyBrokerImpl;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MainServiceImpl {

	@Value("${broker.executor-core-pool-size:100}")
	int executorCorePoolSize;
	
	@Value("${broker.executor-max-pool-size:1000}")
	int executorMaxPoolSize;
	
	@Value("${broker.session-authentication-timeout-in-seconds:5}")
	int authenticationTimeout;
	
	@Value("${broker.session-idle-timeout-in-seconds:90}")
	int idleTimeout;
	
	MyBrokerImpl broker;
	
	@Autowired(required = false)
	MyBroker.Listener listener;
	
	public MainServiceImpl() {
	}
	
	// ======
	
	@PostConstruct
	void start() throws Exception {
		broker = new MyBrokerImpl();
		broker.setExecutorCorePoolSize(executorCorePoolSize);
		broker.setExecutorMaxPoolSize(executorMaxPoolSize);
		broker.setAuthenticationTimeout(authenticationTimeout);
		broker.setIdleTimeout(idleTimeout);
		
		if (listener != null) {
			log.info("Set Listener - {}", listener);			
			broker.setListener(listener);
		}
		
		log.info("ExecutorCorePoolSize: {}, ExecutorMaxPoolSize: {}, IdleTimeout: {}s", executorCorePoolSize, executorMaxPoolSize, idleTimeout);
		
		broker.start();
	}
	
	@PreDestroy
	void stop() throws Exception {
		broker.stop();
	}
}
