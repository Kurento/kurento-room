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

	private static final String PARTICIPANT_LEFT_METHOD = "participantLeft";
	private static final String PARTICIPANT_JOINED_METHOD = "participantJoined";
	private static final String PARTICIPANT_PUBLISHED_METHOD = "participantPublished";
	private static final String PARTICIPANT_SEND_MESSAGE_METHOD = "sendMessage";

	public static final String PARTICIPANT_ICE_CANDIDATE_METHOD = "iceCandidate";
	public static final String ON_ICE_EP_NAME_PARAM = "endpointName";
	public static final String ON_ICE_CANDIDATE_PARAM = "candidate";
	public static final String ON_ICE_SDP_MID_PARAM = "sdpMid";
	public static final String ON_ICE_SDP_M_LINE_INDEX_PARAM = "sdpMLineIndex";

	private static final String ROOM_CLOSED_METHOD = "roomClosed";

	private UserNotificationService notifService;

	public DefaultRoomEventHandler(UserNotificationService notifService) {
		this.notifService = notifService;
	}

	@Override
	public void onRoomCreated(ParticipantRequest request, String roomName) {
		// nothing to do
	}

	@Override
	public void onRoomClosed(String roomName, Set<String> participantIds) {
		JsonObject notifParams = new JsonObject();
		notifParams.addProperty("room", roomName);
		for (String pid : participantIds)
			notifService.sendNotification(pid, ROOM_CLOSED_METHOD, notifParams);
	}

	@Override
	public void onParticipantJoined(ParticipantRequest request,
			String newUserName, Set<UserParticipant> existingParticipants,
			RoomException error) {
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
			Set<String> remainingParticipantIds, RoomException error) {
		if (error != null) {
			notifService.sendErrorResponse(request, null, error);
			return;
		}

		JsonObject params = new JsonObject();
		params.addProperty("name", userName);
		for (String pid : remainingParticipantIds)
			notifService.sendNotification(pid, PARTICIPANT_LEFT_METHOD, params);

		// TODO or null instead of empty JsonObj?
		notifService.sendResponse(request, new JsonObject());
		notifService.closeSession(request);
	}

	@Override
	public void onPublishVideo(ParticipantRequest request,
			String publisherName, String sdpAnswer, Set<String> participantIds,
			RoomException error) {
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

		for (String pid : participantIds)
			if (pid.equals(request.getParticipantId()))
				continue;
			else
				notifService.sendNotification(pid,
						PARTICIPANT_PUBLISHED_METHOD, params);
	}

	@Override
	public void onReceiveMedia(ParticipantRequest request, String sdpAnswer,
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
	public void onSendMessage(ParticipantRequest request, String message,
			String userName, String roomName, Set<String> participantIds,
			RoomException error) {
		if (error != null) {
			notifService.sendErrorResponse(request, null, error);
			return;
		}

		// TODO or null instead of empty JsonObj?
		notifService.sendResponse(request, new JsonObject());

		JsonObject params = new JsonObject();
		params.addProperty("room", roomName);
		params.addProperty("user", userName);
		params.addProperty("message", message);

		for (String pid : participantIds)
			notifService.sendNotification(pid, PARTICIPANT_SEND_MESSAGE_METHOD,
					params);
	}

	@Override
	public void onRecvIceCandidate(ParticipantRequest request,
			RoomException error) {
		if (error != null) {
			notifService.sendErrorResponse(request, null, error);
			return;
		}

		// TODO or null instead of empty JsonObj?
		notifService.sendResponse(request, new JsonObject());
	}

	@Override
	public void onSendIceCandidate(String participantId, String endpointName,
			IceCandidate candidate) {
		JsonObject params = new JsonObject();
		params.addProperty(ON_ICE_EP_NAME_PARAM, endpointName);
		params.addProperty(ON_ICE_SDP_M_LINE_INDEX_PARAM,
				candidate.getSdpMLineIndex());
		params.addProperty(ON_ICE_SDP_MID_PARAM, candidate.getSdpMid());
		params.addProperty(ON_ICE_CANDIDATE_PARAM, candidate.getCandidate());
		notifService.sendNotification(participantId,
				PARTICIPANT_ICE_CANDIDATE_METHOD, params);
	}

}
