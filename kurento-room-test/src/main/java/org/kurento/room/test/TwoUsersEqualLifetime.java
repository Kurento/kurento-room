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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.Test;
import org.openqa.selenium.WebDriver;

/**
 * Room demo integration test (basic version).
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 5.0.0
 */
public class TwoUsersEqualLifetime extends RoomTest {

	private static final int PLAY_TIME = 5; // seconds

	private static final String USER1_NAME = "user1";
	private static final String USER2_NAME = "user2";

	private WebDriver user1Browser;
	private WebDriver user2Browser;

	@Test
	public void twoUsersRoomTest() throws InterruptedException,
			ExecutionException, TimeoutException {

		browsers = createBrowsers(2);
		user1Browser = browsers.get(0);
		user2Browser = browsers.get(1);

		joinToRoom(user1Browser, USER1_NAME, roomName);
		joinToRoom(user2Browser, USER2_NAME, roomName);

		waitForStream(USER1_NAME, user1Browser, "native-video-" + USER1_NAME
				+ "_webcam");
		waitForStream(USER2_NAME, user2Browser, "native-video-" + USER2_NAME
				+ "_webcam");

		waitForStream(USER1_NAME, user1Browser, "native-video-" + USER2_NAME
				+ "_webcam");
		waitForStream(USER2_NAME, user1Browser, "native-video-" + USER1_NAME
				+ "_webcam");

		// Guard time to see application in action
		Thread.sleep(PLAY_TIME * 1000);

		// Stop application by caller
		exitFromRoom(USER1_NAME, user1Browser);
		exitFromRoom(USER2_NAME, user2Browser);
	}
}
