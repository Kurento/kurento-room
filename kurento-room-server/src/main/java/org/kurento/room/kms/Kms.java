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

package org.kurento.room.kms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;

public class Kms {

	public interface KmsCallback<F> {
		void execute(F target);
	}

	private List<MediaPipeline> pipelines = Collections.synchronizedList(new ArrayList<MediaPipeline>());
	private LoadManager loadManager = new MaxWebRtcLoadManager(10000);
	private KurentoClient client;
	private String kmsUri;

	public Kms(KurentoClient client, String kmsUri) {
		this.client = client;
		this.kmsUri = kmsUri;
	}

	public MediaPipeline newPipeline() {
		MediaPipeline pipeline = client.createMediaPipeline();
		pipelines.add(pipeline);
		return pipeline;
	}

	public void removePipeline(MediaPipeline pipeline) {
		synchronized (pipelines) {
			Iterator<MediaPipeline> it = pipelines.iterator();
			while (it.hasNext()) {
				MediaPipeline mp = it.next();
				if (mp.getId().equals(pipeline.getId()))
					it.remove();
			}
		}
	}

	public void executeForEachPipeline(KmsCallback<MediaPipeline> cont) {
		synchronized (pipelines) {
			Iterator<MediaPipeline> it = pipelines.iterator();
			while (it.hasNext())
				cont.execute(it.next());
		}
	}

	public void setLoadManager(LoadManager loadManager) {
		this.loadManager = loadManager;
	}

	public double getLoad() {
		return loadManager.calculateLoad(this);
	}

	public boolean allowMoreElements() {
		return loadManager.allowMoreElements(this);
	}

	public String getUri() {
		return kmsUri;
	}
}
