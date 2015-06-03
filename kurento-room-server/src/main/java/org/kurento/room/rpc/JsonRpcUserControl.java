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

package org.kurento.room.rpc;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.kurento.client.IceCandidate;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.room.RoomManager;
import org.kurento.room.api.pojo.ParticipantRequest;
import org.kurento.room.api.pojo.UserParticipant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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

	//TODO redo comments

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
	public void joinRoom(Transaction transaction, Request<JsonObject> request,
			ParticipantRequest participantRequest) throws IOException,
			InterruptedException, ExecutionException {
		final String roomName = request.getParams()
				.get(JsonRpcProtocolElements.JOIN_ROOM_ROOM_PARAM)
				.getAsString();

		final String userName = request.getParams()
				.get(JsonRpcProtocolElements.JOIN_ROOM_USER_PARAM)
				.getAsString();

		roomManager.joinRoom(userName, roomName, participantRequest);
	}

	public void publishVideo(Transaction transaction,
			Request<JsonObject> request, ParticipantRequest participantRequest) {
		final String sdpOffer = request.getParams()
				.get(JsonRpcProtocolElements.PUBLISH_VIDEO_SDPOFFER_PARAM)
				.getAsString();

		roomManager.publishMedia(sdpOffer, participantRequest);
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
			final Request<JsonObject> request,
			ParticipantRequest participantRequest) {

		String senderName = request.getParams()
				.get(JsonRpcProtocolElements.RECEIVE_VIDEO_SENDER_PARAM)
				.getAsString();

		senderName = senderName.substring(0, senderName.indexOf("_"));

		final String sdpOffer = request.getParams()
				.get(JsonRpcProtocolElements.RECEIVE_VIDEO_SDPOFFER_PARAM)
				.getAsString();

		roomManager.receiveMedia(senderName, sdpOffer, participantRequest);
	}

	/**
	 * Represents a user's request to leave a room. Besides removing the user
	 * from the room, this method will also cleanup the WebSocket session.
	 * 
	 * @param request
	 * 
	 * @param preq
	 *            a JSON RPC transaction
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public void leaveRoom(Transaction transaction, Request<JsonObject> request,
			ParticipantRequest participantRequest) {
		boolean exists = false;
		//TODO maintain room name in session
		for (String room : roomManager.getRooms()) {
			for (UserParticipant part : roomManager.getParticipants(room))
				if (part.getParticipantId().equals(
						participantRequest.getParticipantId())) {
					exists = true;
					break;
				}
			if (exists)
				break;
		}
		if (exists)
			roomManager.leaveRoom(participantRequest);
		else
			log.warn("Participant with session Id {} has already left",
					participantRequest.getParticipantId());
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
			Request<JsonObject> request, ParticipantRequest participantRequest) {
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

		roomManager.onIceCandidate(endpointName, candidate, sdpMLineIndex,
				sdpMid, participantRequest);
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
	public void sendMessage(Transaction transaction,
			Request<JsonObject> request, ParticipantRequest participantRequest) {
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

		roomManager
		.sendMessage(message, userName, roomName, participantRequest);
	}
}
