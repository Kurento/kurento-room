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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;

/**
 * Tests multiple fake WebRTC and Selenium (Chrome) users concurrently joining
 * the same room. Configured media sources for the fake participants should
 * include at least one file (cfg key
 * {@link BaseFakeTest#KURENTO_TEST_FAKE_WR_FILENAMES}). There will be as many
 * Chrome participants as possible (max between sizes of
 * {@link BaseFakeTest#KURENTO_TEST_CHROME_FILENAMES_Y4M} and
 * {@link BaseFakeTest#KURENTO_TEST_CHROME_FILENAMES_WAV}). If no chrome src
 * filenames are configured, a fixed number of Chrome browser will be started
 * using the "fake ui for media stream" option.
 * 
 * @see BaseFakeTest#KURENTO_TEST_FAKE_WR_FILENAMES
 * @see BaseFakeTest#KURENTO_TEST_CHROME_FILENAMES_WAV
 * @see BaseFakeTest#KURENTO_TEST_CHROME_FILENAMES_Y4M
 * @see #WR_USERNUM_VALUE
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class ChromeAndFakeWRUsersOneRoom extends BaseFakeTest {

	/**
	 * Total fake WR users in the test: {@value} .
	 */
	public final static int WR_USERNUM_VALUE = 1;

	private final static int CHROME_SPINNER_USERS = 1;

	public ChromeAndFakeWRUsersOneRoom(Logger log) {
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

		final CountDownLatch joinLatch = parallelJoinWR();

		if (!execExceptions.isEmpty())
			failWithExceptions();

		// FIXME test fails in Jenkins when the video src is not y4m
		if (chromeSrcFiles.isEmpty())
			joinChromeSpinner(CHROME_SPINNER_USERS);
		else
			joinChrome();

		await(joinLatch, JOIN_ROOM_TOTAL_TIMEOUT_IN_SECONDS, "joinRoom",
				execExceptions);

		log.info("\n-----------------\n" + "Join concluded in room '{}'"
				+ "\n-----------------\n", roomName);

		final CountDownLatch waitForLatch = parallelWaitActiveLiveWR();

		await(waitForLatch, ACTIVE_LIVE_TOTAL_TIMEOUT_IN_SECONDS,
				"waitForActiveLive", execExceptions);

		boolean[] activeChromeUsers = new boolean[browsers.size()];
		for (int i = 0; i < activeChromeUsers.length; i++)
			activeChromeUsers[i] = true;
		Map<String, Boolean> activeFakeWrUsers = new HashMap<String, Boolean>();
		for (int i = 0; i < getDefaultFakeWRUsersNum(); i++)
			activeFakeWrUsers.put(FAKE_WR_USER_PREFIX + i, true);
		verify(browsers, activeChromeUsers, activeFakeWrUsers);

		idlePeriod();

		final CountDownLatch leaveLatch = parallelLeaveWR();

		await(leaveLatch, LEAVE_ROOM_TOTAL_TIMEOUT_IN_SECONDS, "leaveRoom",
				execExceptions);

		for (int i = 0; i < getDefaultFakeWRUsersNum(); i++)
			activeFakeWrUsers.put(FAKE_WR_USER_PREFIX + i, false);
		verify(browsers, activeChromeUsers, activeFakeWrUsers);

		leaveChrome();

		log.info("\n-----------------\n" + "Leave concluded in room '{}'"
				+ "\n-----------------\n", roomName);

		failWithExceptions();
	}
}
