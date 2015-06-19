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

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.kurento.client.Continuation;
import org.kurento.client.ErrorEvent;
import org.kurento.client.EventListener;
import org.kurento.client.IceCandidate;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.commons.exception.KurentoException;
import org.kurento.room.api.RoomEventHandler;
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
public class Room {
	private final Logger log = LoggerFactory.getLogger(Room.class);

	private final ConcurrentMap<String, Participant> participants =
			new ConcurrentHashMap<String, Participant>();
	private final String name;

	private MediaPipeline pipeline;
	private CountDownLatch pipelineLatch = new CountDownLatch(1);

	private KurentoClient kurentoClient;

	private RoomEventHandler roomEventHandler;

	private volatile boolean closed = false;

	private AtomicInteger activePublishers = new AtomicInteger(0);

	public Room(String roomName, KurentoClient kurentoClient,
			RoomEventHandler roomEventHandler) {
		this.name = roomName;
		this.kurentoClient = kurentoClient;
		this.roomEventHandler = roomEventHandler;
		log.info("ROOM {} has been created", roomName);
	}

	public String getName() {
		return name;
	}

	public MediaPipeline getPipeline() {
		try {
			pipelineLatch.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return this.pipeline;
	}

	public void join(String participantId, String userName) {

		checkClosed();

		if (userName == null || userName.isEmpty())
			throw new RoomException(Code.GENERIC_ERROR_CODE,
					"Empty user name is not allowed");
		for (Participant p : participants.values()) {
			if (p.getName().equals(userName))
				throw new RoomException(Code.EXISTING_USER_IN_ROOM_ERROR_CODE,
						"User '" + userName + "' already exists in room '" + name + "'");
		}

		if (pipeline == null) {
			log.info("ROOM {}: Creating MediaPipeline", name);
			pipeline = kurentoClient.createMediaPipeline();
			pipeline.addErrorListener(new EventListener<ErrorEvent>() {
				@Override
				public void onEvent(ErrorEvent event) {
					String desc =
							event.getType() + ": " + event.getDescription()
									+ "(errCode=" + event.getErrorCode() + ")";
					log.warn("ROOM {}: Pipeline error encountered: {}", name,
							desc);
					roomEventHandler.onPipelineError(name, getParticipantIds(),
							desc);
				}
			});
			pipelineLatch.countDown();
		}

		participants.put(participantId, new Participant(participantId,
				userName, this, this.pipeline));
		
		log.info("ROOM {}: Added participant {}", name, userName);
	}

	public void newPublisher(Participant participant) {
		registerPublisher();

		// pre-load endpoints to recv video from the new publisher
		for (Participant participant1 : participants.values()) {
			if (participant.equals(participant1))
				continue;
			participant1.addSubscriber(participant.getName());
		}

		log.debug(
				"ROOM {}: Virtually subscribed other participants {} to new publisher {}",
				name, participants.values(), participant.getName());
	}

	public void cancelPublisher(Participant participant) {
		deregisterPublisher();

		// cancel recv video from this publisher
		for (Participant subscriber : participants.values()) {
			if (participant.equals(subscriber))
				continue;
			subscriber.cancelReceivingMedia(participant.getName());
		}

		log.debug(
				"ROOM {}: Unsubscribed other participants {} from the publisher {}",
				name, participants.values(), participant.getName());

	}

	public void leave(String participantId) throws RoomException {

		checkClosed();

		Participant participant = participants.get(participantId);
		if (participant == null)
			throw new RoomException(Code.USER_NOT_FOUND_ERROR_CODE, "User #"
					+ participantId + " not found in room '" + name + "'");
		log.debug("PARTICIPANT {}: Leaving room {}", participant.getName(),
				this.name);
		if (participant.isStreaming())
			this.deregisterPublisher();
		this.removeParticipant(participant);
		participant.close();
	}

	public Collection<Participant> getParticipants() {

		checkClosed();

		return participants.values();
	}

	public Set<String> getParticipantIds() {

		checkClosed();

		return participants.keySet();
	}

	public Participant getParticipant(String participantId) {

		checkClosed();

		return participants.get(participantId);
	}

	public Participant getParticipantByName(String userName) {

		checkClosed();

		for (Participant p : participants.values())
			if (p.getName().equals(userName))
				return p;

		return null;
	}

	public void close() {
		if (!closed) {

			for (Participant user : participants.values())
				user.close();

			participants.clear();

			if (pipeline != null) {
				pipeline.release(new Continuation<Void>() {

					@Override
					public void onSuccess(Void result) throws Exception {
						log.trace("ROOM {}: Released Pipeline", Room.this.name);
					}

					@Override
					public void onError(Throwable cause) throws Exception {
						log.warn("PARTICIPANT " + Room.this.name
								+ ": Could not release Pipeline", cause);
					}
				});
			}

			log.debug("Room {} closed", this.name);

			this.closed = true;
		} else {
			log.warn("Closing an already closed room {}", this.name);
		}
	}

	public void sendIceCandidate(String participantId, String endpointName,
			IceCandidate candidate) {
		this.roomEventHandler.onSendIceCandidate(participantId, endpointName,
				candidate);
	}

	public void sendMediaError(String participantId, String description) {
		this.roomEventHandler.onParticipantMediaError(participantId,
				description);
	}

	public boolean isClosed() {
		return closed;
	}

	private void checkClosed() {
		if (closed) {
			throw new KurentoException("The room '" + name + "' is closed");
		}
	}

	private void removeParticipant(Participant participant) {

		checkClosed();

		participants.remove(participant.getId());

		log.debug(
				"ROOM {}: Cancel receiving media from user '{}' for other users",
				this.name, participant.getName());
		for (Participant other : participants.values())
			other.cancelReceivingMedia(participant.getName());
	}

	public int getActivePublishers() {
		return activePublishers.get();
	}

	public void registerPublisher() {
		this.activePublishers.incrementAndGet();
	}

	public void deregisterPublisher() {
		this.activePublishers.decrementAndGet();
	}
}
