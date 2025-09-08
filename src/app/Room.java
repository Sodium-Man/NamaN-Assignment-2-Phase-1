package app;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Room implements Serializable {
    private final String roomId;
    private final List<Bed> beds = new ArrayList<>();

    public Room(String roomId) { this.roomId = roomId; }

    public String getRoomId() { return roomId; }
    public List<Bed> getBeds() { return beds; }

    public Bed getVacantBed() {
        return beds.stream().filter(Bed::isVacant).findFirst().orElse(null);
    }

    @Override
    public String toString() {
        return "Room{" + roomId + ", beds=" + beds + "}";
    }
}
