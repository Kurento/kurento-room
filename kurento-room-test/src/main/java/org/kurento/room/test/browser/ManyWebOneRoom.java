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
public abstract class ManyWebOneRoom extends RoomTest<WebPage> {

  public final static Lifecycle[] USERS = { new WebLifecycle(0, DEFAULT_ROOM),
    new WebLifecycle(1, DEFAULT_ROOM), new WebLifecycle(2, DEFAULT_ROOM),
    new WebLifecycle(3, DEFAULT_ROOM) };

  @Override
  public void setupBrowserTest() throws InterruptedException {
    super.setupBrowserTest();
    ITERATIONS = 2;
    PLAY_TIME = 5;
  }

  /**
   * Test scenario:
   * <ol>
   * <li>join 4 browsers</li>
   * <li>play</li>
   * <li>exit room</li>
   * <li>repeat...</li>
   * </ol>
   */
  @Test
  public void test() {
    simpleUsersTest(USERS);
  }
}
