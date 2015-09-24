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
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;
import org.kurento.room.test.fake.util.FakeSession;
import org.slf4j.Logger;

/**
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class ParallelUsersOneRoomLifetime extends BaseFakeTest {

	private static final long JOIN_ROOM_TOTAL_TIMEOUT_IN_SECONDS = 30;
	private static final long ACTIVE_LIVE_TOTAL_TIMEOUT_IN_SECONDS = 180;
	private static final long LEAVE_ROOM_TOTAL_TIMEOUT_IN_SECONDS = 10;

	private static final int USERS = 10;
	private static final String USER_PREFIX = "user";

	public ParallelUsersOneRoomLifetime(Logger log) {
		super(log);
	}

	//TODO make it testable
	@Ignore
	@Test
	public void test() {
		final CountDownLatch joinLatch = new CountDownLatch(USERS);
		Map<String, Exception> exceptions = new HashMap<String, Exception>();
		parallelTasks(USERS, USER_PREFIX, "parallelJoinRoom", exceptions,
				new Task() {
					@Override
					public void exec(int numTask) throws Exception {
						try {
							FakeSession s = createSession(roomName);
							s.newParticipant(USER_PREFIX + numTask, true);
						} finally {
							joinLatch.countDown();
						}
					}
				});

		await(joinLatch, JOIN_ROOM_TOTAL_TIMEOUT_IN_SECONDS, "joinRoom",
				exceptions);

		log.info("\n-----------------\n" + "Join concluded in room '{}'"
				+ "\n-----------------\n", roomName);

		final CountDownLatch waitForLatch = new CountDownLatch(USERS);
		parallelTasks(USERS, USER_PREFIX, "parallelWaitForActiveLive",
				exceptions, new Task() {
					@Override
					public void exec(int numTask) throws Exception {
						getSession(roomName).getParticipant(
								USER_PREFIX + numTask).waitForActiveLive(
								waitForLatch);
					}
				});

		await(waitForLatch, ACTIVE_LIVE_TOTAL_TIMEOUT_IN_SECONDS,
				"waitForActiveLive", exceptions);

		log.info("\n-----------------\n"
				+ "Wait for active live concluded in room '{}'"
				+ "\n-----------------\n" + "Waiting 30 seconds", roomName);

		try {
			Thread.sleep(30 * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		log.info("Leaving room '{}'", roomName);

		final CountDownLatch leaveLatch = new CountDownLatch(USERS);
		parallelTasks(USERS, USER_PREFIX, "parallelLeaveRoom", exceptions,
				new Task() {
					@Override
					public void exec(int numTask) throws Exception {
						try {
							getSession(roomName).getParticipant(
									USER_PREFIX + numTask).leaveRoom();
						} finally {
							leaveLatch.countDown();
						}
					}
				});

		await(leaveLatch, LEAVE_ROOM_TOTAL_TIMEOUT_IN_SECONDS, "leaveRoom",
				exceptions);

		log.info("\n-----------------\n" + "Leave concluded in room '{}'"
				+ "\n-----------------\n", roomName);

		failWithExceptions(exceptions);
	}

	private void await(CountDownLatch waitLatch, long actionTimeoutInSeconds,
			String action, Map<String, Exception> awaitExceptions) {
		try {
			if (!waitLatch.await(actionTimeoutInSeconds, TimeUnit.SECONDS))
				awaitExceptions.put(action, new Exception(
						"Timeout waiting for '" + action + "' of " + USERS
								+ " tasks (max " + actionTimeoutInSeconds
								+ "s)"));
			else
				log.debug("Finished waiting for {}", action);
		} catch (InterruptedException e) {
			log.warn("Interrupted when waiting for {} of {} tasks (max {}s)",
					action, USERS, actionTimeoutInSeconds, e);
		}
	}
}
