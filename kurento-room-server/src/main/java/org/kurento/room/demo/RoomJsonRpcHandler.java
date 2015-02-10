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
package org.kurento.room.demo;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.client.Continuation;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.kurento.jsonrpc.message.ResponseError;
import org.kurento.room.demo.RoomManager.ParticipantSession;
import org.kurento.room.demo.RoomManager.RMContinuation;
import org.kurento.room.demo.RoomManager.ReceiveVideoFromResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @author Ivan Gracia (izanmail@gmail.com)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 1.0.0
 */
public class RoomJsonRpcHandler extends DefaultJsonRpcHandler<JsonObject> {

    private static final Logger log = LoggerFactory
            .getLogger(RoomJsonRpcHandler.class);

    private static final String SENDMESSAGE_ROOM_METHOD = "sendMessage"; //CHAT
    private static final String SENDMESSAGE_USER_PARAM = "userMessage"; //CHAT
    private static final String SENDMESSAGE_ROOM_PARAM = "roomMessage"; //CHAT
    private static final String SENDMESSAGE_MESSAGE_PARAM = "message"; //CHAT

    private static final String LEAVE_ROOM_METHOD = "leaveRoom";

    private static final String JOIN_ROOM_METHOD = "joinRoom";
    private static final String JOIN_ROOM_USER_PARAM = "user";
    private static final String JOIN_ROOM_ROOM_PARAM = "room";

    private static final String RECEIVE_VIDEO_METHOD = "receiveVideoFrom";
    private static final String RECEIVE_VIDEO_SDPOFFER_PARAM = "sdpOffer";
    private static final String RECEIVE_VIDEO_SENDER_PARAM = "sender";

    private static final String PARTICIPANT_SESSION_ATTRIBUTE = "user";

    private static final String HANDLER_THREAD_NAME = "handler";

    private static class ParticipantSessionJsonRpc implements
            ParticipantSession {

        private Session session;
        private Participant roomParticipant;

        public ParticipantSessionJsonRpc(Session session) {
            this.session = session;
        }

        @Override
        public void setParticipant(Participant roomParticipant) {
            this.roomParticipant = roomParticipant;
        }

        public Participant getRoomParticipant() {
            return roomParticipant;
        }

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

        public Session getSession() {
            return session;
        }
    }

    @Autowired
    private RoomManager roomManager;

    @Override
    public void handleRequest(final Transaction transaction,
            final Request<JsonObject> request) throws Exception {

        updateThreadName(HANDLER_THREAD_NAME + "_"
                + transaction.getSession().getSessionId());

        final ParticipantSessionJsonRpc participantSession = getParticipantSession(transaction
                .getSession());

        if (participantSession.getRoomParticipant() != null) {
            log.debug("Incoming message from user '{}': {}",
                    participantSession.getName(), request);
        } else {
            log.debug("Incoming message from new user: {}", request);
        }

        switch (request.getMethod()) {
            case RECEIVE_VIDEO_METHOD:
                receiveVideoFrom(transaction, request, participantSession);
                break;
            case JOIN_ROOM_METHOD:
                joinRoom(transaction, request, participantSession);
                break;
            case LEAVE_ROOM_METHOD:
                leaveRoom(participantSession);
                break;
            case SENDMESSAGE_ROOM_METHOD: //CHAT
                sendMessage(transaction, request, participantSession);
                break;
            default:
                log.error("Unrecognized request {}", request);
                break;
        }

        updateThreadName(HANDLER_THREAD_NAME);
    }

    private void leaveRoom(final ParticipantSessionJsonRpc participantSession)
            throws IOException, InterruptedException, ExecutionException {
        Participant roomParticipant = participantSession.getRoomParticipant();
        if (roomParticipant != null) {
            roomManager.leaveRoom(roomParticipant);
            removeParticipantForSession(participantSession);
        } else {
            log.warn("User is trying to leave from room but session has no info about user");
        }
    }

