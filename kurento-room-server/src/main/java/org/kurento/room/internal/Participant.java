/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package org.kurento.room.internal;

import static org.kurento.room.internal.ThreadLogUtils.updateThreadName;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import org.kurento.client.Continuation;
import org.kurento.client.IceCandidate;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.client.internal.server.KurentoServerException;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.kurento.room.api.ParticipantSession;
import org.kurento.room.endpoint.IceWebRtcEndpoint;
import org.kurento.room.endpoint.PublisherEndpoint;
import org.kurento.room.endpoint.SubscriberEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @author Ivan Gracia (izanmail@gmail.com)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 1.0.0
 */
@Configurable
public class Participant {

	private static final Logger log = LoggerFactory
			.getLogger(Participant.class);

	private final String name;
	private final Room room;

	private final ParticipantSession session;
	private final MediaPipeline pipeline;

	private PublisherEndpoint publisher;
	private CountDownLatch endPointLatch = new CountDownLatch(1);

	private final ConcurrentMap<String, SubscriberEndpoint> subscribers = new ConcurrentHashMap<String, SubscriberEndpoint>();

	private BlockingQueue<Request<JsonObject>> messages = new ArrayBlockingQueue<>(
			10);
	private Thread senderThread;

	private BlockingQueue<RpcNotification> notifications = new ArrayBlockingQueue<>(
			10);
	private Thread notifThread;

	private volatile boolean streaming = false;
	private volatile boolean closed;

	public Participant(String name, Room room, ParticipantSession session,
			MediaPipeline pipeline) {

		this.pipeline = pipeline;
		this.name = name;
		this.session = session;
		this.room = room;
		this.publisher = new PublisherEndpoint(this, name, pipeline);

		this.senderThread = new Thread("sender:" + name) {
			@Override
			public void run() {
				try {
					internalSendMessage();
				} catch (InterruptedException e) {
					return;
				}
			}
		};

		this.notifThread = new Thread("notif:" + name) {
			@Override
			public void run() {
				try {
					internalSendNotification();
				} catch (InterruptedException e) {
					return;
				}
			}
		};

		for (Participant other : room.getParticipants())
			if (!other.getName().equals(this.name))
				subscribers.put(other.getName(), new SubscriberEndpoint(this,
						other.getName(), pipeline));

		this.senderThread.start();
		this.notifThread.start();
	}

	public void createReceivingEndpoint() {
		this.publisher.createEndpoint();
		endPointLatch.countDown();
	}

	public String getName() {
		return name;
	}

	/**
	 * @return the session
	 */
	public ParticipantSession getSession() {
		return session;
	}

