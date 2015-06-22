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

package org.kurento.room;

import org.kurento.room.api.UserNotificationService;
import org.kurento.room.api.pojo.ParticipantRequest;
import org.kurento.room.exception.RoomException;

public class TestNotifcationService implements UserNotificationService {

	@Override
	public void sendResponse(ParticipantRequest participantRequest,
			Object result) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendErrorResponse(ParticipantRequest participantRequest,
			Object data, RoomException error) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendNotification(String participantId, String method,
			Object params) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void closeSession(ParticipantRequest participantRequest) {
		// TODO Auto-generated method stub
		
	}

}
