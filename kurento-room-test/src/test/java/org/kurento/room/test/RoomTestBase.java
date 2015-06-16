package org.kurento.room.test;

/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

import static org.junit.Assert.fail;

import java.io.File;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.SystemUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.kurento.room.KurentoRoomServerApp;
import org.kurento.test.base.KurentoTest;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.google.common.base.Function;

/**
 * Base class for integration testing of Room API.
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 5.0.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = KurentoRoomServerApp.class)
@WebAppConfiguration
@IntegrationTest
public class RoomTestBase extends KurentoTest {

	public interface UserLifecycle {
		public void run(int numUser, WebDriver browser) throws Exception;
	}

	protected Logger log = LoggerFactory.getLogger(this.getClass());

	protected static String APP_URL = "http://127.0.0.1:8080/room.html";

	protected static SecureRandom random;
	
	private static final String ROOM_NAME = "room";
	protected String roomName;
	static {
		random = new SecureRandom();
	}
	
	private static final int TEST_TIMEOUT = 20; // seconds

	private static final int MAX_WIDTH = 1200;

	private static final int BROWSER_WIDTH = 400;
	private static final int BROWSER_HEIGHT = 400;

	private static final int LEFT_BAR_WIDTH = 40;

	private static final int FIND_LATENCY = 100;

	protected List<WebDriver> browsers;
	final protected Object browsersLock = new Object();

	@Before
	public void setup() {
		super.setupKurentoTest();
		roomName = ROOM_NAME + random.nextInt(9999);
	}
	
	@After
	public void tearDown() {
		super.teardownKurentoTest();
		closeBrowsers();
	}

	protected WebDriver newWebDriver() {

		ChromeOptions options = new ChromeOptions();
		// This flag avoids a warning in Chrome. See:
		// https://code.google.com/p/chromedriver/issues/detail?id=799
		options.addArguments("--test-type");
		// This flag avoids granting camera/microphone
		options.addArguments("--use-fake-ui-for-media-stream");
		// This flag makes using a synthetic video (green with spinner) in
		// WebRTC instead of real media from camera/microphone
		options.addArguments("--use-fake-device-for-media-stream");

		// Path to chrome driver binary
		String chromedriver = null;
		if (SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_LINUX) {
			chromedriver = "chromedriver";
		} else if (SystemUtils.IS_OS_WINDOWS) {
			chromedriver = "chromedriver.exe";
		}
		System.setProperty("webdriver.chrome.driver", new File(
				"target/webdriver/" + chromedriver).getAbsolutePath());

		ChromeDriver chromeDriver = new ChromeDriver(options);

		return chromeDriver;
	}

	protected void exitFromRoom(WebDriver userBrowser) {
		try {
			userBrowser.findElement(By.id("buttonLeaveRoom")).click();
		} catch (ElementNotVisibleException e) {
			log.warn("Button leave is not visible. Session can't be closed");
		}
	}

	protected void joinToRoom(WebDriver userBrowser, String userName,
			String roomName) {

		userBrowser.get(APP_URL);

		userBrowser.findElement(By.id("name")).sendKeys(userName);
		userBrowser.findElement(By.id("roomName")).sendKeys(roomName);
		userBrowser.findElement(By.id("joinBtn")).submit();

		log.info("User '" + userName + "' joined to room '" + roomName + "'");
	}

	@SuppressWarnings("unused")
	private Object execFunc(WebDriver user, String javaScript) {
		return ((JavascriptExecutor) user).executeScript(javaScript);
	}

	protected void waitForStream(WebDriver driver, String videoTagId) {

		int i = 0;
		for (; i < TEST_TIMEOUT; i++) {
			WebElement video = findElement(driver, videoTagId);
			String srcAtt = video.getAttribute("src");
			if (srcAtt != null && srcAtt.startsWith("blob")) {
				break;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		if (i == TEST_TIMEOUT) {
			Assert.fail("Video tag '" + videoTagId
					+ "' is not playing media after " + TEST_TIMEOUT
					+ " seconds");
		}
	}

	protected void unpublish(WebDriver userBrowser) {
		try {
			userBrowser.findElement(By.id("buttonDisconnect")).click();
		} catch (ElementNotVisibleException e) {
			log.warn("Button 'buttonDisconnect' is not visible. Can't unpublish media.");
		}
	}

	protected void unsubscribe(WebDriver userBrowser, String clickableVideoTagId) {
		try {
			userBrowser.findElement(By.id(clickableVideoTagId)).click();
		} catch (ElementNotVisibleException e) {
			String msg =
					"Video tag "
							+ clickableVideoTagId
							+ " is not visible. Can't select video to unsubscribe from.";
			log.warn(msg);
			fail(msg);
		}
		try {
			userBrowser.findElement(By.id("buttonDisconnect")).click();
		} catch (ElementNotVisibleException e) {
			log.warn("Button 'buttonDisconnect' is not visible. Can't unsubscribe from media.");
		}
	}

	protected WebElement findElement(WebDriver browser, String id) {
		return findElement(null, browser, id);
	}

	protected void waitWhileElement(String label, WebDriver browser, String id)
			throws TimeoutException {

		int i = 0;
		int numIters = TEST_TIMEOUT * 1000 / FIND_LATENCY;
		for (; i < numIters; i++) {
			try {
				browser.findElement(By.id(id));
				log.debug("Found element with id=" + id + " in browser "
						+ label + ". Waiting until removed in " + FIND_LATENCY
						+ " millis with max of " + TEST_TIMEOUT + " seconds.");
			} catch (NoSuchElementException e) {
				return;
			}

			try {
				Thread.sleep(FIND_LATENCY);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		throw new TimeoutException("Element with id=" + id
				+ "' is present in page after " + TEST_TIMEOUT + " seconds");

	}

	protected WebElement findElement(String label, WebDriver browser, String id) {

		int i = 0;
		boolean shown = false;
		int numIters = TEST_TIMEOUT * 1000 / FIND_LATENCY;
		for (; i < numIters; i++) {
			try {
				return browser.findElement(By.id(id));
			} catch (NoSuchElementException e) {
				if (!shown) {
					log.debug("Not found element with id=" + id
							+ " in browser " + label + ". Retrying in "
							+ FIND_LATENCY + " millis until " + TEST_TIMEOUT
							+ " seconds.");
				}
			}

			try {
				Thread.sleep(FIND_LATENCY);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		throw new NoSuchElementException("Element with id=" + id
				+ "' not found after " + TEST_TIMEOUT + " seconds");

	}

	public void parallelUsers(int numUsers, final UserLifecycle user)
			throws Exception {

		browsers = createBrowsers(numUsers);

		ExecutorService threadPool = Executors.newFixedThreadPool(numUsers);
		ExecutorCompletionService<Void> exec =
				new ExecutorCompletionService<>(threadPool);
		List<Future<Void>> futures = new ArrayList<>();

		for (int i = 0; i < numUsers; i++) {
			final int numUser = i;
			final WebDriver browser = browsers.get(numUser);
			futures.add(exec.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					Thread.currentThread().setName("browser" + numUser);
					user.run(numUser, browser);
					return null;
				}
			}));
		}

		try {
			for (int i = 0; i < numUsers; i++) {
				try {
					Future<Void> f = exec.take();
					// doing it on tear down
					// int indexOf = futures.indexOf(f);
					// WebDriver webDriver = browsers.set(indexOf, null);
					// webDriver.close();
					// webDriver.quit();
					f.get();
				} catch (ExecutionException e) {
					e.printStackTrace();
					throw e;
				}
			}
		} finally {
			threadPool.shutdownNow();
			// closeBrowsers();
		}
	}

	protected List<WebDriver> createBrowsers(int numUsers)
			throws InterruptedException, ExecutionException {

		final List<WebDriver> browsers =
				Collections.synchronizedList(new ArrayList<WebDriver>());

		parallelTask(numUsers, new Function<Integer, Void>() {
			@Override
			public Void apply(Integer num) {
				WebDriver browser = newWebDriver();
				browsers.add(browser);
				return null;
			}
		});

		int row = 0;
		int col = 0;
		for (WebDriver browser : browsers) {

			browser.manage().window()
					.setSize(new Dimension(BROWSER_WIDTH, BROWSER_HEIGHT));
			browser.manage()
					.window()
					.setPosition(
							new Point(col * BROWSER_WIDTH + LEFT_BAR_WIDTH, row
									* BROWSER_HEIGHT));

			col++;

			if (col * BROWSER_WIDTH + LEFT_BAR_WIDTH > MAX_WIDTH) {
				col = 0;
				row++;
			}
		}
		return browsers;
	}

	protected void parallelTask(int num, final Function<Integer, Void> function)
			throws InterruptedException, ExecutionException {

		ExecutorService threadPool = Executors.newFixedThreadPool(num);
		ExecutorCompletionService<Void> exec =
				new ExecutorCompletionService<>(threadPool);

		for (int i = 0; i < num; i++) {
			final int current = i;
			exec.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					function.apply(current);
					return null;
				}
			});
		}

		try {
			for (int i = 0; i < num; i++) {
				try {
					exec.take().get();
				} catch (ExecutionException e) {
					e.printStackTrace();
					throw e;
				}
			}
		} finally {
			threadPool.shutdownNow();
		}
	}

	public void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void sleepRandom(long millis) {
		try {
			Thread.sleep((long) (Math.random() * millis));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	protected void closeBrowsers() {
		if (browsers != null && !browsers.isEmpty())
			for (WebDriver browser : browsers)
				if (browser != null)
					try {
						browser.close();
						browser.quit();
					} catch (Exception e) {
						log.warn("Error closing browser", e);
						fail("Unable to close browser: " + e.getMessage());
					}
	}

	protected void verify(List<WebDriver> browsers, boolean[] activeUsers) {

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < activeUsers.length; i++) {
			if (activeUsers[i]) {
				sb.append("user" + i + ",");
			}
		}
		log.debug("Checking active users: [{}]", sb);

		long startTime = System.nanoTime();

		for (int i = 0; i < activeUsers.length; i++) {

			if (activeUsers[i]) {
				WebDriver browser = browsers.get(i);

				for (int j = 0; j < activeUsers.length; j++) {

					String videoElementId = "video-" + getUserName(j);

					if (activeUsers[j]) {
						// log.debug("Finding element video-user" + j
						// + " in browser for user" + i);

						WebElement video =
								findElement(getUserName(i), browser,
										videoElementId);
						if (video == null) {
							fail("Video element for user" + j
									+ " is not found in browser from user" + i);
						}
					} else {
						// log.debug("Verifing element video-user" + j
						// + " is hide in browser for user" + i);
						try {
							waitWhileElement(getUserName(i), browser,
									videoElementId);
						} catch (TimeoutException e) {
							fail(e.getMessage());
						}
					}
				}
			}
		}

		long endTime = System.nanoTime();

		double duration = ((double) endTime - startTime) / 1_000_000;

		log.debug("Checked active users: [{}] in {} millis", sb, duration);
	}

	private String getUserName(int i) {
		return "user" + i + "_webcam";
	}

	protected CountDownLatch[] createCdl(int numLatches, int numUsers) {
		final CountDownLatch[] cdl = new CountDownLatch[numLatches];
		for (int i = 0; i < numLatches; i++) {
			cdl[i] = new CountDownLatch(numUsers);
		}
		return cdl;
	}

}
