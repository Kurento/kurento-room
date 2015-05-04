/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

import java.util.LinkedList;

import org.kurento.client.IceCandidate;
import org.kurento.client.WebRtcEndpoint;

public class IceWebRtcEndpoint {
	private WebRtcEndpoint endpoint = null;
	private LinkedList<IceCandidate> candidates;

	public IceWebRtcEndpoint() {
		candidates = new LinkedList<IceCandidate>();
	}

	public WebRtcEndpoint getEndpoint() {
		return endpoint;
	}

	public synchronized WebRtcEndpoint setEndpoint(WebRtcEndpoint endpoint) {
		WebRtcEndpoint old = this.endpoint;
		if (this.endpoint == null) {
			this.endpoint = endpoint;
			while (!candidates.isEmpty())
				this.endpoint.addIceCandidate(candidates.removeFirst());
		}
		return old;
	}

	public synchronized void addIceCandidate(IceCandidate candidate) {
		if (endpoint == null)
			candidates.addLast(candidate);
		else
			endpoint.addIceCandidate(candidate);
	}
}
