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

import org.kurento.jsonrpc.Session;
import org.kurento.room.api.ParticipantSession;
import org.kurento.room.api.control.JsonRpcProtocolElements;
import org.kurento.room.api.control.JsonRpcUserControl;

public class JsonRpcSLAUserControl extends JsonRpcUserControl {

	@Override
	public ParticipantSession getParticipantSession(Session session) {
		SLAParticipantSessionJsonRpc participantSession = (SLAParticipantSessionJsonRpc) session
				.getAttributes().get(
						JsonRpcProtocolElements.PARTICIPANT_SESSION_ATTRIBUTE);
		if (participantSession == null) {
			participantSession = new SLAParticipantSessionJsonRpc(session);
			session.getAttributes().put(
					JsonRpcProtocolElements.PARTICIPANT_SESSION_ATTRIBUTE,
					participantSession);
		}
		return participantSession;
	}
}
