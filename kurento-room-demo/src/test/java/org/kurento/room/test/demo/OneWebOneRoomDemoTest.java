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
package org.kurento.room.test.demo;

import java.lang.invoke.MethodHandles;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;
import org.kurento.room.test.browser.OneWebOneRoom;
import org.kurento.test.browser.WebPageType;

/**
 * @see OneWebOneRoom
 * @author Radu Tom Vlad (rvlad@naevatec.com)
 * @since 6.3.2
 */
public class OneWebOneRoomDemoTest extends OneWebOneRoom {

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return localChromes(MethodHandles.lookup().lookupClass().getSimpleName(), WebPageType.ROOT,
        USERS);
  }

  @Override
  public void setupBrowserTest() throws InterruptedException {
    webPageType = WebPageType.ROOT;
    super.setupBrowserTest();
  }

}
