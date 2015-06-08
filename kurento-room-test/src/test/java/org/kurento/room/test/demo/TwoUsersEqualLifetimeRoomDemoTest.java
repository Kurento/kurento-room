/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

package org.kurento.room.test.demo;

import org.junit.BeforeClass;
import org.kurento.room.test.TwoUsersEqualLifetimeRoomBasicTest;

/**
 * Room demo integration test (demo version).
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class TwoUsersEqualLifetimeRoomDemoTest extends
		TwoUsersEqualLifetimeRoomBasicTest {

	@BeforeClass
	public static void setupBeforeClass() {
		APP_URL = "http://127.0.0.1:8080";
	}
	
}
