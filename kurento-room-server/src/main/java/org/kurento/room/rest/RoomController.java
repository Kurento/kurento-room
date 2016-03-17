package org.kurento.room.rest;

import static org.kurento.commons.PropertiesManager.getProperty;

import java.util.Set;

import org.kurento.room.NotificationRoomManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Raquel Díaz González
 */
@RestController
public class RoomController {

  private static final int UPDATE_SPEAKER_INTERVAL_DEFAULT = 1800;
  private static final int THRESHOLD_SPEAKER_DEFAULT = -50;

  @Autowired
  private NotificationRoomManager roomManager;

  @RequestMapping("/getAllRooms")
  public Set<String> getAllRooms() {
    return roomManager.getRooms();
  }

  @RequestMapping("/getUpdateSpeakerInterval")
  public Integer getUpdateSpeakerInterval() {
    return Integer.valueOf(getProperty("updateSpeakerInterval", UPDATE_SPEAKER_INTERVAL_DEFAULT));
  }

  @RequestMapping("/getThresholdSpeaker")
  public Integer getThresholdSpeaker() {
    return Integer.valueOf(getProperty("thresholdSpeaker", THRESHOLD_SPEAKER_DEFAULT));
  }
}
