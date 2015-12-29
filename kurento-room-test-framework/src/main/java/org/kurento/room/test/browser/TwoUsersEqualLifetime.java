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
 * Room demo integration test (basic version).
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Radu Tom Vlad (rvlad@naevatec.com)
 * @since 5.0.0
 */
public class TwoUsersEqualLifetime extends RoomFunctionalBrowserTest<WebPage> {

  public static final int NUM_USERS = 2;

  @Test
  public void test() throws Exception {

    String user0Name = getBrowserKey(0);
    String user1Name = getBrowserKey(1);

    joinToRoom(0, user0Name, roomName);
    log.info("User '{}' joined to room '{}'", user0Name, roomName);

    joinToRoom(1, user1Name, roomName);
    log.info("User '{}' joined to room '{}'", user1Name, roomName);

    // FIXME it fails sporadically (could be the TrickleICE mechanism)

    waitForStream(0, user0Name, 0);
    log.debug("Received media from '{}' in '{}'", user0Name, user0Name);
    waitForStream(0, user0Name, 1);
    log.debug("Received media from '{}' in '{}'", user1Name, user0Name);

    waitForStream(1, user1Name, 0);
    log.debug("Received media from '{}' in '{}'", user0Name, user1Name);
    waitForStream(1, user1Name, 1);
    log.debug("Received media from '{}' in '{}'", user1Name, user1Name);

    // Guard time to see application in action
    sleep(PLAY_TIME * 1000);

    // Stop application by caller
    exitFromRoom(0, user0Name);
    exitFromRoom(1, user1Name);
  }
}
