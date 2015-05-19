package org.kurento.room;

import org.kurento.room.KurentoRoomServerApp;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(KurentoRoomServerApp.class)
public class KurentoRoomTestApp {
	public static void main(String[] args) {
		SpringApplication.run(KurentoRoomTestApp.class, args);
	}
}
