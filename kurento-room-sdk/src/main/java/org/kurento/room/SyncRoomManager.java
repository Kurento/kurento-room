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
import org.kurento.client.MediaType;
import org.kurento.room.api.KurentoClientProvider;
import org.kurento.room.api.KurentoClientSessionInfo;
import org.kurento.room.api.MutedMediaType;
import org.kurento.room.api.RoomHandler;
import org.kurento.room.api.pojo.UserParticipant;
import org.kurento.room.endpoint.SdpType;
import org.kurento.room.exception.AdminException;
import org.kurento.room.exception.RoomException;
import org.kurento.room.exception.RoomException.Code;
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
public class SyncRoomManager {
	private final Logger log = LoggerFactory.getLogger(SyncRoomManager.class);

	private RoomHandler roomHandler;
	private KurentoClientProvider kcProvider;

	private final ConcurrentMap<String, Room> rooms =
			new ConcurrentHashMap<String, Room>();

	/**
	 * Provides an instance of the room manager by setting a room handler and
	 * the {@link KurentoClient} provider.
	 * 
	 * @param roomHandler the room handler implementation
	 * @param kcProvider enables the manager to obtain Kurento Client instances
	 */
	public SyncRoomManager(RoomHandler roomHandler,
			KurentoClientProvider kcProvider) {
		super();
		this.roomHandler = roomHandler;
		this.kcProvider = kcProvider;
	}

	/**
	 * Represents a client’s request to join a room. The room must exist in
	 * order to perform the join.<br/>
	 * <strong>Dev advice:</strong> Send notifications to the existing
	 * participants in the room to inform about the new peer.
	 * 
	 * @param userName name or identifier of the user in the room. Will be used
	 *        to identify her WebRTC media peer (from the client-side).
	 * @param roomName name or identifier of the room
	 * @param participantId identifier of the participant
	 * @return set of existing peers of type {@link UserParticipant}, can be
	 *         empty if first
	 * @throws AdminException on error while joining (like the room is not found
	 *         or is closing)
	 */
	public Set<UserParticipant> joinRoom(String userName, String roomName,
			String participantId) throws AdminException {
		log.debug("Request [JOIN_ROOM] user={}, room={} ({})", userName,
				roomName, participantId);
		Room room = rooms.get(roomName);
		if (room == null) {
			log.warn("Room '{}' not found");
			throw new AdminException("Room '" + roomName
					+ "' was not found, must be created before '" + userName
					+ "' can join");
		}
		if (room.isClosed()) {
			log.warn("'{}' is trying to join room '{}' but it is closing",
					userName, roomName);
			throw new AdminException("'" + userName
					+ "' is trying to join room '" + roomName
					+ "' but it is closing");
		}
		Set<UserParticipant> existingParticipants = getParticipants(roomName);
		try {
			room.join(participantId, userName);
			return existingParticipants;
		} catch (RoomException e) {
			log.warn("PARTICIPANT {}: Error joining/creating room {}",
					userName, roomName, e);
			throw new AdminException("Error on '" + userName
					+ "' to join room '" + roomName + "': " + e.getMessage());
		}
	}

	/**
	 * Represents a client’s notification that she’s leaving the room. Will also
	 * close the room if there're no more peers.<br/>
	 * <strong>Dev advice:</strong> Send notifications to the other participants
	 * in the room to inform about the one that's just left.
	 * 
	 * @param participantId identifier of the participant
	 * @return set of remaining peers of type {@link UserParticipant}, if empty
	 *         this method has closed the room
	 * @throws AdminException on error leaving the room
	 */
	public Set<UserParticipant> leaveRoom(String participantId)
			throws AdminException {
		log.debug("Request [LEAVE_ROOM] ({})", participantId);
		Participant participant = getParticipant(participantId);
		Room room = participant.getRoom();
		String roomName = room.getName();
		if (room.isClosed()) {
			log.warn(
					"'{}' is trying to leave from room '{}' but it is closing",
					participant.getName(), roomName);
			throw new AdminException("'" + participant.getName()
					+ "' is trying to leave from room '" + roomName
					+ "' but it is closing");
		}
		try {
			room.leave(participantId);
			Set<UserParticipant> remainingParticipants =
					getParticipants(roomName);
			if (remainingParticipants.isEmpty()) {
				log.debug(
						"No more participants in room '{}', removing it and closing it",
						roomName);
				room.close();
				rooms.remove(roomName);
				log.warn("Room '{}' removed and closed", roomName);
			}
			return remainingParticipants;
		} catch (RoomException e) {
			log.warn("Error leaving room", e);
			throw new AdminException("Error on '" + participant.getName()
					+ "' leaving room '" + roomName + "': " + e.getMessage());
		}
	}

