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

package org.kurento.room.api.pojo;

/**
 * This POJO uniquely identifies a participant's request.
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 *
 */
public class ParticipantRequest {
	private String requestId = null;
	private String participantId = null;

	public ParticipantRequest(String participantId, String requestId) {
		super();
		this.requestId = requestId;
		this.participantId = participantId;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String id) {
		this.requestId = id;
	}

	public String getParticipantId() {
		return participantId;
	}

	public void setParticipantId(String participantId) {
		this.participantId = participantId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result =
				prime * result
						+ ((requestId == null) ? 0 : requestId.hashCode());
		result =
				prime
						* result
						+ ((participantId == null) ? 0 : participantId
								.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ParticipantRequest))
			return false;
		ParticipantRequest other = (ParticipantRequest) obj;
		if (requestId == null) {
			if (other.requestId != null)
				return false;
		} else if (!requestId.equals(other.requestId))
			return false;
		if (participantId == null) {
			if (other.participantId != null)
				return false;
		} else if (!participantId.equals(other.participantId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		if (requestId != null)
			builder.append("requestId=").append(requestId).append(", ");
		if (participantId != null)
			builder.append("participantId=").append(participantId);
		builder.append("]");
		return builder.toString();
	}
}
