package JAVA_project;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

public class CalendarDdayFrame extends JFrame {
    private int roomnum;
    private DB db;
    
    // 달력 관련
    private JLabel monthLabel;
    private DefaultTableModel calModel;
    private JTable calTable;
    private YearMonth currentMonth;
    
    // D-Day 리스트 관련
    private DefaultListModel<String> listModel;
    private JList<String> scheduleList;

    public CalendarDdayFrame(int roomnum, DB db) {
        this.roomnum = roomnum;
        this.db = db;
        this.currentMonth = YearMonth.now();

        setTitle("일정 캘린더 및 D-Day (Room: " + roomnum + ")");
        setSize(450, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        /* 1. 상단: 캘린더 패널 */
        JPanel calendarPanel = new JPanel(new BorderLayout());
        calendarPanel.setBorder(BorderFactory.createTitledBorder("이번 달 캘린더"));
        
        // 달력 컨트롤 (이전 달, 다음 달)
        JPanel calControlPanel = new JPanel(new FlowLayout());
        JButton prevBtn = new JButton("<");
        JButton nextBtn = new JButton(">");
        monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        
        prevBtn.addActionListener(e -> changeMonth(-1));
        nextBtn.addActionListener(e -> changeMonth(1));
        
        calControlPanel.add(prevBtn);
        calControlPanel.add(monthLabel);
        calControlPanel.add(nextBtn);
        
        // 달력 테이블 (7열: 일~토)
        String[] days = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
        calModel = new DefaultTableModel(null, days) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        calTable = new JTable(calModel);
        calTable.setRowHeight(40);
        calTable.getTableHeader().setReorderingAllowed(false);
        
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for(int i=0; i<7; i++) {
            calTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane calScroll = new JScrollPane(calTable);
        calScroll.setPreferredSize(new Dimension(400, 200));
        
        calendarPanel.add(calControlPanel, BorderLayout.NORTH);
        calendarPanel.add(calScroll, BorderLayout.CENTER);

        /* 2. 하단: D-Day 리스트 패널 (기존 ScheduleFrame 기능) */
        JPanel ddayPanel = new JPanel(new BorderLayout());
        ddayPanel.setBorder(BorderFactory.createTitledBorder("작업 D-Day 목록"));
        
        listModel = new DefaultListModel<>();
        scheduleList = new JList<>(listModel);
        scheduleList.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        JScrollPane listScroll = new JScrollPane(scheduleList);
        
        JPanel inputPanel = new JPanel(new BorderLayout());
        JTextField titleField = new JTextField();
        JTextField dateField = new JTextField("YYYY-MM-DD");
        JButton addBtn = new JButton("추가");
        
        JPanel fieldPanel = new JPanel(new GridLayout(1, 2));
        fieldPanel.add(titleField);
        fieldPanel.add(dateField);
        inputPanel.add(fieldPanel, BorderLayout.CENTER);
        inputPanel.add(addBtn, BorderLayout.EAST);
        
        ddayPanel.add(listScroll, BorderLayout.CENTER);
        ddayPanel.add(inputPanel, BorderLayout.SOUTH);

        /* 추가 이벤트 */
        addBtn.addActionListener(e -> {
            String title = titleField.getText().trim();
            String dateStr = dateField.getText().trim();
            try {
                LocalDate.parse(dateStr); // 날짜 형식 검사
                db.addSchedule(roomnum, title, dateStr);
                loadScheduleData(); // 리스트 새로고침
                titleField.setText("");
                dateField.setText("YYYY-MM-DD");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "날짜를 YYYY-MM-DD 형식으로 입력하세요.");
            }
        });

        add(calendarPanel, BorderLayout.NORTH);
        add(ddayPanel, BorderLayout.CENTER);

        updateCalendar();
        loadScheduleData();
        
        setVisible(true);
    }

    // 달력 날짜 채우기 로직
    private void updateCalendar() {
        calModel.setRowCount(0); 
        monthLabel.setText(currentMonth.getYear() + "년 " + currentMonth.getMonthValue() + "월");
        
        LocalDate firstDay = currentMonth.atDay(1);
        int dayOfWeek = firstDay.getDayOfWeek().getValue() % 7; // 일요일(0) ~ 토요일(6)
        int daysInMonth = currentMonth.lengthOfMonth();
        
        String[] row = new String[7];
        int col = dayOfWeek;
        
        for (int i = 1; i <= daysInMonth; i++) {
            row[col] = String.valueOf(i);
            col++;
            if (col == 7) {
                calModel.addRow(row);
                row = new String[7];
                col = 0;
            }
        }
        if (col > 0) {
            calModel.addRow(row);
        }
    }

    // 달력 이동 버튼
    private void changeMonth(int offset) {
        currentMonth = currentMonth.plusMonths(offset);
        updateCalendar();
    }

    //  D-Day 계산
    private void loadScheduleData() {
        listModel.clear();
        ArrayList<ScheduleDTO> schedules = db.getSchedules(roomnum);
        LocalDate today = LocalDate.now();

        for (ScheduleDTO s : schedules) {
        	LocalDate deadline = LocalDate.parse(String.valueOf(s.getDeadline()));
            long dDay = ChronoUnit.DAYS.between(today, deadline);
            
            String dDayStr;
            if (dDay > 0) dDayStr = "D-" + dDay;
            else if (dDay == 0) dDayStr = "D-Day";
            else dDayStr = "D+" + Math.abs(dDay) + " (마감됨)";

            listModel.addElement("[ " + dDayStr + " ] " + s.getTitle() + " (" + s.getDeadline() + ")");
        }
    }

}