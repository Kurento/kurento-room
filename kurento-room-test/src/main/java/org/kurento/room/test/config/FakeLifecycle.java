/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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
package org.kurento.room.test.config;


/**
 * Lifecycle of fake session users.
 *
 * @author Radu Tom Vlad (rvlad@naevatec.com)
 * @since 6.3.2
 */
public class FakeLifecycle extends AbstractLifecycle {

  private String mediaUri;

  public FakeLifecycle(int key, String room, String mediaUri) {
    super(key, room, Type.FAKE);
    this.mediaUri = mediaUri;
  }

  public String getMediaUri() {
    return mediaUri;
  }

  @Override
  public String getUserKey() {
    return RoomTestUtils.getFakeKey(getUserIndex(), getRoom());
  }

  @Override
  public String getUserName() {
    return RoomTestUtils.getFakeUserName(getUserIndex());
  }
}