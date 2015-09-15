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
public class ParticipantLeftInfo extends Notification {

	private String name;

	public ParticipantLeftInfo(String name) {
		super(ProtocolElements.PARTICIPANTLEFT_METHOD);
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		if (getMethod() != null)
			builder.append("method=").append(getMethod()).append(", ");
		if (name != null)
			builder.append("name=").append(name);
		builder.append("]");
		return builder.toString();
	}
}
