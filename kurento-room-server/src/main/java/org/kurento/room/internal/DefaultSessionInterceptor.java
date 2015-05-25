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

public class DefaultSessionInterceptor implements SessionInterceptor {

	private final Logger log = LoggerFactory
			.getLogger(DefaultSessionInterceptor.class);

	@Autowired
	private KmsManager kmsManager;

	public void setKmsManager(KmsManager kmsManager) {
		this.kmsManager = kmsManager;
	}

	@Override
	public void authorizeUserRequest(Request<JsonObject> request,
			SessionState sessionState) throws RoomException {
		// default is empty
	}

	@Override
	public Kms getKmsForNewRoom(ParticipantSession participantSession)
			throws RoomException {
		String type = "next one";
		Kms kms = kmsManager.getKms();
		if (!kms.allowMoreElements()) {
			kms = kmsManager.getNextLessLoadedKms();
			type = "next less loaded";
			if (!kms.allowMoreElements()) {
				kms = kmsManager.getLessLoadedKms();
				type = "less loaded one";
			}
		}
		if (!kms.allowMoreElements()) {
			throw new RoomException(
					RoomException.NO_MEDIA_RESOURCES_ERROR_CODE,
					"No resources left to create new room");
		}
		log.debug("Offering Kms: {}, uri={}", type, kms.getUri());
		return kms;
	}

	@Override
	public void shapePreparingMedia(MediaShapingEndpoint publisher,
			MediaPipeline pipeline, boolean isOnlyPublisher)
					throws RoomException {
		// default is empty
	}

	@Override
	public void shapeStreamingMedia(MediaShapingEndpoint publisher,
			MediaPipeline pipeline, boolean isOnlyPublisher) {
		// default is empty
	}

}
