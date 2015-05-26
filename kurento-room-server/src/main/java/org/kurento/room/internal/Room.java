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

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.kurento.client.Continuation;
import org.kurento.client.MediaPipeline;
import org.kurento.commons.exception.KurentoException;
import org.kurento.room.api.ParticipantSession;
import org.kurento.room.api.RoomException;
import org.kurento.room.kms.Kms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * @author Ivan Gracia (izanmail@gmail.com)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 1.0.0
 */
public class Room {

	private static final String PARTICIPANT_LEFT_METHOD = "participantLeft";
	private static final String PARTICIPANT_JOINED_METHOD = "participantJoined";
	private static final String PARTICIPANT_PUBLISHED_METHOD = "participantPublished";
	private static final String PARTICIPANT_SEND_MESSAGE_METHOD = "sendMessage"; //CHAT

	private final Logger log = LoggerFactory.getLogger(Room.class);

	private final ConcurrentMap<String, Participant> participants = new ConcurrentHashMap<>();
	private final String name;

	private MediaPipeline pipeline;
	private CountDownLatch pipelineLatch = new CountDownLatch(1);

	private Kms kms;

	private volatile boolean closed = false;

	private AtomicInteger activePublishers = new AtomicInteger(0);

	private ExecutorService executor = Executors.newFixedThreadPool(1);

	public Room(String roomName, Kms kms) {
		this.name = roomName;
		this.kms = kms;
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

	public Participant join(String userName, ParticipantSession session) {

		checkClosed();

		if (participants.containsKey(userName)) {
			throw new RoomException(
					RoomException.EXISTING_USER_IN_ROOM_ERROR_CODE, "User "
							+ userName + " exists in room " + name);
		}

		if (pipeline == null) {
			log.info("ROOM {}: Creating MediaPipeline", userName);
			pipeline = kms.newPipeline();
			pipelineLatch.countDown();
		}

		log.info("ROOM {}: adding participant {}", userName, userName);
		final Participant participant = new Participant(userName, this,
				session, this.pipeline);

		log.debug(
				"ROOM {}: notifying other participants {} of new participant {}",
				name, participants.values(), participant.getName());

		JsonObject params = new JsonObject();
		params.addProperty("id", participant.getName());

		for (Participant participant1 : participants.values()) {
			if (participant.equals(participant1))
				continue;
			participant1.sendNotification(PARTICIPANT_JOINED_METHOD, params);
			participant1.addSubscriber(userName);
		}

		participants.put(participant.getName(), participant);

		log.debug(
				"ROOM {}: Notified other participants {} of new participant {}",
				name, participants.values(), participant.getName());

		return participant;
	}

	public void newPublisher(Participant participant) {
		registerPublisher();

		JsonObject params = new JsonObject();
		params.addProperty("id", participant.getName());
		JsonObject stream = new JsonObject();
		stream.addProperty("id", "webcam");
		JsonArray streamsArray = new JsonArray();
		streamsArray.add(stream);
		params.add("streams", streamsArray);

		for (Participant participant1 : participants.values()) {
			if (participant.equals(participant1))
				continue;
			participant1.sendNotification(PARTICIPANT_PUBLISHED_METHOD, params);
			participant1.addSubscriber(participant.getName());
		}

		log.debug(
				"ROOM {}: Notified other participants {} of publisher {}",
				name, participants.values(), participant.getName());
	}

	public void leave(Participant user) {

		checkClosed();

		log.debug("PARTICIPANT {}: Leaving room {}", user.getName(), this.name);
		if (user.isStreaming())
			this.deregisterPublisher();
		this.removeParticipant(user.getName());
		user.close();
	}

	public Collection<Participant> getParticipants() {

		checkClosed();

		return participants.values();
	}

	public Participant getParticipant(String name) {

		checkClosed();

		return participants.get(name);
	}

	public void close() {

		if (!closed) {

			executor.shutdown();

			for (final Participant user : participants.values()) {
				user.close();
			}

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
				kms.removePipeline(pipeline);
			}

			log.debug("Room {} closed", this.name);

			this.closed = true;
		} else {
			log.warn("Closing a yet closed room {}", this.name);
		}
	}

	public void executeRoomTask(Runnable task) {

		checkClosed();

		if (!executor.isShutdown()) {
			try {
				executor.submit(task).get();
			} catch (InterruptedException e) {
				return;
			} catch (ExecutionException e) {
				log.warn("Exception while executing a task in room " + name, e);
			}
		}
	}

	public boolean isClosed() {
		return closed;
	}

	public void sendMessage(String room, String user, String message) {

		log.debug("ROOM {}: notifying all users that {} is sending a message {}",
				room, user, message);

		final JsonObject params = new JsonObject();
		params.addProperty("room", room);
		params.addProperty("user", user);
		params.addProperty("message", message);

		for (final Participant participant : participants.values()) {
			participant.sendNotification(PARTICIPANT_SEND_MESSAGE_METHOD,
					params);
		}
	}

	private void checkClosed() {
		if (closed) {
			throw new KurentoException("The room '" + name + "' is closed");
		}
	}

	private void removeParticipant(String name) {

		checkClosed();

		participants.remove(name);

		log.debug("ROOM {}: notifying all users that {} is leaving the room",
				this.name, name);

		final JsonObject params = new JsonObject();
		params.addProperty("name", name);

		for (final Participant participant : participants.values()) {

			participant.cancelSendingVideoTo(name);
			participant.sendNotification(PARTICIPANT_LEFT_METHOD, params);
		}
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
