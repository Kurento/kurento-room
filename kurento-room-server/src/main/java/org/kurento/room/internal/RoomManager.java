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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PreDestroy;

import org.kurento.room.api.ParticipantSession;
import org.kurento.room.api.RoomException;
import org.kurento.room.api.SessionInterceptor;
import org.kurento.room.api.TrickleIceEndpoint.EndpointBuilder;
import org.kurento.room.kms.Kms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Ivan Gracia (izanmail@gmail.com)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 1.0.0
 */
public class RoomManager {

	public interface RMContinuation<F> {
		void result(Throwable error, F result);
	}

	private final Logger log = LoggerFactory.getLogger(RoomManager.class);

	@Autowired
	private SessionInterceptor interceptor;

	public void setSessionFilter(SessionInterceptor interceptor) {
		this.interceptor = interceptor;
	}

	@Autowired
	private EndpointBuilder endpointBuilder;

	private final ConcurrentMap<String, Room> rooms = new ConcurrentHashMap<>();

	private static final ExecutorService executor = Executors
			.newFixedThreadPool(10);

	@PreDestroy
	public void close() {
		for (Room room : rooms.values()) {
			room.close();
		}
		executor.shutdown();
	}

	/**
	 * @param roomName
	 *            the name of the room
	 * @param participantSession
	 *            current session
	 * @return the room if it was already created, or a new one if it is the
	 *         first time this room is accessed
	 */
	public Room getRoom(String roomName, ParticipantSession participantSession) {

		Room room = rooms.get(roomName);

		if (room == null) {
			Kms kms = interceptor.getKmsForNewRoom(participantSession);
			room = new Room(roomName, kms, endpointBuilder);
			Room oldRoom = rooms.putIfAbsent(roomName, room);
			if (oldRoom != null) {
				return oldRoom;
			} else {
				log.warn("Room {} not existent. Created new on Kms uri={})",
						kms.getUri());
				return room;
			}
		} else {
			return room;
		}
	}

	public void receiveVideoFrom(final Participant recvParticipant,
			final String senderParticipantName, final String sdpOffer,
			final RMContinuation<ReceiveVideoFromResponse> cont) {

		executor.submit(new Runnable() {
			@Override
			public void run() {
				updateThreadName("rv:" + recvParticipant.getName());

				Room room = recvParticipant.getRoom();

				final Participant senderParticipant = room
						.getParticipant(senderParticipantName);

				if (senderParticipant != null) {
					String sdpAnswer = recvParticipant.receiveVideoFrom(
							senderParticipant, sdpOffer);

					if (sdpAnswer != null) {

						log.trace("USER {}: SdpAnswer for {} is {}",
								recvParticipant.getName(),
								senderParticipant.getName(), sdpAnswer);

						cont.result(null, new ReceiveVideoFromResponse(
								senderParticipant.getName(), sdpAnswer));

					} else {

						cont.result(new RoomException(RoomException.SDP_ERROR_CODE,
								"Error generating sdpAnswer for user "
										+ recvParticipant.getName()), null);
					}

				} else {

					log.warn(
							"PARTICIPANT {}: Requesting send video for user {} in room {} but it is not found",
							recvParticipant.getName(), senderParticipantName,
							recvParticipant.getRoom().getName());

					cont.result(new RoomException(
							RoomException.USER_NOT_FOUND_ERROR_CODE, "User "
									+ recvParticipant.getName()
									+ " not found in room " + room.getName()),
									null);
				}

				updateThreadName("roomManager");
			}
		});
	}

	public void joinRoom(String roomName, final String userName,
			final ParticipantSession session,
			final RMContinuation<Collection<Participant>> cont)
					throws IOException, InterruptedException, ExecutionException {

		updateThreadName(userName);

		log.info("PARTICIPANT {}: trying to join room {}", userName, roomName);

		final Room room;
		try {
			room = getRoom(roomName, session);
		} catch (RoomException e) {
			cont.result(e, null);
			return;
		}

		if (!room.isClosed()) {

			room.executeRoomTask(new Runnable() {
				@Override
				public void run() {
					updateThreadName("room> user:" + userName);

					try {

						Collection<Participant> participants = new ArrayList<>(
								room.getParticipants());

						final Participant participant = room.join(userName,
								session);

						session.setParticipant(participant);

						cont.result(null, participants);

						executor.submit(new Runnable() {
							@Override
							public void run() {
								participant.createReceivingEndpoint();
							}
						});

					} catch (RoomException e) {
						cont.result(e, null);
					}

					updateThreadName("room");
				}
			});

		} else {
			log.error("Trying to join room {} but it is closing",
					room.getName());
			cont.result(new RoomException(RoomException.ROOM_CLOSED_ERROR_CODE,
					"Trying to join room '" + room.getName()
					+ "' but it is closing"), null);
		}
	}

	public void leaveRoom(final Participant user) throws IOException,
	InterruptedException, ExecutionException {

		final Room room = user.getRoom();

		if (!room.isClosed()) {

			room.executeRoomTask(new Runnable() {
				@Override
				public void run() {
					updateThreadName("room> user:" + user.getName());
					room.leave(user);
					if (room.getParticipants().isEmpty()) {
						room.close();
						rooms.remove(room.getName());
						log.info("Room {} removed and closed", room.getName());
					}
					updateThreadName("room");
				}
			});
		} else {
			log.warn("Trying to leave from room {} but it is closing",
					room.getName());
		}
	}

	public Map<String, Room> getAllRooms() {
		return rooms;
	}
}
