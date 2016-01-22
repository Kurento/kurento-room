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
