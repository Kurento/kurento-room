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

import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.room.api.pojo.ParticipantRequest;
import org.kurento.room.rpc.JsonRpcNotificationService;
import org.kurento.room.rpc.JsonRpcProtocolElements;
import org.kurento.room.rpc.JsonRpcUserControl;
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

	//	@Autowired
	private JsonRpcNotificationService notificationService;

	public RoomJsonRpcHandler(JsonRpcNotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@Override
	public final void handleRequest(Transaction transaction,
			Request<JsonObject> request) throws Exception {

		updateThreadName(HANDLER_THREAD_NAME + "_"
				+ transaction.getSession().getSessionId());

		log.debug("Session #{} - request: {} (requestSessionId={})", transaction.getSession().getSessionId(), request, request.getSessionId());

		notificationService.addTransaction(transaction, request);

		ParticipantRequest participantRequest = new ParticipantRequest(
				transaction.getSession().getSessionId(), Integer.toString(request.getId()));

		transaction.startAsync();

		switch (request.getMethod()) {
		case JsonRpcProtocolElements.JOIN_ROOM_METHOD:
			userControl.joinRoom(transaction, request, participantRequest);
			break;
		case JsonRpcProtocolElements.PUBLISH_VIDEO_METHOD:
			userControl.publishVideo(transaction, request, participantRequest);
			break;
		case JsonRpcProtocolElements.RECEIVE_VIDEO_METHOD:
			userControl.receiveVideoFrom(transaction, request,
					participantRequest);
			break;
		case JsonRpcProtocolElements.ON_ICE_CANDIDATE_METHOD:
			userControl
			.onIceCandidate(transaction, request, participantRequest);
			break;
		case JsonRpcProtocolElements.LEAVE_ROOM_METHOD:
			userControl.leaveRoom(transaction, request, participantRequest);
			break;
		case JsonRpcProtocolElements.SENDMESSAGE_ROOM_METHOD:
			userControl.sendMessage(transaction, request, participantRequest);
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
		ParticipantRequest preq = new ParticipantRequest(
				session.getSessionId(), null);
		updateThreadName(session.getSessionId() + "|wsclosed");
		userControl.leaveRoom(null, null, preq);
		updateThreadName(HANDLER_THREAD_NAME);
	}

	@Override
	public void handleTransportError(Session session, Throwable exception)
			throws Exception {
		log.warn("Transport error for session id {}", session.getSessionId(),
				exception);
	}

	private void updateThreadName(final String name) {
		Thread.currentThread().setName("user:" + name);
	}
}
