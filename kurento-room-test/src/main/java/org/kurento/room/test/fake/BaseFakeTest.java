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
package org.kurento.room.test.fake;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.kurento.client.KurentoClient;
import org.kurento.client.KurentoConnectionListener;
import org.kurento.commons.PropertiesManager;
import org.kurento.room.test.fake.util.FakeSession;
import org.kurento.test.base.KurentoTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 *
 */
public class BaseFakeTest extends KurentoTest {

	public interface Task {
		public void exec(int numTask) throws Exception;
	}

	private static final long TASKS_TIMEOUT_IN_MINUTES = 15 * 60;

	protected static Logger log = LoggerFactory.getLogger(BaseFakeTest.class);

	private volatile KurentoClient kurento;

	protected static String appUrl;

	private static final String KMS_TEST_WS_URI = PropertiesManager
			.getProperty("kms.test.uri", "ws://localhost:8888/kurento");

	private static String serverPort = PropertiesManager.getProperty(
			"server.port", "8080");
	private static String serverAddress = PropertiesManager.getProperty(
			"server.address", "127.0.0.1");
	protected static String serverUriBase = "http://" + serverAddress + ":"
			+ serverPort;

	protected static String serviceUrl = "ws://" + serverAddress + ":"
			+ serverPort;

	protected static final String BASIC_ROOM_APP_URL = serverUriBase
			+ "/room.html";
	protected static final String DEMO_ROOM_APP_URL = serverUriBase;

	private static final String ROOM_NAME = "room";
	protected String roomName;
	protected static SecureRandom random = new SecureRandom();

	private ConcurrentMap<String, FakeSession> sessions =
			new ConcurrentHashMap<String, FakeSession>();

	public BaseFakeTest(Logger log) {
		if (log != null)
			BaseFakeTest.log = log;
	}

	@BeforeClass
	public static void setupClass() {
		appUrl = BASIC_ROOM_APP_URL;
	}

	@Before
	public void setup() {
		roomName = ROOM_NAME + random.nextInt(9999);
		log.info("Will connect to WS of room app: {}", serviceUrl);
	}

	@After
	public void tearDown() {
		for (FakeSession s : sessions.values())
			try {
				s.close();
			} catch (IOException e) {
				log.warn("Error closing session", e);
			}
		if (kurento != null) {
			kurento.destroy();
			log.info("Closed testing KurentoClient");
		}
	}

	protected synchronized KurentoClient getKurento() {

		if (kurento == null) {
			kurento =
					KurentoClient.create(KMS_TEST_WS_URI,
							new KurentoConnectionListener() {
								@Override
								public void connected() {}

								@Override
								public void connectionFailed() {}

								@Override
								public void disconnected() {
									kurento = null;
								}

								@Override
								public void reconnected(boolean sameServer) {}
							});
		}

		return kurento;
	}

	protected FakeSession getSession(String room) {
		return sessions.get(room);
	}

	protected FakeSession createSession(String room) {
		if (sessions.containsKey(room))
			return sessions.get(room);
		FakeSession s = new FakeSession(serviceUrl, room, getKurento());
		FakeSession old = sessions.putIfAbsent(room, s);
		if (old != null)
			return old;
		s.createPipeline();
		return s;
	}

	protected void parallelTasks(int numThreads, final String thPrefix,
			String taskName, Map<String, Exception> exceptions, final Task task) {
		ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
		ExecutorCompletionService<Void> exec =
				new ExecutorCompletionService<>(threadPool);
		List<Future<Void>> futures = new ArrayList<>();

		try {
			for (int i = 0; i < numThreads; i++) {
				final int numTask = i;
				futures.add(exec.submit(new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						String thname = Thread.currentThread().getName();
						Thread.currentThread().setName(thPrefix + numTask);
						task.exec(numTask);
						Thread.currentThread().setName(thname);
						return null;
					}
				}));
			}
			for (int i = 0; i < numThreads; i++) {
				try {
					exec.take().get();
				} catch (ExecutionException e) {
					log.debug("Execution exception", e);
					exceptions.put(taskName + "-" + thPrefix + i, e);
				} catch (InterruptedException e) {
					log.error(
							"Interrupted while waiting for execution of task{}",
							i, e);
				}
			}
		} finally {
			threadPool.shutdownNow();
			try {
				threadPool.awaitTermination(TASKS_TIMEOUT_IN_MINUTES,
						TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				log.warn(
						"Tasks were executed more than {} minutes. Stopping it",
						TASKS_TIMEOUT_IN_MINUTES);
				threadPool.shutdownNow();
			}
		}
	}
	
	protected void failWithExceptions(Map<String, Exception> exceptions) {
		if (!exceptions.isEmpty()) {
			StringBuffer sb = new StringBuffer();
			for (String exKey : exceptions.keySet()) {
				Exception e = exceptions.get(exKey);
				log.error("Error on '{}'", exKey, e);
				sb.append(exKey).append(" - ").append(e.getMessage())
						.append("\n");
			}
			sb.append("Check logs for more details");
			Assert.fail(sb.toString());
		}
	}
}
