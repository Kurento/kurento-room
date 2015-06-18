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
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.room.api.pojo.ParticipantRequest;
import org.kurento.room.rpc.JsonRpcProtocolElements;
import org.kurento.room.rpc.JsonRpcUserControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * User control that applies a face overlay filter when publishing video.
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class DemoJsonRpcUserControl extends JsonRpcUserControl {
	private static final Logger log = LoggerFactory
			.getLogger(DemoJsonRpcUserControl.class);

	private boolean enableHatFilter = false;
	private String hatUrl;
	private boolean onlyOnFirst = true;

	public void setEnableHatFilter(boolean enableFilter) {
		this.enableHatFilter = enableFilter;
	}

	public void setHatUrl(String hatUrl) {
		this.hatUrl = hatUrl;
	}

	public void setHatOnlyOnFirst(boolean onlyOnFirst) {
		this.onlyOnFirst = onlyOnFirst;
	}

	@Override
	public void publishVideo(Transaction transaction,
			Request<JsonObject> request, ParticipantRequest participantRequest) {
		final String sdpOffer = request.getParams()
				.get(JsonRpcProtocolElements.PUBLISH_VIDEO_SDPOFFER_PARAM)
				.getAsString();

		boolean firstOrOnlyPublisher = false;
		if (enableHatFilter) {
			if (onlyOnFirst) {
				String roomName = getParticipantSession(transaction)
						.getRoomName();
				firstOrOnlyPublisher = roomManager.getPublishers(roomName)
						.isEmpty();
			}
		}
		
		if (!enableHatFilter || (onlyOnFirst && !firstOrOnlyPublisher))
			roomManager.publishMedia(sdpOffer, participantRequest);
		else {
			log.info("Applying face overlay filter to session {}",
					participantRequest.getParticipantId());
			FaceOverlayFilter faceOverlayFilter = new FaceOverlayFilter.Builder(
					roomManager.getPipeline(participantRequest
							.getParticipantId())).build();
			faceOverlayFilter.setOverlayedImage(this.hatUrl, -0.35F, -1.2F,
					1.6F, 1.6F);
			roomManager.publishMedia(sdpOffer, participantRequest,
					faceOverlayFilter);
		}
	}
}
