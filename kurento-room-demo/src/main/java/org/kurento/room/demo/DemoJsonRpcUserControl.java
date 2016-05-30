/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kurento.room.demo;

import java.io.IOException;

import org.kurento.client.FaceOverlayFilter;
import org.kurento.client.MediaElement;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.room.NotificationRoomManager;
import org.kurento.room.api.pojo.ParticipantRequest;
import org.kurento.room.rpc.JsonRpcUserControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * User control that applies a face overlay filter when publishing video.
 *
 * @author Radu Tom Vlad (rvlad@naevatec.com)
 */
public class DemoJsonRpcUserControl extends JsonRpcUserControl {

  private static final String SESSION_ATTRIBUTE_HAT_FILTER = "hatFilter";

  private static final String CUSTOM_REQUEST_HAT_PARAM = "hat";

  private static final Logger log = LoggerFactory.getLogger(DemoJsonRpcUserControl.class);

  private String hatUrl;

  private float offsetXPercent;
  private float offsetYPercent;
  private float widthPercent;
  private float heightPercent;

  public DemoJsonRpcUserControl(NotificationRoomManager roomManager) {
    super(roomManager);
  }

  public void setHatUrl(String hatUrl) {
    this.hatUrl = hatUrl;
    log.info("Hat URL: {}", hatUrl);
  }

  public void setHatCoords(JsonObject hatCoords) {
    if (hatCoords.get("offsetXPercent") != null) {
      offsetXPercent = hatCoords.get("offsetXPercent").getAsFloat();
    }
    if (hatCoords.get("offsetYPercent") != null) {
      offsetYPercent = hatCoords.get("offsetYPercent").getAsFloat();
    }
    if (hatCoords.get("widthPercent") != null) {
      widthPercent = hatCoords.get("widthPercent").getAsFloat();
    }
    if (hatCoords.get("heightPercent") != null) {
      heightPercent = hatCoords.get("heightPercent").getAsFloat();
    }
    log.info("Hat coords:\n\toffsetXPercent = {}\n\toffsetYPercent = {}"
        + "\n\twidthPercent = {}\n\theightPercent = {}", offsetXPercent, offsetYPercent,
        widthPercent, heightPercent);
  }

  @Override
  public void customRequest(Transaction transaction, Request<JsonObject> request,
      ParticipantRequest participantRequest) {
    try {
      if (request.getParams() == null || request.getParams().get(CUSTOM_REQUEST_HAT_PARAM) == null) {
        throw new RuntimeException("Request element '" + CUSTOM_REQUEST_HAT_PARAM + "' is missing");
      }
      boolean hatOn = request.getParams().get(CUSTOM_REQUEST_HAT_PARAM).getAsBoolean();
      String pid = participantRequest.getParticipantId();
      if (hatOn) {
        if (transaction.getSession().getAttributes().containsKey(SESSION_ATTRIBUTE_HAT_FILTER)) {
          throw new RuntimeException("Hat filter already on");
        }
        log.info("Applying face overlay filter to session {}", pid);
        FaceOverlayFilter faceOverlayFilter = new FaceOverlayFilter.Builder(
            roomManager.getPipeline(pid)).build();
        faceOverlayFilter.setOverlayedImage(this.hatUrl, this.offsetXPercent, this.offsetYPercent,
            this.widthPercent, this.heightPercent);
        roomManager.addMediaElement(pid, faceOverlayFilter);
        transaction.getSession().getAttributes()
        .put(SESSION_ATTRIBUTE_HAT_FILTER, faceOverlayFilter);
      } else {
        if (!transaction.getSession().getAttributes().containsKey(SESSION_ATTRIBUTE_HAT_FILTER)) {
          throw new RuntimeException("This user has no hat filter yet");
        }
        log.info("Removing face overlay filter from session {}", pid);
        roomManager.removeMediaElement(pid, (MediaElement) transaction.getSession().getAttributes()
            .get(SESSION_ATTRIBUTE_HAT_FILTER));
        transaction.getSession().getAttributes().remove(SESSION_ATTRIBUTE_HAT_FILTER);
      }
      transaction.sendResponse(new JsonObject());
    } catch (Exception e) {
      log.error("Unable to handle custom request", e);
      try {
        transaction.sendError(e);
      } catch (IOException e1) {
        log.warn("Unable to send error response", e1);
      }
    }
  }
}