	/**
	 * Represents a client’s request to start streaming her local media to
	 * anyone inside the room. The media elements should have been created using
	 * the same pipeline as the publisher's. The streaming media endpoint
	 * situated on the server can be connected to itself thus realizing what is
	 * known as a loopback connection. The loopback is performed after applying
	 * all additional media elements specified as parameters (in the same order
	 * as they appear in the params list).
	 * 
	 * <br/>
	 * <strong>Dev advice:</strong> Send notifications to the existing
	 * participants in the room to inform about the new stream that has been
	 * published. Answer to the peer's request by sending it the SDP response
	 * (answer or updated offer) generated by the WebRTC endpoint on the server.
	 * 
	 * @param participantId identifier of the participant
	 * @param isOffer if true, the sdp is an offer from remote, otherwise is the
	 *        answer to the offer generated previously by the server endpoint
	 * @param sdp SDP String <strong>offer</strong> or <strong>answer</strong>,
	 *        that's been generated by the client’s WebRTC peer
	 * @param loopbackAlternativeSrc instead of connecting the endpoint to
	 *        itself, use this {@link MediaElement} as source
	 * @param loopbackConnectionType the connection type for the loopback; if
	 *        null, will stream both audio and video media
	 * @param doLoopback loopback flag
	 * @param mediaElements variable array of media elements (filters,
	 *        recorders, etc.) that are connected between the source WebRTC
	 *        endpoint and the subscriber endpoints
	 * @return the SDP response generated by the WebRTC endpoint on the server
	 *         (answer to the client's offer or the updated offer previously
	 *         generated by the server endpoint)
	 * @throws AdminException on error
	 */
	public String publishMedia(String participantId, boolean isOffer,
			String sdp, MediaElement loopbackAlternativeSrc,
			MediaType loopbackConnectionType, boolean doLoopback,
			MediaElement... mediaElements) throws AdminException {
		log.debug(
				"Request [PUBLISH_MEDIA] isOffer={} sdp={} "
						+ "loopbackAltSrc={} lpbkConnType={} doLoopback={} mediaElements={} ({})",
				isOffer, sdp, loopbackAlternativeSrc == null,
				loopbackConnectionType, doLoopback, mediaElements,
				participantId);

		SdpType sdpType = isOffer ? SdpType.OFFER : SdpType.ANSWER;
		Participant participant = getParticipant(participantId);
		try {
			String name = participant.getName();
			Room room = participant.getRoom();

			participant.createPublishingEndpoint();

			for (MediaElement elem : mediaElements)
				participant.getPublisher().apply(elem);

			String sdpResponse =
					participant.publishToRoom(sdpType, sdp, doLoopback,
							loopbackAlternativeSrc, loopbackConnectionType);
			if (sdpResponse == null)
				throw new RoomException(Code.SDP_ERROR_CODE,
						"Error generating SDP response for publishing user "
								+ name);

			room.newPublisher(participant);
			return sdpResponse;
		} catch (RoomException e) {
			log.warn("Error publishing media of '{}'", participant.getName(), e);
			throw new AdminException("Error publishing '"
					+ participant.getName() + "': " + e.getMessage());
		}
	}

	/**
	 * Same as
	 * {@link #publishMedia(String, boolean, String, MediaElement, MediaType, boolean, MediaElement...)}
	 * , using as loopback the published stream and no specific type of loopback
	 * connection.
	 * 
	 * @see #publishMedia(String, boolean, String, boolean, MediaElement,
	 *      MediaElement...)
	 */
	public String publishMedia(String participantId, boolean isOffer,
			String sdp, boolean doLoopback, MediaElement... mediaElements)
			throws AdminException {
		return publishMedia(participantId, isOffer, sdp, null, null,
				doLoopback, mediaElements);
	}

