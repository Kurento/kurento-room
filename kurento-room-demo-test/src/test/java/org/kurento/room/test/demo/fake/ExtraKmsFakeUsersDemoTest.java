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

import java.lang.invoke.MethodHandles;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;
import org.kurento.room.test.fake.ExtraKmsFakeUsers;
import org.kurento.test.browser.WebPageType;

/**
 * @see ExtraKmsFakeUsers
 * @author Radu Tom Vlad (rvlad@naevatec.com)
 */
public class ExtraKmsFakeUsersDemoTest extends ExtraKmsFakeUsers {

  @Override
  public void setupBrowserTest() throws InterruptedException {
    webPageType = WebPageType.ROOT;
    super.setupBrowserTest();
  }

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return localChromes(MethodHandles.lookup().lookupClass().getSimpleName(), NUM_USERS,
        WebPageType.ROOT);
  }

}
