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

import org.kurento.jsonrpc.message.Request;
import org.kurento.room.internal.Participant;

import com.google.gson.JsonObject;

/**
 * Filter that will be applied before processing the user JSON-RPC requests.
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public interface RoomRequestsFilter {

	/**
	 * The user's state depending on the info that can be gathered from the JSON
	 * RPC session.
	 * 
	 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
	 */
	public enum SessionState {
		/**
		 * The user info is not yet registered in this session.
		 */
		NEW,
		/**
		 * The user session has been closed, indicating a disconnection.
		 */
		DISCONNECTED,
		/**
		 * The current message will be processed inside a registered session.
		 */
		REGISTERED;
	}

	/**
	 * This method is invoked by the JSON RPC handler before processing the
	 * request.
	 * 
	 * @param request
	 *            JSON RPC request, can be null if the session is
	 *            {@link SessionState#DISCONNECTED}
	 * @param participantSession
	 *            the user info stored in the current JSON RPC session; the
	 *            contained {@link Participant} could be null if the session is
	 *            {@link SessionState#NEW}
	 * @param sessionState
	 *            the session's state
	 * @throws RoomException
	 *             if thrown, the handler will not continue processing the
	 *             request, but will respond with an error message
	 */
	public void filterUserRequest(Request<JsonObject> request,
			ParticipantSession participantSession, SessionState sessionState)
					throws RoomException;
}
