package com.cht.iot.mqtt;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.executor.IoEventQueueThrottle;
import org.apache.mina.filter.executor.OrderedThreadPoolExecutor;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cht.iot.mqtt.protocol.ConnackPacket;
import com.cht.iot.mqtt.protocol.ConnectPacket;
import com.cht.iot.mqtt.protocol.DisconnectPacket;
import com.cht.iot.mqtt.protocol.IllegalProtocolException;
import com.cht.iot.mqtt.protocol.Packet;
import com.cht.iot.mqtt.protocol.PacketBuilder;
import com.cht.iot.mqtt.protocol.PingreqPacket;
import com.cht.iot.mqtt.protocol.PingrespPacket;
import com.cht.iot.mqtt.protocol.PubackPacket;
import com.cht.iot.mqtt.protocol.PubcompPacket;
import com.cht.iot.mqtt.protocol.PublishPacket;
import com.cht.iot.mqtt.protocol.SubackPacket;
import com.cht.iot.mqtt.protocol.SubscribePacket;
import com.cht.iot.mqtt.protocol.UnsubackPacket;
import com.cht.iot.mqtt.protocol.UnsubscribePacket;

public class MyBrokerImpl implements MyBroker {
	static final Logger LOG = LoggerFactory.getLogger(MyBrokerImpl.class);
	
	int executorCorePoolSize = 100;
	int executorMaxPoolSize = 1000;
	int executorKeepAliveTime = 60;
	
	int port = 1883;
	IoAcceptor acceptor;
	int eventQueueThrottle = 8192;
	
	int authenticationTimeout = 5; // login timeout in seconds
	int idleTimeout = 90; // idle timeout in seconds
	int packetBufferInitialSize = 1000;
	
	Listener listener = new Listener() {		
		public boolean challenge(MqttSlave slave, String username, String password) { return true; }		
		public void onSlaveArrived(MqttSlave slave) {}
		public void onSlaveExited(MqttSlave slave) {}		
		public void onSubscribe(MqttSlave slave, String topic) {};		
		public void onMessage(MqttSlave slave, String topic, byte[] payload) {};
	};
	
	Map<String, TopicRoom> rooms = Collections.synchronizedMap(new HashMap<String, TopicRoom>());
	
	OrderedThreadPoolExecutor executor;
	
	public MyBrokerImpl() {	
	}
	
	/**
	 * How many threads are ready.
	 * 
	 * @param executorCorePoolSize
	 */
	public void setExecutorCorePoolSize(int executorCorePoolSize) {
		this.executorCorePoolSize = executorCorePoolSize;
	}
	
	/**
	 * Maximum number of threads will be created.
	 * 
	 * @param executorMaxPoolSize
	 */
	public void setExecutorMaxPoolSize(int executorMaxPoolSize) {
		this.executorMaxPoolSize = executorMaxPoolSize;
	}
	
	/**
	 * Thread's idle timeout in seconds;
	 * 
	 * @param executorKeepAliveTime
	 */	
	public void setExecutorKeepAliveTime(int executorKeepAliveTime) {
		this.executorKeepAliveTime = executorKeepAliveTime;
	}
	
	/**
	 * Set the MQTT listening port.
	 * 
	 * @param port
	 */
	public void setPort(int port) {
		this.port = port;
	}
	
	/**
	 * Set the flow control. Default is 8192.
	 * 
	 * @param eventQueueThrottle
	 */
	public void setEventQueueThrottle(int eventQueueThrottle) {
		this.eventQueueThrottle = eventQueueThrottle;
	}
	
	/**
	 * Set authentication timeout in seconds. default is 5 seconds
	 * 
	 * @param authenticationTimeout
	 */
	public void setAuthenticationTimeout(int authenticationTimeout) {
		this.authenticationTimeout = authenticationTimeout;
	}
	
	/**
	 * Set session idle timeout in seconds. default is 90 seconds
	 * 
	 * @param timeout
	 */
	public void setIdleTimeout(int idleTimeout) {
		this.idleTimeout = idleTimeout;
	}
	
	/**
	 * Packet buffer size per session. Default is 1000.
	 * 
	 * @param packetBufferSize
	 */
	public void setPacketBufferInitialSize(int packetBufferInitialSize) {
		this.packetBufferInitialSize = packetBufferInitialSize;
	}
	
	public void setListener(Listener listener) {
		this.listener = listener;
	}
	
	// ======
	
