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

package org.kurento.room;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PreDestroy;

import org.kurento.client.IceCandidate;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaElement;
import org.kurento.client.MediaPipeline;
import org.kurento.room.api.KurentoClientProvider;
import org.kurento.room.api.RoomEventHandler;
import org.kurento.room.api.UserNotificationService;
import org.kurento.room.api.pojo.ParticipantRequest;
import org.kurento.room.api.pojo.UserParticipant;
import org.kurento.room.endpoint.SdpType;
import org.kurento.room.exception.AdminException;
import org.kurento.room.exception.RoomException;
import org.kurento.room.exception.RoomException.Code;
import org.kurento.room.internal.DefaultRoomEventHandler;
import org.kurento.room.internal.Participant;
import org.kurento.room.internal.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Kurento room manager represents an SDK for any developer that wants to
 * implement the Room server-side application. They can build their application
 * on top of the manager’s Java API and implement their desired business logic
 * without having to consider room or media-specific details.
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class RoomManager {
	private final Logger log = LoggerFactory.getLogger(RoomManager.class);

	private RoomEventHandler roomEventHandler;
	private KurentoClientProvider kcProvider;

	private final ConcurrentMap<String, Room> rooms =
			new ConcurrentHashMap<String, Room>();

	/**
	 * Provides an instance of the room manager by setting an user notification
	 * service that will be used by the default event handler to send responses
	 * and notifications back to the clients.
	 * 
	 * @param notificationService encapsulates the communication layer, used to
	 *        instantiate {@link DefaultRoomEventHandler}
	 * @param kcProvider enables the manager to obtain Kurento Client instances
	 */
	public RoomManager(UserNotificationService notificationService,
			KurentoClientProvider kcProvider) {
		super();
		this.roomEventHandler =
				new DefaultRoomEventHandler(notificationService);
		this.kcProvider = kcProvider;
	}

	/**
	 * Provides an instance of the room manager by setting an event handler.
	 * 
	 * @param roomEventHandler the room event handler implementation
	 * @param kcProvider enables the manager to obtain Kurento Client instances
	 */
	public RoomManager(RoomEventHandler roomEventHandler,
			KurentoClientProvider kcProvider) {
		super();
		this.roomEventHandler = roomEventHandler;
		this.kcProvider = kcProvider;
	}

	/**
	 * Represents a client’s request to join a room. If the room does not exist,
	 * it is created.<br/>
	 * <strong>Side effects:</strong> The room event handler should send
	 * notifications to the existing participants in the room to inform about
	 * the new peer.
	 * 
	 * @param userName name or identifier of the user in the room. Will be used
	 *        to identify her WebRTC media peer (from the client-side).
	 * @param roomName name or identifier of the room
	 * @param request instance of {@link ParticipantRequest} POJO containing the
	 *        participant’s id and a request id (optional identifier of the
	 *        request at the communications level, included when responding back
	 *        to the client)
	 */
	public void joinRoom(String userName, String roomName,
			ParticipantRequest request) {
		log.debug("Request [JOIN_ROOM] user={}, room={} ({})", userName,
				roomName, request);
		try {
			Room room = getRoom(roomName, request, true);
			if (!room.isClosed()) {
				Set<UserParticipant> existingParticipants =
						getParticipants(roomName);
				room.join(request.getParticipantId(), userName);
				roomEventHandler.onParticipantJoined(request, roomName,
						userName, existingParticipants, null);
			} else {
				log.error("Trying to join room {} but it is closing",
						room.getName());
				throw new RoomException(Code.ROOM_CLOSED_ERROR_CODE,
						"Trying to join room '" + room.getName()
								+ "' but it is closing");
			}
		} catch (RoomException e) {
			log.warn("PARTICIPANT {}: Error joining/creating room {}",
					userName, roomName, e);
			roomEventHandler.onParticipantJoined(request, roomName, userName,
					null, e);
		} catch (AdminException e) {
			log.warn("PARTICIPANT {}: Error joining/creating room {}",
					userName, roomName, e);
			roomEventHandler.onParticipantJoined(
					request,
					roomName,
					userName,
					null,
					new RoomException(Code.ROOM_NOT_FOUND_ERROR_CODE, e
							.getMessage()));
		}
	}

	/**
	 * Represents a client’s notification that she’s leaving the room. <br/>
	 * <strong>Side effects:</strong> The room event handler should acknowledge
	 * the client’s request by sending an empty message. Should also send
	 * notifications to the other participants in the room to inform about the
	 * one that has just left.
	 * 
	 * @param request instance of {@link ParticipantRequest} POJO
	 */
	public void leaveRoom(ParticipantRequest request) {
		log.debug("Request [LEAVE_ROOM] ({})", request);
		try {
			Participant participant =
					getParticipant(request.getParticipantId());
			if (participant == null)
				throw new RoomException(Code.USER_NOT_FOUND_ERROR_CODE,
						"No participant with id '" + request.getParticipantId()
								+ "' was found");
			Room room = participant.getRoom();
			String roomName = room.getName();
			if (!room.isClosed()) {
				room.leave(request.getParticipantId());
				Set<String> remainingParticipantIds = room.getParticipantIds();
				if (remainingParticipantIds.isEmpty()) {
					room.close();
					rooms.remove(room.getName());
					log.warn("Room '{}' removed and closed", roomName);
				}
				roomEventHandler.onParticipantLeft(request, roomName,
						participant.getName(), remainingParticipantIds, null);
			} else {
				log.warn("Trying to leave from room '{}' but it is closing",
						room.getName());
			}
		} catch (RoomException e) {
			log.warn("Error leaving room", e);
			roomEventHandler.onParticipantLeft(request, null, null, null, e);
		}
	}

	/**
	 * Represents a client’s request to start streaming her local media to
	 * anyone inside the room. The media elements should have been created using
	 * the same pipeline as the publisher's. The streaming media endpoint
	 * situated on the server is automatically connected to itself thus
	 * realizing what is known as a loopback connection. The loopback is
	 * performed after applying all additional media elements specified as
	 * parameters (in the same order as they appear in the params list).<br/>
	 * <strong>Side effects:</strong> The room event handler should send
	 * notifications to the existing participants in the room to inform about
	 * the new stream that has been published. Should also answer to the
	 * client’s endpoint by sending it the SDP answer generated by the WebRTC
	 * endpoint on the server.
	 * 
	 * @param request instance of {@link ParticipantRequest} POJO
	 * @param sdpOffer SDP offer String generated by the client’s WebRTC peer
	 * @param doLoopback loopback flag
	 * @param mediaElements variable array of media elements (filters,
	 *        recorders, etc.) that are connected between the source WebRTC
	 *        endpoint and the subscriber endpoints
	 */
	public void publishMedia(ParticipantRequest request, String sdpOffer,
			boolean doLoopback, MediaElement... mediaElements) {
		log.debug(
				"Request [PUBLISH_MEDIA] sdpOffer={} dooLoopback={} mediaElements={} ({})",
				sdpOffer, doLoopback, mediaElements, request);

		try {
			Participant participant = getParticipant(request);
			if (participant == null)
				throw new RoomException(Code.USER_NOT_FOUND_ERROR_CODE,
						"No participant with id '" + request.getParticipantId()
								+ "' was found");
			String name = participant.getName();
			Room room = participant.getRoom();

			participant.createPublishingEndpoint();

			for (MediaElement elem : mediaElements)
				participant.getPublisher().apply(elem);

			String sdpAnswer =
					participant.publishToRoom(SdpType.OFFER, sdpOffer,
							doLoopback, null);
			if (sdpAnswer != null)
				roomEventHandler.onPublishMedia(request, name, sdpAnswer,
						room.getParticipantIds(), null);
			else
				throw new RoomException(Code.SDP_ERROR_CODE,
						"Error generating SDP answer for publishing user "
								+ name);

			room.newPublisher(participant);
		} catch (RoomException e) {
			log.warn("Error publishing media", e);
			roomEventHandler.onPublishMedia(request, null, null, null, e);
		}
	}

	/**
	 * Represents a client’s request to stop publishing her media stream. All
	 * media elements on the server-side connected to this peer will be
	 * disconnected and released. The peer is left ready for publishing her
	 * media in the future.<br/>
	 * <strong>Side effects:</strong> The room event handler should send
	 * notifications to the existing participants in the room to inform that
	 * streaming from this endpoint has ended. Should also acknowledge the
	 * client’s request by sending an empty message.
	 * 
	 * @param request instance of {@link ParticipantRequest} POJO
	 */
	public void unpublishMedia(ParticipantRequest request) {
		log.debug("Request [UNPUBLISH_MEDIA] ({})", request);

		try {
			Participant participant = getParticipant(request);
			if (participant == null)
				throw new RoomException(Code.USER_NOT_FOUND_ERROR_CODE,
						"No participant with id '" + request.getParticipantId()
								+ "' was found");
			if (!participant.isStreaming())
				throw new RoomException(Code.USER_NOT_STREAMING_ERROR_CODE,
						"Participant with id '" + request.getParticipantId()
								+ "' is not streaming media");
			String name = participant.getName();
			Room room = participant.getRoom();
			participant.unpublishMedia();
			room.cancelPublisher(participant);
			roomEventHandler.onUnpublishMedia(request, name,
					room.getParticipantIds(), null);
		} catch (RoomException e) {
			log.warn("Error unpublishing media", e);
			roomEventHandler.onUnpublishMedia(request, null, null, e);
		}
	}

	/**
	 * Represents a client’s request to receive media from room participants
	 * that published their media. Will have the same result when a publisher
	 * requests its own media stream.<br/>
	 * <strong>Side effects:</strong> The room event handler should answer to
	 * the client’s endpoint by sending it the SDP answer generated by the its
	 * receiving WebRTC endpoint on the server.
	 * 
	 * @param remoteName identification of the remote stream which is
	 *        effectively the peer’s name (participant)
	 * @param sdpOffer SDP offer String generated by the client’s WebRTC peer
	 * @param request instance of {@link ParticipantRequest} POJO
	 */
	public void subscribe(String remoteName, String sdpOffer,
			ParticipantRequest request) {
		log.debug("Request [SUBSCRIBE] remoteParticipant={} sdpOffer={} ({})",
				remoteName, sdpOffer, request);

		try {
			Participant participant = getParticipant(request);
			if (participant == null)
				throw new RoomException(Code.USER_NOT_FOUND_ERROR_CODE,
						"No participant with id '" + request.getParticipantId()
								+ "' was found");
			String name = participant.getName();
			Room room = participant.getRoom();

			Participant senderParticipant =
					room.getParticipantByName(remoteName);
			if (senderParticipant == null) {
				log.warn(
						"PARTICIPANT {}: Requesting to recv media from user {} "
								+ "in room {} but user could not be found",
						name, remoteName, room.getName());
				throw new RoomException(Code.USER_NOT_FOUND_ERROR_CODE, "User "
						+ remoteName + " not found in room " + room.getName());
			}

			String sdpAnswer =
					participant.receiveMediaFrom(senderParticipant, sdpOffer);
			if (sdpAnswer != null)
				roomEventHandler.onSubscribe(request, sdpAnswer, null);
			else
				throw new RoomException(Code.SDP_ERROR_CODE,
						"Error generating SDP answer for receiving user "
								+ name + " from " + remoteName);
		} catch (RoomException e) {
			log.warn("Error subscribing to {}", remoteName, e);
			roomEventHandler.onSubscribe(request, null, e);
		}
	}

	/**
	 * Represents a client’s request to stop receiving media from the remote
	 * peer. <br/>
	 * <strong>Side effects:</strong> The room event handler should acknowledge
	 * the client’s request by sending an empty message.
	 * 
	 * @param remoteName identification of the remote stream which is
	 *        effectively the peer’s name (participant)
	 * @param request instance of {@link ParticipantRequest} POJO
	 */
	public void unsubscribe(String remoteName, ParticipantRequest request) {
		log.debug("Request [UNSUBSCRIBE] remoteParticipant={} ({})",
				remoteName, request);

		try {
			Participant participant = getParticipant(request);
			if (participant == null)
				throw new RoomException(Code.USER_NOT_FOUND_ERROR_CODE,
						"No participant with id '" + request.getParticipantId()
								+ "' was found");
			String name = participant.getName();
			Room room = participant.getRoom();

			Participant senderParticipant =
					room.getParticipantByName(remoteName);
			if (senderParticipant == null) {
				log.warn(
						"PARTICIPANT {}: Requesting to unsubscribe from user {} "
								+ "in room {} but user could not be found",
						name, remoteName, room.getName());
				throw new RoomException(Code.USER_NOT_FOUND_ERROR_CODE, "User "
						+ remoteName + " not found in room " + room.getName());
			}
			participant.cancelReceivingMedia(remoteName);
			roomEventHandler.onUnsubscribe(request, null);
		} catch (RoomException e) {
			log.warn("Error unsubscribing from {}", remoteName, e);
			roomEventHandler.onUnsubscribe(request, e);
		}
	}

	/**
	 * Used by clients to send written messages to all other participants in the
	 * room.<br/>
	 * <strong>Side effects:</strong> The room event handler should acknowledge
	 * the client’s request by sending an empty message. Should also send
	 * notifications to the all participants in the room with the message and
	 * its sender.
	 * 
	 * @param message message contents
	 * @param userName name or identifier of the user in the room
	 * @param roomName room’s name
	 * @param request instance of {@link ParticipantRequest} POJO
	 */
	public void sendMessage(String message, String userName, String roomName,
			ParticipantRequest request) {
		log.debug("Request [SEND_MESSAGE] message={} ({})", message, request);

		try {
			Participant participant = getParticipant(request);
			if (participant == null)
				throw new RoomException(Code.USER_NOT_FOUND_ERROR_CODE,
						"No participant with id '" + request.getParticipantId()
								+ "' was found");
			String name = participant.getName();
			if (!name.equals(userName))
				throw new RoomException(Code.USER_NOT_FOUND_ERROR_CODE,
						"Provided username '" + userName
								+ "' differs from the participant's name");
			Room room = participant.getRoom();
			if (!room.getName().equals(roomName))
				throw new RoomException(Code.ROOM_NOT_FOUND_ERROR_CODE,
						"Provided room name '" + roomName
								+ "' differs from the participant's room");
			roomEventHandler.onSendMessage(request, message, userName,
					roomName, room.getParticipantIds(), null);
		} catch (RoomException e) {
			log.warn("Error sending message", e);
			roomEventHandler.onSendMessage(request, null, null, null, null, e);
		}
	}

	/**
	 * Request that carries info about an ICE candidate gathered on the client
	 * side. This information is required to implement the trickle ICE
	 * mechanism. Should be triggered or called whenever an icecandidate event
	 * is created by a RTCPeerConnection.<br/>
	 * <strong>Side effects:</strong> The room event handler should acknowledge
	 * the client’s request by sending an empty message.
	 * 
	 * @param endpointName the name of the peer whose ICE candidate was gathered
	 * @param candidate the candidate attribute information
	 * @param sdpMLineIndex the index (starting at zero) of the m-line in the
	 *        SDP this candidate is associated with
	 * @param sdpMid media stream identification, "audio" or "video", for the
	 *        m-line this candidate is associated with
	 * @param request instance of {@link ParticipantRequest} POJO
	 */
	public void onIceCandidate(String endpointName, String candidate,
			int sdpMLineIndex, String sdpMid, ParticipantRequest request) {
		log.debug("Request [ICE_CANDIDATE] endpoint={} candidate={} "
				+ "sdpMLineIdx={} sdpMid={} ({})", endpointName, candidate,
				sdpMLineIndex, sdpMid, request);

		try {
			Participant participant = getParticipant(request);
			if (participant == null)
				throw new RoomException(Code.USER_NOT_FOUND_ERROR_CODE,
						"No participant with id '" + request.getParticipantId()
								+ "' was found");
			participant.addIceCandidate(endpointName, new IceCandidate(
					candidate, sdpMid, sdpMLineIndex));
			roomEventHandler.onRecvIceCandidate(request, null);
		} catch (RoomException e) {
			log.warn("Error receiving ICE candidate", e);
			roomEventHandler.onRecvIceCandidate(request, e);
		}
	}

	// ----------------- ADMIN (DIRECT or SERVER-SIDE) REQUESTS ------------
	/**
	 * Closes all resources. This method has been annotated with the @PreDestroy
	 * directive (javax.annotation package) so that it will be automatically
	 * called when the RoomManager instance is container-managed. <br/>
	 * <strong>Side effects:</strong> The room event handler should send
	 * notifications to all participants to inform that their room has been
	 * forcibly closed.
	 * 
	 * @see RoomManager#closeRoom(String)
	 */
	@PreDestroy
	public void close() {
		for (String roomName : rooms.keySet())
			try {
				closeRoom(roomName);
			} catch (Exception e) {
				log.warn("Error closing room '{}'", roomName, e);
			}
	}

	/**
	 * Method that tries to remove a participant from her room. <br/>
	 * <strong>Side effects:</strong> The room event handler should notify the
	 * remaining participants. If none are left and the room gets closed, the
	 * {@link RoomEventHandler#onRoomClosed(String, Set)} method will be called.
	 * 
	 * @param participantId identifier of the participant
	 * @throws AdminException in case that no participant exists with the given
	 *         id or it was impossible to perform the operation
	 */
	public void evictParticipant(String participantId) throws AdminException {
		try {
			Participant participant = getParticipant(participantId);
			if (participant == null)
				throw new AdminException("No participant with id '"
						+ participantId + "' was found");
			Room room = participant.getRoom();
			String roomName = room.getName();
			if (!room.isClosed()) {
				room.leave(participantId);
				Set<String> remainingParticipantIds = room.getParticipantIds();
				if (remainingParticipantIds.isEmpty()) {
					room.close();
					rooms.remove(room.getName());
					log.warn("Room '{}' removed and closed", roomName);
					roomEventHandler.onRoomClosed(roomName,
							remainingParticipantIds);
				} else
					roomEventHandler.onParticipantLeft(null, roomName,
							participant.getName(), remainingParticipantIds,
							null);
			} else
				log.warn(
						"Trying to evict participant with id '{}' from room '{}' but it is closing",
						participantId, room.getName());
		} catch (RoomException e) {
			log.warn("Error evicting participant with id {}", participantId, e);
			throw new AdminException("Unable to evict participant with id "
					+ participantId + ": " + e.getMessage());
		}
	}

	/**
	 * Returns all currently active (opened) rooms.
	 * 
	 * @return set of the rooms’ identifiers (names)
	 */
	public Set<String> getRooms() {
		return new HashSet<String>(rooms.keySet());
	}

	/**
	 * Returns all the participants inside a room.
	 * 
	 * @param roomName name or identifier of the room
	 * @return set of {@link UserParticipant} POJOS (an instance contains the
	 *         participant’s identifier and her user name)
	 * @throws AdminException in case the room doesn’t exist
	 */
	public Set<UserParticipant> getParticipants(String roomName)
			throws AdminException {
		Room room = rooms.get(roomName);
		if (room == null)
			throw new AdminException("Room '" + roomName + "' not found");
		Collection<Participant> participants = room.getParticipants();
		Set<UserParticipant> userParts = new HashSet<UserParticipant>();
		for (Participant p : participants)
			if (!p.isClosed())
				userParts.add(new UserParticipant(p.getId(), p.getName(), p
						.isStreaming()));
		return userParts;
	}

	/**
	 * Returns all the publishers (participants streaming their media) inside a
	 * room.
	 * 
	 * @param roomName name or identifier of the room
	 * @return set of {@link UserParticipant} POJOS representing the existing
	 *         publishers
	 * @throws AdminException in case the room doesn’t exist
	 */
	public Set<UserParticipant> getPublishers(String roomName)
			throws AdminException {
		Room r = rooms.get(roomName);
		if (r == null)
			throw new AdminException("Room '" + roomName + "' not found");
		Collection<Participant> participants = r.getParticipants();
		Set<UserParticipant> userParts = new HashSet<UserParticipant>();
		for (Participant p : participants)
			if (!p.isClosed() && p.isStreaming())
				userParts
						.add(new UserParticipant(p.getId(), p.getName(), true));
		return userParts;
	}

	/**
	 * Returns all the subscribers (participants subscribed to a least one
	 * stream of another user) inside a room. A publisher is automatically
	 * subscribed to its own stream (loopback) and will not be included in the
	 * returned values unless it requests explicitly a connection to its own or
	 * another user’s stream.
	 * 
	 * @param roomName name or identifier of the room
	 * @return set of {@link UserParticipant} POJOS representing the existing
	 *         subscribers
	 * @throws AdminException in case the room doesn’t exist
	 */
	public Set<UserParticipant> getSubscribers(String roomName)
			throws AdminException {
		Room r = rooms.get(roomName);
		if (r == null)
			throw new AdminException("Room '" + roomName + "' not found");
		Collection<Participant> participants = r.getParticipants();
		Set<UserParticipant> userParts = new HashSet<UserParticipant>();
		for (Participant p : participants)
			if (!p.isClosed() && p.isSubscribed())
				userParts.add(new UserParticipant(p.getId(), p.getName(), p
						.isStreaming()));
		return userParts;
	}

	/**
	 * Returns the peer’s publishers (participants from which the peer is
	 * receiving media). The own stream doesn’t count.
	 * 
	 * @param participantId identifier of the participant
	 * @return set of {@link UserParticipant} POJOS representing the publishers
	 *         this participant is currently subscribed to
	 * @throws AdminException in case the participant doesn’t exist
	 */
	public Set<UserParticipant> getPeerPublishers(String participantId)
			throws AdminException {
		Participant participant = getParticipant(participantId);
		if (participant == null)
			throw new AdminException("No participant with id '" + participantId
					+ "' was found");
		Set<String> subscribedEndpoints =
				participant.getConnectedSubscribedEndpoints();
		Room room = participant.getRoom();
		Set<UserParticipant> userParts = new HashSet<UserParticipant>();
		for (String epName : subscribedEndpoints) {
			Participant p = room.getParticipantByName(epName);
			userParts.add(new UserParticipant(p.getId(), p.getName()));
		}
		return userParts;
	}

	/**
	 * Returns the peer’s subscribers (participants towards the peer is
	 * streaming media). The own stream doesn’t count.
	 * 
	 * @param participantId identifier of the participant
	 * @return set of {@link UserParticipant} POJOS representing the
	 *         participants subscribed to this peer
	 * @throws AdminException in case the participant doesn’t exist
	 */
	public Set<UserParticipant> getPeerSubscribers(String participantId)
			throws AdminException {
		Participant participant = getParticipant(participantId);
		if (participant == null)
			throw new AdminException("No participant with id '" + participantId
					+ "' was found");
		if (!participant.isStreaming())
			throw new AdminException("Participant with id '" + participantId
					+ "' is not a publisher yet");
		Set<UserParticipant> userParts = new HashSet<UserParticipant>();
		Room room = participant.getRoom();
		String endpointName = participant.getName();
		for (Participant p : room.getParticipants()) {
			if (p.equals(participant))
				continue;
			Set<String> subscribedEndpoints =
					p.getConnectedSubscribedEndpoints();
			if (subscribedEndpoints.contains(endpointName))
				userParts.add(new UserParticipant(p.getId(), p.getName()));
		}
		return userParts;
	}

	/**
	 * Creates a room if it doesn’t already exist.
	 * 
	 * @param roomName name or identifier of the room
	 * @return true if the room has been created, false if it already exists
	 *         with same name.
	 * @throws AdminException in case there was a problem obtaining the required
	 *         instance of the Kurento Client
	 */
	public boolean createRoom(String roomName) throws AdminException {
		Room room = rooms.get(roomName);
		try {
			if (room != null) {
				log.info("Room '{}' already exists");
				return false;
			}
			KurentoClient kurentoClient = kcProvider.getKurentoClient(null);
			room = new Room(roomName, kurentoClient, roomEventHandler);
			Room oldRoom = rooms.putIfAbsent(roomName, room);
			if (oldRoom != null) {
				log.info("Room '{}' has just been created by another thread");
				return false;
			} else
				log.warn("No room '{}' exists yet. Created one "
						+ "using KurentoClient '{}')", roomName, kurentoClient
						.getServerManager().getName());
			return true;
		} catch (RoomException e) {
			log.warn("Error creating room {}", roomName, e);
			throw new AdminException("Error creating room - " + e.toString());
		}
	}

	/**
	 * Returns the media pipeline used by the participant.
	 * 
	 * @param participantId identifier of the participant
	 * @return the Media Pipeline object
	 * @throws AdminException in case the participant doesn’t exist
	 */
	public MediaPipeline getPipeline(String participantId)
			throws AdminException {
		Participant participant = getParticipant(participantId);
		if (participant == null)
			throw new AdminException("No participant with id '" + participantId
					+ "' was found");
		return participant.getPipeline();
	}

	/**
	 * Closes an existing room by releasing all resources that were allocated
	 * for the room. Once closed, the room can be reopened (will be empty and it
	 * will use another Media Pipeline). Existing participants will be evicted. <br/>
	 * <strong>Side effects:</strong> The room event handler should send
	 * notifications to the existing participants in the room to inform that the
	 * room was forcibly closed.
	 * 
	 * @param roomName name or identifier of the room
	 * @throws AdminException in case the room doesn’t exist or has been already
	 *         closed
	 */
	public void closeRoom(String roomName) throws AdminException {
		Room room = rooms.get(roomName);
		if (room == null)
			throw new AdminException("Room '" + roomName + "' not found");
		if (room.isClosed())
			throw new AdminException("Room '" + roomName + "' already closed");
		// copy the ids as they will be removed from the map
		Set<String> pids = new HashSet<String>(room.getParticipantIds());
		for (String pid : pids) {
			try {
				room.leave(pid);
			} catch (RoomException e) {
				log.warn(
						"Error evicting participant with id '{}' from room '{}'",
						pid, roomName, e);
			}
		}
		roomEventHandler.onRoomClosed(roomName, pids);
		room.close();
		rooms.remove(roomName);
		log.warn("Room '{}' removed and closed", roomName);
	}

	/**
	 * Applies a media element (filter, recorder, etc.) to media that is
	 * currently streaming or that might get streamed sometime in the future.
	 * The element should have been created using the same pipeline as the
	 * publisher's.
	 * 
	 * @param participantId identifier of the owner of the stream
	 * @param element media element to be added
	 * @throws AdminException in case the participant doesn’t exist, has been
	 *         closed or on error when applying the filter
	 */
	public void addMediaElement(String participantId, MediaElement element)
			throws AdminException {
		Participant participant = getParticipant(participantId);
		if (participant == null)
			throw new AdminException("No participant with id '" + participantId
					+ "' was found");
		String name = participant.getName();
		if (participant.isClosed())
			throw new AdminException("Participant '" + name
					+ "' has been closed");
		try {
			participant.getPublisher().apply(element);
		} catch (RoomException e) {
			throw new AdminException("Error connecting media element - "
					+ e.toString());
		}
	}

	/**
	 * Disconnects and removes media element (filter, recorder, etc.) from a
	 * media stream.
	 * 
	 * @param participantId identifier of the participant
	 * @param element media element to be removed
	 * @throws AdminException in case the participant doesn’t exist, has been
	 *         closed or on error when removing the filter
	 */
	public void removeMediaElement(String participantId, MediaElement element)
			throws AdminException {
		Participant participant = getParticipant(participantId);
		if (participant == null)
			throw new AdminException("No participant with id '" + participantId
					+ "' was found");
		String name = participant.getName();
		if (participant.isClosed())
			throw new AdminException("Participant '" + name
					+ "' has been closed");
		try {
			participant.getPublisher().revert(element);
		} catch (RoomException e) {
			throw new AdminException("Error disconnecting media element - "
					+ e.toString());
		}
	}

	// ------------------ HELPERS ------------------------------------------

	/**
	 * @param roomName the name of the room
	 * @param request request for which a room is needed
	 * @param getOrCreate if true, creates the {@link Room} if not found
	 * @return the room if it was already created, a new one if it is the first
	 *         time this room is accessed and {@code getOrCreate} is
	 *         <em>true</em>, null otherwise
	 * @throws RoomException on error creating the room
	 */
	private Room getRoom(String roomName, ParticipantRequest request,
			boolean getOrCreate) {
		if (roomName == null || roomName.trim().isEmpty())
			throw new RoomException(Code.CANNOT_CREATE_ROOM_ERROR_CODE,
					"Empty room name is not allowed");

		Room room = rooms.get(roomName);

		if (room == null && getOrCreate) {
			KurentoClient kurentoClient =
					kcProvider.getKurentoClient(request.getParticipantId());
			if (kurentoClient == null)
				throw new RoomException(Code.CANNOT_CREATE_ROOM_ERROR_CODE,
						"Unable to obtain a KurentoClient instance");
			room = new Room(roomName, kurentoClient, roomEventHandler);
			Room oldRoom = rooms.putIfAbsent(roomName, room);
			if (oldRoom != null)
				return oldRoom;
			else {
				log.warn("Created room '{}' using the provided KurentoClient",
						roomName);
				roomEventHandler.onRoomCreated(request, roomName);
			}
		}
		return room;
	}

	private Participant getParticipant(ParticipantRequest request) {
		return getParticipant(request.getParticipantId());
	}

	private Participant getParticipant(String pid) {
		for (Room r : rooms.values())
			if (!r.isClosed()) {
				if (r.getParticipantIds().contains(pid))
					return r.getParticipant(pid);
			}
		return null;
	}
}
