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
package org.kurento.room.test.fake.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores the fake WebRTC participants for a session (room). Each participant will have assigned a
 * shared {@link MediaPipeline} which it can use to create media elements and interact with the
 * session. More than one pipelines are allowed for each session.
 *
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class FakeSession implements Closeable {
  private static Logger log = LoggerFactory.getLogger(FakeSession.class);

  private String serviceUrl;
  private String room;
  private KurentoClient defaultKurento;

  private Map<KurentoClient, TestPipeline> pipelines = new ConcurrentHashMap<KurentoClient, TestPipeline>();

  private Map<String, FakeParticipant> participants = new HashMap<String, FakeParticipant>();

  public FakeSession(String serviceUrl, String room, KurentoClient kurento) {
    this.serviceUrl = serviceUrl;
    this.room = room;
    this.defaultKurento = kurento;
    this.pipelines.put(kurento, new TestPipeline(kurento, room));
  }

  @Override
  public void close() throws IOException {
    log.debug("Closing Session '{}'", room);
    for (FakeParticipant p : participants.values()) {
      p.close();
    }
    for (TestPipeline pipeline : this.pipelines.values()) {
      pipeline.closePipeline();
    }
  }

  public void newParticipant(String name, String playerUri, boolean autoMedia, boolean loopMedia) {
    newParticipant(name, playerUri, autoMedia, loopMedia, defaultKurento);
  }

  public void newParticipant(String name, String playerUri, boolean autoMedia, boolean loopMedia,
      KurentoClient kurento) {
    TestPipeline pipeline = getOrCreatePipeline(kurento);
    pipeline.createPipeline();
    FakeParticipant participant = new FakeParticipant(serviceUrl, name, room, playerUri,
        pipeline.getPipeline(), autoMedia, loopMedia);
    participants.put(name, participant);
    participant.joinRoom();
  }

  public FakeParticipant getParticipant(String name) {
    return participants.get(name);
  }

  public void waitForActiveLive(CountDownLatch waitForLatch) {
    for (FakeParticipant p : participants.values()) {
      p.waitForActiveLive(waitForLatch);
    }
  }

  private TestPipeline getOrCreatePipeline(KurentoClient kurento) {
    TestPipeline pipeline = this.pipelines.get(kurento);
    if (pipeline == null) {
      String desc = kurento.getServerManager().getId();
      pipeline = this.pipelines.putIfAbsent(kurento, new TestPipeline(kurento, room, desc));
      if (pipeline != null) {
        log.debug("Pipeline already created for room '{}' and kurento '{}'", room, desc);
      }
      pipeline = this.pipelines.get(kurento);
    }
    return pipeline;
  }
}
