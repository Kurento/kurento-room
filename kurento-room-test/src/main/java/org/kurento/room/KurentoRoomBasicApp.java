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
package org.kurento.room;

import org.kurento.commons.ConfigFileManager;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Import;

/**
 * Basic demo application for Kurento Room, uses the Room Server and the Room Client JS libraries.
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Radu Tom Vlad (rvlad@naevatec.com)
 * @since 5.0.0
 */
@Import(KurentoRoomServerApp.class)
public class KurentoRoomBasicApp {

  private final static String BASICAPP_CFG_FILENAME = "kroombasic.conf.json";

  static {
    ConfigFileManager.loadConfigFile(BASICAPP_CFG_FILENAME);
  }

  public static void main(String[] args) {
    SpringApplication.run(KurentoRoomBasicApp.class, args);
  }
}
