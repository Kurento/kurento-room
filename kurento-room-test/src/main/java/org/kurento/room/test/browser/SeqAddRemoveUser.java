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
public class SeqAddRemoveUser extends RoomFunctionalBrowserTest<WebPage> {

  private static final int WAIT_TIME = 1;

  public static final int NUM_USERS = 2;

  @Test
  public void test() throws Exception {

    boolean[] activeUsers = new boolean[NUM_USERS];

    for (int cycle = 0; cycle < ITERATIONS; cycle++) {

      for (int i = 0; i < NUM_USERS; i++) {
        String userName = getBrowserKey(i);
        log.info("User '{}' joining room '{}'", userName, roomName);
        joinToRoom(i, userName, roomName);
        activeUsers[i] = true;
        sleep(WAIT_TIME);
        verify(activeUsers);
        log.info("User '{}' joined to room '{}'", userName, roomName);
      }

      for (int i = 0; i < NUM_USERS; i++) {
        for (int j = 0; j < NUM_USERS; j++) {
          waitForStream(i, getBrowserKey(i), j);
          log.debug("Received media from '{}' in browser of '{}'", getBrowserKey(j),
              getBrowserKey(i));
        }
      }

      // Guard time to see application in action
      sleep(PLAY_TIME);

      // Stop application by caller
      for (int i = 0; i < NUM_USERS; i++) {
        String userName = getBrowserKey(i);
        log.info("User '{}' is exiting from room '{}'", userName, roomName);
        exitFromRoom(i, userName);
        activeUsers[i] = false;
        sleep(WAIT_TIME);
        verify(activeUsers);
        log.info("User '{}' exited from room '{}'", userName, roomName);
      }
    }
  }

}
