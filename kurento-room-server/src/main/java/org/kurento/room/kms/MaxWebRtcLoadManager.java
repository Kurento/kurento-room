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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaxWebRtcLoadManager implements LoadManager {
  private static final Logger log = LoggerFactory.getLogger(MaxWebRtcLoadManager.class);

  private int maxWebRtcPerKms;

  public MaxWebRtcLoadManager(int maxWebRtcPerKms) {
    this.maxWebRtcPerKms = maxWebRtcPerKms;
  }

  @Override
  public double calculateLoad(Kms kms) {
    int numWebRtcs = countWebRtcEndpoints(kms);
    if (numWebRtcs > maxWebRtcPerKms) {
      return 1;
    } else {
      return numWebRtcs / (double) maxWebRtcPerKms;
    }
  }

  @Override
  public boolean allowMoreElements(Kms kms) {
    return countWebRtcEndpoints(kms) < maxWebRtcPerKms;
  }

  private synchronized int countWebRtcEndpoints(Kms kms) {
    try {
      return kms.getKurentoClient().getServerManager().getPipelines().size();
    } catch (Throwable e) {
      log.warn("Error counting KurentoClient pipelines", e);
      return 0;
    }
  }
}
