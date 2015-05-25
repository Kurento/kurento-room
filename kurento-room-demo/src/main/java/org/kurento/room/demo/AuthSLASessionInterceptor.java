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

package org.kurento.room.demo;

import org.kurento.client.FaceOverlayFilter;
import org.kurento.client.MediaPipeline;
import org.kurento.jsonrpc.message.Request;
import org.kurento.room.api.MediaShapingEndpoint;
import org.kurento.room.api.ParticipantSession;
import org.kurento.room.api.RoomException;
import org.kurento.room.api.SessionInterceptor;
import org.kurento.room.kms.Kms;
import org.kurento.room.kms.KmsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.JsonObject;

public class AuthSLASessionInterceptor implements SessionInterceptor {

	private static final Logger log = LoggerFactory
			.getLogger(AuthSLASessionInterceptor.class);

	@Autowired
	private KmsManager kmsManager;

	private String hatUrl;

	public void setKmsManager(KmsManager kmsManager) {
		this.kmsManager = kmsManager;
	}

	public void setHatUrl(String hatUrl) {
		this.hatUrl = hatUrl;
	}

	@Override
	public void authorizeUserRequest(Request<JsonObject> request,
			SessionState sessionState) throws RoomException {
		log.trace("REQ-FILTER> {} | {}", sessionState, request);
		if (request != null && request.getParams() != null) {
			String token = null;
			if (request.getParams().has("token")) {
				token = request.getParams().get("token").getAsString();
				log.trace("Security token: {}", token);
				// TODO check token, etc ...
			}
			if (token == null)
				throw new RoomException(RoomException.REQ_FILTER_ERROR_CODE,
						"Not authorized");
		}
	}

	@Override
	public Kms getKmsForNewRoom(ParticipantSession participantSession)
			throws RoomException {
		String userName = participantSession.getName();
		if (!canCreateRoom(userName))
			throw new RoomException(
					RoomException.CANNOT_CREATE_ROOM_ERROR_CODE,
					"User cannot create a new room");
		Kms kms = null;
		String type = "";
		boolean hq = isUserHQ(participantSession.getName());
		if (hq)
			kms = kmsManager.getLessLoadedKms();
		else {
			kms = kmsManager.getNextLessLoadedKms();
			if (!kms.allowMoreElements())
				kms = kmsManager.getLessLoadedKms();
			else
				type = "next ";
		}
		if (!kms.allowMoreElements()) {
			throw new RoomException(
					RoomException.NO_MEDIA_RESOURCES_ERROR_CODE,
					"No resources left to create new room");
		}
		log.debug("Offering Kms: highQ={}, {}less loaded KMS, uri={}", hq,
				type, kms.getUri());
		return kms;
	}

	private boolean isUserHQ(String userName) {
		return userName.toLowerCase().startsWith("special");
	}

	private boolean canCreateRoom(String userName) {
		return userName.toLowerCase().startsWith("special");
	}

	@Override
	public void shapePreparingMedia(MediaShapingEndpoint publisher,
			MediaPipeline pipeline, boolean isOnlyPublisher)
					throws RoomException {
		if (!isOnlyPublisher) // only the first publisher will have a hat
			return;
		FaceOverlayFilter faceOverlayFilterPirate = new FaceOverlayFilter.Builder(
				pipeline).build();
		faceOverlayFilterPirate.setOverlayedImage(this.hatUrl, -0.35F, -1.2F,
				1.6F, 1.6F);
		publisher.apply(faceOverlayFilterPirate);
	}

	@Override
	public void shapeStreamingMedia(MediaShapingEndpoint publisher,
			MediaPipeline pipeline, boolean isOnlyPublisher) {
		// not used ... yet
	}

}
