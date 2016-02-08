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
public abstract class OneWebManyRoom extends RoomTest<WebPage> {

  public final static Lifecycle[] USERS = { new WebLifecycle(0, DEFAULT_ROOM + "0"),
      new WebLifecycle(0, DEFAULT_ROOM + "1"), new WebLifecycle(0, DEFAULT_ROOM + "2"),
      new WebLifecycle(0, DEFAULT_ROOM + "3") };

  @Override
  public void setupBrowserTest() throws InterruptedException {
    super.setupBrowserTest();
    ITERATIONS = 2;
    PLAY_TIME = 5;
  }

  /**
   * Test scenario:
   * <ol>
   * <li>join 4 browsers, each in a different room</li>
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
