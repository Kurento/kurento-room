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
package org.kurento.room.test;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kurento.jsonrpc.client.JsonRpcClientLocal;
import org.kurento.room.NotificationRoomManager;
import org.kurento.room.RoomJsonRpcHandler;
import org.kurento.room.client.KurentoRoomClient;
import org.kurento.room.client.ServerJsonRpcHandler;
import org.kurento.room.exception.RoomException;
import org.kurento.room.exception.RoomException.Code;
import org.kurento.room.rpc.JsonRpcNotificationService;
import org.kurento.room.rpc.JsonRpcUserControl;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Integration tests for the room server protocol.
 *
 * @author Radu Tom Vlad (rvlad@naevatec.com)
 * @since 6.3.1
 */
@RunWith(MockitoJUnitRunner.class)
public class RoomProtocolTest {

  @Mock
  private NotificationRoomManager roomManager;
  @Mock
  private JsonRpcNotificationService notificationService;

  @InjectMocks
  private JsonRpcUserControl userControl;
  @InjectMocks
  private RoomJsonRpcHandler roomJsonRpcHandler;

  private JsonRpcClientLocal localClient;

  private KurentoRoomClient client;
  private ServerJsonRpcHandler serverHandler;

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);

    // FIXME dependencies are not correctly autowired

    // userControl = new JsonRpcUserControl();
    // roomJsonRpcHandler = new RoomJsonRpcHandler();

    localClient = new JsonRpcClientLocal(roomJsonRpcHandler);

    serverHandler = new ServerJsonRpcHandler();
    client = new KurentoRoomClient(localClient, serverHandler);
  }

  @Ignore
  @Test
  public void joinRoom() {
    doThrow(new RoomException(Code.USER_GENERIC_ERROR_CODE, "Join error")).when(roomManager)
    .joinRoom("user", "room", true, null);
    try {
      client.joinRoom("room", "user");
      fail("RoomException should be thrown");
    } catch (Exception e) {
      assertThat(e.getMessage(), containsString("Join error"));
    }
  }
}
