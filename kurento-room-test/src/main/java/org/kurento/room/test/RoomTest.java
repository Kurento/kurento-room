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

import static org.junit.Assert.assertEquals;
import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.room.test.config.RoomTestConfiguration.ROOM_APP_CLASSNAME_DEFAULT;
import static org.kurento.room.test.config.RoomTestConfiguration.ROOM_APP_CLASSNAME_PROP;
import static org.kurento.room.test.config.RoomTestUtils.createRoomsCdl;
import static org.kurento.room.test.config.RoomTestUtils.getBrowserKey;
import static org.kurento.room.test.config.RoomTestUtils.getBrowserNativeStreamName;
import static org.kurento.room.test.config.RoomTestUtils.getBrowserUserName;
import static org.kurento.room.test.config.RoomTestUtils.getBrowserVideoStreamName;
import static org.kurento.room.test.config.RoomTestUtils.getFakeActivityMapByRoomAndUserName;
import static org.kurento.room.test.config.RoomTestUtils.getFakeKey;
import static org.kurento.room.test.config.RoomTestUtils.getFakeNativeStreamName;
import static org.kurento.room.test.config.RoomTestUtils.getFakeUserName;
import static org.kurento.room.test.config.RoomTestUtils.getFakeVideoStreamName;
import static org.kurento.room.test.config.RoomTestUtils.getNumFakeUsers;
import static org.kurento.room.test.config.RoomTestUtils.getNumWebUsers;
import static org.kurento.room.test.config.RoomTestUtils.getPlaySourcePath;
import static org.kurento.room.test.config.RoomTestUtils.getUniqueRooms;
import static org.kurento.room.test.config.RoomTestUtils.getWebActivityMapByRoomAndUserName;
import static org.kurento.room.test.config.RoomTestUtils.sleep;
import static org.kurento.test.config.TestConfiguration.SELENIUM_SCOPE_PROPERTY;
import static org.kurento.test.config.TestConfiguration.TEST_HOST_PROPERTY;
import static org.kurento.test.config.TestConfiguration.TEST_PORT_PROPERTY;
import static org.kurento.test.config.TestConfiguration.TEST_PROTOCOL_DEFAULT;
import static org.kurento.test.config.TestConfiguration.TEST_PROTOCOL_PROPERTY;
import static org.kurento.test.config.TestConfiguration.TEST_PUBLIC_IP_DEFAULT;
import static org.kurento.test.config.TestConfiguration.TEST_PUBLIC_IP_PROPERTY;
import static org.kurento.test.config.TestConfiguration.TEST_PUBLIC_PORT_PROPERTY;
import static org.kurento.test.config.TestConfiguration.TEST_URL_TIMEOUT_DEFAULT;
import static org.kurento.test.config.TestConfiguration.TEST_URL_TIMEOUT_PROPERTY;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.kurento.client.KurentoClient;
import org.kurento.commons.PropertiesManager;
import org.kurento.commons.exception.KurentoException;
import org.kurento.commons.testing.SystemFunctionalTests;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.room.test.config.FakeLifecycle;
import org.kurento.room.test.config.FakeLifecycleTask;
import org.kurento.room.test.config.Lifecycle;
import org.kurento.room.test.config.Lifecycle.Type;
import org.kurento.room.test.config.RoomTestConfiguration;
import org.kurento.room.test.config.WebLifecycleTask;
import org.kurento.room.test.fake.util.FakeSession;
import org.kurento.test.base.BrowserTest;
import org.kurento.test.base.KurentoTest;
import org.kurento.test.browser.Browser;
import org.kurento.test.browser.BrowserType;
import org.kurento.test.browser.WebPage;
import org.kurento.test.browser.WebPageType;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;
import org.kurento.test.docker.Docker;
import org.kurento.test.services.FakeKmsService;
import org.kurento.test.services.KmsService;
import org.kurento.test.services.Service;
import org.kurento.test.services.TestService;
import org.kurento.test.services.WebServerService;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.google.gson.JsonArray;

/**
 * Base for Kurento Room tests with browsers and fake clients.
 *
 * @author Radu Tom Vlad (rvlad@naevatec.com)
 * @since 6.3.2
 */
@Category(SystemFunctionalTests.class)
public abstract class RoomTest<W extends WebPage> extends BrowserTest<W> {

  static class KmsUriSetterService extends TestService {

    private KmsService kms;

    public KmsUriSetterService(KmsService kms) {
      this.kms = kms;
    }

    @Override
    public TestServiceScope getScope() {
      return kms.getScope();
    }

    @Override
    public void start() {
      super.start();
      String uri = kms.getWsUri();
      System.setProperty("kms.uris", "[\"" + uri + "\"]");
      System.setProperty("kms.uri", uri);
      log.debug("Set system properties 'kms.uri' to {} & 'kms.uris' to [{}] ", uri, uri);
    }

  }

