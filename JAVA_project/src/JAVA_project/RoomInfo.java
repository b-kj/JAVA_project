package JAVA_project;

public class RoomInfo {
    int roomnum;
    String roomName;

    public RoomInfo(int roomnum, String roomName) {
        this.roomnum = roomnum;
        this.roomName = roomName;
    }

    @Override
    public String toString() {
        return roomName;
    }
}
