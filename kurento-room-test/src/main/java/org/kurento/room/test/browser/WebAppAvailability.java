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

package org.kurento.room.test.browser;

import org.junit.Test;
import org.kurento.room.test.RoomFunctionalBrowserTest;
import org.kurento.test.browser.WebPage;

/**
 * Web app availability basic test.
 *
 * @author Radu Tom Vlad (rvlad@naevatec.com)
 * @since 6.2.1
 */
public class WebAppAvailability extends RoomFunctionalBrowserTest<WebPage> {

  public static final int NUM_USERS = 1;

  @Test
  public void test() throws Exception {
    boolean[] activeUsers = new boolean[NUM_USERS];

    int numUser = 0;
    String userName = getBrowserKey(numUser);
    log.info("User '{}' is joining room '{}'", userName, roomName);
    joinToRoom(numUser, userName, roomName);
    activeUsers[numUser] = true;
    verify(activeUsers);
    log.info("User '{}' joined room '{}'", userName, roomName);

    long start = System.currentTimeMillis();
    waitForStream(numUser, userName, numUser);
    long duration = System.currentTimeMillis() - start;
    log.info("Video received in browser of user '{}' for user '{}' in {} millis", userName,
        userName, duration);

    log.info("User '{}' is exiting from room '{}'", userName, roomName);
    exitFromRoom(numUser, userName);
    activeUsers[numUser] = false;
    verify(activeUsers);
    log.info("User '{}' exited from room '{}'", userName, roomName);
  }
}
