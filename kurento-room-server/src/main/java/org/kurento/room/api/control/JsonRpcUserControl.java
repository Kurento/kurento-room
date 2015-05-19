/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

package org.kurento.room.api.control;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import org.kurento.client.IceCandidate;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.kurento.jsonrpc.message.ResponseError;
import org.kurento.room.api.ParticipantSession;
import org.kurento.room.api.RoomException;
import org.kurento.room.internal.Participant;
import org.kurento.room.internal.ReceiveVideoFromResponse;
import org.kurento.room.internal.RoomManager;
import org.kurento.room.internal.RoomManager.RMContinuation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Uses the room api to handle the user's requests. Some of these requests are
 * processed asynchronously. The responses are sent using JSON-RPC over an
 * opened WebSocket connection, sometime in the future.
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class JsonRpcUserControl {

	private static final Logger log = LoggerFactory
			.getLogger(JsonRpcUserControl.class);

	@Autowired
	private RoomManager roomManager;

	/**
	 * Represents a user's request to join a room. If the room does not exist,
	 * it is created. The user will be added as a room's participant.
	 * 
	 * @param transaction
	 *            a JSON RPC transaction
	 * @param request
	 *            JSON RPC request
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public void joinRoom(final Transaction transaction,
			Request<JsonObject> request)
					throws IOException, InterruptedException, ExecutionException {
		final String roomName = request.getParams()
				.get(JsonRpcProtocolElements.JOIN_ROOM_ROOM_PARAM)
				.getAsString();

		final String userName = request.getParams()
				.get(JsonRpcProtocolElements.JOIN_ROOM_USER_PARAM)
				.getAsString();

		ParticipantSession participantSession = getParticipantSession(transaction);

		transaction.startAsync();

		roomManager.joinRoom(roomName, userName, participantSession,
				new RMContinuation<Collection<Participant>>() {
			@Override
			public void result(Throwable error,
					Collection<Participant> participants) {
				try {
					if (error != null) {
						log.error("Exception processing joinRoom",
								error);

						if (error instanceof RoomException) {
							RoomException e = (RoomException) error;
							transaction.sendError(e.getCode(),
									e.getMessage(), null);
						} else {
							transaction.sendError(error);
						}
					} else {

						JsonArray result = new JsonArray();

						for (Participant participant : participants) {

							JsonObject participantJson = new JsonObject();
							participantJson.addProperty("id",
									participant.getName());
							JsonObject stream = new JsonObject();
							stream.addProperty("id", "webcam");
							JsonArray streamsArray = new JsonArray();
							streamsArray.add(stream);
							participantJson
							.add("streams", streamsArray);

							result.add(participantJson);
						}

						transaction.sendResponse(result);
					}
				} catch (IOException e) {
					log.error("Exception responding to user", e);
				}
			}
		});
	}

	/**
	 * Represents a user's request to receive video from another participant in
	 * the room (supports loopback).
	 * 
	 * @param transaction
	 *            a JSON RPC transaction
	 * @param request
	 *            JSON RPC request
	 */
	public void receiveVideoFrom(final Transaction transaction,
			final Request<JsonObject> request) {
		ParticipantSession participantSession = getParticipantSession(transaction);

		String senderName = request.getParams()
				.get(JsonRpcProtocolElements.RECEIVE_VIDEO_SENDER_PARAM)
				.getAsString();

		senderName = senderName.substring(0, senderName.indexOf("_"));

		final String sdpOffer = request.getParams()
				.get(JsonRpcProtocolElements.RECEIVE_VIDEO_SDPOFFER_PARAM)
				.getAsString();

		transaction.startAsync();

		roomManager.receiveVideoFrom(participantSession.getParticipant(),
				senderName, sdpOffer,
				new RMContinuation<ReceiveVideoFromResponse>() {
			@Override
			public void result(Throwable error,
					ReceiveVideoFromResponse result) {

				Response<JsonObject> response;

				if (error != null
						&& error instanceof RoomException) {

					RoomException e = (RoomException) error;

					response = new Response<>(new ResponseError(e
							.getCode(), e.getMessage()));
				} else {

					final JsonObject resultJson = new JsonObject();
					resultJson.addProperty("name", result.name);
					resultJson.addProperty("sdpAnswer",
							result.sdpAnswer);

					response = new Response<>(resultJson);
				}

				try {
					transaction.sendResponseObject(response);
				} catch (IOException e) {
					log.error("Exception sending response to request: "
							+ request);
				}
			}
		});
	}

	/**
	 * Represents a user's request to leave a room. Besides removing the user
	 * from the room, this method will also cleanup the WebSocket session.
	 * 
	 * @param transaction
	 *            a JSON RPC transaction
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public void leaveRoom(Transaction transaction) throws IOException,
	InterruptedException, ExecutionException {
		Session session = transaction.getSession();
		ParticipantSession participantSession = getParticipantSession(session);
		Participant roomParticipant = participantSession.getParticipant();
		if (roomParticipant != null) {
			roomManager.leaveRoom(roomParticipant);
			session.getAttributes().remove(
					JsonRpcProtocolElements.PARTICIPANT_SESSION_ATTRIBUTE);
			log.info("Removed session participantInfo about "
					+ participantSession.getName());
		} else {
			log.warn("User is trying to leave from room but session has no info about user");
		}
	}

	/**
	 * Represents a user's request to add a new {@link IceCandidate} to an
	 * connected {@link WebRtcEndpoint}.
	 * 
	 * @param transaction
	 *            a JSON RPC transaction
	 * @param request
	 *            JSON RPC request
	 */
	public void onIceCandidate(Transaction transaction,
			Request<JsonObject> request) {
		String endpointName = request.getParams()
				.get(JsonRpcProtocolElements.ON_ICE_EP_NAME_PARAM)
				.getAsString();
		String candidate = request.getParams()
				.get(JsonRpcProtocolElements.ON_ICE_CANDIDATE_PARAM)
				.getAsString();
		String sdpMid = request.getParams()
				.get(JsonRpcProtocolElements.ON_ICE_SDP_MID_PARAM)
				.getAsString();
		int sdpMLineIndex = request.getParams()
				.get(JsonRpcProtocolElements.ON_ICE_SDP_M_LINE_INDEX_PARAM)
				.getAsInt();
		getParticipantSession(transaction).getParticipant().addIceCandidate(
				endpointName,
				new IceCandidate(candidate, sdpMid, sdpMLineIndex));
	}

	/**
	 * Represents a user's request to send a message to all the participants in
	 * the room.
	 * 
	 * @param transaction
	 *            a JSON RPC transaction
	 * @param request
	 *            JSON RPC request
	 */
	public void sendMessage(Transaction transaction, Request<JsonObject> request) {
		final String userName = request.getParams()
				.get(JsonRpcProtocolElements.SENDMESSAGE_USER_PARAM)
				.getAsString();
		final String roomName = request.getParams()
				.get(JsonRpcProtocolElements.SENDMESSAGE_ROOM_PARAM)
				.getAsString();
		final String message = request.getParams()
				.get(JsonRpcProtocolElements.SENDMESSAGE_MESSAGE_PARAM)
				.getAsString();

		log.debug("Message from {} in room {}: '{}'", userName, roomName,
				message);
		getParticipantSession(transaction).getParticipant().getRoom()
		.sendMessage(roomName, userName, message);
	}

	/**
	 * Represents a request to remove the user from the room. Usually called
	 * when the WebSocket has been closed unexpectedly.
	 * 
	 * @param participant
	 *            user info obtained from the WebSocket session
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public void leaveRoom(Participant participant) throws IOException,
	InterruptedException, ExecutionException {
		roomManager.leaveRoom(participant);
	}

	/**
	 * The user's info stored in the WebSocket session as a
	 * {@link ParticipantSession}. The session is obtained from the provided
	 * {@link Transaction}.
	 * 
	 * @see JsonRpcUserControl#getParticipantSession(Session)
	 * @param transaction
	 *            a JSON RPC transaction
	 * @return the {@link ParticipantSession} extracted from the session, a new
	 *         instance if the user is new
	 */
	public ParticipantSession getParticipantSession(
			Transaction transaction) {
		return getParticipantSession(transaction.getSession());
	}

	/**
	 * The user's info stored in the WebSocket session as a
	 * {@link ParticipantSession}.
	 * 
	 * @param session
	 *            a JSON RPC session
	 * @return the {@link ParticipantSession} extracted from the session, a new
	 *         instance if the user is new
	 */
	public ParticipantSession getParticipantSession(Session session) {
		ParticipantSessionJsonRpc participantSession = (ParticipantSessionJsonRpc) session
				.getAttributes().get(
						JsonRpcProtocolElements.PARTICIPANT_SESSION_ATTRIBUTE);
		if (participantSession == null) {
			participantSession = new ParticipantSessionJsonRpc(session);
			session.getAttributes().put(
					JsonRpcProtocolElements.PARTICIPANT_SESSION_ATTRIBUTE,
					participantSession);
		}
		return participantSession;
	}

}
