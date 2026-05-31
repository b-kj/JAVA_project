package dto;

import java.time.LocalDate;


public class TodoDTO {
    private String title;
    private LocalDate deadline;
    private boolean completed;

    public TodoDTO(String title, LocalDate deadline) {
        this.title = title;
        this.deadline = deadline;
        this.completed = false;
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
