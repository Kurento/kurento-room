/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

package org.kurento.room.api;

import org.kurento.client.KurentoClient;
import org.kurento.room.exception.RoomException;

/**
 * This service interface was designed so that the room manager could obtain a
 * {@link KurentoClient} instance at any time, without requiring knowledge about
 * the placement of the media server instances. It is left for the developer to
 * provide an implementation for this API.
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public interface KurentoClientProvider {

	/**
	 * Obtains a {@link KurentoClient} instance given the custom session bean.
	 * Normally, it’d be called during a room’s instantiation.
	 * 
	 * @param sessionInfo custom information object required by the implementors
	 *        of this interface
	 * @return the {@link KurentoClient} instance
	 * @throws RoomException in case there is an error obtaining a
	 *         {@link KurentoClient} instance
	 */
	KurentoClient getKurentoClient(KurentoClientSessionInfo sessionInfo)
			throws RoomException;
}
