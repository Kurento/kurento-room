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

import org.kurento.jsonrpc.Session;
import org.kurento.room.api.control.ParticipantSessionJsonRpc;

public class SLAParticipantSessionJsonRpc extends ParticipantSessionJsonRpc {

	private Boolean hq = null;

	public SLAParticipantSessionJsonRpc(Session session) {
		super(session);
	}

	@Override
	public Boolean isHQ() {
		return hq;
	}

	public void setHQ(Boolean hq) {
		this.hq = hq;
	}
}
