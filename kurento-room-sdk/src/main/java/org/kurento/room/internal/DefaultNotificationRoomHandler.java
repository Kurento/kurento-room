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
import org.kurento.room.api.NotificationRoomHandler;
import org.kurento.room.api.UserNotificationService;
import org.kurento.room.api.pojo.ParticipantRequest;
import org.kurento.room.api.pojo.UserParticipant;
import org.kurento.room.exception.RoomException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Default implementation that assumes that JSON-RPC messages specification was used for the
 * client-server communications.
 *
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class DefaultNotificationRoomHandler implements NotificationRoomHandler {

  private UserNotificationService notifService;

  public DefaultNotificationRoomHandler(UserNotificationService notifService) {
    this.notifService = notifService;
  }

  @Override
  public void onRoomClosed(String roomName, Set<UserParticipant> participants) {
    JsonObject notifParams = new JsonObject();
    notifParams.addProperty(ProtocolElements.ROOMCLOSED_ROOM_PARAM, roomName);
    for (UserParticipant participant : participants) {
      notifService.sendNotification(participant.getParticipantId(),
          ProtocolElements.ROOMCLOSED_METHOD, notifParams);
    }
  }

  @Override
  public void onParticipantJoined(ParticipantRequest request, String roomName, String newUserName,
      Set<UserParticipant> existingParticipants, RoomException error) {
    if (error != null) {
      notifService.sendErrorResponse(request, null, error);
      return;
    }

    JsonArray result = new JsonArray();
    for (UserParticipant participant : existingParticipants) {
      JsonObject participantJson = new JsonObject();
      participantJson
          .addProperty(ProtocolElements.JOINROOM_PEERID_PARAM, participant.getUserName());
      if (participant.isStreaming()) {
        JsonObject stream = new JsonObject();
        stream.addProperty(ProtocolElements.JOINROOM_PEERSTREAMID_PARAM, "webcam");
        JsonArray streamsArray = new JsonArray();
        streamsArray.add(stream);
        participantJson.add(ProtocolElements.JOINROOM_PEERSTREAMS_PARAM, streamsArray);
      }
      result.add(participantJson);

      JsonObject notifParams = new JsonObject();
      notifParams.addProperty(ProtocolElements.PARTICIPANTJOINED_USER_PARAM, newUserName);
      notifService.sendNotification(participant.getParticipantId(),
          ProtocolElements.PARTICIPANTJOINED_METHOD, notifParams);
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
    params.addProperty(ProtocolElements.PARTICIPANTLEFT_NAME_PARAM, userName);
    for (UserParticipant participant : remainingParticipants) {
      notifService.sendNotification(participant.getParticipantId(),
          ProtocolElements.PARTICIPANTLEFT_METHOD, params);
    }

    notifService.sendResponse(request, new JsonObject());
    notifService.closeSession(request);
  }

  @Override
  public void onPublishMedia(ParticipantRequest request, String publisherName, String sdpAnswer,
      Set<UserParticipant> participants, RoomException error) {
    if (error != null) {
      notifService.sendErrorResponse(request, null, error);
      return;
    }
    JsonObject result = new JsonObject();
    result.addProperty(ProtocolElements.PUBLISHVIDEO_SDPANSWER_PARAM, sdpAnswer);
    notifService.sendResponse(request, result);

    JsonObject params = new JsonObject();
    params.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_USER_PARAM, publisherName);
    JsonObject stream = new JsonObject();
    stream.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_STREAMID_PARAM, "webcam");
    JsonArray streamsArray = new JsonArray();
    streamsArray.add(stream);
    params.add(ProtocolElements.PARTICIPANTPUBLISHED_STREAMS_PARAM, streamsArray);

    for (UserParticipant participant : participants) {
      if (participant.getParticipantId().equals(request.getParticipantId())) {
        continue;
      } else {
        notifService.sendNotification(participant.getParticipantId(),
            ProtocolElements.PARTICIPANTPUBLISHED_METHOD, params);
      }
    }
  }

  @Override
  public void onUnpublishMedia(ParticipantRequest request, String publisherName,
      Set<UserParticipant> participants, RoomException error) {
    if (error != null) {
      notifService.sendErrorResponse(request, null, error);
      return;
    }
    notifService.sendResponse(request, new JsonObject());

    JsonObject params = new JsonObject();
    params.addProperty(ProtocolElements.PARTICIPANTUNPUBLISHED_NAME_PARAM, publisherName);

    for (UserParticipant participant : participants) {
      if (participant.getParticipantId().equals(request.getParticipantId())) {
        continue;
      } else {
        notifService.sendNotification(participant.getParticipantId(),
            ProtocolElements.PARTICIPANTUNPUBLISHED_METHOD, params);
      }
    }
  }

  @Override
  public void onSubscribe(ParticipantRequest request, String sdpAnswer, RoomException error) {
    if (error != null) {
      notifService.sendErrorResponse(request, null, error);
      return;
    }
    JsonObject result = new JsonObject();
    result.addProperty(ProtocolElements.RECEIVEVIDEO_SDPANSWER_PARAM, sdpAnswer);
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
  public void onSendMessage(ParticipantRequest request, String message, String userName,
      String roomName, Set<UserParticipant> participants, RoomException error) {
    if (error != null) {
      notifService.sendErrorResponse(request, null, error);
      return;
    }
    notifService.sendResponse(request, new JsonObject());

    JsonObject params = new JsonObject();
    params.addProperty(ProtocolElements.PARTICIPANTSENDMESSAGE_ROOM_PARAM, roomName);
    params.addProperty(ProtocolElements.PARTICIPANTSENDMESSAGE_USER_PARAM, userName);
    params.addProperty(ProtocolElements.PARTICIPANTSENDMESSAGE_MESSAGE_PARAM, message);

    for (UserParticipant participant : participants) {
      notifService.sendNotification(participant.getParticipantId(),
          ProtocolElements.PARTICIPANTSENDMESSAGE_METHOD, params);
    }
  }

  @Override
  public void onRecvIceCandidate(ParticipantRequest request, RoomException error) {
    if (error != null) {
      notifService.sendErrorResponse(request, null, error);
      return;
    }

    notifService.sendResponse(request, new JsonObject());
  }

  @Override
  public void onParticipantLeft(String userName, Set<UserParticipant> remainingParticipants) {
    JsonObject params = new JsonObject();
    params.addProperty(ProtocolElements.PARTICIPANTLEFT_NAME_PARAM, userName);
    for (UserParticipant participant : remainingParticipants) {
      notifService.sendNotification(participant.getParticipantId(),
          ProtocolElements.PARTICIPANTLEFT_METHOD, params);
    }
  }

  @Override
  public void onParticipantEvicted(UserParticipant participant) {
    notifService.sendNotification(participant.getParticipantId(),
        ProtocolElements.PARTICIPANTEVICTED_METHOD, new JsonObject());
  }

  // ------------ EVENTS FROM ROOM HANDLER -----

  @Override
  public void onIceCandidate(String roomName, String participantId, String endpointName,
      IceCandidate candidate) {
    JsonObject params = new JsonObject();
    params.addProperty(ProtocolElements.ICECANDIDATE_EPNAME_PARAM, endpointName);
    params.addProperty(ProtocolElements.ICECANDIDATE_SDPMLINEINDEX_PARAM,
        candidate.getSdpMLineIndex());
    params.addProperty(ProtocolElements.ICECANDIDATE_SDPMID_PARAM, candidate.getSdpMid());
    params.addProperty(ProtocolElements.ICECANDIDATE_CANDIDATE_PARAM, candidate.getCandidate());
    notifService.sendNotification(participantId, ProtocolElements.ICECANDIDATE_METHOD, params);
  }

  @Override
  public void onPipelineError(String roomName, Set<String> participantIds, String description) {
    JsonObject notifParams = new JsonObject();
    notifParams.addProperty(ProtocolElements.MEDIAERROR_ERROR_PARAM, description);
    for (String pid : participantIds) {
      notifService.sendNotification(pid, ProtocolElements.MEDIAERROR_METHOD, notifParams);
    }
  }

  @Override
  public void onMediaElementError(String roomName, String participantId, String description) {
    JsonObject notifParams = new JsonObject();
    notifParams.addProperty(ProtocolElements.MEDIAERROR_ERROR_PARAM, description);
    notifService.sendNotification(participantId, ProtocolElements.MEDIAERROR_METHOD, notifParams);
  }

}
