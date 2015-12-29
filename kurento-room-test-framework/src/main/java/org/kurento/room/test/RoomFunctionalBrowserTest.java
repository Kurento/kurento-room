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
package org.kurento.room.test;

import org.junit.experimental.categories.Category;
import org.kurento.commons.testing.SystemFunctionalTests;
import org.kurento.test.browser.WebPage;

/**
 * Functional Kurento Room tests (using browser clients).
 *
 * @author Radu Tom Vlad (rvlad@naevatec.com)
 * @since 6.2.1
 */
@Category(SystemFunctionalTests.class)
public class RoomFunctionalBrowserTest<W extends WebPage> extends RoomClientBrowserTest<W> {
}
