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
package org.kurento.room.demo;

import static org.kurento.room.demo.ThreadLogUtils.updateThreadName;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PreDestroy;

import org.kurento.client.KurentoClient;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @author Ivan Gracia (izanmail@gmail.com)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 1.0.0
 */
public class RoomManager {

	private static final int SDP_ERROR_CODE = 101;
	private static final int USER_NOT_FOUND_ERROR_CODE = 102;
	private static final int ROOM_CLOSED_ERROR_CODE = 103;
	public static final int EXISTING_USER_IN_ROOM_ERROR_CODE = 104;

	private final Logger log = LoggerFactory.getLogger(RoomManager.class);

	@Autowired
	private KurentoClient kurento;

	private final ConcurrentMap<String, Room> rooms = new ConcurrentHashMap<>();

	private static final ExecutorService executor = Executors
			.newFixedThreadPool(10);

	public static class ReceiveVideoFromResponse {

		public String name;
		public String sdpAnswer;

		public ReceiveVideoFromResponse(String name, String sdpAnswer) {
			super();
			this.name = name;
			this.sdpAnswer = sdpAnswer;
		}
	}

	public static interface ParticipantSession {
		public void setParticipant(RoomParticipant participant);

		public void sendRequest(
				Request<JsonObject> request,
				org.kurento.jsonrpc.client.Continuation<Response<JsonElement>> continuation)
				throws IOException;
	}

	public interface RMContinuation<F> {
		void result(Throwable error, F result);
	}

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
	 * @return the room if it was already created, or a new one if it is the
	 *         first time this room is accessed
	 */
	public Room getRoom(String roomName) {

		Room room = rooms.get(roomName);

		if (room == null) {

			room = new Room(roomName, kurento);
			Room oldRoom = rooms.putIfAbsent(roomName, room);
			if (oldRoom != null) {
				return oldRoom;
			} else {
				log.debug("Room {} not existent. Created new!", roomName);
				return room;
			}
		} else {
			return room;
		}
	}

	public void receiveVideoFrom(final RoomParticipant recvParticipant,
			final String senderParticipantName, final String sdpOffer,
			final RMContinuation<ReceiveVideoFromResponse> cont) {

		executor.submit(new Runnable() {
			@Override
			public void run() {
				updateThreadName("rv:" + recvParticipant.getName());

				Room room = recvParticipant.getRoom();

				final RoomParticipant senderParticipant = room
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

						cont.result(new RoomManagerException(SDP_ERROR_CODE,
								"Error generating sdpAnswer for user "
										+ recvParticipant.getName()), null);
					}

				} else {

					log.warn(
							"PARTICIPANT {}: Requesting send video for user {} in room {} but it is not found",
							recvParticipant.getName(), senderParticipantName,
							recvParticipant.getRoom().getName());

					cont.result(new RoomManagerException(
							USER_NOT_FOUND_ERROR_CODE, "User "
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
			final RMContinuation<List<String>> cont) throws IOException,
			InterruptedException, ExecutionException {

		updateThreadName(userName);

		log.info("PARTICIPANT {}: trying to join room {}", userName, roomName);

		final Room room = getRoom(roomName);

		if (!room.isClosed()) {

			room.execute(new Runnable() {
				public void run() {
					updateThreadName("room> user:" + userName);

					try {

						List<String> participantNames = room
								.getParticipantNames();

						final RoomParticipant user = room.join(userName,
								session);

						session.setParticipant(user);

						cont.result(null, participantNames);

						executor.submit(new Runnable() {
							@Override
							public void run() {
								user.createWebRtcEndpoint();
							}
						});

					} catch (RoomManagerException e) {
						cont.result(e, null);
					}

					updateThreadName("room");
				}
			});

		} else {
			log.error("Trying to join room {} but it is closing",
					room.getName());
			cont.result(new RoomManagerException(ROOM_CLOSED_ERROR_CODE,
					"Trying to join room '" + room.getName()
							+ "' but it is closing"), null);
		}
	}

	public void leaveRoom(final RoomParticipant user) throws IOException,
			InterruptedException, ExecutionException {

		final Room room = user.getRoom();

		if (!room.isClosed()) {

			room.execute(new Runnable() {
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
}
