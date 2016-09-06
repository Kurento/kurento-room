package org.kurento.room.api;

import org.kurento.client.Filter;

/**
 * Manages filters
 */
public interface FilterUpdater {

  String getFilterId();
  String getStateId();
  Filter updateFilter (Filter filter, String state);
  String getNewState (String oldState);
}
