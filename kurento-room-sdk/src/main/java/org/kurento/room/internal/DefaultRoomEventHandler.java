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

package org.kurento.room.internal;

import java.util.Set;

import org.kurento.client.IceCandidate;
import org.kurento.room.api.RoomEventHandler;
import org.kurento.room.api.UserNotificationService;
import org.kurento.room.api.pojo.ParticipantRequest;
import org.kurento.room.api.pojo.UserParticipant;
import org.kurento.room.exception.RoomException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Default implementation that assumes that JSON-RPC messages specification was
 * used for the client-server communications.
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class DefaultRoomEventHandler implements RoomEventHandler {

	public static final String PARTICIPANT_LEFT_METHOD = "participantLeft";
	public static final String PARTICIPANT_JOINED_METHOD = "participantJoined";
	public static final String PARTICIPANT_PUBLISHED_METHOD =
			"participantPublished";
	public static final String PARTICIPANT_UNPUBLISHED_METHOD =
			"participantUnpublished";
	public static final String PARTICIPANT_SEND_MESSAGE_METHOD = "sendMessage";
	public static final String ROOM_CLOSED_METHOD = "roomClosed";
	public static final String MEDIA_ERROR_METHOD = "mediaError";

	public static final String ICE_CANDIDATE_METHOD = "iceCandidate";
	public static final String ON_ICE_EP_NAME_PARAM = "endpointName";
	public static final String ON_ICE_CANDIDATE_PARAM = "candidate";
	public static final String ON_ICE_SDP_MID_PARAM = "sdpMid";
	public static final String ON_ICE_SDP_M_LINE_INDEX_PARAM = "sdpMLineIndex";

	private UserNotificationService notifService;

	public DefaultRoomEventHandler(UserNotificationService notifService) {
		this.notifService = notifService;
	}

	@Override
	public void onRoomClosed(String roomName, Set<UserParticipant> participants) {
		JsonObject notifParams = new JsonObject();
		notifParams.addProperty("room", roomName);
		for (UserParticipant participant : participants)
			notifService.sendNotification(participant.getParticipantId(),
					ROOM_CLOSED_METHOD, notifParams);
	}

	@Override
	public void onParticipantJoined(ParticipantRequest request,
			String roomName, String newUserName,
			Set<UserParticipant> existingParticipants, RoomException error) {
		if (error != null) {
			notifService.sendErrorResponse(request, null, error);
			return;
		}

		JsonArray result = new JsonArray();
		for (UserParticipant participant : existingParticipants) {
			JsonObject participantJson = new JsonObject();
			participantJson.addProperty("id", participant.getUserName());
			if (participant.isStreaming()) {
				JsonObject stream = new JsonObject();
				stream.addProperty("id", "webcam");
				JsonArray streamsArray = new JsonArray();
				streamsArray.add(stream);
				participantJson.add("streams", streamsArray);
			}
			result.add(participantJson);

			JsonObject notifParams = new JsonObject();
			notifParams.addProperty("id", newUserName);
			notifService.sendNotification(participant.getParticipantId(),
					PARTICIPANT_JOINED_METHOD, notifParams);
		}
		notifService.sendResponse(request, result);
	}

	@Override
	public void onParticipantLeft(ParticipantRequest request, String userName,
			Set<UserParticipant> remainingParticipants, RoomException error) {
		if (error != null) {
			notifService.sendErrorResponse(request, null, error);
			return;
		}

		JsonObject params = new JsonObject();
		params.addProperty("name", userName);
		for (UserParticipant participant : remainingParticipants)
			notifService.sendNotification(participant.getParticipantId(),
					PARTICIPANT_LEFT_METHOD, params);

		notifService.sendResponse(request, new JsonObject());
		notifService.closeSession(request);
	}

	@Override
	public void onPublishMedia(ParticipantRequest request,
			String publisherName, String sdpAnswer,
			Set<UserParticipant> participants, RoomException error) {
		if (error != null) {
			notifService.sendErrorResponse(request, null, error);
			return;
		}
		JsonObject result = new JsonObject();
		result.addProperty("sdpAnswer", sdpAnswer);
		notifService.sendResponse(request, result);

		JsonObject params = new JsonObject();
		params.addProperty("id", publisherName);
		JsonObject stream = new JsonObject();
		stream.addProperty("id", "webcam");
		JsonArray streamsArray = new JsonArray();
		streamsArray.add(stream);
		params.add("streams", streamsArray);

		for (UserParticipant participant : participants)
			if (participant.getParticipantId().equals(
					request.getParticipantId()))
				continue;
			else
				notifService.sendNotification(participant.getParticipantId(),
						PARTICIPANT_PUBLISHED_METHOD, params);
	}

	@Override
	public void onUnpublishMedia(ParticipantRequest request,
			String publisherName, Set<UserParticipant> participants,
			RoomException error) {
		if (error != null) {
			notifService.sendErrorResponse(request, null, error);
			return;
		}
		notifService.sendResponse(request, new JsonObject());

		JsonObject params = new JsonObject();
		params.addProperty("name", publisherName);

		for (UserParticipant participant : participants)
			if (participant.getParticipantId().equals(
					request.getParticipantId()))
				continue;
			else
				notifService.sendNotification(participant.getParticipantId(),
						PARTICIPANT_UNPUBLISHED_METHOD, params);
	}

	@Override
	public void onSubscribe(ParticipantRequest request, String sdpAnswer,
			RoomException error) {
		if (error != null) {
			notifService.sendErrorResponse(request, null, error);
			return;
		}
		JsonObject result = new JsonObject();
		result.addProperty("sdpAnswer", sdpAnswer);
		notifService.sendResponse(request, result);
	}

	@Override
	public void onUnsubscribe(ParticipantRequest request, RoomException error) {
		if (error != null) {
			notifService.sendErrorResponse(request, null, error);
			return;
		}
		notifService.sendResponse(request, new JsonObject());
	}

	@Override
	public void onSendMessage(ParticipantRequest request, String message,
			String userName, String roomName,
			Set<UserParticipant> participants, RoomException error) {
		if (error != null) {
			notifService.sendErrorResponse(request, null, error);
			return;
		}
		notifService.sendResponse(request, new JsonObject());

		JsonObject params = new JsonObject();
		params.addProperty("room", roomName);
		params.addProperty("user", userName);
		params.addProperty("message", message);

		for (UserParticipant participant : participants)
			notifService.sendNotification(participant.getParticipantId(),
					PARTICIPANT_SEND_MESSAGE_METHOD, params);
	}

	@Override
	public void onRecvIceCandidate(ParticipantRequest request,
			RoomException error) {
		if (error != null) {
			notifService.sendErrorResponse(request, null, error);
			return;
		}

		notifService.sendResponse(request, new JsonObject());
	}

	@Override
	public void onParticipantLeft(String userName,
			Set<UserParticipant> remainingParticipants) {
		JsonObject params = new JsonObject();
		params.addProperty("name", userName);
		for (UserParticipant participant : remainingParticipants)
			notifService.sendNotification(participant.getParticipantId(),
					PARTICIPANT_LEFT_METHOD, params);
	}

	@Override
	public void onParticipantEvicted(UserParticipant participant) {
		//trick the peer into thinking the room was closed
		//TODO add specific method??
		notifService.sendNotification(participant.getParticipantId(),
				ROOM_CLOSED_METHOD, new JsonObject());
	}

	// ------------ EVENTS FROM ROOM HANDLER -----

	@Override
	public void onIceCandidate(String roomName, String participantId,
			String endpointName, IceCandidate candidate) {
		JsonObject params = new JsonObject();
		params.addProperty(ON_ICE_EP_NAME_PARAM, endpointName);
		params.addProperty(ON_ICE_SDP_M_LINE_INDEX_PARAM,
				candidate.getSdpMLineIndex());
		params.addProperty(ON_ICE_SDP_MID_PARAM, candidate.getSdpMid());
		params.addProperty(ON_ICE_CANDIDATE_PARAM, candidate.getCandidate());
		notifService.sendNotification(participantId, ICE_CANDIDATE_METHOD,
				params);
	}

	@Override
	public void onPipelineError(String roomName, Set<String> participantIds,
			String description) {
		JsonObject notifParams = new JsonObject();
		notifParams.addProperty("error", description);
		for (String pid : participantIds)
			notifService.sendNotification(pid, MEDIA_ERROR_METHOD, notifParams);
	}

	@Override
	public void onMediaElementError(String roomName, String participantId,
			String description) {
		JsonObject notifParams = new JsonObject();
		notifParams.addProperty("error", description);
		notifService.sendNotification(participantId, MEDIA_ERROR_METHOD,
				notifParams);
	}

}
