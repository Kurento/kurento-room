/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
package org.kurento.room.performance.test;

import static org.kurento.commons.PropertiesManager.getProperty;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.kurento.room.demo.KurentoRoomServerApp;
import org.kurento.test.base.PerformanceTest;
import org.kurento.test.client.Browser;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.BrowserRunner;
import org.kurento.test.client.Client;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Room demo integration test.
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = KurentoRoomServerApp.class)
@WebAppConfiguration
@IntegrationTest("server.port:"
		+ KurentoServicesTestHelper.APP_HTTP_PORT_DEFAULT)
public class RoomPerformanceTest extends PerformanceTest {

	// Number of nodes
	private static final String NUM_NODES_PROPERTY = "perf.room.numnodes";
	private static final int NUM_NODES_DEFAULT = 1;

	// Browser per node
	private static final String NUM_BROWSERS_PROPERTY = "perf.room.numbrowsers";
	private static final int NUM_BROWSERS_DEFAULT = 2;

	// Client rate in milliseconds
	private static final String CLIENT_RATE_PROPERTY = "perf.room.clientrate";
	private static final int CLIENT_RATE_DEFAULT = 1000;

	// Hold time in milliseconds
	private static final String HOLD_TIME_PROPERTY = "perf.room.holdtime";
	private static final int HOLD_TIME_DEFAULT = 10000;

	private static final String ROOM_NAME = "room";

	private int playTime;

	public RoomPerformanceTest() {

		int numNodes = getProperty(NUM_NODES_PROPERTY, NUM_NODES_DEFAULT);

		int numBrowsers = getProperty(NUM_BROWSERS_PROPERTY,
				NUM_BROWSERS_DEFAULT);

		setNumBrowsersPerNode(numBrowsers);

		setBrowserCreationRate(getProperty(CLIENT_RATE_PROPERTY,
				CLIENT_RATE_DEFAULT));

		setNodes(getRandomNodes(numNodes, Browser.CHROME, getPathTestFiles()
				+ "/video/15sec/rgbHD.y4m", null, numBrowsers));

		playTime = getAllBrowsersStartedTime()
				+ getProperty(HOLD_TIME_PROPERTY, HOLD_TIME_DEFAULT);
	}

	protected void joinToRoom(BrowserClient browser, String userName,
			String roomName) {

		WebDriver driver = browser.getWebDriver();

		driver.findElement(By.id("name")).sendKeys(userName);
		driver.findElement(By.id("roomName")).clear();
		driver.findElement(By.id("roomName")).sendKeys(roomName);
		((JavascriptExecutor) driver).executeScript("register()");

		log.info("User '" + userName + "' joined to room '" + roomName + "'");
	}

	protected void exitFromRoom(BrowserClient browser) {
		try {
			browser.getWebDriver().findElement(By.id("button-leave")).click();
		} catch (ElementNotVisibleException e) {
			log.warn("Button leave is not visible. Session can't be closed");
		}
	}

	@Test
	public void test() throws Exception {
		parallelBrowsers(new BrowserRunner() {
			public void run(BrowserClient browser, int num, String name)
					throws Exception {

				final String userName = "user" + num;

				joinToRoom(browser, userName, ROOM_NAME);

				log.info("User '{}' joined to room '{}'", userName, ROOM_NAME);

				Thread.sleep(playTime);

				log.info("User '{}' exiting from room '{}'", userName,
						ROOM_NAME);
				exitFromRoom(browser);
				log.info("User '{}' exited from room '{}'", userName, ROOM_NAME);
			}
		}, Client.ROOM);
	}
}
