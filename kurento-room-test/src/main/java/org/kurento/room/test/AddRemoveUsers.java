package org.kurento.room.test;

/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.openqa.selenium.WebDriver;

/**
 * Room demo integration test (basic version).
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 5.0.0
 */
public class AddRemoveUsers extends RoomTest {

	private static final int PLAY_TIME = 5; // seconds

	private static final int NUM_USERS = 4;

	protected static final int ITERATIONS = 2;

	@Test
	public void test() throws Exception {

		final boolean[] activeUsers = new boolean[NUM_USERS];

		final CountDownLatch[] joinCdl = createCdl(ITERATIONS, NUM_USERS);
		final CountDownLatch[] leaveCdl = createCdl(ITERATIONS, NUM_USERS);

		iterParallelUsers(NUM_USERS, ITERATIONS, new UserLifecycle() {
			@Override
			public void run(int numUser, int iteration, final WebDriver browser)
					throws InterruptedException, ExecutionException {

				final String userName = "user" + numUser;

				log.info("User '{}' is joining room '{}'", userName, roomName);
				synchronized (browsersLock) {
					joinToRoom(browser, userName, roomName);
					activeUsers[numUser] = true;
					verify(browsers, activeUsers);
					joinCdl[iteration].countDown();
				}
				log.info("User '{}' joined room '{}'", userName, roomName);

				joinCdl[iteration].await(PLAY_TIME * 5000L,
						TimeUnit.MILLISECONDS);
				sleep(1000);

				log.info("User '{}' is exiting from room '{}'", userName,
						roomName);
				synchronized (browsersLock) {
					exitFromRoom(userName, browser);
					activeUsers[numUser] = false;
					verify(browsers, activeUsers);
					leaveCdl[iteration].countDown();
				}
				log.info("User '{}' exited from room '{}'", userName, roomName);
				leaveCdl[iteration].await(PLAY_TIME * 5000L,
						TimeUnit.MILLISECONDS);
			}
		});
	}

}
