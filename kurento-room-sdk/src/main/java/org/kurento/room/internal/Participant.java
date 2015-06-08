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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

import org.kurento.client.Continuation;
import org.kurento.client.IceCandidate;
import org.kurento.client.MediaElement;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.client.internal.server.KurentoServerException;
import org.kurento.room.endpoint.IceWebRtcEndpoint;
import org.kurento.room.endpoint.PublisherEndpoint;
import org.kurento.room.endpoint.SubscriberEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivan Gracia (izanmail@gmail.com)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Radu Tom Vlad (rvlad@naevatec.com)
 * @since 1.0.0
 */
public class Participant {

	private static final Logger log = LoggerFactory
			.getLogger(Participant.class);

	private String id;
	private String name;

	private final Room room;

	private final MediaPipeline pipeline;

	private PublisherEndpoint publisher;
	private CountDownLatch endPointLatch = new CountDownLatch(1);

	private final ConcurrentMap<String, SubscriberEndpoint> subscribers = new ConcurrentHashMap<String, SubscriberEndpoint>();

	private volatile boolean streaming = false;
	private volatile boolean closed;

	public Participant(String id, String name, Room room, MediaPipeline pipeline) {
		this.id = id;
		this.name = name;
		this.pipeline = pipeline;
		this.room = room;
		this.publisher = new PublisherEndpoint(this, name, pipeline);

		for (Participant other : room.getParticipants())
			if (!other.getName().equals(this.name))
				subscribers.put(other.getName(), new SubscriberEndpoint(this,
						other.getName(), pipeline));
	}

	public void createPublishingEndpoint() {
		this.publisher.createEndpoint();
		endPointLatch.countDown();
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
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

	public MediaPipeline getPipeline() {
		return pipeline;
	}

	public String publishToRoom(String sdpOffer) {
		log.info("USER {}: Request to publish video in room {}", this.name,
				this.room.getName());
		log.trace("USER {}: Publishing SdpOffer is {}", this.name, sdpOffer);

		String sdpAnswer = this.getPublisher().publish(sdpOffer);
		this.streaming = true;

		log.trace("USER {}: Publishing SdpAnswer is {}", this.name, sdpAnswer);
		log.info("USER {}: Is now publishing video in room {}", this.name,
				this.room.getName());

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
					"PARTICIPANT {}: Trying to connect to a user without "
							+ "receiving endpoint (it seems is not yet fully connected)",
							this.name);
			return null;
		}

		if (senderName.equals(this.name)) {
			// FIXME: Use another message type for receiving sdp offer
			log.debug("PARTICIPANT {}: configuring loopback", this.name);
			// TODO throw exception
			// OR return null; ???
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
			log.warn("PARTICIPANT {}: Two threads are trying to create at "
					+ "the same time a subscriber endpoint for user {}",
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
			// TODO Check object status when KurentoClient sets this info in the
			// object
			if (e.getCode() == 40101)
				log.warn("Receiving endpoint is released when trying "
						+ "to connect a sending endpoint to it", e);
			else
				log.error("Exception connecting receiving endpoint "
						+ "to sending endpoint", e);
			this.subscribers.remove(senderName);
			this.releaseElement(senderName, subscriber.getEndpoint());
		}
		return null;
	}

	public void cancelReceivingVideo(final String senderName) {

		log.debug("PARTICIPANT {}: canceling video recv from {}", this.name,
				senderName);

		IceWebRtcEndpoint sendingEndpoint = subscribers.remove(senderName);

		if (sendingEndpoint == null || sendingEndpoint.getEndpoint() == null) {
			log.warn(
					"PARTICIPANT {}: Trying to cancel sending video from user {}. "
							+ "But there is no such sending endpoint",
							this.name, senderName);
		} else {
			log.debug(
					"PARTICIPANT {}: Cancelling sending endpoint linked to user {}",
					this.name, senderName);

			releaseElement(senderName, sendingEndpoint.getEndpoint());
		}
	}

	public void close() {
		log.debug("PARTICIPANT {}: Closing user", this.name);

		this.closed = true;

		for (final String remoteParticipantName : subscribers.keySet()) {

			IceWebRtcEndpoint ep = this.subscribers.get(remoteParticipantName);

			if (ep.getEndpoint() != null) {
				releaseElement(remoteParticipantName, ep.getEndpoint());
				log.debug("PARTICIPANT {}: Released sending EP for {}",
						this.name, remoteParticipantName);
			} else
				log.warn("PARTICIPANT {}: Trying to close sending EP for {}. "
						+ "But the endpoint was never instantiated.",
						this.name, remoteParticipantName);
		}

		if (publisher != null && publisher.getEndpoint() != null) {
			for (MediaElement el : publisher.getMediaElements())
				releaseElement(name, el);
			releaseElement(name, publisher.getEndpoint());
			publisher = null;
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
					"PARTICIPANT {}: There is an existing placeholder for WebRtcEndpoint "
							+ "with ICE candidates queue for user {}",
							this.name, newUserName);
		} else
			log.debug("PARTICIPANT {}: New placeholder for WebRtcEndpoint "
					+ "with ICE candidates queue for user {}", this.name,
					newUserName);
		return iceSendingEndpoint;
	}

	public void addIceCandidate(String endpointName, IceCandidate iceCandidate) {
		if (this.name.equals(endpointName))
			this.publisher.addIceCandidate(iceCandidate);
		else
			this.addSubscriber(endpointName).addIceCandidate(iceCandidate);
	}

	public void sendIceCandidate(String endpointName, IceCandidate candidate) {
		room.sendIceCandidate(id, endpointName, candidate);
	}

	private void releaseElement(final String senderName,
			final MediaElement element) {
		element.release(new Continuation<Void>() {
			@Override
			public void onSuccess(Void result) throws Exception {
				log.debug("PARTICIPANT {}: Released successfully {} for {}",
						Participant.this.name, element.getClass().getName(),
						senderName);
			}

			@Override
			public void onError(Throwable cause) throws Exception {
				log.warn("PARTICIPANT {}: Could not release {} for {}",
						Participant.this.name, element.getClass().getName(),
						senderName, cause);
			}
		});
	}

	public boolean isClosed() {
		return closed;
	}

	public boolean isStreaming() {
		return streaming;
	}

	public boolean isSubscribed() {
		for (SubscriberEndpoint se : subscribers.values())
			if (se.isConnectedToPublisher())
				return true;
		return false;
	}

	public Set<String> getConnectedSubscribedEndpoints() {
		Set<String> subscribedToSet = new HashSet<String>();
		for (SubscriberEndpoint se : subscribers.values())
			if (se.isConnectedToPublisher())
				subscribedToSet.add(se.getEndpointName());
		return subscribedToSet;
	}

	@Override
	public String toString() {
		return "[User: " + name + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Participant))
			return false;
		Participant other = (Participant) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
