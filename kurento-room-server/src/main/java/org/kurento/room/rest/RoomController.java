package org.kurento.room.rest;

import java.util.Set;

import org.kurento.room.NotificationRoomManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Raquel Díaz González
 */
@RestController
public class RoomController {

	@Autowired
	private NotificationRoomManager roomManager;

	@RequestMapping("/getAllRooms")
	public Set<String> getAllRooms() {
		return roomManager.getRooms();
	}
}
