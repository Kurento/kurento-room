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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Subscriber aspect of the {@link TrickleIceEndpoint}.
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class SubscriberEndpoint extends IceWebRtcEndpoint {
	private final static Logger log = LoggerFactory.getLogger(SubscriberEndpoint.class);
	
	private boolean connectedToPublisher = false;

	public SubscriberEndpoint(Participant owner, String endpointName,
			MediaPipeline pipeline) {
		super(owner, endpointName, pipeline, log);
	}

	public synchronized String subscribe(String sdpOffer,
			PublisherEndpoint publisher) {
		registerOnIceCandidateEventListener();
		String sdpAnswer = processOffer(sdpOffer);
		gatherCandidates();
		publisher.connect(this.endpoint);
		setConnectedToPublisher(true);
		return sdpAnswer;
	}

	public boolean isConnectedToPublisher() {
		return connectedToPublisher;
	}

	public void setConnectedToPublisher(boolean connectedToPublisher) {
		this.connectedToPublisher = connectedToPublisher;
	}
}