  public static int POLLING_LATENCY = 100;
  public static int WEB_TEST_MAX_WIDTH = 1200;
  public static int WEB_TEST_LEFT_BAR_WIDTH = 60;
  public static int WEB_TEST_TOP_BAR_WIDTH = 30;
  public static int WEB_TEST_BROWSER_WIDTH = 500;
  public static int WEB_TEST_BROWSER_HEIGHT = 400;

  public static @Service(1) KmsService kms = new KmsService();
  public static @Service(2) KmsUriSetterService kmsUriSetter = new KmsUriSetterService(kms);
  public static @Service(3) KmsService fakeKms = new FakeKmsService();

  public static String testFiles = KurentoTest.getTestFilesDiskPath();

  public static BrowserScope testScope = BrowserScope.LOCAL;
  public static Class<?> webServerClass;

  static {
    loadWebServerClass();
    if (webServerClass == null) {
      Assert.fail("Unable to load any of the provided classnames for the web server test service");
    }

    String scopeProp = PropertiesManager.getProperty(SELENIUM_SCOPE_PROPERTY);
    if (scopeProp != null) {
      testScope = BrowserScope.valueOf(scopeProp.toUpperCase());
    }
  }

  public static @Service(99) WebServerService webServer = new WebServerService(webServerClass);

  public static int testTimeout;

  // overwritten if running in Docker
  public static String serverAddress = PropertiesManager.getProperty(TEST_HOST_PROPERTY,
      getProperty(TEST_PUBLIC_IP_PROPERTY, TEST_PUBLIC_IP_DEFAULT));
  public static int serverPort = getProperty(TEST_PORT_PROPERTY,
      getProperty(TEST_PUBLIC_PORT_PROPERTY, WebServerService.getAppHttpPort()));
  public static Protocol protocol = Protocol.valueOf(getProperty(TEST_PROTOCOL_PROPERTY,
      TEST_PROTOCOL_DEFAULT).toUpperCase());

  // might get overwritten by custom room apps
  public static String appWsPath = "/room";

  public static URI appWsUrl;
  public static URI appBaseUrl;
  public static URI appUrl;

  public static WebPageType webPageType = WebPageType.ROOM;

  public int ROOM_INOUT_AWAIT_TIME_IN_SECONDS = RoomTestConfiguration.DEFAULT_ROOM_INOUT_AWAIT_TIME_IN_SECONDS;
  public int PLAY_TIME = RoomTestConfiguration.DEFAULT_PLAY_TIME_IN_SECONDS;
  public int ACTIVE_LIVE_TOTAL_TIMEOUT_IN_SECONDS = RoomTestConfiguration.DEFAULT_ACTIVE_LIVE_TOTAL_TIMEOUT_IN_SECONDS;

  public int ITERATIONS = 2;

  public Object browsersLock = new Object();

  public Map<String, Exception> execExceptions = new HashMap<String, Exception>();

  public boolean failed = false;

  public ConcurrentMap<String, FakeSession> sessions = new ConcurrentHashMap<String, FakeSession>();

  public KurentoClient fakeKurentoClient;

  public RoomTest() {
    setDeleteLogsIfSuccess(false); // always keep the logs
  }

  @BeforeClass
  public static void beforeClass() {
    testTimeout = getProperty(TEST_URL_TIMEOUT_PROPERTY, TEST_URL_TIMEOUT_DEFAULT);
    log.debug("Test timeout: {}", testTimeout);

    calculateUrl();
    String wsProtocol = "ws";
    if (protocol.equals(Protocol.HTTPS)) {
      wsProtocol = "wss";
    }
    String hostName = appUrl.getHost();
    try {
      appWsUrl = new URI(wsProtocol, null, hostName, serverPort, appWsPath, null, null);
    } catch (URISyntaxException e) {
      throw new KurentoException("Exception generating WS URI from " + wsProtocol + ", " + hostName
          + ", server port " + serverPort + " and WS path " + appWsPath);
    }
    log.debug("Protocol: {}, Hostname: {}, Port: {}, Path: {}", wsProtocol, hostName, serverPort,
        appWsPath);
  }