	public PublisherEndpoint getPublisher() {
		try {
			endPointLatch.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return this.publisher;
	}

	public Room getRoom() {
		return this.room;
	}

	public String publishToRoom(String sdpOffer) {
		log.info("USER {}: Request to publish video in room {}", this.name,
				this.room.getName());
		log.trace("USER {}: Publishing SdpOffer is {}", this.name, sdpOffer);
		String sdpAnswer = this.getPublisher().publish(sdpOffer);
		log.trace("USER {}: Publishing SdpAnswer is {}", this.name, sdpAnswer);
		log.info("USER {}: Is now publishing video in room {}", this.name,
				this.room.getName());
		this.streaming = true;
		return sdpAnswer;
	}

	public String receiveVideoFrom(Participant sender, String sdpOffer) {
		final String senderName = sender.getName();

		log.info("USER {}: Request to receive video from {} in room {}",
				this.name, senderName, this.room.getName());
		log.trace("USER {}: SdpOffer for {} is {}", this.name, senderName,
				sdpOffer);

		if (sender.getPublisher() == null) {
			log.warn(
					"PARTICIPANT {}: Trying to connect to a user without receiving endpoint (it seems is not yet fully connected)",
					this.name);
			return null;
		}

		if (senderName.equals(this.name)) {
			// FIXME: Use another message type for receiving sdp offer
			log.debug("PARTICIPANT {}: configuring loopback", this.name);
			// TODO throw exception???
			//			return null;
		}

		log.debug("PARTICIPANT {}: Creating a subscriber endpoint to user {}",
				this.name, senderName);

		SubscriberEndpoint subscriber = new SubscriberEndpoint(this,
				senderName, pipeline);
		SubscriberEndpoint oldSubscriber = this.subscribers.putIfAbsent(
				senderName, subscriber);
		if (oldSubscriber != null)
			subscriber = oldSubscriber;

		WebRtcEndpoint oldWrEndpoint = subscriber.createEndpoint();
		if (oldWrEndpoint != null) {
			log.warn(
					"PARTICIPANT {}: Two threads are trying to create at the same time a subscriber endpoint for user {}",
					this.name, senderName);
			return null;
		}

		log.debug("PARTICIPANT {}: Created subscriber endpoint for user {}",
				this.name, senderName);
		try {
			String sdpAnswer = subscriber.subscribe(sdpOffer,
					sender.getPublisher());
			log.trace("USER {}: Subscribing SdpAnswer is {}", this.name,
					sdpAnswer);
			log.info("USER {}: Is now receiving video from {} in room {}",
					this.name, senderName, this.room.getName());
			return sdpAnswer;
		} catch (KurentoServerException e) {
			// TODO Check object status when KurentoClient set this info in the
			// object
			if (e.getCode() == 40101)
				log.warn(
						"Receiving endpoint is released when trying to connect a sending endpoint to it",
						e);
			else
				log.error(
						"Exception connecting receiving endpoint to sending endpoint",
						e);
			this.subscribers.remove(senderName);
			this.releaseEndpoint(senderName, subscriber.getEndpoint());
		}
		return null;
	}

	public void cancelSendingVideoTo(final String senderName) {

		log.debug("PARTICIPANT {}: canceling video sending to {}", this.name,
				senderName);

		IceWebRtcEndpoint sendingEndpoint = subscribers
				.remove(senderName);

		if (sendingEndpoint == null || sendingEndpoint.getEndpoint() == null) {
			log.warn(
					"PARTICIPANT {}: Trying to cancel sending video to user {}. But there is no such sending endpoint",
					this.name, senderName);
		} else {
			log.debug("PARTICIPANT {}: Cancelling sending endpoint to user {}",
					this.name, senderName);

			releaseEndpoint(senderName, sendingEndpoint.getEndpoint());
		}
	}

	public void leaveFromRoom() throws IOException, InterruptedException,
	ExecutionException {

		final Room room = getRoom();

		final String threadName = Thread.currentThread().getName();

		if (!room.isClosed()) {

			room.executeRoomTask(new Runnable() {
				@Override
				public void run() {
					updateThreadName("room>" + threadName);
					room.leave(Participant.this);
					updateThreadName("room");
				}
			});
		} else {
			log.warn("Trying to leave from room {} but it is closed",
					room.getName());
		}
	}

	public void close() {
		log.debug("PARTICIPANT {}: Closing user", this.name);

		this.closed = true;

		for (final String remoteParticipantName : subscribers.keySet()) {

			IceWebRtcEndpoint ep = this.subscribers
					.get(remoteParticipantName);

			if (ep.getEndpoint() != null) {
				releaseEndpoint(remoteParticipantName, ep.getEndpoint());
				log.debug("PARTICIPANT {}: Released sending EP for {}",
						this.name, remoteParticipantName);
			} else
				log.warn(
						"PARTICIPANT {}: Trying to close sending EP for {}. But the endpoint was never instantiated.",
						this.name, remoteParticipantName);
		}

		if (publisher != null
				&& publisher.getEndpoint() != null) {
			releaseEndpoint(name, publisher.getEndpoint());
			publisher = null;
		}

		this.senderThread.interrupt();
		this.notifThread.interrupt();
	}

	public void sendMessage(Request<JsonObject> request) {
		log.debug("USER {}: Enqueueing message {}", name, request);
		try {
			messages.put(request);
			log.debug("USER {}: Enqueued message {}", name, request);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void sendNotification(String method, JsonObject params) {
		RpcNotification notification = new RpcNotification(method, params);
		log.debug("USER {}: Enqueueing notification {}", name, notification);
		try {
			notifications.put(notification);
			log.debug("USER {}: Enqueued notification {}", name, notification);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public SubscriberEndpoint addSubscriber(String newUserName) {
		SubscriberEndpoint iceSendingEndpoint = new SubscriberEndpoint(this,
				newUserName, pipeline);
		SubscriberEndpoint existingIceSendingEndpoint = this.subscribers
				.putIfAbsent(newUserName, iceSendingEndpoint);
		if (existingIceSendingEndpoint != null) {
			iceSendingEndpoint = existingIceSendingEndpoint;
			log.trace(
					"PARTICIPANT {}: There is an existing placeholder for WebRtcEndpoint with ICE candidates queue for user {}",
					this.name, newUserName);
		} else
			log.debug(
					"PARTICIPANT {}: New placeholder for WebRtcEndpoint with ICE candidates queue for user {}",
					this.name, newUserName);
		return iceSendingEndpoint;
	}

	public void addIceCandidate(String endpointName, IceCandidate iceCandidate) {
		if (this.name.equals(endpointName))
			this.publisher.addIceCandidate(iceCandidate);
		else
			this.addSubscriber(endpointName).addIceCandidate(iceCandidate);
	}

	private void releaseEndpoint(final String senderName,
			final WebRtcEndpoint sendingEndpoint) {
		sendingEndpoint.release(new Continuation<Void>() {
			@Override
			public void onSuccess(Void result) throws Exception {
				log.debug(
						"PARTICIPANT {}: Released successfully sending EP for {}",
						Participant.this.name, senderName);
			}

			@Override
			public void onError(Throwable cause) throws Exception {
				log.warn(
						"PARTICIPANT {}: Could not release sending EP for user {}",
						Participant.this.name, senderName, cause);
			}
		});
	}

	private void internalSendMessage() throws InterruptedException {
		while (true) {
			try {
				Request<JsonObject> request = messages.take();

				log.debug("Sending message {} to user {}", request,
						Participant.this.name);

				Participant.this.session
				.sendRequest(
						request,
						new org.kurento.jsonrpc.client.Continuation<Response<JsonElement>>() {
							@Override
							public void onSuccess(
									Response<JsonElement> result) {
							}

							@Override
							public void onError(Throwable cause) {
								log.error(
										"Exception while sending message to user '"
												+ Participant.this.name
												+ "'", cause);
							}
						});
			} catch (InterruptedException e) {
				return;
			} catch (Exception e) {
				log.warn("Exception while sending message to user '"
						+ Participant.this.name + "'", e);
			}
		}
	}

	private void internalSendNotification() throws InterruptedException {
		while (true) {
			try {
				RpcNotification notification = notifications.take();
				log.debug("Sending notification {} to user {}", notification,
						Participant.this.name);
				Participant.this.session.sendNotification(
						notification.getMethod(), notification.getParams());
			} catch (InterruptedException e) {
				return;
			} catch (Exception e) {
				log.warn("Exception while sending notification to user '"
						+ Participant.this.name + "'", e);
			}
		}
	}

	public boolean isClosed() {
		return closed;
	}

	public boolean isStreaming() {
		return streaming;
	}

	@Override
	public String toString() {
		return "[User: " + name + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((room == null) ? 0 : room.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Participant other = (Participant) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (room == null) {
			if (other.room != null)
				return false;
		} else if (!room.equals(other.room))
			return false;
		return true;
	}
}
