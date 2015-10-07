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
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;

/**
 * Tests multiple (fake WebRTC) users' concurrently joining the same room.
 * Configured media sources should include at least one file (cfg key
 * {@link BaseFakeTest#KURENTO_TEST_FAKE_WR_FILENAMES}). The property
 * {@link BaseFakeTest#KURENTO_TEST_FAKE_WR_USERS} indicates how many fake users
 * to create (default is {@link #WR_USERNUM_VALUE}).
 * 
 * @see BaseFakeTest#KURENTO_TEST_FAKE_WR_USERS
 * @see #WR_USERNUM_VALUE
 * @see BaseFakeTest#KURENTO_TEST_FAKE_WR_FILENAMES
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class FakeWRUsersOneRoom extends BaseFakeTest {

	/**
	 * Total fake WR users in the test: {@value}.
	 */
	public final static int WR_USERNUM_VALUE = 9;

	public FakeWRUsersOneRoom(Logger log) {
		super(log);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.kurento.room.test.fake.BaseFakeTest#getDefaultFakeWRUsersNum()
	 */
	@Override
	protected int getDefaultFakeWRUsersNum() {
		return WR_USERNUM_VALUE;
	}

	// TODO configure accessible files' list in Jenkins job
    @Ignore
	@Test
	public void test() {
		Assert.assertTrue("This test requires at least one media source file",
				playerFakeWRUris.size() > 0);

		final CountDownLatch joinLatch = parallelJoinWR();

		await(joinLatch, JOIN_ROOM_TOTAL_TIMEOUT_IN_SECONDS, "joinRoom",
				execExceptions);

		log.info("\n-----------------\n" + "Join concluded in room '{}'"
				+ "\n-----------------\n", roomName);

		final CountDownLatch waitForLatch = parallelWaitActiveLiveWR();

		await(waitForLatch, ACTIVE_LIVE_TOTAL_TIMEOUT_IN_SECONDS,
				"waitForActiveLive", execExceptions);

		idlePeriod();

		final CountDownLatch leaveLatch = parallelLeaveWR();

		await(leaveLatch, LEAVE_ROOM_TOTAL_TIMEOUT_IN_SECONDS, "leaveRoom",
				execExceptions);

		log.info("\n-----------------\n" + "Leave concluded in room '{}'"
				+ "\n-----------------\n", roomName);

		failWithExceptions();
	}
}
