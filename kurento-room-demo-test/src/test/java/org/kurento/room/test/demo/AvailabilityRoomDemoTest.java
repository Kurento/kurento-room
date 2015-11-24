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

package org.kurento.room.test.demo;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kurento.room.test.RoomTest;
import org.kurento.test.browser.WebPageType;
import org.openqa.selenium.WebDriver;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@DemoTestConfig
public class AvailabilityRoomDemoTest extends RoomTest {

	private static final int NUM_USERS = 1;

	@BeforeClass
	public static void setupBeforeClass() {
		webPageType = WebPageType.ROOT;
	}

	@Test
	public void test() throws Exception {

		final boolean[] activeUsers = new boolean[NUM_USERS];

		parallelUsers(NUM_USERS, new UserLifecycle() {
			@Override
			public void run(int numUser, int iteration, WebDriver browser)
					throws Exception {
				String userName = "user" + numUser;

				joinToRoom(browser, userName, roomName);
				activeUsers[numUser] = true;
				verify(browsers, activeUsers);
				log.info("User '{}' joined to room '{}'", userName, roomName);

				long start = System.currentTimeMillis();
				waitForStream(userName, browser, "native-video-" + userName
						+ "_webcam");
				long duration = System.currentTimeMillis() - start;
				log.info(
						"Video received in browser of user {} for user '{}' in {} millis",
						userName, userName, duration);

				log.info("User '{}' exiting from room '{}'", userName, roomName);
				exitFromRoom(userName, browser);
				activeUsers[numUser] = false;
				verify(browsers, activeUsers);
				log.info("User '{}' exited from room '{}'", userName, roomName);

				log.info("User '{}' close browser", userName);
			}
		});
	}
}
