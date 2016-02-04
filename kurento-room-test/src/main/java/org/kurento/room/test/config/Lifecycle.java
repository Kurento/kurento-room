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
 * A test user's lifecycle.
 *
 * @author Radu Tom Vlad (rvlad@naevatec.com)
 * @since 6.2.1
 */
public interface Lifecycle {
  public enum Type {
    WEB, FAKE;
  }

  /**
   * The index or position of this user amongst users of its type.
   *
   * @return the index
   */
  public int getUserIndex();

  /**
   * A unique key generated using the user index prefixed by the user's type and the room it belongs
   * to.
   *
   * @return the key
   */
  public String getUserKey();

  /**
   * The username generated using the user index prefixed by the user's type.
   *
   * @return the username
   */
  public String getUserName();

  /**
   * Indicates to which group belongs this user's lifecycle.
   *
   * @return the type
   */
  public Type getType();

  /**
   * @return the room name that the user connects to
   */
  public String getRoom();
}