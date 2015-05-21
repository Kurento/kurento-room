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

package org.kurento.room.api;

import java.io.IOException;

import org.kurento.jsonrpc.client.Continuation;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.kurento.room.internal.Participant;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public interface ParticipantSession {

	public void setParticipant(Participant participant);

	public Participant getParticipant();

	public String getName();

	public void setName(String name);

	public void sendRequest(
			Request<JsonObject> request,
			Continuation<Response<JsonElement>> continuation)
					throws IOException;

	public void sendNotification(String method, Object params)
			throws IOException;
}