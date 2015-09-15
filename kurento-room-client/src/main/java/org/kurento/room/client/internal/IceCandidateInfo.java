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

import org.kurento.client.IceCandidate;
import org.kurento.room.internal.ProtocolElements;

/**
 * @see Notification
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class IceCandidateInfo extends Notification {

	private IceCandidate iceCandidate;
	private String endpointName;

	public IceCandidateInfo(IceCandidate iceCandidate, String endpointName) {
		super(ProtocolElements.ICECANDIDATE_METHOD);
		this.iceCandidate = iceCandidate;
		this.endpointName = endpointName;
	}

	public IceCandidate getIceCandidate() {
		return iceCandidate;
	}

	public void setIceCandidate(IceCandidate iceCandidate) {
		this.iceCandidate = iceCandidate;
	}

	public String getEndpointName() {
		return endpointName;
	}

	public void setEndpointName(String endpointName) {
		this.endpointName = endpointName;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		if (getMethod() != null)
			builder.append("method=").append(getMethod()).append(", ");
		if (endpointName != null)
			builder.append("endpointName=").append(endpointName).append(", ");
		if (iceCandidate != null)
			builder.append("iceCandidate=[sdpMLineIndex= ")
					.append(iceCandidate.getSdpMLineIndex())
					.append(", sdpMid=").append(iceCandidate.getSdpMid())
					.append(", candidate=").append(iceCandidate.getCandidate())
					.append("]");
		builder.append("]");
		return builder.toString();
	}

}
