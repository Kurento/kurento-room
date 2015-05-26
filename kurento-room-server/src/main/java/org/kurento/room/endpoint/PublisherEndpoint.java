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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.kurento.client.MediaElement;
import org.kurento.client.MediaPipeline;
import org.kurento.room.api.MediaShapingEndpoint;
import org.kurento.room.api.RoomException;
import org.kurento.room.internal.Participant;

/**
 * Publisher aspect of the {@link TrickleIceEndpoint}.
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class PublisherEndpoint extends IceWebRtcEndpoint implements MediaShapingEndpoint {

	private Map<String, MediaElement> elements = new HashMap<String, MediaElement>();
	private LinkedList<String> elementIds = new LinkedList<String>();
	private boolean connected = false;

	public PublisherEndpoint(Participant owner, String endpointName,
			MediaPipeline pipeline) {
		super(owner, endpointName, pipeline);
	}

	public synchronized String publish(String sdpOffer) {
		registerOnIceCandidateEventListener();
		connect(endpoint); //loopback
		String sdpAnswer = processOffer(sdpOffer);
		gatherCandidates();
		return sdpAnswer;
	}

	public synchronized void connect(MediaElement other) {
		if (!connected)
			innerConnect();
		passThru.connect(other);
	}

	@Override
	public synchronized String apply(MediaElement shaper) throws RoomException {
		String id = shaper.getId();
		if (elements.containsKey(id))
			throw new RoomException(RoomException.WEBRTC_ENDPOINT_ERROR_CODE,
					"This endpoint already has a media element with id " + id);
		MediaElement first = null;
		if (!elementIds.isEmpty())
			first = elements.get(elementIds.getFirst());
		if (connected) {
			if (first != null)
				first.connect(shaper);
			else
				endpoint.connect(shaper);
			shaper.connect(passThru);
		}
		elementIds.addFirst(id);
		elements.put(id, shaper);
		return id;
	}

	@Override
	public synchronized void revert(String shaperId) throws RoomException {
		if (!elements.containsKey(shaperId))
			throw new RoomException(RoomException.WEBRTC_ENDPOINT_ERROR_CODE,
					"This endpoint has no media element with id " + shaperId);
		MediaElement element = elements.get(shaperId);
		// TODO do it inside a transaction??
		element.release();
		if (!connected)
			return;

		String nextId = getNext(shaperId);
		String prevId = getPrevious(shaperId);
		// next connects to prev
		MediaElement prev = null;
		MediaElement next = null;
		if (nextId != null)
			next = elements.get(nextId);
		else
			next = endpoint;
		if (prevId != null)
			prev = elements.get(prevId);
		else
			prev = passThru;
		next.connect(prev);
	}

	private String getNext(String uid) {
		int idx = elementIds.indexOf(uid);
		if (idx < 0 || idx + 1 == elementIds.size())
			return null;
		return elementIds.get(idx + 1);
	}

	private String getPrevious(String uid) {
		int idx = elementIds.indexOf(uid);
		if (idx <= 0)
			return null;
		return elementIds.get(idx - 1);
	}

	private void innerConnect() {
		if (endpoint == null)
			throw new RoomException(RoomException.WEBRTC_ENDPOINT_ERROR_CODE,
					"Can't connect null WebRtcEndpoint");
		MediaElement current = endpoint;
		String prevId = elementIds.peekLast();
		while (prevId != null) {
			MediaElement prev = elements.get(prevId);
			if (prev == null)
				throw new RoomException(
						RoomException.WEBRTC_ENDPOINT_ERROR_CODE,
						"No media element with id " + prevId);
			current.connect(prev);
			current = prev;
			prevId = getPrevious(prevId);
		}
		current.connect(passThru);
		connected = true;
	}
}
