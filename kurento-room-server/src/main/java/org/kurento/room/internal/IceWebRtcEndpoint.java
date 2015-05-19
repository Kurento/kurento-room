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

package org.kurento.room.internal;

import java.util.LinkedList;

import org.kurento.client.EventListener;
import org.kurento.client.IceCandidate;
import org.kurento.client.MediaPipeline;
import org.kurento.client.OnIceCandidateEvent;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.commons.exception.KurentoException;
import org.kurento.room.api.TrickleIceEndpoint;
import org.kurento.room.api.control.JsonRpcProtocolElements;

import com.google.gson.JsonObject;

/**
 * Plain implementation of the {@link TrickleIceEndpoint}. Connections to other
 * peers are opened using the corresponding method of the internal endpoint.
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class IceWebRtcEndpoint implements TrickleIceEndpoint {

	private MediaPipeline pipeline = null;
	private WebRtcEndpoint endpoint = null;
	private LinkedList<IceCandidate> candidates;

	protected IceWebRtcEndpoint() {
		candidates = new LinkedList<IceCandidate>();
	}

	@Override
	public WebRtcEndpoint getEndpoint() {
		return endpoint;
	}

	@Override
	public synchronized WebRtcEndpoint createEndpoint() {
		WebRtcEndpoint old = this.endpoint;
		if (this.endpoint == null)
			this.endpoint = new WebRtcEndpoint.Builder(pipeline).build();
		while (!candidates.isEmpty())
			this.endpoint.addIceCandidate(candidates.removeFirst());
		return old;
	}

	@Override
	public MediaPipeline getPipeline() {
		return this.pipeline;
	}

	@Override
	public void setMediaPipeline(MediaPipeline pipeline) {
		this.pipeline = pipeline;
	}

	@Override
	public synchronized void addIceCandidate(IceCandidate candidate) {
		if (endpoint == null)
			candidates.addLast(candidate);
		else
			endpoint.addIceCandidate(candidate);
	}

	@Override
	public void connect(TrickleIceEndpoint other) {
		this.getEndpoint().connect(other.getEndpoint());
	}

	@Override
	public void registerOnIceCandidateEventListener(final String endpointName,
			final Participant owner) {
		if (endpoint == null)
			throw new KurentoException(
					"Can't register event listener for null WebRtcEndpoint");
		endpoint.addOnIceCandidateListener(new EventListener<OnIceCandidateEvent>() {
			@Override
			public void onEvent(OnIceCandidateEvent event) {
				JsonObject params = new JsonObject();
				params.addProperty(
						JsonRpcProtocolElements.ON_ICE_EP_NAME_PARAM,
						endpointName);
				params.addProperty(
						JsonRpcProtocolElements.ON_ICE_SDP_M_LINE_INDEX_PARAM,
						event.getCandidate().getSdpMLineIndex());
				params.addProperty(
						JsonRpcProtocolElements.ON_ICE_SDP_MID_PARAM, event
						.getCandidate().getSdpMid());
				params.addProperty(
						JsonRpcProtocolElements.ON_ICE_CANDIDATE_PARAM, event
						.getCandidate().getCandidate());
				owner.sendNotification(
						JsonRpcProtocolElements.ICE_CANDIDATE_EVENT, params);
			}
		});
	}

	@Override
	public String processOffer(String offer) {
		if (endpoint == null)
			throw new KurentoException(
					"Can't process offer when WebRtcEndpoint is null");
		return endpoint.processOffer(offer);
	}

	@Override
	public void gatherCandidates() {
		if (endpoint == null)
			throw new KurentoException(
					"Can't start gathering ICE candidates on null WebRtcEndpoint");
		endpoint.gatherCandidates();
	}

	/**
	 * Basic builder of {@link IceWebRtcEndpoint} instances.
	 * 
	 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
	 */
	public static class Builder implements EndpointBuilder {

		/**
		 * {@inheritDoc} This implementation ignores the qualifiers when
		 * creating a new instance of {@link IceWebRtcEndpoint}.
		 */
		@Override
		public TrickleIceEndpoint build(MediaPipeline pipeline,
				EndpointQualifier... qualifier) {
			IceWebRtcEndpoint ep = new IceWebRtcEndpoint();
			ep.setMediaPipeline(pipeline);
			return ep;
		}

	}
}
