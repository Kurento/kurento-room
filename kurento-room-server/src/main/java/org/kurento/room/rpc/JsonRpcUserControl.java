/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

package org.kurento.room.rpc;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.room.RoomManager;
import org.kurento.room.api.pojo.ParticipantRequest;
import org.kurento.room.api.pojo.UserParticipant;
import org.kurento.room.exception.AdminException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.JsonObject;

/**
 * Controls the user interactions by delegating her JSON-RPC requests to the
 * room API.
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class JsonRpcUserControl {

	private static final Logger log = LoggerFactory
			.getLogger(JsonRpcUserControl.class);

	@Autowired
	protected RoomManager roomManager;

	public void joinRoom(Transaction transaction, Request<JsonObject> request,
			ParticipantRequest participantRequest) throws IOException,
			InterruptedException, ExecutionException {
		String roomName =
				request.getParams()
						.get(JsonRpcProtocolElements.JOIN_ROOM_ROOM_PARAM)
						.getAsString();
		String userName =
				request.getParams()
						.get(JsonRpcProtocolElements.JOIN_ROOM_USER_PARAM)
						.getAsString();

		ParticipantSession participantSession =
				getParticipantSession(transaction);
		participantSession.setParticipantName(userName);
		participantSession.setRoomName(roomName);

		roomManager.joinRoom(userName, roomName, participantRequest);
	}

	public void publishVideo(Transaction transaction,
			Request<JsonObject> request, ParticipantRequest participantRequest) {
		String sdpOffer =
				request.getParams()
						.get(JsonRpcProtocolElements.PUBLISH_VIDEO_SDPOFFER_PARAM)
						.getAsString();

		roomManager.publishMedia(sdpOffer, participantRequest);
	}

	public void unpublishVideo(Transaction transaction,
			Request<JsonObject> request, ParticipantRequest participantRequest) {
		roomManager.unpublishMedia(participantRequest);
	}

	public void receiveVideoFrom(final Transaction transaction,
			final Request<JsonObject> request,
			ParticipantRequest participantRequest) {

		String senderName =
				request.getParams()
						.get(JsonRpcProtocolElements.RECEIVE_VIDEO_SENDER_PARAM)
						.getAsString();

		senderName = senderName.substring(0, senderName.indexOf("_"));

		String sdpOffer =
				request.getParams()
						.get(JsonRpcProtocolElements.RECEIVE_VIDEO_SDPOFFER_PARAM)
						.getAsString();

		roomManager.subscribe(senderName, sdpOffer, participantRequest);
	}

	public void unsubscribeFromVideo(Transaction transaction,
			Request<JsonObject> request, ParticipantRequest participantRequest) {

		String senderName =
				request.getParams()
						.get(JsonRpcProtocolElements.RECEIVE_VIDEO_SENDER_PARAM)
						.getAsString();

		senderName = senderName.substring(0, senderName.indexOf("_"));

		roomManager.unsubscribe(senderName, participantRequest);
	}

	public void leaveRoomAfterConnClosed(String sessionId) {
		try {
			roomManager.evictParticipant(sessionId);
			log.info("Evicted participant with sessionId {}", sessionId);
		} catch (AdminException e) {
			log.warn("Unable to evict: {}", e.getMessage());
			log.trace("Unable to evict user", e);
		}
	}

	public void leaveRoom(Transaction transaction, Request<JsonObject> request,
			ParticipantRequest participantRequest) {
		boolean exists = false;
		String pid = participantRequest.getParticipantId();
		// trying with room info from session
		String roomName = null;
		if (transaction != null)
			roomName = getParticipantSession(transaction).getRoomName();
		if (roomName == null) { // null when afterConnectionClosed
			log.warn(
					"No room information found for participant with session Id {}. "
							+ "Using the admin method to evict the user.", pid);
			leaveRoomAfterConnClosed(pid);
		} else {
			// sanity check, don't call leaveRoom unless the id checks out
			for (UserParticipant part : roomManager.getParticipants(roomName))
				if (part.getParticipantId().equals(
						participantRequest.getParticipantId())) {
					exists = true;
					break;
				}
			if (exists) {
				log.debug("Participant with sessionId {} is leaving room {}",
						pid, roomName);
				roomManager.leaveRoom(participantRequest);
				log.info("Participant with sessionId {} has left room {}", pid,
						roomName);
			} else {
				log.warn(
						"Participant with session Id {} not found in room {}. "
								+ "Using the admin method to evict the user.",
						pid, roomName);
				leaveRoomAfterConnClosed(pid);
			}
		}
	}

	public void onIceCandidate(Transaction transaction,
			Request<JsonObject> request, ParticipantRequest participantRequest) {
		String endpointName =
				request.getParams()
						.get(JsonRpcProtocolElements.ON_ICE_EP_NAME_PARAM)
						.getAsString();
		String candidate =
				request.getParams()
						.get(JsonRpcProtocolElements.ON_ICE_CANDIDATE_PARAM)
						.getAsString();
		String sdpMid =
				request.getParams()
						.get(JsonRpcProtocolElements.ON_ICE_SDP_MID_PARAM)
						.getAsString();
		int sdpMLineIndex =
				request.getParams()
						.get(JsonRpcProtocolElements.ON_ICE_SDP_M_LINE_INDEX_PARAM)
						.getAsInt();

		roomManager.onIceCandidate(endpointName, candidate, sdpMLineIndex,
				sdpMid, participantRequest);
	}

	public void sendMessage(Transaction transaction,
			Request<JsonObject> request, ParticipantRequest participantRequest) {
		String userName =
				request.getParams()
						.get(JsonRpcProtocolElements.SENDMESSAGE_USER_PARAM)
						.getAsString();
		String roomName =
				request.getParams()
						.get(JsonRpcProtocolElements.SENDMESSAGE_ROOM_PARAM)
						.getAsString();
		String message =
				request.getParams()
						.get(JsonRpcProtocolElements.SENDMESSAGE_MESSAGE_PARAM)
						.getAsString();

		log.debug("Message from {} in room {}: '{}'", userName, roomName,
				message);

		roomManager
				.sendMessage(message, userName, roomName, participantRequest);
	}

	public ParticipantSession getParticipantSession(Transaction transaction) {
		Session session = transaction.getSession();
		ParticipantSession participantSession =
				(ParticipantSession) session.getAttributes().get(
						ParticipantSession.SESSION_KEY);
		if (participantSession == null) {
			participantSession = new ParticipantSession();
			session.getAttributes().put(ParticipantSession.SESSION_KEY,
					participantSession);
		}
		return participantSession;
	}
}
