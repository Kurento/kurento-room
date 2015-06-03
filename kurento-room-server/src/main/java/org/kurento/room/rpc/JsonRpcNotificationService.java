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

package org.kurento.room.rpc;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.room.api.UserNotificationService;
import org.kurento.room.api.pojo.ParticipantRequest;
import org.kurento.room.exception.RoomException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

public class JsonRpcNotificationService implements UserNotificationService {
	private static final Logger log = LoggerFactory
			.getLogger(JsonRpcNotificationService.class);

	private static ConcurrentMap<String, SessionWrapper> sessions = new ConcurrentHashMap<String, SessionWrapper>();

	public void addTransaction(Transaction t, Request<JsonObject> request) {
		String sessionId = t.getSession().getSessionId();
		SessionWrapper sw = sessions.get(sessionId);
		if (sw == null) {
			sw = new SessionWrapper(t.getSession());
			SessionWrapper oldSw = sessions.putIfAbsent(sessionId, sw);
			if (oldSw != null) {
				log.warn("Concurrent initialization of session wrapper #{}",
						sessionId);
				sw = oldSw;
			}
		}
		sw.addTransaction(request.getId(), t);
	}

	private Transaction getTransaction(ParticipantRequest participantRequest) {
		Integer tid = null;
		String tidVal = participantRequest.getRequestId();
		try {
			tid = Integer.parseInt(tidVal);
		} catch (NumberFormatException e) {
			log.error(
					"Invalid transaction id, a number was expected but recv: {}",
					tidVal, e);
			return null;
		}
		String sessionId = participantRequest.getParticipantId();
		SessionWrapper sw = sessions.get(sessionId);
		if (sw == null) {
			log.warn("Invalid session id {}", sessionId);
			return null;
		}
		return sw.getTransaction(tid);
	}

	@Override
	public void sendResponse(ParticipantRequest participantRequest,
			Object result) {
		Transaction t = getTransaction(participantRequest);
		if (t == null) {
			log.error("No transaction found for {}, unable to send result {}",
					participantRequest, result);
			return;
		}
		try {
			t.sendResponse(result);
		} catch (IOException e) {
			log.error("Exception responding to user", e);
		}
		// TODO remove transaction from map?
	}

	@Override
	public void sendErrorResponse(ParticipantRequest participantRequest,
			Object data, RoomException error) {
		Transaction t = getTransaction(participantRequest);
		if (t == null) {
			log.error("No transaction found for {}, unable to send result {}",
					participantRequest, data);
			return;
		}
		try {
			t.sendError(error.getCodeValue(), error.getMessage(),
					data.toString());
		} catch (IOException e) {
			log.error("Exception sending error response to user", e);
		}
		// TODO remove transaction from map?
	}

	@Override
	public void sendNotification(final String participantId,
			final String method, final Object params) {
		SessionWrapper sw = sessions.get(participantId);
		if (sw == null || sw.getSession() == null) {
			log.error(
					"No session found for id {}, unable to send notification {}: {}",
					participantId, method, params);
			return;
		}
		Session s = sw.getSession();

		try {
			s.sendNotification(method, params);
			// not supported s.sendNotification(method, params, new
			// Continuation<JsonElement>() {
			// @Override
			// public void onSuccess(JsonElement result) {
			// }
			//
			// @Override
			// public void onError(Throwable cause) {
			// log.error(
			// "Exception while sending '{}: {}' notification using session id {}",
			// method, params, participantId, cause);
			// }
			// });
		} catch (IOException e) {
			log.error("Exception sending notification to user", e);
		}
	}

	@Override
	public void closeSession(ParticipantRequest participantRequest) {
		String sessionId = participantRequest.getParticipantId();
		SessionWrapper sw = sessions.get(sessionId);
		if (sw == null || sw.getSession() == null) {
			log.error("No session found for id {}, unable to cleanup",
					sessionId);
			return;
		}
		Session s = sw.getSession();
		try {
			s.close();
		} catch (IOException e) {
			log.error("Error closing session", e);
		}
		sessions.remove(sessionId);
	}

}
