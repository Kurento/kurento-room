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

public class JsonRpcProtocolElements {

	public static final String PARTICIPANT_SESSION_ATTRIBUTE = "user";

	public static final String SENDMESSAGE_ROOM_METHOD = "sendMessage"; //CHAT
	public static final String SENDMESSAGE_USER_PARAM = "userMessage"; //CHAT
	public static final String SENDMESSAGE_ROOM_PARAM = "roomMessage"; //CHAT
	public static final String SENDMESSAGE_MESSAGE_PARAM = "message"; //CHAT
	public static final String LEAVE_ROOM_METHOD = "leaveRoom";
	public static final String JOIN_ROOM_METHOD = "joinRoom";
	public static final String JOIN_ROOM_USER_PARAM = "user";
	public static final String JOIN_ROOM_ROOM_PARAM = "room";
	public static final String PUBLISH_VIDEO_METHOD = "publishVideo";
	public static final String UNPUBLISH_VIDEO_METHOD = "unpublishVideo";
	public static final String PUBLISH_VIDEO_SDPOFFER_PARAM = "sdpOffer";
	public static final String PUBLISH_VIDEO_DOLOOPBACK_PARAM = "doLoopback";
	public static final String RECEIVE_VIDEO_METHOD = "receiveVideoFrom";
	public static final String RECEIVE_VIDEO_SDPOFFER_PARAM = "sdpOffer";
	public static final String RECEIVE_VIDEO_SENDER_PARAM = "sender";
	public static final String UNSUBSCRIBE_VIDEO_METHOD = "unsubscribeFromVideo";
	public static final String ON_ICE_CANDIDATE_METHOD = "onIceCandidate";
	public static final String ICE_CANDIDATE_EVENT = "iceCandidate";
	public static final String ON_ICE_EP_NAME_PARAM = "endpointName";
	public static final String ON_ICE_CANDIDATE_PARAM = "candidate";
	public static final String ON_ICE_SDP_MID_PARAM = "sdpMid";
	public static final String ON_ICE_SDP_M_LINE_INDEX_PARAM = "sdpMLineIndex";
}
