package org.kurento.room.test;

import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.kurento.client.internal.KmsProvider;
import org.kurento.client.internal.NotEnoughResourcesException;
import org.kurento.room.KurentoRoomServerApp;
import org.kurento.room.NotificationRoomManager;
import org.kurento.room.RoomManager;
import org.kurento.room.api.KurentoClientSessionInfo;
import org.kurento.room.internal.DefaultKurentoClientSessionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

import com.google.common.base.StandardSystemProperty;

public class AutodiscoveryKmsUrlTest {

	private static final Logger log = LoggerFactory
			.getLogger(AutodiscoveryKmsUrlTest.class);

	public static BlockingQueue<Boolean> queue = new ArrayBlockingQueue<>(10);

	public static class TestKmsUrlProvider implements KmsProvider {

		@Override
		public String reserveKms(String id, int loadPoints)
				throws NotEnoughResourcesException {
			if (loadPoints == 50) {
				queue.add(true);
			} else {
				log.error("getKmsUrl called with {} instead of 50", loadPoints);
				queue.add(false);
			}

			return "ws://fakeUrl";
		}

		@Override
		public String reserveKms(String id) throws NotEnoughResourcesException {

			log.error("getKmsUrl called without load points");
			queue.add(false);

			return "ws://fakeUrl";
		}

		@Override
		public void releaseKms(String id) throws NotEnoughResourcesException {
			// TODO Auto-generated method stub

		}
	}

	@Test
	public void test() throws IOException {

		Path backup = null;

		Path configFile = Paths.get(StandardSystemProperty.USER_HOME.value(),
				".kurento", "config.properties");

		System.setProperty("kms.uris", "[\"autodiscovery\"]");

		try {

			if (Files.exists(configFile)) {

				backup = configFile.getParent()
						.resolve("config.properties.old");

				Files.move(configFile, backup);
			}

			Files.createDirectories(configFile.getParent());

			try (BufferedWriter writer = Files.newBufferedWriter(configFile,
					StandardCharsets.UTF_8)) {
				writer.write("kms.url.provider: "
						+ TestKmsUrlProvider.class.getName() + "\r\n");
			}

			ConfigurableApplicationContext app = KurentoRoomServerApp
					.start(new String[] { "--server.port=7777" });

			NotificationRoomManager notifRoomManager = app
					.getBean(NotificationRoomManager.class);

			final RoomManager roomManager = notifRoomManager.getRoomManager();

			final KurentoClientSessionInfo kcSessionInfo = new DefaultKurentoClientSessionInfo(
					"participantId", "roomName");

			new Thread(new Runnable() {
				public void run() {
					roomManager.joinRoom("userName", "roomName", false,
							kcSessionInfo, "participantId");
				}
			}).start();

			try {
				Boolean result = queue.poll(10, TimeUnit.SECONDS);

				if (result == null) {
					fail("Event in KmsUrlProvider not called");
				} else {
					if (!result) {
						fail("Test failed");
					}
				}

			} catch (InterruptedException e) {
				fail("KmsUrlProvider was not called");
			}

		} finally {

			Files.delete(configFile);

			if (backup != null) {
				Files.move(backup, configFile);
			}
		}
	}

}
