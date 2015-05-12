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

package org.kurento.room.demo.api;

import org.kurento.client.IceCandidate;
import org.kurento.client.WebRtcEndpoint;

public interface TrickleIceEndpoint {

	public WebRtcEndpoint getEndpoint();

	/**
	 * Sets the {@link WebRtcEndpoint} in a thread-safe way.
	 * 
	 * @param endpoint
	 * @return the old endpoint, if any
	 */
	public WebRtcEndpoint setEndpoint(WebRtcEndpoint endpoint);

	public void addIceCandidate(IceCandidate candidate);
}
