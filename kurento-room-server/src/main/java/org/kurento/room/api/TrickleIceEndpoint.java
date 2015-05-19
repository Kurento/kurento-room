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

package org.kurento.room.api;

import org.kurento.client.IceCandidate;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.room.internal.Participant;

/**
 * {@link WebRtcEndpoint} wrapper that supports buffering of
 * {@link IceCandidate}s until the {@link WebRtcEndpoint} is created.
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public interface TrickleIceEndpoint {
	/**
	 * Qualities enumerator that can define a {@link TrickleIceEndpoint}.
	 * 
	 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
	 */
	public static enum EndpointQualifier {
		/**
		 * The endpoint represents a local stream.
		 */
		LOCAL,
		/**
		 * The endpoint is remote.
		 */
		REMOTE,
		/**
		 * The endpoint is the first one created in a room.
		 */
		FIRST;
	}

	/**
	 * Builder of {@link TrickleIceEndpoint} instances.
	 * 
	 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
	 */
	public static interface EndpointBuilder {

		/**
		 * Builds a new {@link TrickleIceEndpoint} instance.
		 * 
		 * @param pipeline
		 *            {@link MediaPipeline} that the {@link TrickleIceEndpoint}
		 *            will be using to create its {@link WebRtcEndpoint}
		 * @param qualifier
		 *            {@link EndpointQualifier}s that can customize the
		 *            {@link TrickleIceEndpoint}'s behavior
		 * @return the new instance
		 */
		public TrickleIceEndpoint build(MediaPipeline pipeline,
				EndpointQualifier... qualifier);
	}

	/**
	 * @return the internal {@link WebRtcEndpoint}
	 */
	public WebRtcEndpoint getEndpoint();

	/**
	 * If this object doesn't have a {@link WebRtcEndpoint}, it is created in a
	 * thread-safe way using the internal {@link MediaPipeline}. Otherwise no
	 * actions are taken.
	 * 
	 * @param endpoint
	 * @return the existing endpoint, if any
	 */
	public WebRtcEndpoint createEndpoint();

	/**
	 * @return the pipeline
	 */
	public MediaPipeline getPipeline();

	/**
	 * Sets the {@link MediaPipeline} used to create the internal
	 * {@link WebRtcEndpoint}.
	 * 
	 * @param pipeline
	 *            the {@link MediaPipeline}
	 */
	public void setMediaPipeline(MediaPipeline pipeline);

	/**
	 * Connects this internal {@link WebRtcEndpoint} to the other's endpoint.
	 * 
	 * @param other
	 *            an instance of a {@link TrickleIceEndpoint}
	 */
	public void connect(TrickleIceEndpoint other);

	/**
	 * Orders the internal {@link WebRtcEndpoint} to process the offer String.
	 * 
	 * @see WebRtcEndpoint#processOffer(String)
	 * @param offer
	 *            String with the Sdp offer
	 * @return the Sdp answer
	 */
	public String processOffer(String offer);

	/**
	 * Registers a listener for when {@link WebRtcEndpoint} gathers a new
	 * {@link IceCandidate} and sends it to the remote User Agent as a
	 * notification using the messaging capabilities of the {@link Participant}.
	 * 
	 * @see WebRtcEndpoint#addOnIceCandidateListener(org.kurento.client.EventListener)
	 * @see Participant#sendNotification(String, com.google.gson.JsonObject)
	 * @param endpointName
	 *            an name that can be used to identify the
	 *            {@link WebRtcEndpoint}
	 * @param owner
	 *            the user from the current session
	 */
	public void registerOnIceCandidateEventListener(String endpointName,
			Participant owner);

	/**
	 * Add a new {@link IceCandidate} received gathered by the remote peer of
	 * this {@link WebRtcEndpoint}.
	 * 
	 * @param candidate
	 *            the remote candidate
	 */
	public void addIceCandidate(IceCandidate candidate);

	/**
	 * Instructs the internal {@link WebRtcEndpoint} to start gathering
	 * {@link IceCandidate}s.
	 */
	public void gatherCandidates();
}
