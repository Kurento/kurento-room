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

package org.kurento.room.demo.kms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.kurento.client.KurentoClient;

public abstract class KmsManager {

	private List<KurentoClient> kurentoClients = new ArrayList<KurentoClient>();
	private Iterator<KurentoClient> usageIterator = null;

	/**
	 * Returns a {@link KurentoClient} using a round-robin strategy.
	 */
	public synchronized KurentoClient getKurentoClient() {
		if (usageIterator == null || !usageIterator.hasNext())
			usageIterator = kurentoClients.iterator();
		return usageIterator.next();
	}

	public synchronized void addKurentoClient(KurentoClient kurentoClient) {
		this.kurentoClients.add(kurentoClient);
	}
}
