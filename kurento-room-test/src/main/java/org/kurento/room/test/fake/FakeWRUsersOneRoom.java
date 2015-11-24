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

import java.util.concurrent.CountDownLatch;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;

/**
 * Tests multiple (fake WebRTC) users' concurrently joining the same room.
 * Configured media sources should include at least one file (cfg key
 * {@link BaseFakeTest#KURENTO_TEST_FAKE_WR_FILENAMES}). The property
 * {@link BaseFakeTest#KURENTO_TEST_FAKE_WR_USERS}, if set, indicates how many
 * fake users to create (default is {@link #WR_USERNUM_VALUE}).
 * 
 * @see BaseFakeTest#KURENTO_TEST_FAKE_WR_USERS
 * @see #WR_USERNUM_VALUE
 * @see BaseFakeTest#KURENTO_TEST_FAKE_WR_FILENAMES
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class FakeWRUsersOneRoom extends BaseFakeTest {
	/**
	 * Total fake WR users in the test: {@value} .
	 */
	public final static int WR_USERNUM_VALUE = 4;

	private final static int ROOM_ACTIVITY_IN_MINUTES = 2;

	public FakeWRUsersOneRoom(Logger log) {
		super(log);
	}

	@Override
	protected int getDefaultFakeWRUsersNum() {
		return WR_USERNUM_VALUE;
	}

	@Test
	public void test() {
		Assert.assertTrue("This test requires at least one media source file",
				playerFakeWRUris.size() > 0);

		CountDownLatch joinLatch = parallelJoinWR();

		await(joinLatch, JOIN_ROOM_TOTAL_TIMEOUT_IN_SECONDS, "joinRoom",
				execExceptions);

		log.info("\n-----------------\n" + "Join concluded in room '{}'"
				+ "\n-----------------\n", roomName);

		CountDownLatch waitForLatch = parallelWaitActiveLiveWR();

		await(waitForLatch, ACTIVE_LIVE_TOTAL_TIMEOUT_IN_SECONDS,
				"waitForActiveLive", execExceptions);

		ROOM_ACTIVITY_IN_SECONDS = ROOM_ACTIVITY_IN_MINUTES * 60;
		idlePeriod();

		CountDownLatch leaveLatch = parallelLeaveWR();

		await(leaveLatch, LEAVE_ROOM_TOTAL_TIMEOUT_IN_SECONDS, "leaveRoom",
				execExceptions);

		log.info("\n-----------------\n" + "Leave concluded in room '{}'"
				+ "\n-----------------\n", roomName);

		failWithExceptions();
	}
}
