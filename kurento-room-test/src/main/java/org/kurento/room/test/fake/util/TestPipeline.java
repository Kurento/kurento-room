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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.kurento.client.Continuation;
import org.kurento.client.ErrorEvent;
import org.kurento.client.EventListener;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.room.internal.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps a pipeline used to execute fake participants in a test session (room).
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class TestPipeline {
	private static Logger log = LoggerFactory.getLogger(TestPipeline.class);

	private KurentoClient kurento;
	private MediaPipeline pipeline;
	private CountDownLatch pipelineLatch = new CountDownLatch(1);
	private Object pipelineCreateLock = new Object();
	private Object pipelineReleaseLock = new Object();
	private volatile boolean pipelineReleased = false;
	private String description = "default";
	private String room = "room";

	public TestPipeline(KurentoClient kurento, String room,
			String pipeDescription) {
		this.kurento = kurento;
		if (room != null)
			this.room = room;
		if (pipeDescription != null)
			this.description = pipeDescription;
	}

	public TestPipeline(KurentoClient kurento, String room) {
		this(kurento, room, null);
	}

	public MediaPipeline getPipeline() {
		try {
			pipelineLatch.await(Room.ASYNC_LATCH_TIMEOUT, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return this.pipeline;
	}

	public void createPipeline() {
		synchronized (pipelineCreateLock) {
			if (pipeline != null)
				return;
			log.info("Session '{}': Creating MediaPipeline-{}", room,
					description);
			try {
				kurento.createMediaPipeline(new Continuation<MediaPipeline>() {
					@Override
					public void onSuccess(MediaPipeline result)
							throws Exception {
						pipeline = result;
						pipelineLatch.countDown();
						log.debug("Session '{}': Created MediaPipeline-{}",
								room, description);
					}

					@Override
					public void onError(Throwable cause) throws Exception {
						pipelineLatch.countDown();
						log.error(
								"Session '{}': Failed to create MediaPipeline-{}",
								room, description, cause);
					}
				});
			} catch (Exception e) {
				log.error("Unable to create MediaPipeline-{} for Session '{}'",
						description, room, e);
				pipelineLatch.countDown();
			}
			if (getPipeline() == null)
				throw new RuntimeException("Unable to create MediaPipeline-"
						+ description + " for session '" + room + "'");

			pipeline.addErrorListener(new EventListener<ErrorEvent>() {
				@Override
				public void onEvent(ErrorEvent event) {
					String desc =
							event.getType() + ": " + event.getDescription()
									+ "(errCode=" + event.getErrorCode() + ")";
					log.warn(
							"Session '{}': Pipeline error encountered for MediaPipeline-{}: {}",
							room, description, desc);
				}
			});
		}
	}

	public void closePipeline() {
		synchronized (pipelineReleaseLock) {
			if (pipeline == null || pipelineReleased)
				return;
			getPipeline().release();
		}
	}
}
