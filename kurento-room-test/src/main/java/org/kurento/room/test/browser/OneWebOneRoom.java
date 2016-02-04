package org.kurento.room.test.browser;

import static org.kurento.room.test.config.RoomTestConfiguration.DEFAULT_ROOM;

import org.junit.Test;
import org.kurento.room.test.RoomTest;
import org.kurento.room.test.config.Lifecycle;
import org.kurento.room.test.config.WebLifecycle;
import org.kurento.test.browser.WebPage;

/**
 * Simple integration test (basic version).
 *
 * @author Radu Tom Vlad (rvlad@naevatec.com)
 *
 * @since 6.3.2
 */
public abstract class OneWebOneRoom extends RoomTest<WebPage> {

  public final static Lifecycle[] USERS = { new WebLifecycle(0, DEFAULT_ROOM) };

  @Override
  public void setupBrowserTest() throws InterruptedException {
    super.setupBrowserTest();
    ITERATIONS = 3;
    PLAY_TIME = 5;
  }

  /**
   * Test scenario:
   * <ol>
   * <li>join 1 browser</li>
   * <li>play</li>
   * <li>exit room</li>
   * <li>repeat...</li>
   * </ol>
   */
  @Test
  public void testOneBrowser() {
    simpleUsersTest(USERS);
  }
}
