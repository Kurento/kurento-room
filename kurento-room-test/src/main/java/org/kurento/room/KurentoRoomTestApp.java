package org.kurento.room;

import java.io.IOException;

import org.kurento.commons.ClassPath;
import org.kurento.commons.ConfigFileManager;
import org.kurento.room.test.RoomTest;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Import;

@Import(KurentoRoomServerApp.class)
public class KurentoRoomTestApp {

	static {
		try {
			System.setProperty("configFilePath",
					ClassPath.get(RoomTest.CONFIG_TEST_FILENAME).toString());
		} catch (IOException e) {
			e.printStackTrace();
		}

		ConfigFileManager.loadConfigFile(RoomTest.CONFIG_TEST_FILENAME);
	}

	public static void main(String[] args) {
		SpringApplication.run(KurentoRoomTestApp.class, args);
	}
}
