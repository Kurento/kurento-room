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

package org.kurento.room.demo.api;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public interface RoomManager {

	public interface RMContinuation<F> {
		void result(Throwable error, F result);
	}

	public Map<String, Room> getAllRooms();

	public Room getRoom(String roomName);

	public void joinRoom(String roomName, String userName,
			ParticipantSession session,
			RMContinuation<Collection<Participant>> cont) throws IOException,
			InterruptedException, ExecutionException;

	public void receiveVideoFrom(Participant recvParticipant,
			String senderParticipantName, String sdpOffer,
			RMContinuation<ReceiveVideoFromResponse> cont);

	public void leaveRoom(Participant user) throws IOException,
	InterruptedException, ExecutionException;
}
