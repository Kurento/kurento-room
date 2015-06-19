/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License (LGPL)
 * version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package org.kurento.room.internal;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

import org.kurento.client.Continuation;
import org.kurento.client.ErrorEvent;
import org.kurento.client.IceCandidate;
import org.kurento.client.MediaElement;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.client.internal.server.KurentoServerException;
import org.kurento.room.endpoint.PublisherEndpoint;
import org.kurento.room.endpoint.SubscriberEndpoint;
import org.kurento.room.exception.RoomException;
import org.kurento.room.exception.RoomException.Code;
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

	private final ConcurrentMap<String, SubscriberEndpoint> subscribers =
			new ConcurrentHashMap<String, SubscriberEndpoint>();

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
		publisher.createEndpoint();
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

	public void unpublishMedia() {
		log.debug("PARTICIPANT {}: unpublishing media stream from room {}",
				this.name, this.room.getName());
		releasePublisherEndpoint();
		this.publisher = new PublisherEndpoint(this, name, pipeline);
		log.debug("PARTICIPANT {}: released publisher endpoint and left it "
				+ "initialized (ready for future streaming)", this.name);
	}

	public String receiveMediaFrom(Participant sender, String sdpOffer) {
		final String senderName = sender.getName();

		log.info("USER {}: Request to receive media from {} in room {}",
				this.name, senderName, this.room.getName());
		log.trace("USER {}: SdpOffer for {} is {}", this.name, senderName,
				sdpOffer);

		if (senderName.equals(this.name)) {
			log.warn("PARTICIPANT {}: trying to configure loopback by subscribing",
					this.name);
			throw new RoomException(Code.USER_NOT_STREAMING_ERROR_CODE,
					"Can loopback only when publishing media");
		}

		if (sender.getPublisher() == null) {
			log.warn("PARTICIPANT {}: Trying to connect to a user without "
					+ "a publishing endpoint", this.name);
			return null;
		}

		log.debug("PARTICIPANT {}: Creating a subscriber endpoint to user {}",
				this.name, senderName);

		SubscriberEndpoint subscriber =
				new SubscriberEndpoint(this, senderName, pipeline);
		SubscriberEndpoint oldSubscriber =
				this.subscribers.putIfAbsent(senderName, subscriber);
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
			String sdpAnswer =
					subscriber.subscribe(sdpOffer, sender.getPublisher());
			log.trace("USER {}: Subscribing SdpAnswer is {}", this.name,
					sdpAnswer);
			log.info("USER {}: Is now receiving video from {} in room {}",
					this.name, senderName, this.room.getName());
			return sdpAnswer;
		} catch (KurentoServerException e) {
			// TODO Check object status when KurentoClient sets this info in the
			// object
			if (e.getCode() == 40101)
				log.warn("Publisher endpoint was already released when trying "
						+ "to connect a subscriber endpoint to it", e);
			else
				log.error("Exception connecting subscriber endpoint "
						+ "to publisher endpoint", e);
			this.subscribers.remove(senderName);
			releaseSubscriberEndpoint(senderName, subscriber);
		}
		return null;
	}

	public void cancelReceivingMedia(String senderName) {
		log.debug("PARTICIPANT {}: cancel receiving media from {}", this.name,
				senderName);
		SubscriberEndpoint subscriberEndpoint = subscribers.remove(senderName);
		if (subscriberEndpoint == null
				|| subscriberEndpoint.getEndpoint() == null) {
			log.warn(
					"PARTICIPANT {}: Trying to cancel receiving video from user {}. "
							+ "But there is no such subscriber endpoint.",
					this.name, senderName);
		} else {
			log.debug(
					"PARTICIPANT {}: Cancel subscriber endpoint linked to user {}",
					this.name, senderName);

			releaseSubscriberEndpoint(senderName, subscriberEndpoint);
		}
	}

	public void close() {
		log.debug("PARTICIPANT {}: Closing user", this.name);
		this.closed = true;
		for (String remoteParticipantName : subscribers.keySet()) {
			SubscriberEndpoint subscriber =
					this.subscribers.get(remoteParticipantName);
			if (subscriber.getEndpoint() != null) {
				releaseSubscriberEndpoint(remoteParticipantName, subscriber);
				log.debug("PARTICIPANT {}: Released subscriber endpoint to {}",
						this.name, remoteParticipantName);
			} else
				log.warn(
						"PARTICIPANT {}: Trying to close subscriber endpoint to {}. "
								+ "But the endpoint was never instantiated.",
						this.name, remoteParticipantName);
		}
		releasePublisherEndpoint();
	}

	public SubscriberEndpoint addSubscriber(String newUserName) {
		SubscriberEndpoint iceSendingEndpoint =
				new SubscriberEndpoint(this, newUserName, pipeline);
		SubscriberEndpoint existingIceSendingEndpoint =
				this.subscribers.putIfAbsent(newUserName, iceSendingEndpoint);
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

	public void sendMediaError(ErrorEvent event) {
		String desc =
				event.getType() + ": " + event.getDescription() + "(errCode="
						+ event.getErrorCode() + ")";
		log.warn("PARTICIPANT {}: Media error encountered: {}", name, desc);
		room.sendMediaError(id, desc);
	}

	private void releasePublisherEndpoint() {
		if (publisher != null && publisher.getEndpoint() != null) {
			this.streaming = false;
			publisher.unregisterErrorListeners();
			for (MediaElement el : publisher.getMediaElements())
				releaseElement(name, el);
			releaseElement(name, publisher.getEndpoint());
			publisher = null;
		} else
			log.warn(
					"PARTICIPANT {}: Trying to release publisher endpoint but is null",
					name);
	}

	private void releaseSubscriberEndpoint(String senderName,
			SubscriberEndpoint subscriber) {
		if (subscriber != null) {
			subscriber.unregisterErrorListeners();
			releaseElement(senderName, subscriber.getEndpoint());
		} else
			log.warn(
					"PARTICIPANT {}: Trying to release subscriber endpoint for '{}' but is null",
					name, senderName);
	}

	private void releaseElement(final String senderName,
			final MediaElement element) {
		final String eid = element.getId();
		try {
			element.release(new Continuation<Void>() {
				@Override
				public void onSuccess(Void result) throws Exception {
					log.debug(
							"PARTICIPANT {}: Released successfully media element #{} for {}",
							Participant.this.name, eid, senderName);
				}

				@Override
				public void onError(Throwable cause) throws Exception {
					log.warn(
							"PARTICIPANT {}: Could not release media element #{} for {}",
							Participant.this.name, eid, senderName, cause);
				}
			});
		} catch (Exception e) {
			log.error(
					"PARTICIPANT {}: Error calling release on elem #{} for {}",
					name, eid, senderName, e);
		}
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
