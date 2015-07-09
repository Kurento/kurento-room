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
import io.github.bonigarcia.wdm.ChromeDriverManager;

import java.io.IOException;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.kurento.commons.ConfigFileFinder;
import org.kurento.commons.PropertiesManager;
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
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

/**
 * Base class for integration testing of Room API.
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 5.0.0
 */
public class RoomTest extends KurentoTest {
	protected Logger log = LoggerFactory.getLogger(this.getClass());

	public final static String CONFIG_TEST_FILENAME = "/kroomtest.conf.json";

	public interface UserLifecycle {
		public void run(int numUser, int iteration, WebDriver browser)
				throws Exception;
	}

	static {
		try {
			System.setProperty("configFilePath", ConfigFileFinder
					.getPathInClasspath(CONFIG_TEST_FILENAME).toString());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private static String serverPort = PropertiesManager.getProperty(
			"server.port", "8080");
	private static String serverAddress = PropertiesManager.getProperty(
			"server.address", "127.0.0.1");
	protected static String serverUriBase = "http://" + serverAddress + ":"
			+ serverPort;

	protected static final String BASIC_ROOM_APP_URL = serverUriBase
			+ "/room.html";
	protected static final String DEMO_ROOM_APP_URL = serverUriBase;

	protected static String appUrl;

	protected static SecureRandom random;

	private static final String ROOM_NAME = "room";
	protected String roomName;
	static {
		random = new SecureRandom();
	}

	private static final int DRIVER_INIT_THREAD_TIMEOUT = 30; // seconds
	private static final int DRIVER_INIT_THREAD_JOIN = 1; // seconds

	private static final int TEST_TIMEOUT = 20; // seconds

	private static final int MAX_WIDTH = 1200;

	private static final int BROWSER_WIDTH = 500;
	private static final int BROWSER_HEIGHT = 400;

	private static final int LEFT_BAR_WIDTH = 60;
	private static final int TOP_BAR_WIDTH = 30;

	private static final int FIND_LATENCY = 100;

	protected List<WebDriver> browsers;
	final protected Object browsersLock = new Object();
	private boolean browsersClosed;

	@BeforeClass
	public static void setupClass() {
		appUrl = BASIC_ROOM_APP_URL;
		// Chrome binary
		ChromeDriverManager.getInstance().setup();
	}

	@Before
	public void setup() {
		try {
			super.setupKurentoTest();
		} catch (InterruptedException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		roomName = ROOM_NAME + random.nextInt(9999);
	}

	@After
	public void tearDown() {
		super.teardownKurentoTest();
		closeBrowsers();
	}

	protected WebDriver newWebDriver() {
		ChromeOptions options = new ChromeOptions();

		// This flag avoids granting camera/microphone
		options.addArguments("--use-fake-ui-for-media-stream");

		// This flag makes using a synthetic video (green with spinner) in
		// WebRTC instead of real media from camera/microphone
		options.addArguments("--use-fake-device-for-media-stream");

		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(ChromeOptions.CAPABILITY, options);
		capabilities.setBrowserName(DesiredCapabilities.chrome()
				.getBrowserName());

		ExecutorService webExec = Executors.newFixedThreadPool(1);
		BrowserInit initThread = new BrowserInit(capabilities);
		webExec.execute(initThread);
		webExec.shutdown();
		try {
			if (!webExec.awaitTermination(DRIVER_INIT_THREAD_TIMEOUT,
					TimeUnit.SECONDS)) {
				log.warn(
						"Webdriver init thread timed-out after {}s, will be interrupted",
						DRIVER_INIT_THREAD_TIMEOUT);
				initThread.interrupt();
				initThread.join(DRIVER_INIT_THREAD_JOIN);
			}
		} catch (InterruptedException e) {
			log.error("Interrupted exception", e);
			fail(e.getMessage());
		}

		return initThread.getBrowser();
	}

	protected void exitFromRoom(String label, WebDriver userBrowser) {
		try {
			Actions actions = new Actions(userBrowser);
			actions.click(findElement(label, userBrowser, "buttonLeaveRoom"))
					.perform();
			log.debug("'buttonLeaveRoom' clicked on in {}", label);
		} catch (ElementNotVisibleException e) {
			log.warn("Button 'buttonLeaveRoom' is not visible. Session can't be closed");
		}
	}

	protected void joinToRoom(WebDriver userBrowser, String userName,
			String roomName) {
		userBrowser.get(appUrl);
		findElement(userName, userBrowser, "name").sendKeys(userName);
		findElement(userName, userBrowser, "roomName").sendKeys(roomName);
		findElement(userName, userBrowser, "joinBtn").submit();
	}

	@SuppressWarnings("unused")
	private Object execFunc(WebDriver user, String javaScript) {
		return ((JavascriptExecutor) user).executeScript(javaScript);
	}

	protected void waitForStream(String label, WebDriver driver,
			String videoTagId) {
		int i = 0;
		for (; i < TEST_TIMEOUT; i++) {
			WebElement video = findElement(label, driver, videoTagId);
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

	protected void waitWhileElement(String label, WebDriver browser, String id)
			throws TimeoutException {
		try {
			(new WebDriverWait(browser, TEST_TIMEOUT, FIND_LATENCY))
					.until(ExpectedConditions.invisibilityOfElementLocated(By
							.id(id)));
		} catch (org.openqa.selenium.TimeoutException e) {
			log.warn(
					"Timeout when waiting for element {} to disappear in browser {}",
					id, label, e);
			throw new TimeoutException("Element with id='" + id
					+ "' is present in page after " + TEST_TIMEOUT + " seconds");
		}
	}

	protected WebElement findElement(String label, WebDriver browser, String id) {
		try {
			return (new WebDriverWait(browser, TEST_TIMEOUT, FIND_LATENCY))
					.until(ExpectedConditions.presenceOfElementLocated(By
							.id(id)));
		} catch (org.openqa.selenium.TimeoutException e) {
			log.warn(
					"Timeout when waiting for element {} to exist in browser {}",
					id, label);
			try {
				WebElement elem = browser.findElement(By.id(id));
				log.info(
						"Additional findElement call was able to locate {} in browser {}",
						id, label);
				return elem;
			} catch (NoSuchElementException e1) {
				log.debug(
						"Additional findElement call couldn't locate {} in browser {} ({})",
						id, label, e1.getMessage());
				throw new NoSuchElementException("Element with id='" + id
						+ "' not found after " + TEST_TIMEOUT
						+ " seconds in browser " + label);
			}
		}
	}

	public void parallelUsers(int numUsers, final UserLifecycle user)
			throws InterruptedException, ExecutionException, TimeoutException {
		iterParallelUsers(numUsers, 1, user);
	}

	public void iterParallelUsers(int numUsers, int iterations,
			final UserLifecycle user) throws InterruptedException,
			ExecutionException, TimeoutException {

		int totalExecutions = iterations * numUsers;
		ExecutorService threadPool =
				Executors.newFixedThreadPool(totalExecutions);
		ExecutorCompletionService<Void> exec =
				new ExecutorCompletionService<>(threadPool);
		List<Future<Void>> futures = new ArrayList<>();

		try {
			for (int j = 0; j < iterations; j++) {
				final int it = j;
				log.info("it#{}: Starting execution of {} users", it, numUsers);
				browsers = createBrowsers(numUsers);
				log.debug("it#{}: Created {} WebDrivers", it, numUsers);
				for (int i = 0; i < numUsers; i++) {
					final int numUser = i;
					final WebDriver browser = browsers.get(numUser);
					futures.add(exec.submit(new Callable<Void>() {
						@Override
						public Void call() throws Exception {
							Thread.currentThread().setName(
									"it" + it + "|browser" + numUser);
							user.run(numUser, it, browser);
							return null;
						}
					}));
				}

				for (int i = 0; i < numUsers; i++) {
					try {
						exec.take().get();
					} catch (ExecutionException e) {
						log.error("Execution exception", e);
						throw e;
					}
				}
				closeBrowsers();
				log.debug("it#{}: Closed {} WebDrivers", it, numUsers);
				log.info("it#{}: Finished execution of {} users", it, numUsers);
			}
		} finally {
			threadPool.shutdownNow();
		}
	}

	protected List<WebDriver> createBrowsers(int numUsers)
			throws InterruptedException, ExecutionException, TimeoutException {

		final List<WebDriver> browsers =
				Collections.synchronizedList(new ArrayList<WebDriver>());

		parallelBrowserInit(numUsers, 0, browsers);

		if (browsers.size() < numUsers) {
			int required = numUsers - browsers.size();
			log.warn("Not enough browsers were created, will retry to "
					+ "recreate the missing ones: {}", required);
			parallelBrowserInit(required, browsers.size(), browsers);
		}

		if (browsers.size() < numUsers)
			fail("Unable to create the required number of browsers: "
					+ numUsers);

		int row = 0;
		int col = 0;
		for (WebDriver browser : browsers) {

			browser.manage().window()
					.setSize(new Dimension(BROWSER_WIDTH, BROWSER_HEIGHT));
			browser.manage()
					.window()
					.setPosition(
							new Point(col * BROWSER_WIDTH + LEFT_BAR_WIDTH, row
									* BROWSER_HEIGHT + TOP_BAR_WIDTH));
			col++;
			if (col * BROWSER_WIDTH + LEFT_BAR_WIDTH > MAX_WIDTH) {
				col = 0;
				row++;
			}
		}

		browsersClosed = false;
		return browsers;
	}

	private void parallelBrowserInit(int required, final int existing,
			final List<WebDriver> browsers) throws InterruptedException,
			ExecutionException, TimeoutException {
		parallelTask(required, new Function<Integer, Void>() {
			@Override
			public Void apply(Integer num) {
				WebDriver browser = newWebDriver();
				if (browser != null) {
					browsers.add(browser);
					log.debug("Created and added browser #{} to browsers list",
							existing + num);
				} else
					log.warn("Browser instance #{} found to be null", existing
							+ num);
				return null;
			}
		});
	}

	protected void parallelTask(int num, final Function<Integer, Void> function)
			throws InterruptedException, ExecutionException, TimeoutException {

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
					log.debug(
							"Waiting for the job execution to complete ({}/{})",
							i + 1, num);
					exec.take().get();
					log.debug("Job completed ({}/{})", i + 1, num);
				} catch (ExecutionException e) {
					log.error("Execution exception of job {}/{}", i + 1, num, e);
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
		if (!browsersClosed && browsers != null && !browsers.isEmpty()) {
			for (WebDriver browser : browsers)
				if (browser != null)
					try {
						browser.close();
						browser.quit();
					} catch (Exception e) {
						log.warn("Error closing browser", e);
						fail("Unable to close browser: " + e.getMessage());
					}
			browsersClosed = true;
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
						log.debug(
								"Verifing element {} exists in browser of user{}",
								videoElementId, i);
						try {
							WebElement video =
									findElement(getUserName(i), browser,
											videoElementId);
							if (video == null)
								fail("Video element " + videoElementId
										+ " was not found in browser of user"
										+ i);
						} catch (NoSuchElementException e) {
							fail(e.getMessage());
						}
						log.debug("OK - element {} found in browser of user{}",
								videoElementId, i);
					} else {
						log.debug(
								"Verifing element {} is missing from browser of user{}",
								videoElementId, i);
						try {
							waitWhileElement(getUserName(i), browser,
									videoElementId);
						} catch (TimeoutException e) {
							fail(e.getMessage());
						}
						log.debug(
								"OK - element {} is missing from browser of user{}",
								videoElementId, i);
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

	static class BrowserInit extends Thread {
		private DesiredCapabilities capabilities;
		private WebDriver browser;

		BrowserInit(DesiredCapabilities capabilities) {
			this.capabilities = capabilities;
		}

		@Override
		public void run() {
			browser = new ChromeDriver(capabilities);
		}

		WebDriver getBrowser() {
			return browser;
		}
	}

}
