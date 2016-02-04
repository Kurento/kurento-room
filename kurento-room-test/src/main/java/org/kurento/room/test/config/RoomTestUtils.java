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
package org.kurento.room.test.config;

import static org.kurento.room.test.config.RoomTestConfiguration.USER_BROWSER_PREFIX;
import static org.kurento.room.test.config.RoomTestConfiguration.USER_FAKE_PREFIX;
import static org.kurento.test.base.KurentoTest.log;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.kurento.room.test.config.Lifecycle.Type;

/**
 * Static utility methods.
 *
 * @author Radu Tom Vlad (rvlad@naevatec.com)
 * @since 6.3.2
 */
public class RoomTestUtils {

  public static void sleep(long seconds) {
    try {
      Thread.sleep(seconds * 1000);
    } catch (InterruptedException e) {
      log.warn("Interrupted while sleeping {}seconds", seconds, e);
    }
  }

  // ---------------- Web users ----------------------------

  public static String getBrowserUserName(int index) {
    return USER_BROWSER_PREFIX + index;
  }

  public static String getBrowserKey(int index, String room) {
    return room + "-" + getBrowserUserName(index);
  }

  public static String getBrowserKey(String userName, String room) {
    return room + "-" + userName;
  }

  public static String getBrowserStreamName(String userName) {
    return userName + "_webcam";
  }

  public static String getBrowserVideoStreamName(String userName) {
    return "video-" + getBrowserStreamName(userName);
  }

  public static String getBrowserNativeStreamName(String userName) {
    return "native-" + getBrowserVideoStreamName(userName);
  }

  // ---------------- Fake users ----------------------------

  public static String getFakeUserName(int index) {
    return USER_FAKE_PREFIX + index;
  }

  public static String getFakeKey(int index, String room) {
    return room + "-" + getFakeUserName(index);
  }

  public static String getFakeStreamName(String userName) {
    return userName + "_webcam";
  }

  public static String getFakeVideoStreamName(String userName) {
    return "video-" + getFakeStreamName(userName);
  }

  public static String getFakeNativeStreamName(String userName) {
    return "native-" + getFakeVideoStreamName(userName);
  }

  // ---------------- Lifecycle utils -----------------------

  public static int getNumUsersByTypeAndRoom(String room, Lifecycle[] users, Lifecycle.Type type) {
    int numUsers = 0;
    for (int i = 0; i < users.length; i++) {
      if (users[i].getType().equals(type) && (room == null || room.equals(users[i].getRoom()))) {
        numUsers++;
      }
    }
    return numUsers;
  }

  public static int getNumUsersByRoom(String room, Lifecycle[] users) {
    int numUsers = 0;
    for (int i = 0; i < users.length; i++) {
      if (room.equals(users[i].getRoom())) {
        numUsers++;
      }
    }
    return numUsers;
  }

  public static int getNumWebUsersByRoom(String room, Lifecycle[] users) {
    return getNumUsersByTypeAndRoom(room, users, Type.WEB);
  }

  public static int getNumFakeUsersByRoom(String room, Lifecycle[] users) {
    return getNumUsersByTypeAndRoom(room, users, Type.FAKE);
  }

  public static int getNumWebUsers(Lifecycle[] users) {
    return getNumUsersByTypeAndRoom(null, users, Type.WEB);
  }

  public static int getNumFakeUsers(Lifecycle[] users) {
    return getNumUsersByTypeAndRoom(null, users, Type.FAKE);
  }

  public static Set<String> getUniqueRooms(Lifecycle[] users) {
    Set<String> rooms = new HashSet<String>();
    for (int i = 0; i < users.length; i++) {
      rooms.add(users[i].getRoom());
    }
    return rooms;
  }

  public static Map<String, Map<String, Boolean>> getActivityMapByRoomAndUserName(
      Lifecycle[] users, Lifecycle.Type type) {
    Map<String, Map<String, Boolean>> activityMap = new HashMap<String, Map<String, Boolean>>();
    for (Lifecycle user : users) {
      if (user.getType().equals(type)) {
        if (!activityMap.containsKey(user.getRoom())) {
          activityMap.put(user.getRoom(), new HashMap<String, Boolean>());
        }
        activityMap.get(user.getRoom()).put(user.getUserName(), false);
      }
    }
    return activityMap;
  }

  public static Map<String, Map<String, Boolean>> getWebActivityMapByRoomAndUserName(
      Lifecycle[] users) {
    return getActivityMapByRoomAndUserName(users, Type.WEB);
  }

  public static Map<String, Map<String, Boolean>> getFakeActivityMapByRoomAndUserName(
      Lifecycle[] users) {
    return getActivityMapByRoomAndUserName(users, Type.FAKE);
  }

  public static Map<String, CountDownLatch[]> createRoomsCdl(int iterations, Lifecycle[] users) {
    Map<String, CountDownLatch[]> latch = new HashMap<String, CountDownLatch[]>();
    for (String room : getUniqueRooms(users)) {
      latch.put(room, createCdl(iterations, getNumUsersByRoom(room, users)));
    }
    return latch;
  }

  public static CountDownLatch[] createCdl(int numLatches, int numUsers) {
    final CountDownLatch[] cdl = new CountDownLatch[numLatches];
    for (int i = 0; i < numLatches; i++) {
      cdl[i] = new CountDownLatch(numUsers);
    }
    return cdl;
  }

  public static String getPlaySourcePath(String userName, String relativePath, String basePath)
      throws Exception {
    if (relativePath == null) {
      throw new Exception("Null play path for user " + userName);
    }
    if (!basePath.startsWith("http://") && !basePath.startsWith("https://")
        && !basePath.startsWith("file://")) {
      basePath = "file://" + basePath;
    }
    URI playerUri = null;
    try {
      playerUri = new URI(basePath + relativePath);
    } catch (URISyntaxException e) {
      throw new Exception("Unable to construct player URI for user " + userName
          + " from base path " + basePath + " and file " + relativePath);
    }
    String fullPlayPath = playerUri.toString();
    log.debug("Fake user '{}': using play URI {}", userName, fullPlayPath);
    return fullPlayPath;
  }
}
