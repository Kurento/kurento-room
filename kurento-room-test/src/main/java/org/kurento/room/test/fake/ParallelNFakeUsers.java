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
package org.kurento.room.test.fake;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import org.kurento.room.test.RoomFunctionalFakeTest;
import org.kurento.test.browser.WebPage;

/**
 * Tests several fake WebRTC users' concurrently joining the same room.
 *
 * @author Radu Tom Vlad (rvlad@naevatec.com)
 * @since 6.2.1
 */
public abstract class ParallelNFakeUsers extends RoomFunctionalFakeTest<WebPage> {

  public final static int NUM_USERS = 0;

  public static String[] relativeUris = { "/video/filter/fiwarecut.webm",
    "/video/filter/fiwarecut_30.webm", "/video/filter/street.webm" };

  @Test
  public void test() {
    int fakeUsers = relativeUris.length;

    CountDownLatch joinLatch = parallelJoinFakeUsers(Arrays.asList(relativeUris), roomName,
        fakeKurentoClient);

    await(joinLatch, JOIN_ROOM_TOTAL_TIMEOUT_IN_SECONDS, "joinRoom", fakeUsers);

    log.info("\n-----------------\n" + "Join concluded in room '{}'" + "\n-----------------\n",
        roomName);

    CountDownLatch waitForLatch = parallelWaitActiveLive(roomName, fakeUsers);

    await(waitForLatch, ACTIVE_LIVE_TOTAL_TIMEOUT_IN_SECONDS, "waitForActiveLive", fakeUsers);

    idlePeriod();

    CountDownLatch leaveLatch = parallelLeaveFakeUsers(roomName, fakeUsers);

    await(leaveLatch, LEAVE_ROOM_TOTAL_TIMEOUT_IN_SECONDS, "leaveRoom", fakeUsers);

    log.info("\n-----------------\n" + "Leave concluded in room '{}'" + "\n-----------------\n",
        roomName);
  }

}
