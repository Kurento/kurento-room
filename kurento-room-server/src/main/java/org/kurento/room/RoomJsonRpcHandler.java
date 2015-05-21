/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
package org.kurento.room;

import java.io.IOException;

import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.room.api.ParticipantSession;
import org.kurento.room.api.RoomException;
import org.kurento.room.api.SessionInterceptor;
import org.kurento.room.api.SessionInterceptor.SessionState;
import org.kurento.room.api.control.JsonRpcProtocolElements;
import org.kurento.room.api.control.JsonRpcUserControl;
import org.kurento.room.internal.Participant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.JsonObject;

/**
 * @author Ivan Gracia (izanmail@gmail.com)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 1.0.0
 */
public class RoomJsonRpcHandler extends DefaultJsonRpcHandler<JsonObject> {

	private static final Logger log = LoggerFactory
			.getLogger(RoomJsonRpcHandler.class);

	private static final String HANDLER_THREAD_NAME = "handler";

	@Autowired
	private JsonRpcUserControl userControl;

	@Autowired
	private SessionInterceptor interceptor;

	public void setSessionFilter(SessionInterceptor interceptor) {
		this.interceptor = interceptor;
	}

	@Override
	public final void handleRequest(final Transaction transaction,
			final Request<JsonObject> request) throws Exception {

		updateThreadName(HANDLER_THREAD_NAME + "_"
				+ transaction.getSession().getSessionId());

		ParticipantSession participantSession = userControl
				.getParticipantSession(transaction);
		if (participantSession.getParticipant() != null) {
			log.debug("Incoming message from user '{}': {}",
					participantSession.getName(), request);
			if (!invokeReqFilter(transaction, request, SessionState.REGISTERED))
				return;
		} else {
			log.debug("Incoming message from new user: {}", request);
			if (!invokeReqFilter(transaction, request, SessionState.NEW))
				return;
		}

		switch (request.getMethod()) {
		case JsonRpcProtocolElements.RECEIVE_VIDEO_METHOD:
			userControl.receiveVideoFrom(transaction, request);
			break;
		case JsonRpcProtocolElements.ON_ICE_CANDIDATE_METHOD:
			userControl.onIceCandidate(transaction, request);
			break;
		case JsonRpcProtocolElements.JOIN_ROOM_METHOD:
			userControl.joinRoom(transaction, request);
			break;
		case JsonRpcProtocolElements.LEAVE_ROOM_METHOD:
			userControl.leaveRoom(transaction);
			break;
		case JsonRpcProtocolElements.SENDMESSAGE_ROOM_METHOD:
			userControl.sendMessage(transaction, request);
			break;
		default:
			log.error("Unrecognized request {}", request);
			break;
		}

		updateThreadName(HANDLER_THREAD_NAME);
	}

	@Override
	public final void afterConnectionClosed(Session session, String status)
			throws Exception {
		ParticipantSession participantSession = userControl
				.getParticipantSession(session);
		Participant participant = participantSession.getParticipant();
		if (participant != null) {
			updateThreadName(participant.getName() + "|wsclosed");
			userControl.leaveRoom(participant);
			updateThreadName(HANDLER_THREAD_NAME);
		}
	}

	@Override
	public void handleTransportError(Session session, Throwable exception)
			throws Exception {
		Participant participant = userControl.getParticipantSession(
				session).getParticipant();
		if (participant != null && !participant.isClosed()) {
			log.warn("Transport error", exception);
		}
	}

	private boolean invokeReqFilter(Transaction transaction,
			Request<JsonObject> request, SessionState sessionState)
					throws IOException {
		try {
			interceptor.authorizeUserRequest(request, sessionState);
		} catch (RoomException e) {
			log.warn("Request filtered with error", e);
			if (transaction != null)
				transaction.sendError(e.getCode(), e.getMessage(), null);
			return false;
		}
		return true;
	}

	private void updateThreadName(final String name) {
		Thread.currentThread().setName("user:" + name);
	}
}
