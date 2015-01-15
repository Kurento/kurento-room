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

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PreDestroy;

import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.kurento.jsonrpc.message.ResponseError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * @author Ivan Gracia (izanmail@gmail.com)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 1.0.0
 */
public class RoomJsonRpcHandler extends DefaultJsonRpcHandler<JsonObject> {

	private static final Logger log = LoggerFactory
			.getLogger(RoomJsonRpcHandler.class);

	private static final String LEAVE_ROOM_METHOD = "leaveRoom";

	private static final String JOIN_ROOM_METHOD = "joinRoom";
	private static final String JOIN_ROOM_NAME_PARAM = "name";
	private static final String JOIN_ROOM_ROOMID_PARAM = "room";

	private static final String RECEIVE_VIDEO_METHOD = "receiveVideoFrom";
	private static final String RECEIVE_VIDEO_SDPOFFER_PARAM = "sdpOffer";
	private static final String RECEIVE_VIDEO_SENDER_PARAM = "sender";

	private static final int SDP_ERROR_CODE = 101;
	private static final int USER_NOT_FOUND_ERROR_CODE = 102;
	private static final int ROOM_CLOSED_ERROR_CODE = 103;

	private static final String USER_ATTRIBUTE = "user";

	private static final String HANDLER_THREAD_NAME = "handler";

	private static final ExecutorService executor = Executors
			.newFixedThreadPool(10);

	@Autowired
	private RoomManager roomManager;

	@PreDestroy
	public void close() {
		executor.shutdown();
	}

	@Override
	public void handleRequest(final Transaction transaction,
			final Request<JsonObject> request) throws Exception {

		Session session = transaction.getSession();

		final RoomParticipant user = (RoomParticipant) session.getAttributes()
				.get(USER_ATTRIBUTE);

		if (user != null) {
			log.debug("Incoming message from user '{}': {}", user.getName(),
					request);
		} else {
			log.debug("Incoming message from new user: {}", request);
		}

		switch (request.getMethod()) {
		case RECEIVE_VIDEO_METHOD:

			transaction.startAsync();

			executor.submit(new Runnable() {
				@Override
				public void run() {
					updateThreadName("rv:" + user.getName());

					Response<JsonObject> response = receiveVideoFrom(user,
							request.getParams());

					try {
						transaction.sendResponseObject(response);
					} catch (IOException e) {
						log.error("Exception sending response to request: "
								+ request);
					}

					updateThreadName(HANDLER_THREAD_NAME);
				}
			});
			break;
		case JOIN_ROOM_METHOD:
			joinRoom(request.getParams(), transaction, session);
			break;
		case LEAVE_ROOM_METHOD:
			leaveRoom(user);
			break;
		default:
			break;
		}

		updateThreadName(HANDLER_THREAD_NAME);
	}

	private Response<JsonObject> receiveVideoFrom(final RoomParticipant user,
			final JsonObject params) {

		final String senderName = params.get(RECEIVE_VIDEO_SENDER_PARAM)
				.getAsString();
		final String sdpOffer = params.get(RECEIVE_VIDEO_SDPOFFER_PARAM)
				.getAsString();

		Room room = user.getRoom();

		final RoomParticipant sender = room.getParticipant(senderName);

		if (sender != null) {
			String sdpAnswer = user.receiveVideoFrom(sender, sdpOffer);

			if (sdpAnswer != null) {

				final JsonObject result = new JsonObject();
				result.addProperty("name", sender.getName());
				result.addProperty("sdpAnswer", sdpAnswer);

				log.trace("USER {}: SdpAnswer for {} is {}", user.getName(),
						sender.getName(), sdpAnswer);

				return new Response<>(result);

			} else {

				return new Response<>(
						new ResponseError(SDP_ERROR_CODE,
								"Error generating sdpAnswer for user "
										+ user.getName()));
			}

		} else {
			log.warn(
					"PARTICIPANT {}: Requesting send video for user {} in room {} but it is not found",
					user.getName(), senderName, user.getRoom().getName());
			return new Response<>(new ResponseError(USER_NOT_FOUND_ERROR_CODE,
					"User " + user.getName() + " not found in room "
							+ room.getName()));
		}
	}

	private void joinRoom(JsonObject params, final Transaction transaction,
			final Session session) throws IOException, InterruptedException,
			ExecutionException {

		final String roomName = params.get(JOIN_ROOM_ROOMID_PARAM)
				.getAsString();
		final String userName = params.get(JOIN_ROOM_NAME_PARAM).getAsString();

		updateThreadName(userName);

		log.info("PARTICIPANT {}: trying to join room {}", userName, roomName);

		final Room room = roomManager.getRoom(roomName);

		if (!room.isClosed()) {

			transaction.startAsync();

			room.execute(new Runnable() {
				public void run() {
					updateThreadName("r>" + userName);
					final RoomParticipant user = room.join(userName, session);
					session.getAttributes().put(USER_ATTRIBUTE, user);

					final JsonArray participants = new JsonArray();
					for (final RoomParticipant participant : room
							.getParticipants()) {
						if (participant != user) {
							final JsonElement participantName = new JsonPrimitive(
									participant.getName());
							participants.add(participantName);
						}
					}

					try {
						transaction.sendResponse(participants);
					} catch (IOException e) {
						log.error("Exception sending participant list from room '"
								+ room + "' to new user '" + userName + "'");
					}

					updateThreadName("r>" + HANDLER_THREAD_NAME);
				}
			});

		} else {
			log.warn("Trying to join room {} but it is closing", room.getName());

			transaction.sendError(ROOM_CLOSED_ERROR_CODE,
					"Trying to join room '" + room.getName()
							+ "' but it is closing", null);
		}
	}

	private void leaveRoom(final RoomParticipant user) throws IOException,
			InterruptedException, ExecutionException {

		final Room room = user.getRoom();

		final String threadName = Thread.currentThread().getName();

		if (!room.isClosed()) {

			room.execute(new Runnable() {
				public void run() {
					updateThreadName("room>" + threadName);
					room.leave(user);
					if (room.getParticipants().isEmpty()) {
						roomManager.removeRoom(room);
					}
					updateThreadName("room>" + HANDLER_THREAD_NAME);
				}
			});
		} else {
			log.warn("Trying to leave from room {} but it is closed",
					room.getName());
		}
	}

	@Override
	public void afterConnectionClosed(Session session, String status)
			throws Exception {

		RoomParticipant user = (RoomParticipant) session.getAttributes().get(
				USER_ATTRIBUTE);
		if (user != null) {
			updateThreadName(user.getName() + "|wsclosed");
			leaveRoom(user);
			updateThreadName(HANDLER_THREAD_NAME);
		}
	}

	@Override
	public void handleTransportError(Session session, Throwable exception)
			throws Exception {

		RoomParticipant user = (RoomParticipant) session.getAttributes().get(
				USER_ATTRIBUTE);

		if (user != null && !user.isClosed()) {
			log.warn("Transport error", exception);
		}
	}

	private void updateThreadName(final String name) {
		Thread.currentThread().setName("user:" + name);
	}

}