	/**
	 * Same as
	 * {@link #publishMedia(String, boolean, String, MediaElement, MediaType, boolean, MediaElement...)}
	 * , using as loopback the published stream and allowing a specific
	 * connection type.
	 * 
	 * @see #publishMedia(String, boolean, String, boolean, MediaElement,
	 *      MediaElement...)
	 */
	public String publishMedia(String participantId, boolean isOffer,
			String sdp, MediaType loopbackConnectionType, boolean doLoopback,
			MediaElement... mediaElements) throws AdminException {
		return publishMedia(participantId, isOffer, sdp, null,
				loopbackConnectionType, doLoopback, mediaElements);
	}

	/**
	 * Represents a client’s request to initiate the media connection from the
	 * server-side (generate the SDP offer and send it back to the client) and
	 * must be followed by processing the SDP answer from the client in order to
	 * establish the streaming.
	 * 
	 * @see #publishMedia(String, String, boolean, MediaElement...)
	 * @param participantId identifier of the participant
	 * @return the SDP offer generated by the WebRTC endpoint on the server
	 * @throws AdminException on error
	 */
	public String generatePublishOffer(String participantId)
			throws AdminException {
		log.debug("Request [GET_PUBLISH_SDP_OFFER] ({})", participantId);

		Participant participant = getParticipant(participantId);
		try {
			String name = participant.getName();
			Room room = participant.getRoom();

			participant.createPublishingEndpoint();

			String sdpOffer = participant.preparePublishConnection();
			if (sdpOffer == null)
				throw new RoomException(Code.SDP_ERROR_CODE,
						"Error generating SDP offer for publishing user "
								+ name);

			room.newPublisher(participant);
			return sdpOffer;
		} catch (RoomException e) {
			log.warn("Error generating Sdp offer for publishing media of '{}'",
					participant.getName(), e);
			throw new AdminException("Error generating Sdp offer '"
					+ participant.getName() + "': " + e.getMessage());
		}
	}

	/**
	 * Represents a client’s request to stop publishing her media stream. All
	 * media elements on the server-side connected to this peer will be
	 * disconnected and released. The peer is left ready for publishing her
	 * media in the future.<br/>
	 * <strong>Dev advice:</strong> Send notifications to the existing
	 * participants in the room to inform that streaming from this endpoint has
	 * ended.
	 * 
	 * @param participantId identifier of the participant
	 * @throws AdminException on error
	 */
	public void unpublishMedia(String participantId) throws AdminException {
		log.debug("Request [UNPUBLISH_MEDIA] ({})", participantId);
		Participant participant = getParticipant(participantId);
		try {
			if (!participant.isStreaming())
				throw new RoomException(Code.USER_NOT_STREAMING_ERROR_CODE,
						"Participant '" + participant.getName()
								+ "' is not streaming media");
			Room room = participant.getRoom();
			participant.unpublishMedia();
			room.cancelPublisher(participant);
		} catch (RoomException e) {
			log.warn("Error unpublishing media", e);
			throw new AdminException("Error unpublishing '"
					+ participant.getName() + "': " + e.getMessage());
		}
	}

	/**
	 * Represents a client’s request to receive media from room participants
	 * that published their media. Will have the same result when a publisher
	 * requests its own media stream.<br/>
	 * <strong>Dev advice:</strong> Answer to the peer's request by sending it
	 * the SDP answer generated by the the receiving WebRTC endpoint on the
	 * server.
	 * 
	 * @param remoteName identification of the remote stream which is
	 *        effectively the peer’s name (participant)
	 * @param sdpOffer SDP offer String generated by the client’s WebRTC peer
	 * @param participantId identifier of the participant
	 * @return the SDP answer generated by the receiving WebRTC endpoint on the
	 *         server
	 * @throws AdminException on error
	 */
	public String subscribe(String remoteName, String sdpOffer,
			String participantId) throws AdminException {
		log.debug("Request [SUBSCRIBE] remoteParticipant={} sdpOffer={} ({})",
				remoteName, sdpOffer, participantId);
		Participant participant = getParticipant(participantId);
		String name = participant.getName();

		try {
			Room room = participant.getRoom();

			Participant senderParticipant =
					room.getParticipantByName(remoteName);
			if (senderParticipant == null) {
				log.warn(
						"PARTICIPANT {}: Requesting to recv media from user {} "
								+ "in room {} but user could not be found",
						name, remoteName, room.getName());
				throw new RoomException(Code.USER_NOT_FOUND_ERROR_CODE,
						"User '" + remoteName + " not found in room '"
								+ room.getName() + "'");
			}
			if (!senderParticipant.isStreaming()) {
				log.warn(
						"PARTICIPANT {}: Requesting to recv media from user {} "
								+ "in room {} but user is not streaming media",
						name, remoteName, room.getName());
				throw new RoomException(Code.USER_NOT_STREAMING_ERROR_CODE,
						"User '" + remoteName
								+ " not streaming media in room '"
								+ room.getName() + "'");
			}

			String sdpAnswer =
					participant.receiveMediaFrom(senderParticipant, sdpOffer);
			if (sdpAnswer == null)
				throw new RoomException(Code.SDP_ERROR_CODE,
						"Unable to generate SDP answer when subscribing '"
								+ name + "' to '" + remoteName + "'");
			return sdpAnswer;
		} catch (RoomException e) {
			log.warn("Error subscribing to {}", remoteName, e);
			throw new AdminException("Error subscribing '" + name + "' to '"
					+ remoteName + "': " + e.getMessage());
		}
	}

