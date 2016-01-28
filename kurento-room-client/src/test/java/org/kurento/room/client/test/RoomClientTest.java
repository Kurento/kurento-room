/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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
package org.kurento.room.client.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.kurento.room.internal.ProtocolElements.JOINROOM_METHOD;
import static org.kurento.room.internal.ProtocolElements.JOINROOM_ROOM_PARAM;
import static org.kurento.room.internal.ProtocolElements.JOINROOM_USER_PARAM;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.room.client.KurentoRoomClient;
import org.kurento.room.client.ServerJsonRpcHandler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Unit tests for the room client protocol.
 * 
 * @author Radu Tom Vlad (rvlad@naevatec.com)
 * @since 6.3.1
 */
public class RoomClientTest {

  private KurentoRoomClient client;
  private ServerJsonRpcHandler serverHandler;
  private JsonRpcClient jsonRpcClient;

  @Before
  public void setup() {
    jsonRpcClient = mock(JsonRpcClient.class);
    serverHandler = new ServerJsonRpcHandler();
    client = new KurentoRoomClient(jsonRpcClient, serverHandler);
  }

  @Test
  public void testRoomJoin() throws IOException {
    JsonObject params = new JsonObject();
    params.addProperty(JOINROOM_ROOM_PARAM, "room");
    params.addProperty(JOINROOM_USER_PARAM, "user");

    JsonObject result = new JsonObject();
    JsonArray value = new JsonArray();
    result.add("value", value);

    Map<String, List<String>> joinResult = new HashMap<String, List<String>>();

    when(jsonRpcClient.sendRequest(JOINROOM_METHOD, params)).thenReturn(result);
    assertThat(client.joinRoom("room", "user"), is(joinResult));

  }
}
