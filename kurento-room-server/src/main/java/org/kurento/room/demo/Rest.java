package org.kurento.room.demo;

import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Raquel Díaz González
 */
@RestController
public class Rest {

    @Autowired
    private RoomManager roomManager;

    @RequestMapping("/getAllRooms")
    public Set<String> getAllRooms() {
        return roomManager.getAllRooms().keySet();
    }
}
