package org.kurento.room.demo;

import org.kurento.room.RoomManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

	private static final Logger log = LoggerFactory.getLogger(DemoController.class);
	
	@Autowired
	private RoomManager roomManager;

	@RequestMapping("/close")
	public void getAllRooms(@RequestParam("room") String room) {
		log.warn("Closing the room '{}'", room);
		roomManager.closeRoom(room);
	}
}
