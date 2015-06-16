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

package org.kurento.room.test.suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.kurento.room.test.AddRemoveUsersNoSinkVerifyRoomBasicTest;
import org.kurento.room.test.AddRemoveUsersRoomBasicTest;
import org.kurento.room.test.NUsersEqualLifetimeRoomBasicTest;
import org.kurento.room.test.SeqAddRemoveUserRoomBasicTest;
import org.kurento.room.test.SeqNUsersEqualLifetimeRoomBasicTest;
import org.kurento.room.test.TwoUsersEqualLifetimeRoomBasicTest;

@SuiteClasses({AddRemoveUsersNoSinkVerifyRoomBasicTest.class, AddRemoveUsersRoomBasicTest.class,
	NUsersEqualLifetimeRoomBasicTest.class, SeqAddRemoveUserRoomBasicTest.class, SeqNUsersEqualLifetimeRoomBasicTest.class,
	TwoUsersEqualLifetimeRoomBasicTest.class})
@RunWith(Suite.class)
public class RoomBasicTestSuite {
}
