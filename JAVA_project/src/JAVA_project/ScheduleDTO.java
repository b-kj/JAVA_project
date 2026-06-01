package JAVA_project;

import java.time.LocalDate;


public class ScheduleDTO {

    private int num;
    private int roomnum;
    private String title;
    private LocalDate deadline;

    public ScheduleDTO(
            int num,
            int roomnum,
            String title,
            LocalDate deadline
    ) {
        this.num = num;
        this.roomnum = roomnum;
        this.title = title;
        this.deadline = deadline;
    }

    public int getNum() {
        return num;
    }

    public int getRoomnum() {
        return roomnum;
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    @Override
    public String toString() {
        return title + " (" + deadline + ")";
    }
}