    private void joinRoom(final Transaction transaction,
            final Request<JsonObject> request,
            final ParticipantSessionJsonRpc participantSession)
            throws IOException, InterruptedException, ExecutionException {

        final String roomName = request.getParams().get(JOIN_ROOM_ROOM_PARAM)
                .getAsString();

        final String userName = request.getParams().get(JOIN_ROOM_USER_PARAM)
                .getAsString();

        transaction.startAsync();

        roomManager.joinRoom(roomName, userName, participantSession,
                new RMContinuation<Collection<Participant>>() {
                    @Override
                    public void result(Throwable error,
                            Collection<Participant> participants) {

                        try {
                            if (error != null) {
                                log.error("Exception processing joinRoom",
                                        error);

                                if (error instanceof RoomManagerException) {
                                    RoomManagerException e = (RoomManagerException) error;
                                    transaction.sendError(e.getCode(),
                                            e.getMessage(), null);
                                } else {
                                    transaction.sendError(error);
                                }
                            } else {

                                JsonArray result = new JsonArray();

                                for (Participant participant : participants) {

                                    JsonObject participantJson = new JsonObject();
                                    participantJson.addProperty("id",
                                            participant.getName());
                                    JsonObject stream = new JsonObject();
                                    stream.addProperty("id", "webcam");
                                    JsonArray streamsArray = new JsonArray();
                                    streamsArray.add(stream);
                                    participantJson
                                    .add("streams", streamsArray);

                                    result.add(participantJson);
                                }

                                transaction.sendResponse(result);
                            }
                        } catch (IOException e) {
                            log.error("Exception responding to user", e);
                        }
                    }
                });
    }

    private void receiveVideoFrom(final Transaction transaction,
            final Request<JsonObject> request,
            final ParticipantSessionJsonRpc participantSession) {

        String senderName = request.getParams().get(RECEIVE_VIDEO_SENDER_PARAM)
                .getAsString();

        senderName = senderName.substring(0, senderName.indexOf("_"));

        final String sdpOffer = request.getParams()
                .get(RECEIVE_VIDEO_SDPOFFER_PARAM).getAsString();

        transaction.startAsync();

        roomManager.receiveVideoFrom(participantSession.getRoomParticipant(),
                senderName, sdpOffer,
                new RMContinuation<ReceiveVideoFromResponse>() {
                    @Override
                    public void result(Throwable error,
                            ReceiveVideoFromResponse result) {

                        Response<JsonObject> response;

                        if (error != null
                        && error instanceof RoomManagerException) {

                            RoomManagerException e = (RoomManagerException) error;

                            response = new Response<>(new ResponseError(e
                                            .getCode(), e.getMessage()));
                        } else {

                            final JsonObject resultJson = new JsonObject();
                            resultJson.addProperty("name", result.name);
                            resultJson.addProperty("sdpAnswer",
                                    result.sdpAnswer);

                            response = new Response<>(resultJson);
                        }

                        try {
                            transaction.sendResponseObject(response);
                        } catch (IOException e) {
                            log.error("Exception sending response to request: "
                                    + request);
                        }
                    }
                });
    }

    private ParticipantSessionJsonRpc getParticipantSession(Session session) {

        ParticipantSessionJsonRpc participantSession = (ParticipantSessionJsonRpc) session
                .getAttributes().get(PARTICIPANT_SESSION_ATTRIBUTE);

        if (participantSession == null) {
            participantSession = new ParticipantSessionJsonRpc(session);
            session.getAttributes().put(PARTICIPANT_SESSION_ATTRIBUTE,
                    participantSession);
        }

        return participantSession;
    }

    private void removeParticipantForSession(
            ParticipantSessionJsonRpc participantSession) {
        log.info("Removing participantInfo about "
                + participantSession.getName());
        participantSession.getSession().getAttributes()
                .remove(PARTICIPANT_SESSION_ATTRIBUTE);
    }

    @Override
    public void afterConnectionClosed(Session session, String status)
            throws Exception {

        Participant participant = getParticipantSession(session)
                .getRoomParticipant();

        if (participant != null) {
            updateThreadName(participant.getName() + "|wsclosed");
            roomManager.leaveRoom(participant);
            updateThreadName(HANDLER_THREAD_NAME);
        }
    }

    @Override
    public void handleTransportError(Session session, Throwable exception)
            throws Exception {

        Participant user = getParticipantSession(session).getRoomParticipant();

        if (user != null && !user.isClosed()) {
            log.warn("Transport error", exception);
        }
    }

    private void updateThreadName(final String name) {
        Thread.currentThread().setName("user:" + name);
    }

    //CHAT
    private void sendMessage(final Transaction transaction,
            final Request<JsonObject> request,
            final ParticipantSessionJsonRpc participantSession)
            throws IOException, InterruptedException, ExecutionException {

        final String userName = request.getParams().get(SENDMESSAGE_USER_PARAM)
                .getAsString();
        final String roomName = request.getParams().get(SENDMESSAGE_ROOM_PARAM)
                .getAsString();
        final String message = request.getParams().get(SENDMESSAGE_MESSAGE_PARAM)
                .getAsString();

        log.debug("User " + userName + " send message " + message + " from room " + roomName);
        participantSession.getRoomParticipant().getRoom().sendMessage(roomName, userName, message);
    }
}