	/**
	 * Represents a client’s request to stop receiving media from the remote
	 * peer.
	 * 
	 * @param remoteName identification of the remote stream which is
	 *        effectively the peer’s name (participant)
	 * @param participantId identifier of the participant
	 * @throws AdminException on error
	 */
	public void unsubscribe(String remoteName, String participantId)
			throws AdminException {
		log.debug("Request [UNSUBSCRIBE] remoteParticipant={} ({})",
				remoteName, participantId);
		Participant participant = getParticipant(participantId);
		String name = participant.getName();
		try {
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
		} catch (RoomException e) {
			log.warn("Error unsubscribing from {}", remoteName, e);
			throw new AdminException("Error unsubscribing '" + name
					+ "' from '" + remoteName + "': " + e.getMessage());
		}
	}

	/**
	 * Request that carries info about an ICE candidate gathered on the client
	 * side. This information is required to implement the trickle ICE
	 * mechanism. Should be triggered or called whenever an icecandidate event
	 * is created by a RTCPeerConnection.
	 * 
	 * @param endpointName the name of the peer whose ICE candidate was gathered
	 * @param candidate the candidate attribute information
	 * @param sdpMLineIndex the index (starting at zero) of the m-line in the
	 *        SDP this candidate is associated with
	 * @param sdpMid media stream identification, "audio" or "video", for the
	 *        m-line this candidate is associated with
	 * @param participantId identifier of the participant
	 * @throws AdminException on error
	 */
	public void onIceCandidate(String endpointName, String candidate,
			int sdpMLineIndex, String sdpMid, String participantId)
			throws AdminException {
		log.debug("Request [ICE_CANDIDATE] endpoint={} candidate={} "
				+ "sdpMLineIdx={} sdpMid={} ({})", endpointName, candidate,
				sdpMLineIndex, sdpMid, participantId);
		Participant participant = getParticipant(participantId);
		try {
			participant.addIceCandidate(endpointName, new IceCandidate(
					candidate, sdpMid, sdpMLineIndex));
		} catch (RoomException e) {
			log.warn("Error receiving ICE candidate", e);
			throw new AdminException("Error receiving ICE candidate '"
					+ candidate + "' for endpoint '" + endpointName + "' of '"
					+ participant.getName() + "': " + e.getMessage());
		}
	}

