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

import org.junit.Test;

/**
 * Room demo integration test (basic version).
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 5.0.0
 */
public class SeqNUsersEqualLifetimeRoomBasicTest extends RoomTestBase {

	private static final int PLAY_TIME = 5; // seconds

	private static final String USER1_NAME = "user1";
	private static final String USER2_NAME = "user2";

	@Test
	public void twoUsersRoomTest() throws InterruptedException,
			ExecutionException {

		browsers = createBrowsers(2);

		joinToRoom(browsers.get(0), USER1_NAME, roomName);
		joinToRoom(browsers.get(1), USER2_NAME, roomName);

		waitForStream(browsers.get(0), "native-video-" + USER2_NAME + "_webcam");
		log.debug("Received media from " + USER2_NAME + " in " + USER1_NAME);

		waitForStream(browsers.get(1), "native-video-" + USER1_NAME + "_webcam");
		log.debug("Received media from " + USER1_NAME + " in " + USER2_NAME);

		// Guard time to see application in action
		Thread.sleep(PLAY_TIME * 1000);

		// Stop application by caller
		exitFromRoom(browsers.get(0));
		exitFromRoom(browsers.get(1));
	}
}
