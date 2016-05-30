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

package org.kurento.room.api;

import java.util.Set;

import org.kurento.client.IceCandidate;

/**
 * Handler for events triggered from media objects.
 *
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public interface RoomHandler {

  /**
   * Called when a new {@link IceCandidate} is gathered for the local WebRTC endpoint. The user
   * should receive a notification with all the provided information so that the candidate is added
   * to the remote WebRTC peer.
   *
   * @param roomName
   *          name of the room
   * @param participantId
   *          identifier of the participant
   * @param endpoint
   *          String the identifier of the local WebRTC endpoint (created in the server)
   * @param candidate
   *          the gathered {@link IceCandidate}
   */
  void onIceCandidate(String roomName, String participantId, String endpoint, IceCandidate candidate);

  /**
   * Called as a result of an error intercepted on a media element of a participant. The participant
   * should be notified.
   *
   * @param roomName
   *          name of the room
   * @param participantId
   *          identifier of the participant
   * @param description
   *          description of the error
   */
  void onMediaElementError(String roomName, String participantId, String errorDescription);

  /**
   * Called as a result of an error intercepted on the media pipeline. The affected participants
   * should be notified.
   *
   * @param roomName
   *          the room where the error occurred
   * @param participantIds
   *          the participants identifiers
   * @param description
   *          description of the error
   */
  void onPipelineError(String roomName, Set<String> participantIds, String errorDescription);
}
