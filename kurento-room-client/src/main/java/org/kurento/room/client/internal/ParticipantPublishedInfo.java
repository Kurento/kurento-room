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

import java.util.List;

import org.kurento.room.internal.ProtocolElements;

/**
 * @see Notification
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class ParticipantPublishedInfo extends Notification {

	private String id;
	private List<String> streams;

	public ParticipantPublishedInfo(String id, List<String> streams) {
		super(ProtocolElements.PARTICIPANTPUBLISHED_METHOD);
		this.id = id;
		this.streams = streams;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<String> getStreams() {
		return streams;
	}

	public void setStreams(List<String> streams) {
		this.streams = streams;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		if (getMethod() != null)
			builder.append("method=").append(getMethod()).append(", ");
		if (id != null)
			builder.append("id=").append(id).append(", ");
		if (streams != null)
			builder.append("streams=").append(streams);
		builder.append("]");
		return builder.toString();
	}
}
