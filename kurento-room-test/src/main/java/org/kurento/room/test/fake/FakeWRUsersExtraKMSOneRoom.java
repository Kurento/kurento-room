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
import org.kurento.client.KurentoClient;
import org.kurento.client.KurentoConnectionListener;
import org.kurento.commons.PropertiesManager;
import org.slf4j.Logger;

/**
 * Tests multiple (fake WebRTC) users' concurrently joining the same room.
 * Configured media sources should include at least one file (cfg key
 * {@link BaseFakeTest#KURENTO_TEST_FAKE_WR_FILENAMES}). The property
 * {@link BaseFakeTest#KURENTO_TEST_FAKE_WR_USERS}, if set, indicates how many
 * fake users to create (default is {@link #WR_USERNUM_VALUE}).
 * 
 * Adds extra users that will use the configured extra KMS for WebRTC support.
 * (cfg key is {@link BaseFakeTest#KURENTO_TEST_FAKE_KMS_URI} {@code + ".extra"}
 * )
 * 
 * @see BaseFakeTest#KURENTO_TEST_FAKE_WR_USERS
 * @see #WR_USERNUM_VALUE
 * @see BaseFakeTest#KURENTO_TEST_FAKE_WR_FILENAMES
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class FakeWRUsersExtraKMSOneRoom extends BaseFakeTest {
	/**
	 * Total fake WR users in the test: {@value} .
	 */
	public final static int WR_USERNUM_VALUE = 2;

	private final static int ROOM_ACTIVITY_IN_MINUTES = 2;

	private static final String FAKE_EXTRA_USER_PREFIX = FAKE_WR_USER_PREFIX
			+ "Extra";

	private static final int FAKE_EXTRA_USERS = 2;

	private static String testExtraFakeKmsWsUri;

	private KurentoClient testExtraFakeKurento;

	public FakeWRUsersExtraKMSOneRoom(Logger log) {
		super(log);
	}

	@Override
	public void setup() {
		super.setup();
		testExtraFakeKmsWsUri = PropertiesManager.getProperty(
				KURENTO_TEST_FAKE_KMS_URI + ".extra", testFakeKmsWsUri);

		log.debug("Extra Fake KMS URI: {}", testExtraFakeKmsWsUri);
	}

	@Override
	public void tearDown() {
		super.tearDown();
		if (testExtraFakeKurento != null) {
			testExtraFakeKurento.destroy();
			testExtraFakeKurento = null;
		}
	}

	@Override
	protected int getDefaultFakeWRUsersNum() {
		return WR_USERNUM_VALUE;
	}

	protected synchronized KurentoClient getTestExtraFakeKurento() {
		if (testExtraFakeKurento == null) {
			testExtraFakeKurento = KurentoClient.create(testExtraFakeKmsWsUri,
					new KurentoConnectionListener() {
						@Override
						public void connected() {
						}

						@Override
						public void connectionFailed() {
						}

						@Override
						public void disconnected() {
							testExtraFakeKurento = null;
						}

						@Override
						public void reconnected(boolean sameServer) {
						}
					});
		}

		return testExtraFakeKurento;
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

		joinLatch = parallelJoinWR(roomName, FAKE_EXTRA_USERS,
				FAKE_EXTRA_USER_PREFIX, getTestExtraFakeKurento());

		await(joinLatch, JOIN_ROOM_TOTAL_TIMEOUT_IN_SECONDS, "extraJoinRoom",
				execExceptions);

		log.info("\n-----------------\n" + "Extra Join concluded in room '{}'"
				+ "\n-----------------\n", roomName);

		CountDownLatch waitForLatch = parallelWaitActiveLiveWR();

		await(waitForLatch, ACTIVE_LIVE_TOTAL_TIMEOUT_IN_SECONDS,
				"waitForActiveLive", execExceptions);

		waitForLatch = parallelWaitActiveLiveWR(roomName, FAKE_EXTRA_USERS,
				FAKE_EXTRA_USER_PREFIX);

		await(waitForLatch, ACTIVE_LIVE_TOTAL_TIMEOUT_IN_SECONDS,
				"extraWaitForActiveLive", execExceptions);

		ROOM_ACTIVITY_IN_SECONDS = ROOM_ACTIVITY_IN_MINUTES * 60;
		idlePeriod();

		CountDownLatch leaveLatch = parallelLeaveWR();

		await(leaveLatch, LEAVE_ROOM_TOTAL_TIMEOUT_IN_SECONDS, "leaveRoom",
				execExceptions);

		leaveLatch = parallelLeaveWR(roomName, FAKE_EXTRA_USERS,
				FAKE_EXTRA_USER_PREFIX);

		await(leaveLatch, LEAVE_ROOM_TOTAL_TIMEOUT_IN_SECONDS,
				"extraLeaveRoom", execExceptions);

		log.info("\n-----------------\n" + "Leave concluded in room '{}'"
				+ "\n-----------------\n", roomName);

		failWithExceptions();
	}
}
