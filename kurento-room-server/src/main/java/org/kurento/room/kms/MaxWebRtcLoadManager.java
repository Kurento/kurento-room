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

import org.kurento.client.MediaPipeline;
import org.kurento.room.kms.Kms.KmsCallback;

public class MaxWebRtcLoadManager implements LoadManager {

	private int maxWebRtcPerKms;

	private int currentMediaCount;

	public MaxWebRtcLoadManager(int maxWebRtcPerKms) {
		this.maxWebRtcPerKms = maxWebRtcPerKms;
	}

	@Override
	public double calculateLoad(Kms kms) {
		int numWebRtcs = countWebRtcEndpoints(kms);
		if (numWebRtcs > maxWebRtcPerKms) {
			return 1;
		} else {
			return numWebRtcs / ((double) maxWebRtcPerKms);
		}
	}

	@Override
	public boolean allowMoreElements(Kms kms) {
		return countWebRtcEndpoints(kms) < maxWebRtcPerKms;
	}

	private synchronized int countWebRtcEndpoints(Kms kms) {
		currentMediaCount = 0;
		kms.executeForEachPipeline(new KmsCallback<MediaPipeline>() {
			@Override
			public void execute(MediaPipeline target) {
				incCurrentMediaCount(target.getChilds().size());
			}
		});
		return currentMediaCount;
	}

	private void incCurrentMediaCount(int value) {
		this.currentMediaCount += value;
	}
}