	@PostConstruct
	public void start() throws IOException, GeneralSecurityException {
		acceptor = new NioSocketAcceptor(); // Mina Server		

		executor = new OrderedThreadPoolExecutor(
				executorCorePoolSize,
				executorMaxPoolSize,
				executorKeepAliveTime, TimeUnit.SECONDS,
				Executors.defaultThreadFactory(), new IoEventQueueThrottle(eventQueueThrottle));
		
		acceptor.getFilterChain().addLast("executor", new ExecutorFilter(executor));
		
		ProtocolCodecFactory pcf = new CodecFactory();
		ProtocolCodecFilter filter = new ProtocolCodecFilter(pcf);
		acceptor.getFilterChain().addLast("mqtt", filter);
		
		IoHandler handler = new ServerHandler();		
		acceptor.setHandler(handler);
		
		acceptor.bind(new InetSocketAddress(port));
		
		LOG.info("Listens at {}", port);
	}
	
	@PreDestroy
	public void stop() {		
		LOG.info("Shutdown the broker");
		
		for (IoSession s : acceptor.getManagedSessions().values()) {
			s.closeNow();
		}
		
		acceptor.unbind();		
		acceptor.dispose();
		
		executor.shutdown();
	}
		
	// ======
	
	/**
	 * Assign a 'Slave' into the connected session.
	 * 
	 * @param session
	 * @return
	 */
	MqttSlave register(IoSession session) {
		MqttSlave slave = new MqttSlave(session, packetBufferInitialSize);
		session.setAttribute("slave", slave);
		
		listener.onSlaveArrived(slave);
		
		return slave;
	}
	
	/**
	 * Get 'Slave' from session.
	 * 
	 * @param session
	 * @return
	 */
	
	MqttSlave getSlave(IoSession session) {
		return (MqttSlave) session.getAttribute("slave");
	}
	
	/**
	 * Fire the advisory message for session disconnection.
	 * 
	 * @param session
	 * @return
	 */
	void unregister(IoSession session) {
		MqttSlave slave = getSlave(session);
		if (slave == null) {
			LOG.error("Failed to get MqttSlave from - {}", SessionUtils.toString(session));			
			return;
		}
		
		for (String topic : slave.getTopics()) {
			TopicRoom room = rooms.get(topic);
			if (room == null) {
				continue;
			}
			
			room.removeSubscriber(session);
			removeTopicRoomIfEmpty(room);
		}			
		
		listener.onSlaveExited(slave);
	}
	
	void removeTopicRoomIfEmpty(TopicRoom room) {
		if (room.isEmpty()) { // FIXME - another subscriber could be added after this statement
			String topic = room.getTopic();				
			rooms.remove(topic);					
			LOG.info("Remove the empty topic - {}", topic);
		}
	}
	
	// ======

	@Override
	public void publish(String topic, byte[] payload) {
		publish(topic, ByteBuffer.wrap(payload));
	}
	
	void publish(final String topic, final ByteBuffer message) {
		try {
			dispatch(topic, message); // publish the message to the local subscribers
	        
		} catch (Exception e) {
			LOG.error("Failed to publish message to topic - " + topic, e);
		}		
	}
	
	void write(final IoSession session, final ByteBuffer[] buffers) {
		try {
			for (ByteBuffer buffer : buffers) {							
				session.write(buffer.slice()); // send the message
			}
			
		} catch (Exception e) {
			LOG.error("Failed to send message to " + SessionUtils.toString(session), e);
		}
	}
	
	void write(final IoSession session, final ByteBuffer buffer) {
		session.write(buffer);
	}
	
	/**
	 * Dispatch the message to the local subscribers.
	 * 	
	 * @param topic
	 * @param message
	 * @throws IOException
	 */
	void dispatch(String topic, ByteBuffer message) throws IOException {
		TopicRoom room = rooms.get(topic);
		if (room != null) {
			PublishPacket pkt = new PublishPacket();
			pkt.setTopic(topic);
			pkt.setMessage(message);

			ByteBuffer[] buffers = pkt.getByteBuffers();
			
			for (IoSession subscriber : room.getSubscribers()) { // return the WHOLE NEW subscriber list
				if (SessionUtils.isOkay(subscriber)) {
					write(subscriber, buffers);
				}
			}			
		}
	}
	
	// ======
	
