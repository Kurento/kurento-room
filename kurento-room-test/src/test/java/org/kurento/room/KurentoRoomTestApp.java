package org.kurento.room;

import java.io.IOException;

import org.kurento.commons.ConfigFileFinder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(KurentoRoomServerApp.class)
public class KurentoRoomTestApp {
	
	public final static String CONFIG_TEST_FILENAME = "/kroomtest.conf.json";
	
	static {
		try {
			System.setProperty("configFilePath", ConfigFileFinder
					.getPathInClasspath(CONFIG_TEST_FILENAME).toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		SpringApplication.run(KurentoRoomTestApp.class, args);
	}
}
