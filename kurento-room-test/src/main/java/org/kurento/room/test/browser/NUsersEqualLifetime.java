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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
public class NUsersEqualLifetime extends RoomFunctionalBrowserTest<WebPage> {

  public static final int NUM_USERS = 4;

  @Test
  public void test() throws Exception {
    ITERATIONS = 1;

    final boolean[] activeUsers = new boolean[NUM_USERS];

    final CountDownLatch[] joinCdl = createCdl(ITERATIONS, NUM_USERS);
    final CountDownLatch[] publishCdl = createCdl(ITERATIONS, NUM_USERS * NUM_USERS);
    final CountDownLatch[] leaveCdl = createCdl(ITERATIONS, NUM_USERS);

    iterParallelUsers(NUM_USERS, ITERATIONS, new UserLifecycle() {

      @Override
      public void run(final int numUser, final int iteration) throws Exception {
        final String userName = getBrowserKey(numUser);
        log.info("User '{}' is joining room '{}'", userName, roomName);
        synchronized (browsersLock) {
          joinToRoom(numUser, userName, roomName);
          activeUsers[numUser] = true;
          verify(activeUsers);
          joinCdl[iteration].countDown();
        }
        log.info("User '{}' joined room '{}'", userName, roomName);

        joinCdl[iteration].await(PLAY_TIME * 5000L, TimeUnit.MILLISECONDS);

        final long start = System.currentTimeMillis();

        parallelTasks(NUM_USERS, USER_BROWSER_PREFIX, "parallelWaitForStream", new Task() {
          @Override
          public void exec(int numTask) throws Exception {
            String videoUserName = getBrowserKey(numTask);
            synchronized (browsersLock) {
              waitForStream(numUser, userName, numTask);
            }
            long duration = System.currentTimeMillis() - start;
            log.info("Video received in browser of user '{}' for user '{}' in {} millis", userName,
                videoUserName, duration);
            publishCdl[iteration].countDown();
          }
        });
        publishCdl[iteration].await(PLAY_TIME * 5000L, TimeUnit.MILLISECONDS);

        sleep(PLAY_TIME * 1000);

        log.info("User '{}' is exiting from room '{}'", userName, roomName);
        synchronized (browsersLock) {
          exitFromRoom(numUser, userName);
          activeUsers[numUser] = false;
          verify(activeUsers);
          leaveCdl[iteration].countDown();
        }
        log.info("User '{}' exited from room '{}'", userName, roomName);
        leaveCdl[iteration].await(PLAY_TIME * 5000L, TimeUnit.MILLISECONDS);
      }
    });

  }

}
