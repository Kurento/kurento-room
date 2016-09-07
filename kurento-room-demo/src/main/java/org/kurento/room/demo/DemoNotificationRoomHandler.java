package org.kurento.room.demo;

import java.util.SortedMap;

import org.kurento.client.Continuation;
import org.kurento.client.Filter;
import org.kurento.module.markerdetector.ArMarkerdetector;
import org.kurento.room.api.UserNotificationService;
import org.kurento.room.internal.DefaultNotificationRoomHandler;
import org.kurento.room.internal.Participant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DemoNotificationRoomHandler extends DefaultNotificationRoomHandler {

  private static final Logger log = LoggerFactory.getLogger(DemoNotificationRoomHandler.class);
  private SortedMap<Integer, String> markerUrls;

  public DemoNotificationRoomHandler(UserNotificationService notifService) {
    super(notifService);
  }

  @Override
  public void updateFilter(String roomName, Participant participant, String filterId,
      String state) {
    Integer newState = -1;

    if (state != null) {
      newState = Integer.parseInt(state);
    }

    String url = markerUrls.get(newState);

    if (url == null) {
      log.info("Removing {} filter from participant {}", filterId, participant.getId());
      participant.disableFilterelement(filterId, false);
      return;
    }

    ArMarkerdetector newFilter;
    Filter filter = participant.getFilterElement(filterId);

    if (filter == null) {
      newFilter = new ArMarkerdetector.Builder(participant.getPipeline()).build();
      log.info("New {} filter for participant {}", filterId, participant.getId());
      participant.addFilterElement(filterId, newFilter);
    } else {
      log.info("Reusing {} filter in participant {}", filterId, participant.getId());
      newFilter = (ArMarkerdetector) filter;
    }

    participant.enableFilterelement(filterId);

    newFilter.setOverlayImage(url, new Continuation<Void>() {
      @Override
      public void onSuccess(Void result) throws Exception {

      }

      @Override
      public void onError(Throwable cause) throws Exception {

      }
    });
  }

  @Override
  public String getNextFilterState(String filterId, String oldState) {
    Integer currentUrlIndex;

    if (oldState == null) {
      currentUrlIndex = -1;
    } else {
      currentUrlIndex = Integer.parseInt(oldState);
    }

    Integer nextIndex = -1; // disable filter

    if (currentUrlIndex < markerUrls.firstKey()) {
      nextIndex = markerUrls.firstKey(); // enable filter using first URL
    } else if (currentUrlIndex < markerUrls.lastKey()) {
      nextIndex = markerUrls.tailMap(currentUrlIndex + 1).firstKey();
    }

    return nextIndex.toString();
  }

  public void setMarkerUrls(SortedMap<Integer, String> markerUrls) {
    this.markerUrls = markerUrls;
  }
}
