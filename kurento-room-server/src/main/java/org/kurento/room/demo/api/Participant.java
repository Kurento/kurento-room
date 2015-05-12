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
import java.util.concurrent.ExecutionException;

import org.kurento.client.IceCandidate;
import org.kurento.jsonrpc.message.Request;

import com.google.gson.JsonObject;

public interface Participant {// extends Closeable {

	public String getName();

	public Room getRoom();

	public void createReceivingEndpoint();

	public TrickleIceEndpoint getReceivingEndpoint();

	public TrickleIceEndpoint addSendingEndpoint(String newUserName);

	public String receiveVideoFrom(Participant sender, String sdpOffer);

	public void cancelSendingVideoTo(final String senderName);

	public void sendMessage(Request<JsonObject> request);

	public void sendNotification(String method, JsonObject params);

	public void addIceCandidate(String endpointName, IceCandidate iceCandidate);

	public void close();

	public boolean isClosed();

	public void leaveFromRoom() throws IOException, InterruptedException,
	ExecutionException;

}