	/**
	 * Register the new subscriber.
	 * 
	 * @param topic
	 * @param subscriber
	 * @throws IOException
	 */
	void subscribe(IoSession subscriber, MqttSlave slave, String topic) throws IOException {
		TopicRoom room;
		
		synchronized (rooms) {
			room = rooms.get(topic);
			if (room == null) {
				room = new TopicRoom(topic);
				rooms.put(topic, room);
			}
		}	
		
		room.addSubscriber(subscriber); // FIXME - the 'room' could be removed from 'rooms' by other threads
		
		slave.getTopics().add(topic);
	}
	
	/**
	 * Un-register the subscriber.
	 * 
	 * @param subscriber
	 * @param slave
	 * @param topic
	 * @throws IOException
	 */	
	void unsubscribe(IoSession subscriber, MqttSlave slave, String topic) throws IOException {
		TopicRoom room = rooms.get(topic);
		if (room != null) {			
			room.removeSubscriber(subscriber);
			removeTopicRoomIfEmpty(room);
			
			slave.getTopics().remove(topic);
		}
	}
	
	// ======
	
	// receive the packets from client side
	void handle(IoSession session, ByteBuffer bytes) throws IOException, InterruptedException {
		MqttSlave slave = getSlave(session);
		if (slave == null) {
			LOG.error("Failed to get MqttSlave from - {}", SessionUtils.toString(session));			
			return;
		}		
		
		PacketBuilder builder = slave.getPacketBuilder();
		List<Packet> packets = builder.build(bytes);
		handle(session, slave, packets);
	}
	
	// handle the MQTT packets
	void handle(IoSession session, MqttSlave slave, List<Packet> packets) throws IOException, InterruptedException {
		for (Packet pkt : packets) {
			if (pkt instanceof PublishPacket) { // publish (notice the issue of memory leak)
				PublishPacket req = (PublishPacket) pkt;
				
				String topic = req.getTopic();
				ByteBuffer message = req.getMessage();				
				
				// send messages from clients to internal service
				byte[] bytes = new byte[message.remaining()];
				message.get(bytes);
				listener.onMessage(slave, topic, bytes); // HINT - I'll not receive the messages from myself
								
				// build the message again
				message = req.getMessage();
				publish(topic, message); // publish the message to local subscribers
					
				int qos = req.getQoS();
				if (qos > 0) {				
					PubackPacket res = new PubackPacket();
					res.setPacketIdentifier(req.getPacketIdentifier());
					
					write(session, res.getByteBuffer());
				}				
				
			} else if (pkt instanceof PubackPacket) { // TODO - QoS 2
				
			} else if (pkt instanceof PubcompPacket) { // TODO - QoS 2								
				
			} else if (pkt instanceof ConnectPacket) { // connect
				ConnectPacket req = (ConnectPacket) pkt;
				
				Account account = new Account(req.getUsername(), req.getPassword());
				slave.setAccount(account);
				slave.setClientId(req.getClientId());
				
				if (listener.challenge(slave, req.getUsername(), req.getPassword()) == false) { // no accepted
					ConnackPacket res = new ConnackPacket();
					res.setSessionPresent(false);
					res.setReturnCode(ConnackPacket.ReturnCode.UNAUTHENTICATED);					
					write(session, res.getByteBuffer());
					
					throw new PermissionException();
				}
				
				// assign the reasonable timeout now
				session.getConfig().setReaderIdleTime(idleTimeout); 
				session.getConfig().setWriterIdleTime(idleTimeout);
				
				ConnackPacket res = new ConnackPacket();
				res.setSessionPresent(false);
				res.setReturnCode(ConnackPacket.ReturnCode.ACCEPTED);
				
				write(session, res.getByteBuffer());
				
				listener.onSlaveArrived(slave); // HINT - could throw the PermissionException here
					
			} else if (pkt instanceof SubscribePacket) { // subscribe
				SubscribePacket req = (SubscribePacket) pkt;
				
				List<SubscribePacket.Topic> topics = req.getTopics();
				for (SubscribePacket.Topic topic : topics) {
					String tf = topic.getTopicFilter();
					
					listener.onSubscribe(slave, tf); // HINT - could throw the PermissionException here
					
					subscribe(session, slave, tf);
				}
					
				SubackPacket res = new SubackPacket();
				res.setPacketIdentifier(req.getPacketIdentifier());
				res.setReturnCode(SubackPacket.ReturnCode.QOS0);				
				write(session, res.getByteBuffer());
				
			} else if (pkt instanceof UnsubscribePacket) { // unsubscribe
				UnsubscribePacket req = (UnsubscribePacket) pkt;
				
				for (String tf : req.getTopicFilters()) {
					unsubscribe(session, slave, tf);
				}
				
				UnsubackPacket res = new UnsubackPacket();
				res.setPacketIdentifier(req.getPacketIdentifier());
				write(session, res.getByteBuffer());				
				
			} else if (pkt instanceof PingreqPacket) { // ping
				PingrespPacket res = new PingrespPacket();
				
				write(session, res.getByteBuffer());
				
			} else if (pkt instanceof DisconnectPacket) {
				
			} else {
				throw new IllegalProtocolException(pkt.getClass().getSimpleName() + " is not yet supported - " + pkt.getType());
			}
		}
	}
	