  @Override
  public void setupBrowserTest() throws InterruptedException {
    super.setupBrowserTest();

    execExceptions.clear();

    if (testScenario != null && testScenario.getBrowserMap() != null
        && testScenario.getBrowserMap().size() > 0) {
      int row = 0;
      int col = 0;
      for (final String browserKey : testScenario.getBrowserMap().keySet()) {
        Browser browser = getPage(browserKey).getBrowser();
        browser.getWebDriver().manage().window()
        .setSize(new Dimension(WEB_TEST_BROWSER_WIDTH, WEB_TEST_BROWSER_HEIGHT));
        browser
        .getWebDriver()
        .manage()
        .window()
        .setPosition(
            new Point(col * WEB_TEST_BROWSER_WIDTH + WEB_TEST_LEFT_BAR_WIDTH, row
                * WEB_TEST_BROWSER_HEIGHT + WEB_TEST_TOP_BAR_WIDTH));
        col++;
        if (col * WEB_TEST_BROWSER_WIDTH + WEB_TEST_LEFT_BAR_WIDTH > WEB_TEST_MAX_WIDTH) {
          col = 0;
          row++;
        }
      }
    }

    fakeKurentoClient = fakeKms.getKurentoClient();
    Assert.assertNotNull("Fake Kurento Client is null", fakeKurentoClient);
  }

  @Override
  public void teardownBrowserTest() {
    for (FakeSession s : sessions.values()) {
      try {
        s.close();
      } catch (IOException e) {
        log.warn("Error closing session", e);
      }
    }
    fakeKms.closeKurentoClient();

    super.teardownBrowserTest();

    failWithExceptions();
  }

  public static void calculateUrl() {
    if (appUrl == null) {
      String hostName = serverAddress;
      if (BrowserScope.DOCKER.equals(testScope)) {
        Docker docker = Docker.getSingleton();
        if (docker.isRunningInContainer()) {
          hostName = docker.getContainerIpAddress();
        } else {
          hostName = docker.getHostIpForContainers();
        }
      }
      log.debug("Protocol: {}, Hostname: {}, Port: {}, Web page type: {}", protocol, hostName,
          serverPort, webPageType);
      try {
        appUrl = new URI(protocol.toString(), null, hostName, serverPort, webPageType.toString(),
            null, null);
      } catch (URISyntaxException e) {
        throw new KurentoException("Exception generating URI from " + protocol + ", " + hostName
            + ", server port " + serverPort + " and webpage type " + webPageType);
      }
      try {
        appBaseUrl = new URI(protocol.toString(), null, hostName, serverPort, null, null, null);
      } catch (URISyntaxException e) {
        throw new KurentoException("Exception generating URI from " + protocol + ", " + hostName
            + ", server port " + serverPort);
      }
    }
  }

