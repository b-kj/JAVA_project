package JAVA_project;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

public class ScheduleFrame extends JFrame {
    private int roomnum;
    private DB db;
    private DefaultListModel<String> listModel;
    private JList<String> scheduleList;
    private JTextField titleField;
    private JTextField dateField;

    public ScheduleFrame(int roomnum, DB db) {
        this.roomnum = roomnum;
        this.db = db;
        
        setTitle("일정 관리 (Room: " + roomnum + ")");
        setSize(350, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 상단 입력 패널
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        titleField = new JTextField();
        dateField = new JTextField("YYYY-MM-DD");
        JButton addButton = new JButton("추가");

        inputPanel.add(new JLabel(" 일정 내용:"));
        inputPanel.add(titleField);
        inputPanel.add(new JLabel(" 마감일(YYYY-MM-DD):"));
        
        JPanel dateAddPanel = new JPanel(new BorderLayout());
        dateAddPanel.add(dateField, BorderLayout.CENTER);
        dateAddPanel.add(addButton, BorderLayout.EAST);
        inputPanel.add(dateAddPanel);
        
        // 목록 패널
        listModel = new DefaultListModel<>();
        scheduleList = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(scheduleList);

        add(inputPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // 일정 불러오기
        loadSchedules();

        // 추가 버튼 이벤트
        addButton.addActionListener(e -> {
            String title = titleField.getText().trim();
            String dateStr = dateField.getText().trim();
            if(title.isEmpty()) return;
            
            try {
                LocalDate deadline = LocalDate.parse(dateStr);
                db.addSchedule(roomnum, title, deadline);
                loadSchedules(); // 목록 갱신
                titleField.setText("");
                dateField.setText("");
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this, "날짜를 '2026-06-30' 형식으로 입력해주세요.");
            }
        });

        setVisible(true);
    }

    private void loadSchedules() {
        listModel.clear();
        ArrayList<ScheduleDTO> schedules = db.getSchedules(roomnum);
        LocalDate today = LocalDate.now();

        for (ScheduleDTO s : schedules) {
            long dDay = ChronoUnit.DAYS.between(today, s.getDeadline());
            String dDayStr;
            
            if (dDay > 0) dDayStr = "D-" + dDay;
            else if (dDay == 0) dDayStr = "D-Day";
            else dDayStr = "D+" + Math.abs(dDay);

            listModel.addElement(s.getTitle() + " (" + s.getDeadline() + ") [" + dDayStr + "]");
        }
    }
}