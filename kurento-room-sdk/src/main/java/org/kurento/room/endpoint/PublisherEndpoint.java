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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.kurento.client.Continuation;
import org.kurento.client.ListenerSubscription;
import org.kurento.client.MediaElement;
import org.kurento.client.MediaPipeline;
import org.kurento.client.MediaType;
import org.kurento.client.PassThrough;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.room.exception.RoomException;
import org.kurento.room.exception.RoomException.Code;
import org.kurento.room.internal.Participant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publisher aspect of the {@link TrickleIceEndpoint}.
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class PublisherEndpoint extends IceWebRtcEndpoint implements
		MediaShapingEndpoint {
	private final static Logger log = LoggerFactory
			.getLogger(PublisherEndpoint.class);

	private PassThrough passThru = null;
	private ListenerSubscription passThruSubscription = null;

	private Map<String, MediaElement> elements =
			new HashMap<String, MediaElement>();
	private LinkedList<String> elementIds = new LinkedList<String>();
	private boolean connected = false;

	private Map<String, ListenerSubscription> elementsErrorSubscriptions =
			new HashMap<String, ListenerSubscription>();

	public PublisherEndpoint(Participant owner, String endpointName,
			MediaPipeline pipeline) {
		super(owner, endpointName, pipeline, log);
	}

	@Override
	protected void internalEndpointInitialization(
			final CountDownLatch endpointLatch) {
		super.internalEndpointInitialization(endpointLatch);
		passThru = new PassThrough.Builder(getPipeline()).build();
		passThruSubscription = registerElemErrListener(passThru);
	}

	@Override
	public synchronized void unregisterErrorListeners() {
		super.unregisterErrorListeners();
		unregisterElementErrListener(passThru, passThruSubscription);
		for (String elemId : elementIds)
			unregisterElementErrListener(elements.get(elemId),
					elementsErrorSubscriptions.remove(elemId));
	}

	/**
	 * @return all media elements created for this publisher, except for the
	 *         main element ({@link WebRtcEndpoint})
	 */
	public synchronized Collection<MediaElement> getMediaElements() {
		if (passThru != null)
			elements.put(passThru.getId(), passThru);
		return elements.values();
	}

	/**
	 * Initializes this {@link WebRtcEndpoint} for publishing media. Registers
	 * an event listener for the ICE candidates, instructs the endpoint to start
	 * gathering the candidates and processes the SDP offer or answer. If
	 * required, it connects to itself (after applying the intermediate media
	 * elements and the {@link PassThrough}) to allow loopback of the media
	 * stream.
	 * 
	 * @param sdpType indicates the type of the sdpString (offer or answer)
	 * @param sdpString offer or answer from the remote peer
	 * @param doLoopback loopback flag
	 * @param loopbackAlternativeSrc alternative loopback source
	 * @return the SDP response (the answer if processing an offer SDP,
	 *         otherwise is the updated offer generated previously by this
	 *         endpoint)
	 */
	public synchronized String publish(SdpType sdpType, String sdpString,
			boolean doLoopback, MediaElement loopbackAlternativeSrc) {
		registerOnIceCandidateEventListener();
		if (doLoopback) {
			if (loopbackAlternativeSrc == null)
				connect(endpoint);
			else
				connectAltLoopbackSrc(loopbackAlternativeSrc);
		}
		String sdpResponse = null;
		switch (sdpType) {
			case ANSWER:
				sdpResponse = processAnswer(sdpString);
				break;
			case OFFER:
				sdpResponse = processOffer(sdpString);
				break;
			default:
				throw new RoomException(Code.SDP_ERROR_CODE,
						"Sdp type not supported: " + sdpType);
		}
		gatherCandidates();
		return sdpResponse;
	}

	public synchronized String preparePublishConnection() {
		return generateOffer();
	}

	private void connectAltLoopbackSrc(MediaElement loopbackAlternativeSrc) {
		if (!connected)
			innerConnect();
		internalSinkConnect(loopbackAlternativeSrc, endpoint);
	}

	public synchronized void connect(MediaElement other) {
		if (!connected)
			innerConnect();
		internalSinkConnect(passThru, other);
	}

	@Override
	public synchronized String apply(MediaElement shaper) throws RoomException {
		return apply(shaper, null);
	}

	@Override
	public String apply(MediaElement shaper, MediaType type)
			throws RoomException {
		String id = shaper.getId();
		if (id == null)
			throw new RoomException(Code.WEBRTC_ENDPOINT_ERROR_CODE,
					"Unable to connect media element with null id");
		if (elements.containsKey(id))
			throw new RoomException(Code.WEBRTC_ENDPOINT_ERROR_CODE,
					"This endpoint already has a media element with id " + id);
		MediaElement first = null;
		if (!elementIds.isEmpty())
			first = elements.get(elementIds.getFirst());
		if (connected) {
			if (first != null)
				internalSinkConnect(first, shaper, type);
			else
				internalSinkConnect(endpoint, shaper, type);
			internalSinkConnect(shaper, passThru, type);
		}
		elementIds.addFirst(id);
		elements.put(id, shaper);
		elementsErrorSubscriptions.put(id, registerElemErrListener(shaper));
		return id;
	}

	@Override
	public synchronized void revert(MediaElement shaper) throws RoomException {
		final String elementId = shaper.getId();
		if (!elements.containsKey(elementId))
			throw new RoomException(Code.WEBRTC_ENDPOINT_ERROR_CODE,
					"This endpoint (" + getEndpointName()
							+ ") has no media element with id " + elementId);

		MediaElement element = elements.remove(elementId);
		unregisterElementErrListener(element,
				elementsErrorSubscriptions.remove(elementId));

		// careful, the order in the elems list is reverted
		if (connected) {
			String nextId = getNext(elementId);
			String prevId = getPrevious(elementId);
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
			internalSinkConnect(next, prev);
		}
		elementIds.remove(elementId);
		element.release(new Continuation<Void>() {
			@Override
			public void onSuccess(Void result) throws Exception {
				log.trace("EP {}: Released media element {}",
						getEndpointName(), elementId);
			}

			@Override
			public void onError(Throwable cause) throws Exception {
				log.error("EP {}: Failed to release media element {}",
						getEndpointName(), elementId, cause);
			}
		});
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
			throw new RoomException(Code.WEBRTC_ENDPOINT_ERROR_CODE,
					"Can't connect null WebRtcEndpoint (ep: "
							+ getEndpointName() + ")");
		MediaElement current = endpoint;
		String prevId = elementIds.peekLast();
		while (prevId != null) {
			MediaElement prev = elements.get(prevId);
			if (prev == null)
				throw new RoomException(Code.WEBRTC_ENDPOINT_ERROR_CODE,
						"No media element with id " + prevId + " (ep: "
								+ getEndpointName() + ")");
			internalSinkConnect(current, prev);
			current = prev;
			prevId = getPrevious(prevId);
		}
		internalSinkConnect(current, passThru);
		connected = true;
	}

	private void internalSinkConnect(final MediaElement source,
			final MediaElement sink) {
		source.connect(sink, new Continuation<Void>() {
			@Override
			public void onSuccess(Void result) throws Exception {
				log.debug(
						"EP {}: Elements have been connected (source {} -> sink {})",
						getEndpointName(), source.getId(), sink.getId());
			}

			@Override
			public void onError(Throwable cause) throws Exception {
				log.warn(
						"EP {}: Failed to connect media elements (source {} -> sink {})",
						getEndpointName(), source.getId(), sink.getId(), cause);
			}
		});
	}

	/**
	 * Same as {@link #internalSinkConnect(MediaElement, MediaElement)}, but can
	 * specify the type of the media that will be streamed.
	 * 
	 * @see #internalSinkConnect(MediaElement, MediaElement)
	 * @param source
	 * @param sink
	 * @param type if null,
	 *        {@link #internalSinkConnect(MediaElement, MediaElement)} will be
	 *        used instead
	 */
	private void internalSinkConnect(final MediaElement source,
			final MediaElement sink, final MediaType type) {
		if (type == null)
			internalSinkConnect(source, sink);
		else
			source.connect(sink, type, new Continuation<Void>() {
				@Override
				public void onSuccess(Void result) throws Exception {
					log.debug(
							"EP {}: {} media elements have been connected (source {} -> sink {})",
							getEndpointName(), type, source.getId(),
							sink.getId());
				}

				@Override
				public void onError(Throwable cause) throws Exception {
					log.warn(
							"EP {}: Failed to connect {} media elements (source {} -> sink {})",
							getEndpointName(), type, source.getId(),
							sink.getId(), cause);
				}
			});
	}
}
