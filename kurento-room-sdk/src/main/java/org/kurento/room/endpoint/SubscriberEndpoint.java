/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

package org.kurento.room.endpoint;

import org.kurento.client.MediaPipeline;
import org.kurento.client.MediaType;
import org.kurento.room.api.MutedMediaType;
import org.kurento.room.exception.RoomException;
import org.kurento.room.exception.RoomException.Code;
import org.kurento.room.internal.Participant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Subscriber aspect of the {@link MediaEndpoint}.
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class SubscriberEndpoint extends MediaEndpoint {
	private final static Logger log = LoggerFactory
			.getLogger(SubscriberEndpoint.class);

	private boolean connectedToPublisher = false;

	private PublisherEndpoint publisher = null;

	public SubscriberEndpoint(boolean web, Participant owner,
			String endpointName, MediaPipeline pipeline) {
		super(web, owner, endpointName, pipeline, log);
	}

	public synchronized String subscribe(String sdpOffer,
			PublisherEndpoint publisher) {
		registerOnIceCandidateEventListener();
		String sdpAnswer = processOffer(sdpOffer);
		gatherCandidates();
		publisher.connect(this.getEndpoint());
		setConnectedToPublisher(true);
		setPublisher(publisher);
		return sdpAnswer;
	}

	public boolean isConnectedToPublisher() {
		return connectedToPublisher;
	}

	public void setConnectedToPublisher(boolean connectedToPublisher) {
		this.connectedToPublisher = connectedToPublisher;
	}

	public PublisherEndpoint getPublisher() {
		return publisher;
	}

	public void setPublisher(PublisherEndpoint publisher) {
		this.publisher = publisher;
	}

	@Override
	public synchronized void mute(MutedMediaType muteType) {
		if (this.publisher == null)
			throw new RoomException(Code.MEDIA_MUTE_ERROR_CODE,
					"Publisher endpoint not found");
		switch (muteType) {
			case ALL:
				this.publisher.disconnectFrom(this.getEndpoint());
				break;
			case AUDIO:
				this.publisher.disconnectFrom(this.getEndpoint(),
						MediaType.AUDIO);
				break;
			case VIDEO:
				this.publisher.disconnectFrom(this.getEndpoint(),
						MediaType.VIDEO);
				break;
		}
		resolveCurrentMuteType(muteType);
	}

	@Override
	public synchronized void unmute() {
		this.publisher.connect(this.getEndpoint());
		setMuteType(null);
	}
}