	/**
	 * Applies a media element (filter, recorder, mixer, etc.) to media that is
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
		addMediaElement(participantId, element, null);
	}

	/**
	 * Applies a media element (filter, recorder, mixer, etc.) to media that is
	 * currently streaming or that might get streamed sometime in the future.
	 * The element should have been created using the same pipeline as the
	 * publisher's. The media connection can be of any type, that is audio,
	 * video, data or any (when the parameter is null).
	 * 
	 * @param participantId identifier of the owner of the stream
	 * @param element media element to be added
	 * @param type the connection type (null is accepted, has the same result as
	 *        calling {@link #addMediaElement(String, MediaElement)})
	 * @throws AdminException in case the participant doesn’t exist, has been
	 *         closed or on error when applying the filter
	 */
	public void addMediaElement(String participantId, MediaElement element,
			MediaType type) throws AdminException {
		log.debug(
				"Add media element {} (connection type: {}) to participant {}",
				element.getId(), type, participantId);
		Participant participant = getParticipant(participantId);
		String name = participant.getName();
		if (participant.isClosed())
			throw new AdminException("Participant '" + name
					+ "' has been closed");
		try {
			participant.shapePublisherMedia(element, type);
		} catch (RoomException e) {
			throw new AdminException("Error connecting "
					+ (type == null ? "" : type + " ") + "media element - "
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
		log.debug("Remove media element {} from participant {}",
				element.getId(), participantId);
		Participant participant = getParticipant(participantId);
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

	/**
	 * Mutes the streamed media of this publisher in a selective manner.
	 * 
	 * @param muteType which leg should be disconnected (audio, video or both)
	 * @param participantId identifier of the participant
	 * @throws AdminException in case the participant doesn’t exist, has been
	 *         closed, is not publishing or on error when performing the mute
	 *         operation
	 */
	public void mutePublishedMedia(MutedMediaType muteType, String participantId)
			throws AdminException {
		log.debug("Request [MUTE_PUBLISHED] muteType={} ({})", muteType,
				participantId);
		Participant participant = getParticipant(participantId);
		String name = participant.getName();
		if (participant.isClosed())
			throw new AdminException("Participant '" + name
					+ "' has been closed");
		if (!participant.isStreaming())
			throw new AdminException("Participant '" + name
					+ "' is not streaming media");
		try {
			participant.mutePublishedMedia(muteType);
		} catch (RoomException e) {
			throw new AdminException("Error applying mute - " + e.toString());
		}
	}

	/**
	 * Reverts the effects of the mute operation.
	 * 
	 * @param participantId identifier of the participant
	 * @throws AdminException in case the participant doesn’t exist, has been
	 *         closed, is not publishing or on error when reverting the mute
	 *         operation
	 */
	public void unmutePublishedMedia(String participantId)
			throws AdminException {
		log.debug("Request [UNMUTE_PUBLISHED] muteType={} ({})", participantId);
		Participant participant = getParticipant(participantId);
		String name = participant.getName();
		if (participant.isClosed())
			throw new AdminException("Participant '" + name
					+ "' has been closed");
		if (!participant.isStreaming())
			throw new AdminException("Participant '" + name
					+ "' is not streaming media");
		try {
			participant.unmutePublishedMedia();
		} catch (RoomException e) {
			throw new AdminException("Error reverting mute - " + e.toString());
		}
	}

	/**
	 * Mutes the incoming media stream from the remote publisher in a selective
	 * manner.
	 * 
	 * @param remoteName identification of the remote stream which is
	 *        effectively the peer’s name (participant)
	 * @param muteType which leg should be disconnected (audio, video or both)
	 * @param participantId identifier of the participant
	 * @throws AdminException in case the participant doesn’t exist, has been
	 *         closed, is not publishing or on error when performing the mute
	 *         operation
	 */
	public void muteSubscribedMedia(String remoteName, MutedMediaType muteType,
			String participantId) throws AdminException {
		log.debug(
				"Request [MUTE_SUBSCRIBED] remoteParticipant={} muteType={} ({})",
				remoteName, muteType, participantId);
		Participant participant = getParticipant(participantId);
		String name = participant.getName();
		try {
			Room room = participant.getRoom();
			Participant senderParticipant =
					room.getParticipantByName(remoteName);
			if (senderParticipant == null) {
				log.warn(
						"PARTICIPANT {}: Requesting to mute streaming from {} "
								+ "in room {} but user could not be found",
						name, remoteName, room.getName());
				throw new RoomException(Code.USER_NOT_FOUND_ERROR_CODE, "User "
						+ remoteName + " not found in room " + room.getName());
			}
			if (!senderParticipant.isStreaming()) {
				log.warn(
						"PARTICIPANT {}: Requesting to mute streaming from {} "
								+ "in room {} but user is not streaming media",
						name, remoteName, room.getName());
				throw new RoomException(Code.USER_NOT_STREAMING_ERROR_CODE,
						"User '" + remoteName
								+ " not streaming media in room '"
								+ room.getName() + "'");
			}
			participant.muteSubscribedMedia(senderParticipant, muteType);
		} catch (RoomException e) {
			log.warn("Error on mute streaming from {}", remoteName, e);
			throw new AdminException("Error on mute streaming from '"
					+ remoteName + "': " + e.getMessage());
		}
	}

	/**
	 * Reverts any previous mute operation.
	 * 
	 * @param remoteName identification of the remote stream which is
	 *        effectively the peer’s name (participant)
	 * @param participantId identifier of the participant
	 * @throws AdminException in case the participant doesn’t exist, has been
	 *         closed or on error when reverting the mute operation
	 */
	public void unmuteSubscribedMedia(String remoteName, String participantId)
			throws AdminException {
		log.debug("Request [UNMUTE_SUBSCRIBED] remoteParticipant={} ({})",
				remoteName, participantId);
		Participant participant = getParticipant(participantId);
		String name = participant.getName();
		try {
			Room room = participant.getRoom();
			Participant senderParticipant =
					room.getParticipantByName(remoteName);
			if (senderParticipant == null) {
				log.warn(
						"PARTICIPANT {}: Requesting to unmute streaming from {} "
								+ "in room {} but user could not be found",
						name, remoteName, room.getName());
				throw new RoomException(Code.USER_NOT_FOUND_ERROR_CODE, "User "
						+ remoteName + " not found in room " + room.getName());
			}
			if (!senderParticipant.isStreaming()) {
				log.warn(
						"PARTICIPANT {}: Requesting to unmute streaming from {} "
								+ "in room {} but user is not streaming media",
						name, remoteName, room.getName());
				throw new RoomException(Code.USER_NOT_STREAMING_ERROR_CODE,
						"User '" + remoteName
								+ " not streaming media in room '"
								+ room.getName() + "'");
			}
			participant.unmuteSubscribedMedia(senderParticipant);
		} catch (RoomException e) {
			log.warn("Error on unmute streaming from {}", remoteName, e);
			throw new AdminException("Error on unmute streaming from '"
					+ remoteName + "': " + e.getMessage());
		}
	}

	// ----------------- ADMIN (DIRECT or SERVER-SIDE) REQUESTS ------------
	/**
	 * Closes all resources. This method has been annotated with the @PreDestroy
	 * directive (javax.annotation package) so that it will be automatically
	 * called when the RoomManager instance is container-managed. <br/>
	 * <strong>Dev advice:</strong> Send notifications to all participants to
	 * inform that their room has been forcibly closed.
	 * 
	 * @see RoomManager#closeRoom(String)
	 */
	@PreDestroy
	public void close() {
		log.info("Closing all rooms");
		for (String roomName : rooms.keySet())
			try {
				closeRoom(roomName);
			} catch (Exception e) {
				log.warn("Error closing room '{}'", roomName, e);
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
	 * Creates a room if it doesn’t already exist. The room's name will be
	 * indicated by the session info bean.
	 * 
	 * @param sessionInfo bean that will be passed to the
	 *        {@link KurentoClientProvider} in order to obtain the
	 *        {@link KurentoClient} that will be used by the room
	 * @return the created {@link Room}
	 * @throws AdminException in case of error while creating the room
	 */
	public void createRoom(KurentoClientSessionInfo sessionInfo)
			throws AdminException {
		String roomName = sessionInfo.getRoomName();
		Room room = rooms.get(sessionInfo);
		if (room != null)
			throw new AdminException("Room '" + roomName + "' already exists");
		KurentoClient kurentoClient = null;
		try {
			kurentoClient = kcProvider.getKurentoClient(sessionInfo);
			room = new Room(roomName, kurentoClient, roomHandler);
		} catch (RoomException e) {
			log.warn("Error creating room {}", roomName, e);
			throw new AdminException("Error creating room - " + e.toString());
		}
		Room oldRoom = rooms.putIfAbsent(roomName, room);
		if (oldRoom != null) {
			log.info("Room '{}' has just been created by another thread");
			throw new AdminException(
					"Room '"
							+ roomName
							+ "' already exists (has just been created by another thread)");
		}
		log.warn("No room '{}' exists yet. Created one "
				+ "using KurentoClient '{}')", roomName, kurentoClient
				.getServerManager().getName());
	}

	/**
	 * Closes an existing room by releasing all resources that were allocated
	 * for the room. Once closed, the room can be reopened (will be empty and it
	 * will use another Media Pipeline). Existing participants will be evicted. <br/>
	 * <strong>Dev advice:</strong> The room event handler should send
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
		room.close();
		rooms.remove(roomName);
		log.warn("Room '{}' removed and closed", roomName);
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

	// ------------------ HELPERS ------------------------------------------

	private Participant getParticipant(String pid) throws AdminException {
		for (Room r : rooms.values())
			if (!r.isClosed()) {
				if (r.getParticipantIds().contains(pid)
						&& r.getParticipant(pid) != null)
					return r.getParticipant(pid);
			}
		throw new AdminException("No participant with id '" + pid
				+ "' was found");
	}
}
