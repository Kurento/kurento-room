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
import org.kurento.module.markerdetector.ArMarkerdetector;
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


  private static final String SESSION_ATTRIBUTE_FILTER = "customFilter";

  private static final Logger log = LoggerFactory.getLogger(DemoJsonRpcUserControl.class);

  private KmsFilterType filterType = KmsFilterType.HAT;

  private String hatUrl;
  private float offsetXPercent;
  private float offsetYPercent;
  private float widthPercent;
  private float heightPercent;

  private String markerUrl;

  public DemoJsonRpcUserControl(NotificationRoomManager roomManager) {
    super(roomManager);
  }

  public void setFilterType(KmsFilterType type) {
    this.filterType = type;
  }

  public void setHatUrl(String hatUrl) {
    this.hatUrl = hatUrl;
    log.info("Hat URL: {}", hatUrl);
  }

  public void setMarkerUrl(String markerUrl) {
    this.markerUrl = markerUrl;
    log.info("Marker URL: {}", markerUrl);
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
      if (request.getParams() == null
          || request.getParams().get(filterType.getCustomRequestParam()) == null) {
        throw new RuntimeException(
            "Request element '" + filterType.getCustomRequestParam() + "' is missing");
      }
      boolean filterOn = request.getParams().get(filterType.getCustomRequestParam()).getAsBoolean();
      String pid = participantRequest.getParticipantId();
      if (filterOn) {
        if (transaction.getSession().getAttributes().containsKey(SESSION_ATTRIBUTE_FILTER)) {
          throw new RuntimeException(filterType + " filter already on");
        }
        log.info("Applying {} filter to session {}", filterType, pid);

        MediaElement filter = createFilter(pid);

        roomManager.addMediaElement(pid, filter);
        transaction.getSession().getAttributes().put(SESSION_ATTRIBUTE_FILTER, filter);
      } else {
        if (!transaction.getSession().getAttributes().containsKey(SESSION_ATTRIBUTE_FILTER)) {
          throw new RuntimeException("This user has no " + filterType + " filter yet");
        }
        log.info("Removing {} filter from session {}", filterType, pid);
        roomManager.removeMediaElement(pid, (MediaElement) transaction.getSession().getAttributes()
            .get(SESSION_ATTRIBUTE_FILTER));
        transaction.getSession().getAttributes().remove(SESSION_ATTRIBUTE_FILTER);
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

  private MediaElement createFilter(String pid) {
    switch (filterType) {
      case MARKER:
        ArMarkerdetector armFilter =
            new ArMarkerdetector.Builder(roomManager.getPipeline(pid)).build();
        armFilter.setShowDebugLevel(0);
        // armFilter.setOverlayText("Huuhaa");
        armFilter.setOverlayImage(markerUrl);
        return armFilter;
      case HAT:
      default:
        FaceOverlayFilter fofilter =
            new FaceOverlayFilter.Builder(roomManager.getPipeline(pid)).build();
        fofilter.setOverlayedImage(this.hatUrl, this.offsetXPercent, this.offsetYPercent,
            this.widthPercent, this.heightPercent);
        return fofilter;
    }
  }
}
