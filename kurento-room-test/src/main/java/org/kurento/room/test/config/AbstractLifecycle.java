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
 * Default implementation of a test {@link Lifecycle}.
 *
 * @author Radu Tom Vlad (rvlad@naevatec.com)
 * @since 6.3.2
 */
public abstract class AbstractLifecycle implements Lifecycle {
  private int userIndex;
  private Type type;
  private String room;

  public AbstractLifecycle(int userIndex, String room, Type type) {
    this.userIndex = userIndex;
    this.room = room;
    this.type = type;
  }

  @Override
  public int getUserIndex() {
    return userIndex;
  }

  @Override
  public String getRoom() {
    return room;
  }

  @Override
  public Type getType() {
    return type;
  }
}