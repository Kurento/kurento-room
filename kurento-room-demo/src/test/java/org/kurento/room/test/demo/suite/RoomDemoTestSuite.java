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

package org.kurento.room.test.demo.suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.kurento.room.test.demo.AddRemoveUsersNoSinkVerifyRoomDemoTest;
import org.kurento.room.test.demo.AddRemoveUsersRoomDemoTest;
import org.kurento.room.test.demo.NUsersEqualLifetimeRoomDemoTest;
import org.kurento.room.test.demo.SeqAddRemoveUserRoomDemoTest;
import org.kurento.room.test.demo.SeqNUsersEqualLifetimeRoomDemoTest;
import org.kurento.room.test.demo.TwoUsersEqualLifetimeRoomDemoTest;
import org.kurento.room.test.demo.UnpublishMediaRoomDemoTest;
import org.kurento.room.test.demo.UnsubscribeFromMediaRoomDemoTest;

@SuiteClasses({AddRemoveUsersNoSinkVerifyRoomDemoTest.class, AddRemoveUsersRoomDemoTest.class,
	NUsersEqualLifetimeRoomDemoTest.class, SeqAddRemoveUserRoomDemoTest.class, SeqNUsersEqualLifetimeRoomDemoTest.class,
	TwoUsersEqualLifetimeRoomDemoTest.class, UnpublishMediaRoomDemoTest.class, UnsubscribeFromMediaRoomDemoTest.class})
@RunWith(Suite.class)
public class RoomDemoTestSuite {
}
