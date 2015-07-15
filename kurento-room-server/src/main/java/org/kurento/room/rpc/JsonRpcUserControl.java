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
				getStringParam(request,
						JsonRpcProtocolElements.JOIN_ROOM_ROOM_PARAM);
		String userName =
				getStringParam(request,
						JsonRpcProtocolElements.JOIN_ROOM_USER_PARAM);

		ParticipantSession participantSession =
				getParticipantSession(transaction);
		participantSession.setParticipantName(userName);
		participantSession.setRoomName(roomName);

		roomManager.joinRoom(userName, roomName, participantRequest);
	}

	public void publishVideo(Transaction transaction,
			Request<JsonObject> request, ParticipantRequest participantRequest) {
		String sdpOffer =
				getStringParam(request,
						JsonRpcProtocolElements.PUBLISH_VIDEO_SDPOFFER_PARAM);
		boolean doLoopback =
				getBooleanParam(request,
						JsonRpcProtocolElements.PUBLISH_VIDEO_DOLOOPBACK_PARAM);

		roomManager.publishMedia(participantRequest, sdpOffer, doLoopback);
	}

	public void unpublishVideo(Transaction transaction,
			Request<JsonObject> request, ParticipantRequest participantRequest) {
		roomManager.unpublishMedia(participantRequest);
	}

	public void receiveVideoFrom(final Transaction transaction,
			final Request<JsonObject> request,
			ParticipantRequest participantRequest) {

		String senderName =
				getStringParam(request,
						JsonRpcProtocolElements.RECEIVE_VIDEO_SENDER_PARAM);
		senderName = senderName.substring(0, senderName.indexOf("_"));

		String sdpOffer =
				getStringParam(request,
						JsonRpcProtocolElements.RECEIVE_VIDEO_SDPOFFER_PARAM);

		roomManager.subscribe(senderName, sdpOffer, participantRequest);
	}

	public void unsubscribeFromVideo(Transaction transaction,
			Request<JsonObject> request, ParticipantRequest participantRequest) {

		String senderName =
				getStringParam(request,
						JsonRpcProtocolElements.RECEIVE_VIDEO_SENDER_PARAM);
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
			ParticipantRequest participantRequest) throws AdminException {
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
				getStringParam(request,
						JsonRpcProtocolElements.ON_ICE_EP_NAME_PARAM);
		String candidate =
				getStringParam(request,
						JsonRpcProtocolElements.ON_ICE_CANDIDATE_PARAM);
		String sdpMid =
				getStringParam(request,
						JsonRpcProtocolElements.ON_ICE_SDP_MID_PARAM);
		int sdpMLineIndex =
				getIntParam(request,
						JsonRpcProtocolElements.ON_ICE_SDP_M_LINE_INDEX_PARAM);

		roomManager.onIceCandidate(endpointName, candidate, sdpMLineIndex,
				sdpMid, participantRequest);
	}

	public void sendMessage(Transaction transaction,
			Request<JsonObject> request, ParticipantRequest participantRequest) {
		String userName =
				getStringParam(request,
						JsonRpcProtocolElements.SENDMESSAGE_USER_PARAM);
		String roomName =
				getStringParam(request,
						JsonRpcProtocolElements.SENDMESSAGE_ROOM_PARAM);
		String message =
				getStringParam(request,
						JsonRpcProtocolElements.SENDMESSAGE_MESSAGE_PARAM);

		log.debug("Message from {} in room {}: '{}'", userName, roomName,
				message);

		roomManager
				.sendMessage(message, userName, roomName, participantRequest);
	}

	public void customRequest(Transaction transaction,
			Request<JsonObject> request, ParticipantRequest participantRequest) {
		throw new RuntimeException("Unsupported method");
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

	protected String getStringParam(Request<JsonObject> request, String key) {
		if (request.getParams() == null || request.getParams().get(key) == null)
			throw new RuntimeException("Request element '" + key
					+ "' is missing");
		return request.getParams().get(key).getAsString();
	}

	protected int getIntParam(Request<JsonObject> request, String key) {
		if (request.getParams() == null || request.getParams().get(key) == null)
			throw new RuntimeException("Request element '" + key
					+ "' is missing");
		return request.getParams().get(key).getAsInt();
	}

	protected boolean getBooleanParam(Request<JsonObject> request, String key) {
		if (request.getParams() == null || request.getParams().get(key) == null)
			throw new RuntimeException("Request element '" + key
					+ "' is missing");
		return request.getParams().get(key).getAsBoolean();
	}
}