	// ====== Mina Server ======
	
	class ServerHandler extends IoHandlerAdapter {
		
		@Override
		public void sessionOpened(IoSession session) throws Exception {
			if (SessionUtils.setup(session) == false) {
				LOG.error("Don't accept the broken session");				
				session.closeNow();
				return;
			}
			
			SocketSessionConfig cfg = (SocketSessionConfig) session.getConfig();
			cfg.setReaderIdleTime(authenticationTimeout);
			cfg.setSoLinger(0); // avoid TIME_WAIT problem			
			
			MqttSlave slave = register(session); // every session must has 'from' and 'slave'

			LOG.info(String.format("Connected - %s, sessions: %,d, topics: %,d, active: %,d, free: %,d bytes",
					slave.getConnection(),
					acceptor.getManagedSessionCount(),
					rooms.size(),
					executor.getActiveCount(),
					Runtime.getRuntime().freeMemory()
					));
			
//			LOG.info("Connected - {}", slave.getConnection());
//			LOG.info(String.format("free: %,d bytes", Runtime.getRuntime().freeMemory()));
//			LOG.info(String.format("topics: %,d", rooms.size()));
//			LOG.info(String.format("active: %,d", executor.getActiveCount()));
//			LOG.info(String.format("sessions: %,d", count));
		}
		
		@Override
		public void messageReceived(IoSession session, Object message) throws Exception {
			ByteBuffer bytes = (ByteBuffer) message;
			try {			
				handle(session, bytes);				
				
			} catch (IllegalProtocolException e) {
				LOG.error("Unsupported MQTT packet is from - " + getSlave(session), e);
				
			} catch (PermissionException e) {
				LOG.error("Permission denied from - " + getSlave(session));
				
				session.closeNow();
				
			} catch (Exception e) {
				LOG.error("Failed to handle the MQTT packet from - " + getSlave(session), e);
			}					
		}
		
		@Override
		public void sessionIdle(final IoSession session, IdleStatus status) throws Exception {
			LOG.info("Idle - {}", getSlave(session));
			
			session.closeNow();
		}
		
		@Override
		public void exceptionCaught(final IoSession session, final Throwable cause) throws Exception {
			LOG.error("[{}] {} - {}", getSlave(session), cause.getClass().getSimpleName(), cause.getMessage());
			
			session.closeNow();
		}
		
		@Override
		public void sessionClosed(final IoSession session) throws Exception {
			LOG.info("Disconnected - {}", getSlave(session));	// session will be recycled by Mina Server, remove it from memory during 'dispatch()'
			
			unregister(session);
		}
	}
	
	static class Encoder implements ProtocolEncoder {
		
		@Override
		public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
			ByteBuffer bytes = (ByteBuffer) message; // ByteBuffer to IoBuffer
			IoBuffer ib = IoBuffer.wrap(bytes);			
			out.write(ib);			
		}
		
		@Override
		public void dispose(IoSession session) throws Exception {			
		}
	}
	
	static class Decoder implements ProtocolDecoder {

		@Override
		public void decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
			ByteBuffer bb = ByteBuffer.wrap(in.array(), in.position(), in.remaining());
			in.position(in.limit()); // eat them all
			out.write(bb);
		}
	
		@Override
		public void finishDecode(IoSession session, ProtocolDecoderOutput out) throws Exception {
		}
	
		@Override
		public void dispose(IoSession session) throws Exception {			
		}		
	}
	
	static class CodecFactory implements ProtocolCodecFactory {
		final ProtocolEncoder encoder = new Encoder();
		final ProtocolDecoder decoder = new Decoder();
		
		@Override
		public ProtocolEncoder getEncoder(IoSession session) throws Exception {
			return encoder;
		}
		
		@Override
		public ProtocolDecoder getDecoder(IoSession session) throws Exception {
			return decoder;
		}
	}	
}
