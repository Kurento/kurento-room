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

package org.kurento.room.endpoint;

import org.kurento.client.MediaPipeline;
import org.kurento.room.internal.Participant;

/**
 * Subscriber aspect of the {@link TrickleIceEndpoint}.
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class SubscriberEndpoint extends IceWebRtcEndpoint {

	public SubscriberEndpoint(Participant owner, String endpointName,
			MediaPipeline pipeline) {
		super(owner, endpointName, pipeline);
	}

	public synchronized String subscribe(String sdpOffer,
			PublisherEndpoint publisher) {
		registerOnIceCandidateEventListener();
		String sdpAnswer = processOffer(sdpOffer);
		gatherCandidates();
		publisher.connect(this.endpoint);
		return sdpAnswer;
	}
}
