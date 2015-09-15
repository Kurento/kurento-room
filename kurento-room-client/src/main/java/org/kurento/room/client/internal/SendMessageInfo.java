/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License (LGPL)
 * version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package org.kurento.room.client.internal;

import org.kurento.room.internal.ProtocolElements;

/**
 * @see Notification
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class SendMessageInfo extends Notification {

	private String room;
	private String user;
	private String message;

	public SendMessageInfo(String room, String user, String message) {
		super(ProtocolElements.PARTICIPANTSENDMESSAGE_METHOD);
		this.room = room;
		this.user = user;
		this.message = message;
	}

	public String getRoom() {
		return room;
	}

	public void setRoom(String room) {
		this.room = room;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		if (getMethod() != null)
			builder.append("method=").append(getMethod()).append(", ");
		if (room != null)
			builder.append("room=").append(room).append(", ");
		if (user != null)
			builder.append("user=").append(user).append(", ");
		if (message != null)
			builder.append("message=").append(message);
		builder.append("]");
		return builder.toString();
	}
}
