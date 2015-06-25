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
import org.kurento.client.MediaElement;
import org.kurento.room.RoomManager;
import org.kurento.room.api.pojo.ParticipantRequest;
import org.kurento.room.api.pojo.UserParticipant;
import org.kurento.room.exception.RoomException;

/**
 * Through this interface, the room API passes the execution result of client
 * primitives to the application and from there to the clients. It’s the
 * application’s duty to respect this contract.
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public interface RoomEventHandler {

	/**
	 * Called as a result of
	 * {@link RoomManager#joinRoom(String, String, ParticipantRequest)} when the
	 * specified room doesn't exist and it's successfully created. Normally this
	 * event won't have to be notified to the user(s).
	 * 
	 * @param request instance of {@link ParticipantRequest} POJO to identify
	 *        the user and the request
	 * @param roomName the room's name
	 */
	void onRoomCreated(ParticipantRequest request, String roomName);

	/**
	 * Called as a result of
	 * {@link RoomManager#joinRoom(String, String, ParticipantRequest)}. The new
	 * participant should be responded with all the available information: the
	 * existing peers and, for any publishers, their stream names. The current
	 * peers should receive a notification of the join event.
	 * 
	 * @param request instance of {@link ParticipantRequest} POJO to identify
	 *        the user and the request
	 * @param roomName the room's name
	 * @param newUserName the new user
	 * @param existingParticipants instances of {@link UserParticipant} POJO
	 *        that represent the already existing peers
	 * @param error instance of {@link RoomException} POJO, includes a code and
	 *        error message. If not null, then the join was unsuccessful and the
	 *        user should be responded accordingly.
	 */
	void onParticipantJoined(ParticipantRequest request, String roomName,
			String newUserName, Set<UserParticipant> existingParticipants,
			RoomException error);

	/**
	 * Called as a result of
	 * {@link RoomManager#leaveRoom(String, String, ParticipantRequest)} or
	 * {@link RoomManager#evictParticipant(String)} (admin action). The user
	 * should receive an acknowledgement if the operation completed
	 * successfully, and the remaining peers should be notified of this event.
	 * 
	 * @param request instance of {@link ParticipantRequest} POJO to identify
	 *        the user and the request
	 * @param roomName the room's name
	 * @param userName the departing user's name
	 * @param remainingParticipantIds identifiers of the participants in the
	 *        room
	 * @param error instance of {@link RoomException} POJO, includes a code and
	 *        error message. If not null, then the operation was unsuccessful
	 *        and the user should be responded accordingly.
	 */
	void onParticipantLeft(ParticipantRequest request, String roomName,
			String userName, Set<String> remainingParticipantIds,
			RoomException error);

	/**
	 * Called as a result of
	 * {@link RoomManager#publishMedia(String, ParticipantRequest, MediaElement...)}
	 * . The user should receive the generated SPD answer from the local WebRTC
	 * endpoint, and the other peers should be notified of this event.
	 * 
	 * @param request instance of {@link ParticipantRequest} POJO to identify
	 *        the user and the request
	 * @param publisherName the user name
	 * @param sdpAnswer String with generated SPD answer from the local WebRTC
	 *        endpoint
	 * @param participantIds identifiers of ALL the participants in the room
	 *        (includes the publisher)
	 * @param error instance of {@link RoomException} POJO, includes a code and
	 *        error message. If not null, then the operation was unsuccessful
	 *        and the user should be responded accordingly.
	 */
	void onPublishMedia(ParticipantRequest request, String publisherName,
			String sdpAnswer, Set<String> participantIds, RoomException error);

	/**
	 * Called as a result of
	 * {@link RoomManager#unpublishMedia(ParticipantRequest)}. The user should
	 * receive an acknowledgement if the operation completed successfully, and
	 * all other peers in the room should be notified of this event.
	 * 
	 * @param request instance of {@link ParticipantRequest} POJO to identify
	 *        the user and the request
	 * @param publisherName the user name
	 * @param participantIds identifiers of ALL the participants in the room
	 *        (includes the publisher)
	 * @param error instance of {@link RoomException} POJO, includes a code and
	 *        error message. If not null, then the operation was unsuccessful
	 *        and the user should be responded accordingly.
	 */
	void onUnpublishMedia(ParticipantRequest request, String publisherName,
			Set<String> participantIds, RoomException error);

	/**
	 * Called as a result of
	 * {@link RoomManager#subscribe(String, String, ParticipantRequest)}. The
	 * user should be responded with generated SPD answer from the local WebRTC
	 * endpoint.
	 * 
	 * @param request instance of {@link ParticipantRequest} POJO to identify
	 *        the user and the request
	 * @param sdpAnswer String with generated SPD answer from the local WebRTC
	 *        endpoint
	 * @param error instance of {@link RoomException} POJO, includes a code and
	 *        error message. If not null, then the operation was unsuccessful
	 *        and the user should be responded accordingly.
	 */
	void onSubscribe(ParticipantRequest request, String sdpAnswer,
			RoomException error);

	/**
	 * Called as a result of
	 * {@link RoomManager#unsubscribe(String, ParticipantRequest)}. The user
	 * should receive an acknowledgement if the operation completed successfully
	 * (no error).
	 * 
	 * @param request instance of {@link ParticipantRequest} POJO to identify
	 *        the user and the request
	 * @param error instance of {@link RoomException} POJO, includes a code and
	 *        error message. If not null, then the operation was unsuccessful
	 *        and the user should be responded accordingly.
	 */
	void onUnsubscribe(ParticipantRequest request, RoomException error);

	/**
	 * Called as a result of
	 * {@link RoomManager#sendMessage(String, String, String, ParticipantRequest)}
	 * . The user should receive an acknowledgement if the operation completed
	 * successfully, and all the peers in the room should be notified with the
	 * message contents and its origin.
	 * 
	 * @param request instance of {@link ParticipantRequest} POJO to identify
	 *        the user and the request
	 * @param message String with the message body
	 * @param userName name of the peer that sent it
	 * @param roomName the current room name
	 * @param participantIds identifiers of ALL the participants in the room
	 *        (includes the sender)
	 * @param error instance of {@link RoomException} POJO, includes a code and
	 *        error message. If not null, then the operation was unsuccessful
	 *        and the user should be responded accordingly.
	 */
	void onSendMessage(ParticipantRequest request, String message,
			String userName, String roomName, Set<String> participantIds,
			RoomException error);

	/**
	 * Called as a result of
	 * {@link RoomManager#onIceCandidate(String, String, int, String, ParticipantRequest)}
	 * . The user should receive an acknowledgement if the operation completed
	 * successfully (no error).
	 * 
	 * @param request instance of {@link ParticipantRequest} POJO to identify
	 *        the user and the request
	 * @param error instance of {@link RoomException} POJO, includes a code and
	 *        error message. If not null, then the operation was unsuccessful
	 *        and the user should be responded accordingly.
	 */
	void onRecvIceCandidate(ParticipantRequest request, RoomException error);

	/**
	 * Called when a new {@link IceCandidate} is gathered for the local WebRTC
	 * endpoint. The user should receive a notification with all the provided
	 * information so that the candidate is added to the remote WebRTC peer.
	 * 
	 * @param participantId identifier of the participant
	 * @param endpointName String the identifier of the local WebRTC endpoint
	 *        (created in the server)
	 * @param candidate the gathered {@link IceCandidate}
	 */
	void onSendIceCandidate(String participantId, String endpointName,
			IceCandidate candidate);

	/**
	 * Called as a result of {@link RoomManager#closeRoom(String)} or
	 * {@link RoomManager#evictParticipant(String)} - server domain methods, not
	 * as a consequence of a room API request. All resources on the server,
	 * associated with the room, have been released. The existing participants
	 * in the room should be notified of this event so that the client-side
	 * application acts accordingly.
	 * 
	 * @param roomName the room that's just been closed
	 * @param participantIds identifiers of the participants in the room
	 */
	void onRoomClosed(String roomName, Set<String> participantIds);

	/**
	 * Called as a result of an error intercepted on the media pipeline. The
	 * affected participants should be notified.
	 * @param roomName the room where the error occurred
	 * @param participantIds the participants identifiers
	 * @param description description of the error
	 */
	void onPipelineError(String roomName, Set<String> participantIds, String description);

	/**
	 * Called as a result of an error intercepted on a media element of a
	 * participant. The participant should be notified.
	 * @param participantId identifier of the participant
	 * @param description description of the error
	 */
	void onParticipantMediaError(String participantId, String description);
}
