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
package org.kurento.room.test.fake;

import static org.junit.Assert.fail;
import io.github.bonigarcia.wdm.ChromeDriverManager;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.kurento.client.KurentoClient;
import org.kurento.client.KurentoConnectionListener;
import org.kurento.commons.ClassPath;
import org.kurento.commons.PropertiesManager;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.room.test.fake.util.AudioVideoFile;
import org.kurento.room.test.fake.util.FakeSession;
import org.kurento.test.browser.Browser;
import org.kurento.test.browser.BrowserType;
import org.kurento.test.browser.WebPageType;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.TestConfiguration;
import org.kurento.test.docker.Docker;
import org.kurento.test.services.KurentoMediaServerManager;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;

/**
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 *
 */
public abstract class BaseFakeTest {
	public interface Task {
		public void exec(int numTask) throws Exception;
	}

	protected static Logger log = LoggerFactory.getLogger(BaseFakeTest.class);

	public static final long TASKS_TIMEOUT_IN_MINUTES = 15 * 60;

	public final static String CONFIG_TEST_FILENAME = "/kroomtest.conf.json";

	static {
		try {
			System.setProperty("configFilePath",
					ClassPath.get(CONFIG_TEST_FILENAME).toString());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	// configuration keys
	/** {@value} */
	public static final String KURENTO_TEST_FAKE_KMS_URI = "kurento.test.fake.kmsUri";
	/** {@value} */
	public static final String KURENTO_TEST_FAKE_WR_USERS = "kurento.test.fake.wr.users";
	/** {@value} */
	public static final String KURENTO_TEST_FAKE_WR_FILES = "kurento.test.fake.wr.files";
	/** {@value} */
	public static final String KURENTO_TEST_FAKE_WR_FILENAMES = "kurento.test.fake.wr.filenames";
	/** {@value} */
	public static final String KURENTO_TEST_CHROME_FILES = "kurento.test.chrome.files";
	/** {@value} */
	public static final String KURENTO_TEST_CHROME_FILENAMES_Y4M = "kurento.test.chrome.filenames.y4m";
	/** {@value} */
	public static final String KURENTO_TEST_CHROME_FILENAMES_WAV = "kurento.test.chrome.filenames.wav";

	protected static final String FAKE_WR_USER_PREFIX = "user";
	protected static final String CHROME_PREFIX = "chrome";

	protected static int FAKE_WR_USERS = 0;

	protected static long JOIN_ROOM_TOTAL_TIMEOUT_IN_SECONDS = 30;
	protected static long ACTIVE_LIVE_TOTAL_TIMEOUT_IN_SECONDS = 180;
	protected static long ROOM_ACTIVITY_IN_SECONDS = 120;
	protected static long LEAVE_ROOM_TOTAL_TIMEOUT_IN_SECONDS = 10;

	protected static List<String> playerFakeWRUris = new ArrayList<String>();
	protected static List<AudioVideoFile> chromeSrcFiles = new ArrayList<AudioVideoFile>();

	protected volatile KurentoClient testFakeKurento;
	protected static String testFakeKmsWsUri;

	private static String serverPort = PropertiesManager.getProperty(
			"server.port", "8080");
	// FIXME running in Docker might change the address?
	private static String serverAddress = PropertiesManager.getProperty(
			"server.address", "127.0.0.1");
	protected static String APP_HTTP_URL = "http://" + serverAddress + ":"
			+ serverPort;
	protected static final String APP_WS_URL = "ws://" + serverAddress + ":"
			+ serverPort;
	// might get overwritten by custom room apps
	protected static String serviceWsUrl = APP_WS_URL + "/room";;

	protected static final String ROOM_NAME = "room";
	protected String roomName;
	protected static SecureRandom random = new SecureRandom();

	private ConcurrentMap<String, FakeSession> sessions = new ConcurrentHashMap<String, FakeSession>();

	protected Map<String, Exception> execExceptions = new HashMap<String, Exception>();

	private static final int WEB_TEST_TIMEOUT = 20; // seconds
	private static final int WEB_TEST_FIND_LATENCY = 100;
	private static final int WEB_TEST_MAX_WIDTH = 1200;
	private static final int WEB_TEST_LEFT_BAR_WIDTH = 60;
	private static final int WEB_TEST_TOP_BAR_WIDTH = 30;

	protected static int WEB_TEST_BROWSER_WIDTH = 500;
	protected static int WEB_TEST_BROWSER_HEIGHT = 400;

	protected List<Browser> browsers;
	final protected Object browsersLock = new Object();
	private boolean browsersClosed;

	private AtomicInteger numBrowsers = new AtomicInteger(0);

	private static KurentoMediaServerManager kms;

	protected static WebPageType webPageType = WebPageType.ROOM;

	public BaseFakeTest(Logger log) {
		if (log != null)
			BaseFakeTest.log = log;
	}

	protected abstract int getDefaultFakeWRUsersNum();

	@BeforeClass
	public static void setupClass() throws IOException {
		// Chrome binary
		ChromeDriverManager.getInstance().setup();

		String kmsAutostart = PropertiesManager.getProperty(
				TestConfiguration.KMS_AUTOSTART_PROP,
				TestConfiguration.KMS_AUTOSTART_DEFAULT);

		if (!kmsAutostart.equals(TestConfiguration.AUTOSTART_FALSE_VALUE)) {

			if (kms == null) {

				kms = KurentoServicesTestHelper.startKurentoMediaServer(false);

				System.setProperty("kms.uris", "[\"" + kms.getWsUri() + "\"]");

			}
		}
	}

	@AfterClass
	public static void teardownClass() throws IOException {

		// String kmsAutostart =
		// getProperty(TestConfiguration.KMS_AUTOSTART_PROP,
		// TestConfiguration.KMS_AUTOSTART_DEFAULT);
		//
		// if (!kmsAutostart.equals(TestConfiguration.AUTOSTART_FALSE_VALUE)) {
		// KurentoServicesTestHelper.teardownKurentoMediaServer();
		// }
	}

	@Before
	public void setup() {
		execExceptions.clear();

		roomName = ROOM_NAME + random.nextInt(9999);
		log.info("Will connect to WS of room app: {}", serviceWsUrl);

		FAKE_WR_USERS = PropertiesManager.getProperty(
				KURENTO_TEST_FAKE_WR_USERS, getDefaultFakeWRUsersNum());

		initKurentoClientForFakeClients();

		String defaultTestFiles = KurentoServicesTestHelper.getTestFilesPath();

		List<String> playerFilenames = JsonUtils.toStringList(PropertiesManager
				.getPropertyJson(KURENTO_TEST_FAKE_WR_FILENAMES,
						"[\"video/format/chrome.mp4\"]", JsonArray.class));
		String filesLoc = PropertiesManager.getProperty(
				KURENTO_TEST_FAKE_WR_FILES, defaultTestFiles);
		if (filesLoc == null)
			filesLoc = "";
		if (!filesLoc.isEmpty()) {
			filesLoc += (!filesLoc.endsWith("/") ? "/" : "");
			if (!filesLoc.startsWith("http://")
					&& !filesLoc.startsWith("https://")
					&& !filesLoc.startsWith("file://"))
				filesLoc = "file://" + filesLoc;
		}
		playerFakeWRUris.clear();
		for (String fileName : playerFilenames)
			try {
				URI playerUri = new URI(filesLoc + fileName);
				playerFakeWRUris.add(playerUri.toString());
			} catch (URISyntaxException e) {
				log.error("Player URI error", e);
				Assert.fail("Error setting player URI: " + e);
			}
		log.info("Fake clients player sources: {}", playerFakeWRUris);

		List<String> chromeFilenamesWav = JsonUtils
				.toStringList(PropertiesManager.getPropertyJson(
						KURENTO_TEST_CHROME_FILENAMES_WAV, "[]",
						JsonArray.class));
		List<String> chromeFilenamesY4M = JsonUtils
				.toStringList(PropertiesManager.getPropertyJson(
						KURENTO_TEST_CHROME_FILENAMES_Y4M, "[]",
						JsonArray.class));

		String chromeFileLoc = PropertiesManager.getProperty(
				KURENTO_TEST_CHROME_FILES, defaultTestFiles);
		chromeFileLoc += (!chromeFileLoc.endsWith("/") ? "/" : "");

		if (!chromeFileLoc.startsWith("/") && !chromeFileLoc.startsWith("http")
				&& !chromeFileLoc.startsWith("https")) {
			String USER_HOME = System.getProperty("user.home");
			USER_HOME += (!USER_HOME.endsWith("/") ? "/" : "");
			chromeFileLoc = USER_HOME + chromeFileLoc;
		}

		chromeSrcFiles.clear();
		int maxLen = Math.max(chromeFilenamesWav.size(),
				chromeFilenamesY4M.size());
		for (int i = 0; i < maxLen; i++) {
			String a = null;
			String v = null;
			if (i < chromeFilenamesWav.size()) {
				try {
					a = new URI(chromeFileLoc + chromeFilenamesWav.get(i))
							.toString();
				} catch (URISyntaxException e) {
					log.error("Audio file URI error", e);
					Assert.fail("Error setting audio file URI: " + e);
				}
			}
			if (i < chromeFilenamesY4M.size()) {
				try {
					v = new URI(chromeFileLoc + chromeFilenamesY4M.get(i))
							.toString();
				} catch (URISyntaxException e) {
					log.error("Video file URI error", e);
					Assert.fail("Error setting video file URI: " + e);
				}
			}
			chromeSrcFiles.add(new AudioVideoFile(a, v));
		}
		log.info("Chrome clients play sources: {}", chromeSrcFiles);
	}

	private void initKurentoClientForFakeClients() {

		String kmsFake = PropertiesManager
				.getProperty(KURENTO_TEST_FAKE_KMS_URI);

		log.info("Property {} = {}", KURENTO_TEST_FAKE_KMS_URI, kmsFake);

		if (kmsFake != null) {
			// Properties set
			testFakeKmsWsUri = kmsFake;
		} else {

			if (!Docker.getSingleton().isRunningInContainer()) {
				// Local
				testFakeKmsWsUri = "ws://127.0.0.1:8888/kurento";
			} else {
				// Jenkins
				log.info("Executing tests dockerized");
				System.setProperty(TestConfiguration.FAKE_KMS_WS_URI_PROP,
						"ws://127.0.0.1:8888/kurento");
				KurentoMediaServerManager kms;

				try {

					kms = KurentoServicesTestHelper
							.startKurentoMediaServer(true);

					testFakeKmsWsUri = kms.getWsUri();

				} catch (IOException e) {
					log.error("Exception creating fake KMS", e);
				}
			}
		}

		log.info("Using KMS for {} fake clients from {}", FAKE_WR_USERS,
				testFakeKmsWsUri);

	}

	@After
	public void tearDown() {
		for (FakeSession s : sessions.values())
			try {
				s.close();
			} catch (IOException e) {
				log.warn("Error closing session", e);
			}
		if (testFakeKurento != null) {
			testFakeKurento.destroy();
			log.info("Closed testing KurentoClient");
		}
		closeBrowsers();
	}

	protected synchronized KurentoClient getTestFakeKurento() {
		if (testFakeKurento == null) {
			testFakeKurento = KurentoClient.create(testFakeKmsWsUri,
					new KurentoConnectionListener() {
						@Override
						public void connected() {
						}

						@Override
						public void connectionFailed() {
						}

						@Override
						public void disconnected() {
							testFakeKurento = null;
						}

						@Override
						public void reconnected(boolean sameServer) {
						}
					});
		}

		return testFakeKurento;
	}

	protected FakeSession getSession(String room) {
		return sessions.get(room);
	}

	protected FakeSession createSession(String room) {
		if (sessions.containsKey(room))
			return sessions.get(room);
		FakeSession s = new FakeSession(serviceWsUrl, room,
				getTestFakeKurento());
		FakeSession old = sessions.putIfAbsent(room, s);
		if (old != null)
			return old;
		return s;
	}

	protected void parallelTasks(int numThreads, final String thPrefix,
			String taskName, Map<String, Exception> exceptions, final Task task) {
		ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
		ExecutorCompletionService<Void> exec = new ExecutorCompletionService<>(
				threadPool);

		try {
			for (int i = 0; i < numThreads; i++) {
				final int numTask = i;
				exec.submit(new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						String thname = Thread.currentThread().getName();
						Thread.currentThread().setName(thPrefix + numTask);
						task.exec(numTask);
						Thread.currentThread().setName(thname);
						return null;
					}
				});
			}
			for (int i = 0; i < numThreads; i++) {
				String thTask = taskName + "-" + thPrefix + i;
				try {
					log.debug(
							"Waiting for the {} execution to complete ({}/{})",
							thTask, i + 1, numThreads);
					exec.take().get();
					log.debug("Job {} completed ({}/{})", thTask, i + 1,
							numThreads);
				} catch (ExecutionException e) {
					log.debug("Execution exception of {} ({}/{})", thTask,
							i + 1, numThreads, e);
					exceptions.put(taskName + "-" + thPrefix + i, e);
				} catch (InterruptedException e) {
					log.error(
							"Interrupted while waiting for execution of task{}",
							i, e);
				}
			}
		} finally {
			threadPool.shutdownNow();
			try {
				threadPool.awaitTermination(TASKS_TIMEOUT_IN_MINUTES,
						TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				log.warn(
						"Tasks were executed more than {} minutes. Stopping it",
						TASKS_TIMEOUT_IN_MINUTES);
				threadPool.shutdownNow();
			}
		}
	}

	protected void failWithExceptions() {
		if (!execExceptions.isEmpty()) {
			StringBuffer sb = new StringBuffer();
			for (String exKey : execExceptions.keySet()) {
				Exception e = execExceptions.get(exKey);
				log.error("Error on '{}'", exKey, e);
				sb.append(exKey).append(" - ").append(e.getMessage())
						.append("\n");
			}
			sb.append("Check logs for more details");
			Assert.fail(sb.toString());
		}
	}

	protected void idlePeriod() {
		idlePeriod("ACTIVE_LIVE", "LEAVE_ROOM", roomName);
	}

	protected void idlePeriod(String room) {
		idlePeriod("ACTIVE_LIVE", "LEAVE_ROOM", room);
	}

	protected void idlePeriod(String previousAction, String nextAction,
			String room) {
		if (!execExceptions.isEmpty()) {
			log.warn("\n-----------------\n"
					+ "Wait for {} concluded in '{}':\n"
					+ "WITH ERROR(s). No idle waiting for this test."
					+ "\n-----------------\n", previousAction, room);
		} else {
			log.info("\n-----------------\n" + "Wait for {} concluded in '{}'"
					+ "\n-----------------\n" + "Waiting {} seconds",
					previousAction, room, ROOM_ACTIVITY_IN_SECONDS);

			try {
				Thread.sleep(ROOM_ACTIVITY_IN_SECONDS * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		log.info("\n-----------------\n" + "{} in '{}'"
				+ "\n-----------------\n", nextAction, room);
	}

	// ------------------------ Fake WebRTC users ----------------------------

	protected void joinWR(int userSuffix, String fakeWRUri) {
		joinWR(roomName, userSuffix, fakeWRUri);
	}

	protected void joinWR(String room, int userSuffix, String fakeWRUri) {
		try {
			FakeSession s = createSession(room);
			s.newParticipant(FAKE_WR_USER_PREFIX + userSuffix, fakeWRUri, true,
					true);
		} catch (Exception e) {
			log.debug("{}-{}: WR join MCU exception", room, FAKE_WR_USER_PREFIX
					+ userSuffix, e);
			execExceptions.put("WRJoinRoom-" + room + "-" + FAKE_WR_USER_PREFIX
					+ userSuffix, e);
		}
	}

	protected void seqJoinWR() {
		seqJoinWR(roomName);
	}

	protected void seqJoinWR(String room) {
		for (int i = 0; i < FAKE_WR_USERS; i++) {
			try {
				FakeSession s = createSession(room);
				s.newParticipant(FAKE_WR_USER_PREFIX + i,
						playerFakeWRUris.get(i % playerFakeWRUris.size()),
						true, true);
			} catch (Exception e) {
				log.debug("{}-{}: WR seq join MCU exception", room,
						FAKE_WR_USER_PREFIX + i, e);
				execExceptions.put("seqWRJoinRoom-" + room + "-"
						+ FAKE_WR_USER_PREFIX + i, e);
			}
		}
	}

	protected CountDownLatch parallelJoinWR() {
		return parallelJoinWR(roomName);
	}

	protected CountDownLatch parallelJoinWR(final String room) {
		return parallelJoinWR(room, FAKE_WR_USERS, FAKE_WR_USER_PREFIX, null);
	}

	protected CountDownLatch parallelJoinWR(final String room, int userThreads,
			final String userPrefix, final KurentoClient kurento) {
		log.debug("Joining room '{}': {} fake WR participants ({}XXX)", room,
				userThreads, userPrefix);
		final CountDownLatch joinLatch = new CountDownLatch(userThreads);
		parallelTasks(userThreads, userPrefix, "parallelWRJoinRoom-" + room,
				execExceptions, new Task() {
					@Override
					public void exec(int numTask) throws Exception {
						try {
							FakeSession s = createSession(room);
							if (kurento == null)
								s.newParticipant(
										userPrefix + numTask,
										playerFakeWRUris.get(numTask
												% playerFakeWRUris.size()),
										true, true);
							else
								s.newParticipant(
										userPrefix + numTask,
										playerFakeWRUris.get(numTask
												% playerFakeWRUris.size()),
										true, true, kurento);
						} finally {
							joinLatch.countDown();
						}
					}
				});
		return joinLatch;
	}

	protected CountDownLatch parallelWaitActiveLiveWR() {
		return parallelWaitActiveLiveWR(roomName);
	}

	protected CountDownLatch parallelWaitActiveLiveWR(final String room) {
		return parallelWaitActiveLiveWR(room, FAKE_WR_USERS);
	}

	protected CountDownLatch parallelWaitActiveLiveWR(final String room,
			int userThreads) {
		return parallelWaitActiveLiveWR(room, userThreads, FAKE_WR_USER_PREFIX);
	}

	protected CountDownLatch parallelWaitActiveLiveWR(final String room,
			int userThreads, final String userPrefix) {
		final CountDownLatch waitForLatch = new CountDownLatch(userThreads);
		parallelTasks(userThreads, userPrefix, "parallelWaitForActiveLive-"
				+ room, execExceptions, new Task() {
			@Override
			public void exec(int numTask) throws Exception {
				getSession(room).getParticipant(userPrefix + numTask)
						.waitForActiveLive(waitForLatch);
			}
		});
		return waitForLatch;
	}

	protected void seqLeaveWR() {
		seqLeaveWR(roomName);
	}

	protected void seqLeaveWR(String room) {
		seqLeaveWR(room, FAKE_WR_USERS);
	}

	protected void seqLeaveWR(String room, int userThreads) {
		for (int i = 0; i < userThreads; i++) {
			try {
				getSession(room).getParticipant(FAKE_WR_USER_PREFIX + i)
						.leaveRoom();
			} catch (Exception e) {
				log.debug("{}-{}: WR seq leave MCU exception", room,
						FAKE_WR_USER_PREFIX + i, e);
				execExceptions.put("seqWRLeaveRoom-" + room + "-"
						+ FAKE_WR_USER_PREFIX + i, e);
			}
		}
	}

	protected CountDownLatch parallelLeaveWR() {
		return parallelLeaveWR(roomName);
	}

	protected CountDownLatch parallelLeaveWR(final String room) {
		return parallelLeaveWR(room, FAKE_WR_USERS);
	}

	protected CountDownLatch parallelLeaveWR(final String room, int userThreads) {
		return parallelLeaveWR(room, userThreads, FAKE_WR_USER_PREFIX);
	}

	protected CountDownLatch parallelLeaveWR(final String room,
			int userThreads, final String userPrefix) {
		final CountDownLatch leaveLatch = new CountDownLatch(userThreads);
		parallelTasks(userThreads, userPrefix, "parallelWRLeaveRoom-" + room,
				execExceptions, new Task() {
					@Override
					public void exec(int numTask) throws Exception {
						try {
							getSession(room).getParticipant(
									userPrefix + numTask).leaveRoom();
						} finally {
							leaveLatch.countDown();
						}
					}
				});
		return leaveLatch;
	}

	protected void await(CountDownLatch waitLatch, long actionTimeoutInSeconds,
			String action, Map<String, Exception> awaitExceptions) {
		await(waitLatch, actionTimeoutInSeconds, action, awaitExceptions,
				FAKE_WR_USERS);
	}

	protected void await(CountDownLatch waitLatch, long actionTimeoutInSeconds,
			String action, Map<String, Exception> awaitExceptions,
			int userThreads) {
		try {
			if (!waitLatch.await(actionTimeoutInSeconds, TimeUnit.SECONDS))
				awaitExceptions.put(action, new Exception(
						"Timeout waiting for '" + action + "' of "
								+ userThreads + " tasks (max "
								+ actionTimeoutInSeconds + "s)"));
			else
				log.debug("Finished waiting for {}", action);
		} catch (InterruptedException e) {
			log.warn("Interrupted when waiting for {} of {} tasks (max {}s)",
					action, userThreads, actionTimeoutInSeconds, e);
		}
	}

	// --------------------- Chrome users -------------------------------------

	protected void joinChrome() {
		joinChrome(chromeSrcFiles.size(), chromeSrcFiles);
	}

	protected void joinChromeSpinner(int numBrowsers) {
		joinChrome(numBrowsers, null);
	}

	protected void joinChrome(int numBrowsers, List<AudioVideoFile> localFiles) {
		int joinFrom = 0;
		try {
			synchronized (browsersLock) {
				if (browsers != null && !browsers.isEmpty()) {
					joinFrom = browsers.size();
					browsers.addAll(createBrowsers(numBrowsers, localFiles));
				} else
					browsers = createBrowsers(numBrowsers, localFiles);
			}
			for (int i = joinFrom; i < browsers.size(); i++)
				joinToRoom(browsers.get(i).getWebDriver(), CHROME_PREFIX + i,
						roomName);
		} catch (Exception e) {
			execExceptions.put("chromeBrowser", e);
			log.debug("Error in joining from browser", e);
		}
	}

	protected void leaveChrome() {
		for (int i = 0; i < browsers.size(); i++)
			exitFromRoom(CHROME_PREFIX + i, browsers.get(i).getWebDriver());
	}

	protected Browser newWebDriver(AudioVideoFile localMedia) {

		int numBrowser = numBrowsers.getAndIncrement();

		Browser.Builder builder = new Browser.Builder()
				.browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL)
				.serverPort(Integer.parseInt(serverPort)).timeout(1)
				.webPageType(webPageType);

		if (localMedia != null) {
			if (localMedia.getVideo() != null) {
				builder.video(localMedia.getVideo());
			}
			// TODO once the option is available in the test framework
			// if (localMedia.getAudio() != null)
			// options.addArguments("--use-file-for-fake-audio-capture="
			// + localMedia.getAudio());
		}

		Browser browser = builder.build();
		browser.setId("browser-" + numBrowser);
		browser.init();
		return browser;
	}

	protected List<Browser> createBrowsers(int numUsers,
			List<AudioVideoFile> localFiles) throws InterruptedException,
			ExecutionException, TimeoutException {

		final List<Browser> browsers = Collections
				.synchronizedList(new ArrayList<Browser>());
		if (numUsers == 0)
			return browsers;

		parallelBrowserInit(numUsers, 0, browsers, localFiles);

		if (browsers.size() < numUsers) {
			int required = numUsers - browsers.size();
			log.warn("Not enough browsers were created, will retry to "
					+ "recreate the missing ones: {}", required);
			parallelBrowserInit(required, browsers.size(), browsers, localFiles);
		}

		if (browsers.size() < numUsers)
			fail("Unable to create the required number of browsers: "
					+ numUsers);

		int row = 0;
		int col = 0;

		for (Browser browser : browsers) {

			browser.getWebDriver()
					.manage()
					.window()
					.setSize(
							new Dimension(WEB_TEST_BROWSER_WIDTH,
									WEB_TEST_BROWSER_HEIGHT));
			browser.getWebDriver()
					.manage()
					.window()
					.setPosition(
							new Point(col * WEB_TEST_BROWSER_WIDTH
									+ WEB_TEST_LEFT_BAR_WIDTH, row
									* WEB_TEST_BROWSER_HEIGHT
									+ WEB_TEST_TOP_BAR_WIDTH));
			col++;
			if (col * WEB_TEST_BROWSER_WIDTH + WEB_TEST_LEFT_BAR_WIDTH > WEB_TEST_MAX_WIDTH) {
				col = 0;
				row++;
			}
		}

		browsersClosed = false;
		return browsers;
	}

	protected void closeBrowsers() {
		if (!browsersClosed && browsers != null && !browsers.isEmpty()) {
			for (Browser browser : browsers)
				if (browser != null)
					try {
						browser.close();
					} catch (Exception e) {
						log.warn("Error closing browser", e);
						fail("Unable to close browser: " + e.getMessage());
					}
			browsersClosed = true;
		} else {
			log.warn("No browsers to close (close flag: {})", browsersClosed);
		}
	}

	protected void verify(List<WebDriver> browsers, boolean[] activeUsers) {

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < activeUsers.length; i++) {
			if (activeUsers[i]) {
				sb.append(CHROME_PREFIX + i + ",");
			}
		}
		log.debug("Checking active users: [{}]", sb);

		long startTime = System.nanoTime();

		for (int i = 0; i < activeUsers.length; i++) {

			if (activeUsers[i]) {
				WebDriver browser = browsers.get(i);

				for (int j = 0; j < activeUsers.length; j++) {

					String videoElementId = "video-" + getChromeUserName(j);

					if (activeUsers[j]) {
						log.debug(
								"Verifing element {} exists in browser of user{}",
								videoElementId, i);
						try {
							WebElement video = findElement(
									getChromeUserName(i), browser,
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
							waitWhileElement(getChromeUserName(i), browser,
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

	protected void verify(List<Browser> browsers, boolean[] activeChromeUsers,
			Map<String, Boolean> activeFakeWrUsers) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < activeChromeUsers.length; i++) {
			if (activeChromeUsers[i])
				sb.append(CHROME_PREFIX + i + ": active, ");
			else
				sb.append(CHROME_PREFIX + i + ": not, ");
		}
		log.debug("Checking Chrome users (active or not): [{}]", sb);
		if (!activeFakeWrUsers.isEmpty())
			log.debug("Checking Fake WR users (active or not): {}",
					activeFakeWrUsers);

		long startTime = System.nanoTime();

		for (int i = 0; i < activeChromeUsers.length; i++) {

			if (activeChromeUsers[i]) {
				WebDriver browser = browsers.get(i).getWebDriver();
				String browserLabel = getChromeUserName(i);
				String chromeName = CHROME_PREFIX + i;

				for (int j = 0; j < activeChromeUsers.length; j++) {
					String videoElementId = "video-" + getChromeUserName(j);
					verifyVideoInBrowser(browser, browserLabel, chromeName,
							videoElementId, activeChromeUsers[j]);
				}

				for (Entry<String, Boolean> fakeEntry : activeFakeWrUsers
						.entrySet()) {
					String videoElementId = "video-"
							+ getFakeWRUserName(fakeEntry.getKey());
					verifyVideoInBrowser(browser, browserLabel, chromeName,
							videoElementId, fakeEntry.getValue());
				}
			}
		}

		long endTime = System.nanoTime();

		double duration = ((double) endTime - startTime) / 1_000_000;

		log.debug("Checked active users: [{}] {} - in {} millis", sb,
				(!activeFakeWrUsers.isEmpty() ? "& " + activeFakeWrUsers : ""),
				duration);
	}

	private void verifyVideoInBrowser(WebDriver browser, String browserLabel,
			String chromeName, String videoElementId, boolean isActive) {
		if (isActive) {
			log.debug("Verifing element {} exists in browser of {}",
					videoElementId, chromeName);
			try {
				WebElement video = findElement(browserLabel, browser,
						videoElementId);
				if (video == null)
					fail("Video element " + videoElementId
							+ " was not found in browser of " + chromeName);
			} catch (NoSuchElementException e) {
				fail(e.getMessage());
			}
			log.debug("OK - element {} found in browser of {}", videoElementId,
					chromeName);
		} else {
			log.debug("Verifing element {} is missing from browser of {}",
					videoElementId, chromeName);
			try {
				waitWhileElement(browserLabel, browser, videoElementId);
			} catch (TimeoutException e) {
				fail(e.getMessage());
			}
			log.debug("OK - element {} is missing from browser of {}",
					videoElementId, chromeName);
		}
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
		WebElement nameInput = findElement(userName, userBrowser, "name");
		nameInput.clear();
		nameInput.sendKeys(userName);
		WebElement roomInput = findElement(userName, userBrowser, "roomName");
		roomInput.clear();
		roomInput.sendKeys(roomName);
		findElement(userName, userBrowser, "joinBtn").submit();
	}

	protected CountDownLatch[] createCdl(int numLatches, int numUsers) {
		final CountDownLatch[] cdl = new CountDownLatch[numLatches];
		for (int i = 0; i < numLatches; i++) {
			cdl[i] = new CountDownLatch(numUsers);
		}
		return cdl;
	}

	protected void waitForStream(String label, WebDriver driver,
			String videoTagId) {
		int i = 0;
		for (; i < WEB_TEST_TIMEOUT; i++) {
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
		if (i == WEB_TEST_TIMEOUT) {
			Assert.fail("Video tag '" + videoTagId
					+ "' is not playing media after " + WEB_TEST_TIMEOUT
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
			String msg = "Video tag "
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
			(new WebDriverWait(browser, WEB_TEST_TIMEOUT, WEB_TEST_FIND_LATENCY))
					.until(ExpectedConditions.invisibilityOfElementLocated(By
							.id(id)));
		} catch (org.openqa.selenium.TimeoutException e) {
			log.warn(
					"Timeout when waiting for element {} to disappear in browser {}",
					id, label, e);
			throw new TimeoutException("Element with id='" + id
					+ "' is present in page after " + WEB_TEST_TIMEOUT
					+ " seconds");
		}
	}

	protected WebElement findElement(String label, WebDriver browser, String id) {
		try {
			return (new WebDriverWait(browser, WEB_TEST_TIMEOUT,
					WEB_TEST_FIND_LATENCY)).until(ExpectedConditions
					.presenceOfElementLocated(By.id(id)));
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
						+ "' not found after " + WEB_TEST_TIMEOUT
						+ " seconds in browser " + label);
			}
		}
	}

	private String getChromeUserName(int i) {
		return CHROME_PREFIX + i + "_webcam";
	}

	private String getFakeWRUserName(String userPrefix) {
		return userPrefix + "_webcam";
	}

	private void parallelBrowserInit(int required, final int existing,
			final List<Browser> browsers, final List<AudioVideoFile> localFiles)
			throws InterruptedException, ExecutionException, TimeoutException {
		Map<String, Exception> exceptions = new HashMap<String, Exception>();
		parallelTasks(required, "web", "browserInit", exceptions, new Task() {

			@Override
			public void exec(int numTask) throws Exception {
				int realNum = existing + numTask;
				AudioVideoFile localFile = (localFiles != null ? localFiles
						.get(realNum) : null);
				Browser browser = newWebDriver(localFile);
				if (browser != null) {
					browsers.add(browser);
					log.debug(
							"Created and added browser #{} to browsers list (localVideo={})",
							realNum, localFile);
				} else
					log.warn("Browser instance #{} found to be null", realNum);
			}
		});
	}
}
