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

import org.kurento.jsonrpc.message.Request;
import org.kurento.room.api.ParticipantSession;
import org.kurento.room.api.RoomException;
import org.kurento.room.api.RoomRequestsFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

public class AuthSLAReqFilter implements RoomRequestsFilter {

	private static final Logger log = LoggerFactory
			.getLogger(AuthSLAReqFilter.class);

	@Override
	public void filterUserRequest(Request<JsonObject> request,
			ParticipantSession participantSession, SessionState sessionState)
					throws RoomException {
		String ps = null;
		if (participantSession != null
				&& participantSession.getParticipant() != null)
			ps = participantSession.getParticipant().toString();
		log.trace("REQ-FILTER> {} | {} | {}", sessionState, request, ps);
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
			if (sessionState.equals(SessionState.NEW)) {
				if (request.getParams().has("user")) {
					if (!(participantSession instanceof SLAParticipantSessionJsonRpc))
						throw new RoomException(
								RoomException.REQ_FILTER_ERROR_CODE,
								"A service level agreement is required");
					SLAParticipantSessionJsonRpc slaSession = (SLAParticipantSessionJsonRpc) participantSession;
					String user = request.getParams().get("user").getAsString();
					boolean hq = user.toLowerCase().startsWith("special");
					log.debug("(new) USER {}: HQ session - {}", user, hq);
					slaSession.setHQ(hq);
				}
			}
		}
	}

}
