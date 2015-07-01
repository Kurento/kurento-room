package org.kurento.room.demo;

import org.kurento.room.RoomManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

	private static final Logger log = LoggerFactory
			.getLogger(DemoController.class);

	@Autowired
	private RoomManager roomManager;

	@ResponseStatus(value = HttpStatus.NOT_FOUND)
	public class ResourceNotFoundException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public ResourceNotFoundException(String msg) {
			super(msg);
		}
	}

	@RequestMapping("/close")
	public void closeRoom(@RequestParam("room") String room) {
		log.warn("Trying to close the room '{}'", room);
		if (!roomManager.getRooms().contains(room)) {
			log.warn("Unable to close room '{}', not found.", room);
			throw new ResourceNotFoundException("Room '" + room + "' not found");
		}
		roomManager.closeRoom(room);
	}
}