  public static Collection<Object[]> localChromes(String caller, WebPageType pageType,
      Lifecycle[] users) {
    TestScenario test = new TestScenario();
    for (int i = 0; i < users.length; i++) {
      if (users[i].getType().equals(Type.WEB)) {
        Browser browser = new Browser.Builder().webPageType(pageType)
            .browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL).build();
        test.addBrowser(getBrowserKey(users[i].getUserIndex(), users[i].getRoom()), browser);
      }
    }
    log.debug("{}: Web browsers: {}, webPageType: {}, test scope: {}, Browsers map keySet: {}",
        caller, getNumWebUsers(users), pageType, testScope.toString(), test.getBrowserMap()
        .keySet());
    return Arrays.asList(new Object[][] { { test } });
  }

  public static void loadWebServerClass() {
    try {
      List<String> auxList = JsonUtils.toStringList(PropertiesManager.getPropertyJson(
          ROOM_APP_CLASSNAME_PROP, ROOM_APP_CLASSNAME_DEFAULT, JsonArray.class));

      for (String aux : auxList) {
        log.info("Loading class '{}' as the test's web server service", aux);
        try {
          webServerClass = Class.forName(aux);
          break;
        } catch (Exception e) {
          log.warn("Couldn't load web server class '{}': {}", aux, e.getMessage());
          log.debug("Couldn't load web server class '{}'", aux, e);
        }
      }
    } catch (Exception e) {
      log.error("Incorrect value for property '{}'", ROOM_APP_CLASSNAME_PROP, e);
    }
  }

  public void parallelLifecycles(int iterations, Lifecycle[] users, final WebLifecycleTask webTask,
      final FakeLifecycleTask fakeTask) {
    int numWebUsers = getNumWebUsers(users);
    int numFakeUsers = getNumFakeUsers(users);
    int totalExecutions = iterations * users.length;
    log.debug("Parallel lifecycles stats:\n\t- users: {} (web={}, fake={})\n\t- iterations: {}",
        users.length, numWebUsers, numFakeUsers, iterations);

    ExecutorService threadPool = Executors.newFixedThreadPool(totalExecutions);
    ExecutorCompletionService<Void> exec = new ExecutorCompletionService<>(threadPool);

    try {
      for (int j = 0; j < iterations; j++) {
        final int it = j;
        log.info("it#{}: Starting execution of {} users", it, users.length);

        // TODO should it be room-separated??
        final CountDownLatch activeLiveLatch = new CountDownLatch(numFakeUsers);
        if (numFakeUsers > 0) {
          Set<String> rooms = getUniqueRooms(users);
          for (String room : rooms) {
            createSession(room);
            log.debug("it#{}: Created fake session for room '{}'", it, room);
          }
        }

        for (int i = 0; i < users.length; i++) {
          final int userIndex = users[i].getUserIndex();
          final String userKey = users[i].getUserKey();
          final String room = users[i].getRoom();

          switch (users[i].getType()) {
            case FAKE :
              final FakeLifecycle fakeUserConfig = (FakeLifecycle) users[i];
              exec.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                  String thname = Thread.currentThread().getName();
                  Thread.currentThread().setName("it" + it + "|" + userKey);
                  fakeTask.run(userIndex, room, it, activeLiveLatch, fakeUserConfig.getMediaUri(),
                      getSession(room));
                  Thread.currentThread().setName(thname);
                  return null;
                }
              });
              break;
            case WEB :
              final Browser browser = getPage(getBrowserKey(userIndex, room)).getBrowser();
              exec.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                  String thname = Thread.currentThread().getName();
                  Thread.currentThread().setName("it" + it + "|" + userKey);
                  if (it > 0) {
                    log.debug("Page reloaded");
                    browser.reload();
                  }
                  webTask.run(userIndex, room, it, browser);
                  Thread.currentThread().setName(thname);
                  return null;
                }
              });
              break;
            default :
              break;
          }
        }
        for (int i = 0; i < users.length; i++) {
          String thTask = users[i].getUserKey() + "-lifecycle";
          try {
            log.debug("Waiting for {} to complete ({}/{})", thTask, i + 1, users.length);
            exec.take().get();
            log.debug("Finished {} ({}/{})", thTask, i + 1, users.length);
          } catch (ExecutionException e) {
            log.debug("Execution exception of {} ({}/{})", thTask, i + 1, users.length, e);
            execExceptions.put("parallelLifecycle" + "-" + thTask, e);
          } catch (InterruptedException e) {
            log.error("Interrupted while waiting for {} execution", thTask, e);
          }
        }
        if (numFakeUsers > 0) {
          try {
            await(activeLiveLatch, ACTIVE_LIVE_TOTAL_TIMEOUT_IN_SECONDS, "waitForActiveLive-MAIN");
          } catch (RuntimeException e) {
            log.debug("ACTIVE_LIVE await for all fake clients has timed out");
          }
          Set<String> rooms = getUniqueRooms(users);
          for (String room : rooms) {
            closeSession(room);
            removeSession(room);
            log.debug("it#{}: Closed and removed fake session for room {}", it, room);
          }
        }
        log.info("it#{}: Finished execution of {} lifecycles", it, users.length);
        if (!execExceptions.isEmpty()) {
          return; // no need to iterate further
        }
      }
    } catch (Exception e) {
      log.warn("Executing parallel iterations", e);
      execExceptions.put("execParallelIterations", e);
    } finally {
      threadPool.shutdown();
      try {
        threadPool.awaitTermination(RoomTestConfiguration.TASKS_TIMEOUT_IN_MINUTES,
            TimeUnit.MINUTES);
      } catch (InterruptedException e) {
        log.warn("Tasks were executed more than {} minutes. Stopping it",
            RoomTestConfiguration.TASKS_TIMEOUT_IN_MINUTES);
        threadPool.shutdownNow();
      }
    }
  }

  // ----------------WebPage actions ----------------------------------
  public void joinToRoom(int userIndex, String room, String userName) {
    String browserKey = getBrowserKey(userIndex, room);

    try {
      WebElement nameInput = findElementById(browserKey, "name");
      WebElement roomInput = findElementById(browserKey, "roomName");
      nameInput.clear();
      nameInput.sendKeys(userName);
      roomInput.clear();
      roomInput.sendKeys(room);
      findElementById(browserKey, "joinBtn").submit();
      log.debug("Clicked on 'joinBtn' in '{}'", browserKey);
    } catch (Exception e) {
      log.warn("Unable to perform join action in '{}'", browserKey, e);
      registerException("joinRoom-" + browserKey, e);
    }
  }

  public void exitFromRoom(int userIndex, String room) {
    String browserKey = getBrowserKey(userIndex, room);
    Browser userBrowser = getPage(browserKey).getBrowser();

    try {
      Actions actions = new Actions(userBrowser.getWebDriver());
      actions.click(findElementById(browserKey, "buttonLeaveRoom")).perform();
      log.debug("'buttonLeaveRoom' clicked on in '{}'", browserKey);
    } catch (Exception e) {
      log.warn("Unable to perform exit action in '{}', session can't be closed", browserKey, e);
      registerException("exitRoom-" + browserKey, e);
    }
  }

  public void disconnectMedia(int userIndex, String room, String subjectUserName) {
    // first, select the subject thumbnail
    String clickableVideoTagId = getBrowserVideoStreamName(subjectUserName);
    selectElement(userIndex, room, clickableVideoTagId);

    String browserKey = getBrowserKey(userIndex, room);
    Browser userBrowser = getPage(browserKey).getBrowser();

    try {
      Actions actions = new Actions(userBrowser.getWebDriver());
      actions.click(findElementById(browserKey, "buttonDisconnect")).perform();
      log.debug("'buttonDisconnect' clicked on in '{}' when selected '{}'", browserKey,
          clickableVideoTagId);
    } catch (Exception e) {
      log.warn("Unable to perform disconnect action in '{}' (from '{}')", browserKey,
          subjectUserName, e);
      registerException("disconnect-" + browserKey, e);
    }
  }

  public void selectElement(int userIndex, String room, String elementId) {
    String browserKey = getBrowserKey(userIndex, room);
    Browser userBrowser = getPage(browserKey).getBrowser();

    try {
      Actions actions = new Actions(userBrowser.getWebDriver());
      actions.moveToElement(findElementById(browserKey, elementId)).click().perform();
      log.debug("'{}' selected in '{}'", elementId, browserKey);
    } catch (Exception e) {
      log.warn("Unable to perform select action in '{}' (for '{}')", browserKey, elementId, e);
      registerException("selectElement-" + browserKey, e);
    }
  }

  public WebElement findElementById(String browserKey, String elementId) {
    Browser browser = getPage(browserKey).getBrowser();
    try {
      return new WebDriverWait(browser.getWebDriver(), testTimeout, POLLING_LATENCY)
      .until(ExpectedConditions.presenceOfElementLocated(By.id(elementId)));
    } catch (TimeoutException e) {
      log.warn("Timeout when waiting for element {} to exist in browser {}", elementId, browserKey);
      int originalTimeout = 60;
      try {
        originalTimeout = browser.getTimeout();
        log.debug("Original browser timeout (s): {}, set to 10", originalTimeout);
        browser.setTimeout(10);
        browser.changeTimeout(10);
        WebElement elem = browser.getWebDriver().findElement(By.id(elementId));
        log.info("Additional findElement call was able to locate {} in browser {}", elementId,
            browserKey);
        return elem;
      } catch (NoSuchElementException e1) {
        log.debug("Additional findElement call couldn't locate {} in browser {} ({})", elementId,
            browserKey, e1.getMessage());
        throw new NoSuchElementException("Element with id='" + elementId + "' not found after "
            + testTimeout + " seconds in browser " + browserKey);
      } finally {
        browser.setTimeout(originalTimeout);
        browser.changeTimeout(originalTimeout);
      }
    }
  }

  public void waitWhileElement(String browserKey, String elementId) throws TimeoutException {
    Browser browser = getPage(browserKey).getBrowser();
    int originalTimeout = 60;
    try {
      originalTimeout = browser.getTimeout();
      log.debug("Original browser timeout (s): {}, set to 1", originalTimeout);
      browser.setTimeout(1);
      browser.changeTimeout(1);
      new WebDriverWait(browser.getWebDriver(), testTimeout, POLLING_LATENCY)
          .until(ExpectedConditions.invisibilityOfElementLocated(By.id(elementId)));
    } catch (TimeoutException e) {
      log.warn("Timeout when waiting for element '{}' to disappear in browser '{}'", elementId,
          browserKey, e);
      throw new TimeoutException("Element with id='" + elementId + "' is present in page after "
          + testTimeout + " seconds");
    } finally {
      browser.setTimeout(originalTimeout);
      browser.changeTimeout(originalTimeout);
    }
  }

  /**
   * Wait for stream of another browser user.
   *
   * @param userIndex
   *          own user index
   * @param room
   *          room name
   * @param subject
   *          the observed user
   */
  public void waitForWebStream(int userIndex, String room, String subject) {
    waitForStream(userIndex, room, getBrowserNativeStreamName(subject));
  }

  /**
   * Wait for stream of a fake user.
   *
   * @param userIndex
   *          own user index
   * @param room
   *          room name
   * @param subject
   *          the observed user
   */
  public void waitForFakeStream(int userIndex, String room, String subject) {
    waitForStream(userIndex, room, getFakeNativeStreamName(subject));
  }

  /**
   * Wait for stream of user whose video tag has already been generated.
   *
   * @param userIndex
   *          own user index
   * @param room
   *          room name
   * @param targetVideoTagId
   *          subject's video tag id
   */
  public void waitForStream(int userIndex, String room, String targetVideoTagId) {
    String browserKey = getBrowserKey(userIndex, room);
    int i = 0;
    for (; i < testTimeout; i++) {
      WebElement video = null;
      try {
        video = findElementById(browserKey, targetVideoTagId);
      } catch (Exception e) {
        registerException("waitForStream-" + browserKey, e);
      }
      String srcAtt = video.getAttribute("src");
      if (srcAtt != null && srcAtt.startsWith("blob")) {
        break;
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        registerException("waitForStream-" + browserKey, e);
      }
    }
    if (i == testTimeout) {
      registerException("waitForStream-" + browserKey, new TimeoutException("Video tag '"
          + targetVideoTagId + "' is not playing media after " + testTimeout + " seconds in '"
          + browserKey + "'"));
    }
  }

  public void verify(String room, Map<String, Boolean> roomWebActivityMap,
      Map<String, Boolean> roomFakeActivityMap) {
    if (roomWebActivityMap != null && !roomWebActivityMap.isEmpty()) {
      log.debug("Checking Web users (active or not): {}", roomWebActivityMap);
    } else {
      log.debug("No entries in the room web activity map, nothing to check.");
      return;
    }
    if (roomFakeActivityMap != null && !roomFakeActivityMap.isEmpty()) {
      log.debug("Checking Fake users (active or not): {}", roomFakeActivityMap);
    }

    long startTime = System.nanoTime();

    for (Entry<String, Boolean> observer : roomWebActivityMap.entrySet()) {
      String observerName = observer.getKey();
      boolean isObserverActive = observer.getValue();
      if (isObserverActive) {
        String observerBrowserKey = getBrowserKey(observerName, room);

        for (Entry<String, Boolean> subject : roomWebActivityMap.entrySet()) {
          String subjectName = subject.getKey();
          boolean isSubjectActive = subject.getValue();

          String videoElementId = getBrowserVideoStreamName(subjectName);
          verifyVideoInBrowser(observerBrowserKey, videoElementId, isSubjectActive);
        }

        if (roomFakeActivityMap != null) {
          for (Entry<String, Boolean> subject : roomFakeActivityMap.entrySet()) {
            String subjectName = subject.getKey();
            boolean isSubjectActive = subject.getValue();

            String videoElementId = getFakeVideoStreamName(subjectName);
            verifyVideoInBrowser(observerBrowserKey, videoElementId, isSubjectActive);
          }
        }

      }
    }

    long endTime = System.nanoTime();
    double duration = ((double) endTime - startTime) / 1_000_000;
    log.debug("Checked room '{}' activity: {}{} - in {} millis", room, roomWebActivityMap,
        roomFakeActivityMap != null && !roomFakeActivityMap.isEmpty()
            ? " & " + roomFakeActivityMap
            : "", duration);
  }

  public void verifyVideoInBrowser(String browserKey, String videoElementId, boolean isActive) {
    if (isActive) {
      log.debug("Verifing element '{}' exists in browser '{}'", videoElementId, browserKey);
      WebElement video = null;
      try {
        video = findElementById(browserKey, videoElementId);
      } catch (Exception e) {
        registerException("verifyVideoExistsInBrowser-" + browserKey, e);
      }
      if (video == null) {
        registerException("verifyVideoExistsInBrowser-" + browserKey, new Exception(
            "Video element '" + videoElementId + "' not found in browser '" + browserKey + "'"));
      } else {
        log.debug("OK - element '{}' found in browser '{}'", videoElementId, browserKey);
      }
    } else {
      log.debug("Verifing element '{}' is missing from browser '{}'", videoElementId, browserKey);
      try {
        waitWhileElement(browserKey, videoElementId);
        log.debug("OK - element '{}' is missing from browser '{}'", videoElementId, browserKey);
      } catch (Exception e) {
        registerException("verifyVideoMissingInBrowser-" + browserKey, e);
      }
    }
  }

  // ---------------- Fake client actions ------------------------------------
  public FakeSession getSession(String room) {
    return sessions.get(room);
  }

  public FakeSession createSession(String room) {
    if (sessions.containsKey(room)) {
      return sessions.get(room);
    }
    FakeSession s = new FakeSession(appWsUrl.toString(), room, fakeKurentoClient);
    FakeSession old = sessions.putIfAbsent(room, s);
    if (old != null) {
      return old;
    }
    return s;
  }

  public void closeSession(String room) {
    FakeSession session = sessions.get(room);
    if (session != null) {
      try {
        session.close();
      } catch (IOException e) {
        log.warn("Error closing session", e);
      }
    }
  }

  public FakeSession removeSession(String room) {
    return sessions.remove(room);
  }

  // --------------------- Lifecycles -----------------------------------

  public WebLifecycleTask createSimpleWebLifecycleTask(final Lifecycle[] users,
      final Map<String, Map<String, Boolean>> webUsersActivityMap,
      final Map<String, Map<String, Boolean>> fakeUsersActivityMap,
      final Map<String, CountDownLatch[]> joinCdl, final Map<String, CountDownLatch[]> publishCdl,
      final Map<String, CountDownLatch[]> playCdl, final Map<String, CountDownLatch[]> leaveCdl) {
    return new WebLifecycleTask() {

      @Override
      public void run(int numUser, String room, int iteration, Browser browser) throws Exception {
        String userName = getBrowserUserName(numUser);
        String userKey = getBrowserKey(numUser, room);
        Map<String, Boolean> webActivityMap = webUsersActivityMap.get(room);
        Map<String, Boolean> fakeActivityMap = fakeUsersActivityMap.get(room);

        log.info("User '{}' is joining room '{}'", userName, room);
        synchronized (browsersLock) {
          joinToRoom(numUser, room, userName);
          webActivityMap.put(userName, true);
          verify(room, webActivityMap, fakeActivityMap);
        }
        log.info("User '{}' joined room '{}'", userName, room);

        joinCdl.get(room)[iteration].countDown();
        await(joinCdl.get(room)[iteration], ROOM_INOUT_AWAIT_TIME_IN_SECONDS, "joinRoom-" + userKey);

        log.info("User '{}' checking media from all browser & fake peers in room '{}'", userName,
            room);
        for (Lifecycle peer : users) {
          if (peer.getRoom().equals(room)) {
            switch (peer.getType()) {
              case FAKE :
                waitForFakeStream(numUser, room, peer.getUserName());
                break;
              case WEB :
                waitForWebStream(numUser, room, peer.getUserName());
                break;
              default :
                break;
            }
          }
        }
        log.info("User '{}' is receiving media from all browser & fake peers in room '{}'",
            userName, room);

        publishCdl.get(room)[iteration].countDown();
        await(publishCdl.get(room)[iteration], ROOM_INOUT_AWAIT_TIME_IN_SECONDS, "publish-"
            + userKey);

        sleep(PLAY_TIME);

        playCdl.get(room)[iteration].countDown();
        await(playCdl.get(room)[iteration], ROOM_INOUT_AWAIT_TIME_IN_SECONDS, "playTime-" + userKey);

        log.info("User '{}' is exiting from room '{}'", userName, room);
        synchronized (browsersLock) {
          exitFromRoom(numUser, room);
          webActivityMap.put(userName, false);
          verify(room, webActivityMap, fakeActivityMap);
        }
        log.info("User '{}' exited from room '{}'", userName, room);

        leaveCdl.get(room)[iteration].countDown();
        await(leaveCdl.get(room)[iteration], ROOM_INOUT_AWAIT_TIME_IN_SECONDS, "leaveRoom-"
            + userKey);
      }
    };
  }

  public FakeLifecycleTask createSimpleFakeLifecycleTask(final Lifecycle[] users,
      final Map<String, Map<String, Boolean>> webUsersActivityMap,
      final Map<String, Map<String, Boolean>> fakeUsersActivityMap,
      final Map<String, CountDownLatch[]> joinCdl, final Map<String, CountDownLatch[]> publishCdl,
      final Map<String, CountDownLatch[]> playCdl, final Map<String, CountDownLatch[]> leaveCdl) {
    return new FakeLifecycleTask() {
      @Override
      public void run(int numUser, String room, int iteration, CountDownLatch activeLiveLatch,
          String mediaUri, FakeSession session) throws Exception {
        String userName = getFakeUserName(numUser);
        String userKey = getFakeKey(numUser, room);
        Map<String, Boolean> webActivityMap = webUsersActivityMap.get(room);
        Map<String, Boolean> fakeActivityMap = fakeUsersActivityMap.get(room);

        log.info("User '{}' is joining room '{}'", userName, room);
        synchronized (browsersLock) {
          session.newParticipant(userName, getPlaySourcePath(userName, mediaUri, testFiles), true,
              true);
          fakeActivityMap.put(userName, true);
          verify(room, webActivityMap, fakeActivityMap);
        }
        log.info("User '{}' joined room '{}'", userName, room);

        joinCdl.get(room)[iteration].countDown();
        await(joinCdl.get(room)[iteration], ROOM_INOUT_AWAIT_TIME_IN_SECONDS, "joinRoom-" + userKey);

        log.info("User '{}' waiting for ACTIVE_LIVE", userName, room);
        session.getParticipant(userName).waitForActiveLive(activeLiveLatch);
        await(activeLiveLatch, ACTIVE_LIVE_TOTAL_TIMEOUT_IN_SECONDS, "waitForActiveLive-" + userKey);

        Set<String> roomPeers = new HashSet<String>();
        for (Lifecycle peer : users) {
          if (peer.getRoom().equals(room)) {
            roomPeers.add(peer.getUserName());
          }
        }
        assertEquals("Fake client's peers don't match the room participants", roomPeers, session
            .getParticipant(userName).getPeers());
        log.info("User '{}' has all peer connections in ACTIVE_LIVE", userName, room);

        // connections established correctly
        publishCdl.get(room)[iteration].countDown();
        await(publishCdl.get(room)[iteration], ROOM_INOUT_AWAIT_TIME_IN_SECONDS, "publish-"
            + userKey);

        sleep(PLAY_TIME);

        playCdl.get(room)[iteration].countDown();
        await(playCdl.get(room)[iteration], ROOM_INOUT_AWAIT_TIME_IN_SECONDS, "playTime-" + userKey);

        log.info("User '{}' is exiting from room '{}'", userName, room);
        synchronized (browsersLock) {
          session.getParticipant(userName).leaveRoom();
          fakeActivityMap.put(userName, false);
          verify(room, webActivityMap, fakeActivityMap);
        }
        log.info("User '{}' exited from room '{}'", userName, room);

        leaveCdl.get(room)[iteration].countDown();
        await(leaveCdl.get(room)[iteration], ROOM_INOUT_AWAIT_TIME_IN_SECONDS, "leaveRoom-"
            + userKey);
      }
    };
  }

  // ---------------------- Test sketches ---------------------------

  protected void simpleUsersTest(final Lifecycle[] users) {

    Map<String, Map<String, Boolean>> webUsersActivityMap = getWebActivityMapByRoomAndUserName(users);
    Map<String, Map<String, Boolean>> fakeUsersActivityMap = getFakeActivityMapByRoomAndUserName(users);

    final Map<String, CountDownLatch[]> joinCdl = createRoomsCdl(ITERATIONS, users);
    final Map<String, CountDownLatch[]> publishCdl = createRoomsCdl(ITERATIONS, users);
    final Map<String, CountDownLatch[]> playCdl = createRoomsCdl(ITERATIONS, users);
    final Map<String, CountDownLatch[]> leaveCdl = createRoomsCdl(ITERATIONS, users);

    parallelLifecycles(
        ITERATIONS,
        users,
        createSimpleWebLifecycleTask(users, webUsersActivityMap, fakeUsersActivityMap, joinCdl,
            publishCdl, playCdl, leaveCdl),
            createSimpleFakeLifecycleTask(users, webUsersActivityMap, fakeUsersActivityMap, joinCdl,
                publishCdl, playCdl, leaveCdl));
  }

  // -------------------- Misc ---------------------------------------

  public void await(CountDownLatch waitLatch, long actionTimeoutInSeconds, String action) {
    boolean timedout = false;
    long start = System.currentTimeMillis();
    try {
      timedout = !waitLatch.await(actionTimeoutInSeconds, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      log.warn("Interrupted when waiting for '{}' (max {}s)", action, actionTimeoutInSeconds, e);
      registerException(action, e);
    }
    if (timedout) {
      log.warn("'{}' timed out (max {}s)", action, actionTimeoutInSeconds);
      registerException(action, new Exception("Timeout waiting for '" + action + "' (max "
          + actionTimeoutInSeconds + "s)"));
    } else {
      log.debug("Finished waiting for '{}' after {}s", action,
          (System.currentTimeMillis() - start) / 1000.0);
    }
  }

  /**
   * This method throws a {@link RuntimeException}.
   *
   * @param exKey
   * @param ex
   */
  public void registerException(String exKey, Exception ex) {
    execExceptions.put(exKey, ex);
    throw new RuntimeException("Failed to " + exKey);
  }

  public void failWithExceptions() {
    if (!failed && !execExceptions.isEmpty()) {
      failed = true;
      StringBuffer sb = new StringBuffer();
      log.warn("\n+-------------------------------------------------------+\n"
          + "|   Failing because of the following test errors:       |\n"
          + "+-------------------------------------------------------+");
      for (String exKey : execExceptions.keySet()) {
        Exception e = execExceptions.get(exKey);
        log.warn("Error on '{}'", exKey, e);
        sb.append(exKey).append(" - ").append(e.getMessage()).append("\n");
      }
      sb.append("Check logs for more details");
      log.warn("\n+-------------------------------------------------------+\n"
          + "|   End of errors list                                  |\n"
          + "+-------------------------------------------------------+");
      Assert.fail(sb.toString());
    }
  }
}
