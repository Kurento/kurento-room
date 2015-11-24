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
package org.kurento.room.test.demo.fake;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.kurento.room.test.demo.DemoTestConfig;
import org.kurento.room.test.fake.FakeWRUsersExtraKMSOneRoom;
import org.kurento.test.browser.WebPageType;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @see FakeWRUsersExtraKMSOneRoom
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DemoTestConfig
public class FakeWRUsersExtraKMSOneRoomDemoTest extends
		FakeWRUsersExtraKMSOneRoom {

	public FakeWRUsersExtraKMSOneRoomDemoTest() {
		super(LoggerFactory.getLogger(FakeWRUsersExtraKMSOneRoomDemoTest.class));
	}

	@BeforeClass
	public static void setupBeforeClass() {
		webPageType = WebPageType.ROOT;
	}

}
