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
}
