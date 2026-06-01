package JAVA_project;

/*
 * 투두리스트(할 일) 정보 저장
 * 방 번호, 내용, 완료 여부
 */
public class TodoDTO {

    private int num;

    // 어느 방의 할 일인지 저장
    private int roomnum;

    // 할 일 내용
    private String content;

    // 완료 여부
    private boolean completed;

    public TodoDTO(
            int num,
            int roomnum,
            String content,
            boolean completed
    ) {
        this.num = num;
        this.roomnum = roomnum;
        this.content = content;
        this.completed = completed;
    }

    public int getNum() {
        return num;
    }
    
    public int getRoomnum() {
        return roomnum;
    }


    public String getContent() {
        return content;
    }

    public boolean isCompleted() {
        return completed;
    }


    @Override
    public String toString() {

        if(completed) {
            return "[완료] " + content;
        }

        return "[진행중] " + content;
    }
}