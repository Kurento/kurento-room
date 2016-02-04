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
package org.kurento.room.test.config;

import static org.kurento.test.config.TestConfiguration.KMS_WS_URI_DEFAULT;
import static org.kurento.test.config.TestConfiguration.KMS_WS_URI_PROP;

/**
 * Kurento Room test properties.
 *
 * @author Radu Tom Vlad (rvlad@naevatec.com)
 * @since 6.2.1
 */
public class RoomTestConfiguration {

  public static final String ROOM_APP_CLASSNAME_PROP = "room.app.classnames";
  public static final String ROOM_APP_CLASSNAME_DEFAULT = "[org.kurento.room.basic.KurentoRoomBasicApp,"
      + "org.kurento.room.demo.KurentoRoomDemoApp]";

  public static final String EXTRA_KMS_WS_URI_PROP = KMS_WS_URI_PROP + ".extra";
  public static final String EXTRA_KMS_WS_URI_DEFAULT = KMS_WS_URI_DEFAULT;

  public static final String ROOM_PREFIX = "room";
  public static final String USER_BROWSER_PREFIX = "browser";
  public static final String USER_FAKE_PREFIX = "user";
  public static final String DEFAULT_ROOM = ROOM_PREFIX;

  public final static int DEFAULT_ROOM_INOUT_AWAIT_TIME_IN_SECONDS = 60;
  public final static int DEFAULT_ACTIVE_LIVE_TOTAL_TIMEOUT_IN_SECONDS = 60;
  public final static int DEFAULT_PLAY_TIME_IN_SECONDS = 30;
  public static final int TASKS_TIMEOUT_IN_MINUTES = 15;
}
