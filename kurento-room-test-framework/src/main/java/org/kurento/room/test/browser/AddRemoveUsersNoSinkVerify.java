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
 * Room integration test (basic version).
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Radu Tom Vlad (rvlad@naevatec.com)
 * @since 5.0.0
 */
public class AddRemoveUsersNoSinkVerify extends RoomFunctionalBrowserTest<WebPage> {

  public static final int NUM_USERS = 4;

  @Test
  public void test() throws Exception {
    iterParallelUsers(NUM_USERS, ITERATIONS, new UserLifecycle() {
      @Override
      public void run(int numUser, int iteration) throws Exception {
        String userName = getBrowserKey(numUser);

        log.info("User '{}' is joining room '{}'", userName, roomName);
        joinToRoom(numUser, userName, roomName);
        log.info("User '{}' joined room '{}'", userName, roomName);

        sleep(PLAY_TIME * 1000);

        log.info("User '{}' is exiting from room '{}'", userName, roomName);
        exitFromRoom(numUser, userName);
        log.info("User '{}' exited from room '{}'", userName, roomName);
      }
    });
  }

}
