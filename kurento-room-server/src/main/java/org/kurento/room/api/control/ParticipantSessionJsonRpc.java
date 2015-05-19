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

package org.kurento.room.api.control;

import java.io.IOException;

import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.client.Continuation;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.kurento.room.api.ParticipantSession;
import org.kurento.room.internal.Participant;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ParticipantSessionJsonRpc implements ParticipantSession {

	private Session session;
	private Participant roomParticipant;

	public ParticipantSessionJsonRpc(Session session) {
		this.session = session;
	}

	@Override
	public void setParticipant(Participant roomParticipant) {
		this.roomParticipant = roomParticipant;
	}

	@Override
	public Participant getParticipant() {
		return roomParticipant;
	}

	@Override
	public String getName() {
		if (roomParticipant != null) {
			return roomParticipant.getName();
		} else {
			return "<UnknownParticipant>";
		}
	}

	@Override
	public void sendRequest(Request<JsonObject> request,
			Continuation<Response<JsonElement>> continuation)
					throws IOException {
		session.sendRequest(request, continuation);
	}

	@Override
	public void sendNotification(String method, Object params)
			throws IOException {
		session.sendNotification(method, params);
	}
}