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

package org.kurento.room.demo.api.control;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.room.demo.api.Participant;
import org.kurento.room.demo.api.ParticipantSession;

import com.google.gson.JsonObject;

public interface JsonRpcParticipantControl {
	public void joinRoom(Transaction transaction, Request<JsonObject> request)
			throws IOException, InterruptedException, ExecutionException;

	public void receiveVideoFrom(Transaction transaction,
			Request<JsonObject> request);

	public void leaveRoom(Session session) throws IOException,
	InterruptedException, ExecutionException;

	public void leaveRoom(Participant participant) throws IOException,
			InterruptedException, ExecutionException;

	public void onIceCandidate(Transaction transaction,
			Request<JsonObject> request);

	void sendMessage(Transaction transaction, Request<JsonObject> request);

	public ParticipantSession getParticipantSession(
			Transaction transaction);

	public ParticipantSession getParticipantSession(Session session);
}
