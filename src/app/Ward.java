package app;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Ward implements Serializable {
    private final String wardId;
    private final String wardName;  // Add this field
    private final List<Room> rooms = new ArrayList<>();

    // Updated constructor
    public Ward(String wardId, String wardName) {
        this.wardId = wardId;
        this.wardName = wardName;
    }

    public String getWardId() { return wardId; }
    public String getWardName() { return wardName; }  // Add this method
    public List<Room> getRooms() { return rooms; }

    // Add other Ward-specific methods here
    public void addRoom(Room room) {
        rooms.add(room);
    }

    public Room findRoom(String roomId) {
        return rooms.stream()
                .filter(room -> room.getRoomId().equals(roomId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public String toString() {
        return "Ward{" + wardId + " (" + wardName + "), rooms=" + rooms + "}";
    }
}