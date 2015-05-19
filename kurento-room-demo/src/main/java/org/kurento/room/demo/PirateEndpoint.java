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

package org.kurento.room.demo;

import org.kurento.client.FaceOverlayFilter;
import org.kurento.client.MediaPipeline;
import org.kurento.room.api.TrickleIceEndpoint;
import org.kurento.room.internal.IceWebRtcEndpoint;

/**
 * Extension of the {@link IceWebRtcEndpoint} class that applies a
 * {@link FaceOverlayFilter} when the overlaid image's URL (the <em>hat</em>) is
 * not null.
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class PirateEndpoint extends IceWebRtcEndpoint {

	private String hatUrl;

	PirateEndpoint() {
		super();
	}

	public String getHatUrl() {
		return hatUrl;
	}

	public void setHatUrl(String hatUrl) {
		this.hatUrl = hatUrl;
	}

	@Override
	public void connect(TrickleIceEndpoint other) {
		if (this.hatUrl == null) {
			super.connect(other);
			return;
		}

		FaceOverlayFilter faceOverlayFilterPirate = new FaceOverlayFilter.Builder(
				this.getPipeline()).build();
		faceOverlayFilterPirate.setOverlayedImage(this.hatUrl, -0.35F, -1.2F,
				1.6F, 1.6F);

		// Connections
		this.getEndpoint().connect(faceOverlayFilterPirate);
		faceOverlayFilterPirate.connect(other.getEndpoint());
	}

	/**
	 * {@link TrickleIceEndpoint} builder which activates a
	 * {@link FaceOverlayFilter} on the endpoint when it's
	 * {@link EndpointQualifier#LOCAL} and {@link EndpointQualifier#FIRST}.
	 * 
	 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
	 *
	 */
	public static class PirateEndpointBuilder implements
			EndpointBuilder {

		private String hatUrl;

		/**
		 * @param hatUrl
		 *            url of the image to be overlaid
		 */
		public PirateEndpointBuilder(String hatUrl) {
			this.hatUrl = hatUrl;
		}

		/**
		 * {@inheritDoc} Sets the image's url to be overlaid on the media stream
		 * when the qualifiers are {@link EndpointQualifier#LOCAL} and
		 * {@link EndpointQualifier#FIRST}.
		 */
		@Override
		public TrickleIceEndpoint build(MediaPipeline pipeline,
				EndpointQualifier... qualifier) {
			PirateEndpoint pep = new PirateEndpoint();
			pep.setMediaPipeline(pipeline);
			boolean first = false;
			boolean local = false;
			for (int i = 0; i < qualifier.length; i++) {
				switch (qualifier[i]) {
				case FIRST:
					first = true;
					break;
				case LOCAL:
					local = true;
					break;
				case REMOTE:
				default:
					break;
				}
			}
			if (first && local)
				pep.setHatUrl(hatUrl);
			return pep;
		}
	}

}
