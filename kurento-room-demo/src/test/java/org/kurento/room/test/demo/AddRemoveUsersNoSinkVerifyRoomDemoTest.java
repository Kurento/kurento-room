package org.kurento.room.test.demo;

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

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.kurento.room.test.AddRemoveUsersNoSinkVerify;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Room demo integration test (demo version).
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DemoTestConfig
public class AddRemoveUsersNoSinkVerifyRoomDemoTest extends
		AddRemoveUsersNoSinkVerify {

	@BeforeClass
	public static void setupBeforeClass() {
		appUrl = DEMO_ROOM_APP_URL;
	}
}
