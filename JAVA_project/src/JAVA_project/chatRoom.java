package JAVA_project;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class chatRoom extends JFrame {

    /* --- GUI 컴포넌트 --- */
    JTextArea chatArea = new JTextArea();
    JTextField inputField = new JTextField();
    JButton sendBtn = new JButton("전송");
    JButton workBtn = new JButton("작업 시작");

    /* 멤버 목록 */
    DefaultListModel<String> memberModel = new DefaultListModel<>();
    JList<String> memberList = new JList<>(memberModel);

    /* 캘린더 및 대시보드 컴포넌트 */
    JProgressBar progressBar;
    JLabel monthLabel;
    DefaultTableModel calModel;
    JTable calTable;
    YearMonth currentMonth;
    DefaultListModel<String> ddayModel = new DefaultListModel<>();
    JList<String> ddayList = new JList<>(ddayModel);

    /* 소켓 */
    Socket socket;
    BufferedReader in;
    PrintWriter out;

    /* 정보 */
    Operator o;
    int roomnum;
    String roomName;

    /* 현재 온라인 사용자 */
    Set<String> onlineUsers = new HashSet<>();
    
    /* 작업 상태 */
    boolean workStarted = false;
    boolean working = false;
    long lastInputTime = System.currentTimeMillis();

    /* 사용자 상태 */
    HashMap<String, String> userStatus = new HashMap<>();
    HashMap<String, Long> userTime = new HashMap<>();
    HashMap<String, Long> lastUpdateTime = new HashMap<>();

    /* 생성자 */
    public chatRoom(int roomnum, String roomName, Operator o) {
        this.roomnum = roomnum;
        this.roomName = roomName;
        this.o = o;
        this.currentMonth = YearMonth.now();

        setTitle("[ " + roomnum + " ] " + roomName + " - 프로젝트 대시보드");
        setSize(1000, 750); 
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10)); 

        /* =======================================
           1. 왼쪽 패널: 대시보드 (진행률 + 캘린더 + 디데이)
           ======================================= */
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setPreferredSize(new Dimension(450, 0)); 
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setForeground(new Color(50, 205, 50));
        progressBar.setPreferredSize(new Dimension(0, 30));
        progressBar.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        leftPanel.add(progressBar, BorderLayout.NORTH);

        JPanel calPanel = new JPanel(new BorderLayout());
        calPanel.setBorder(BorderFactory.createTitledBorder("이번 달 캘린더"));
        
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
        
        String[] days = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
        calModel = new DefaultTableModel(null, days) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        calTable = new JTable(calModel);
        calTable.setRowHeight(65); 
        calTable.getTableHeader().setReorderingAllowed(false);
        
        DefaultTableCellRenderer topRenderer = new DefaultTableCellRenderer();
        topRenderer.setVerticalAlignment(JLabel.TOP);
        topRenderer.setHorizontalAlignment(JLabel.CENTER);
        for(int i=0; i<7; i++) {
            calTable.getColumnModel().getColumn(i).setCellRenderer(topRenderer);
        }
        
        JScrollPane calScroll = new JScrollPane(calTable);
        calPanel.add(calControlPanel, BorderLayout.NORTH);
        calPanel.add(calScroll, BorderLayout.CENTER);
        
        JPanel ddayPanel = new JPanel(new BorderLayout());
        ddayPanel.setBorder(BorderFactory.createTitledBorder("다가오는 마감일 (D-Day)"));
        ddayList.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        JScrollPane ddayScroll = new JScrollPane(ddayList);
        ddayScroll.setPreferredSize(new Dimension(0, 150));
        ddayPanel.add(ddayScroll, BorderLayout.CENTER);

        JPanel calAndDdayPanel = new JPanel(new BorderLayout(5, 5));
        calAndDdayPanel.add(calPanel, BorderLayout.CENTER);
        calAndDdayPanel.add(ddayPanel, BorderLayout.SOUTH);
        leftPanel.add(calAndDdayPanel, BorderLayout.CENTER);

        /* =======================================
           2. 오른쪽 패널: 기존 채팅방 (멤버 + 채팅 + 방코드)
           ======================================= */
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));

        JScrollPane memberScroll = new JScrollPane(memberList);
        memberScroll.setPreferredSize(new Dimension(0, 150));
        memberScroll.setBorder(BorderFactory.createTitledBorder("참여 목록"));

        // 방 초대 코드 라벨이 들어간 버튼 패널
        JPanel topBtnPanel = new JPanel(new BorderLayout());
        JLabel roomCodeLabel = new JLabel(" 🔑 방 초대 코드 : " + roomnum + " ");
        roomCodeLabel.setFont(new Font("맑은 고딕", Font.BOLD, 17));
        roomCodeLabel.setForeground(new Color(220, 20, 60));
        
        JPanel btnFlowPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton scheduleBtn = new JButton("일정 추가");
        JButton todoBtn = new JButton("투두 리스트 관리");
        btnFlowPanel.add(scheduleBtn);
        btnFlowPanel.add(todoBtn);
        
        topBtnPanel.add(roomCodeLabel, BorderLayout.WEST);
        topBtnPanel.add(btnFlowPanel, BorderLayout.EAST);
        
        JPanel rightTopPanel = new JPanel(new BorderLayout());
        rightTopPanel.add(topBtnPanel, BorderLayout.NORTH);
        rightTopPanel.add(memberScroll, BorderLayout.CENTER);

        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        JScrollPane chatScroll = new JScrollPane(chatArea);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(workBtn, BorderLayout.WEST);
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendBtn, BorderLayout.EAST);

        rightPanel.add(rightTopPanel, BorderLayout.NORTH);
        rightPanel.add(chatScroll, BorderLayout.CENTER);
        rightPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);

        /* =======================================
           3. 이벤트 및 데이터 로드
           ======================================= */
        scheduleBtn.addActionListener(e -> new ScheduleFrame(roomnum, o.db));
        todoBtn.addActionListener(e -> new TodoFrame(roomnum, o.db));

        updateCalendar();
        loadDashboardData();

        loadChatHistory();
        ArrayList<String> members = o.db.getRoomMembers(roomnum);
        updateMemberList(members);
        connectToServer();

        SendListener sl = new SendListener();
        sendBtn.addActionListener(sl);
        inputField.addActionListener(sl);
        workBtn.addActionListener(new WorkListener());
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });
        
        setVisible(true);
        
        new javax.swing.Timer(1000, e -> checkIdle()).start();
        new javax.swing.Timer(1000, e -> refreshWorkTime()).start();
        new javax.swing.Timer(5000, e -> loadDashboardData()).start();
        }
        

    /* --- 대시보드 관련 메서드 (취소선 기능 포함) --- */
    private void updateCalendar() {
        calModel.setRowCount(0); 
        monthLabel.setText(currentMonth.getYear() + "년 " + currentMonth.getMonthValue() + "월");
        
        LocalDate firstDay = currentMonth.atDay(1);
        int dayOfWeek = firstDay.getDayOfWeek().getValue() % 7; 
        int daysInMonth = currentMonth.lengthOfMonth();
        
        ArrayList<ScheduleDTO> schedules = o.db.getSchedules(roomnum);
        LocalDate today = LocalDate.now();
        
        String[] row = new String[7];
        int col = dayOfWeek;
        
        for (int i = 1; i <= daysInMonth; i++) {
            LocalDate currentDate = currentMonth.atDay(i);
            StringBuilder cellHtml = new StringBuilder();
            cellHtml.append("<html><center><b>").append(i).append("</b>");
            
            for(ScheduleDTO s : schedules) {
                try {
                    LocalDate dbDate = LocalDate.parse(String.valueOf(s.getDeadline()).trim().substring(0, 10));
                    if(dbDate.equals(currentDate)) {
                        if(dbDate.isBefore(today)) {
                            // 마감일 지남 -> 취소선 + 회색 처리
                            cellHtml.append("<br><font size='2' color='#999999'><s>")
                                    .append(s.getTitle())
                                    .append("</s></font>");
                        } else {
                            // 마감일 안 지남 -> 파란색 표시
                            cellHtml.append("<br><font size='2' color='#0066CC'>")
                                    .append(s.getTitle())
                                    .append("</font>");
                        }
                    }
                } catch(Exception ex) {}
            }
            cellHtml.append("</center></html>");
            row[col] = cellHtml.toString();
            col++;
            if (col == 7) {
                calModel.addRow(row);
                row = new String[7];
                col = 0;
            }
        }
        if (col > 0) calModel.addRow(row);
    }

    private void changeMonth(int offset) {
        currentMonth = currentMonth.plusMonths(offset);
        updateCalendar();
    }

    private void loadDashboardData() {
        ArrayList<TodoDTO> todos = o.db.getTodos(roomnum);
        int total = todos.size();
        int completed = 0;
        for (TodoDTO t : todos) {
            if (t.isCompleted()) completed++;
        }
        int progress = (total == 0) ? 0 : (int) Math.round((double) completed / total * 100);
        progressBar.setValue(progress);
        progressBar.setString("프로젝트 진행률 : " + progress + "%");

        ddayModel.clear();
        ArrayList<ScheduleDTO> schedules = o.db.getSchedules(roomnum);
        LocalDate today = LocalDate.now();

        for (ScheduleDTO s : schedules) {
            try {
                LocalDate deadline = LocalDate.parse(String.valueOf(s.getDeadline()).trim().substring(0, 10));
                long dDay = ChronoUnit.DAYS.between(today, deadline);
                
                if (dDay < 0) {
                    // D-Day 지남 -> 취소선 + 회색 처리
                    String dDayStr = "D+" + Math.abs(dDay) + " (마감)";
                    ddayModel.addElement("<html><font color='#999999'><s> [ " + dDayStr + " ]  " + s.getTitle() + "</s></font></html>");
                } else {
                    String dDayStr = (dDay > 0) ? "D-" + dDay : "D-Day";
                    ddayModel.addElement(" [ " + dDayStr + " ]  " + s.getTitle());
                }
            } catch (Exception e) {}
        }
        updateCalendar();
    }

    /* --- 기존 채팅 및 통신 메서드 --- */
    private void loadChatHistory() {
        ArrayList<String> history = o.db.getChatHistory(roomnum);
        for (String msg : history) chatArea.append(msg + "\n");
    }

    private void connectToServer() { 
        try { 
            socket = new Socket("127.0.0.1", 12345); 
            in = new BufferedReader(new InputStreamReader(socket.getInputStream())); 
            out = new PrintWriter(socket.getOutputStream(), true); 
            new Thread(new Receiver()).start();
        } catch (Exception e) { 
            chatArea.append("서버 연결 실패\n"); 
        } 
    }

    private void sendMessage() {
        lastInputTime = System.currentTimeMillis();
        String message = inputField.getText().trim();
        if (message.equals("")) return;

        chatArea.append("[" + o.loginNick + "] " + message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
        if (out != null) out.println(message);
        o.db.saveMessage(roomnum, o.loginNick, message);
        inputField.setText("");
        inputField.requestFocus();
    }
    
    private void checkIdle() {
        if(!working) return;
        long idle = inputMonitor.getIdleTime();
        if(idle >= 10000) {
            working = false;
            workBtn.setText("작업 재개");
            if(out != null) out.println("WORK_PAUSE");
            SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(chatRoom.this, "입력이 없어 타이머가 중지되었습니다."));
        }
    }
    
    private void refreshWorkTime() {
        boolean changed = false;
        for(String user : userStatus.keySet()) {
            if("WORKING".equals(userStatus.get(user))) {
                Long sec = userTime.get(user);
                if(sec != null) {
                    userTime.put(user, sec + 1);
                    changed = true;
                }
            }
        }
        if(changed) updateMemberList(o.db.getRoomMembers(roomnum));
    }
    
    private void disconnect() {
        try { if (socket != null) socket.close(); } 
        catch (Exception e) {}
    }

    private void updateMemberList(ArrayList<String> allMembers) { 
        memberModel.clear();
        for (String nick : allMembers) {
            if (onlineUsers.contains(nick)) {
                String status = userStatus.getOrDefault(nick,"REST");
                long sec = userTime.getOrDefault(nick, 0L);
                String time = String.format("%02d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60);
                String text = (status.equals("WORKING")) ? nick + " ● 작업중 (" + time + ")" : nick + " ● 쉬는중 (" + time + ")";
                memberModel.addElement(text);
            } else {
                memberModel.addElement(nick + " ○ 오프라인");
            }
        }
    }

    class Receiver implements Runnable {
        @Override
        public void run() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.equals("SUBMIT_ROOM")) out.println(roomName);
                    else if (line.equals("SUBMIT_NAME")) out.println(o.loginNick);
                    else if(line.startsWith("STATUS:")) {
                        userStatus.clear(); userTime.clear();
                        String[] users = line.substring(7).split(",");
                        for(String user : users) {
                            if(user.trim().equals("")) continue;
                            String[] info = user.split("\\|");
                            if(info.length >= 3) {
                                userStatus.put(info[0], info[1]);
                                userTime.put(info[0], Long.parseLong(info[2]));
                                lastUpdateTime.put(info[0], System.currentTimeMillis());
                            }
                        }
                        updateMemberList(o.db.getRoomMembers(roomnum));
                    }
                    else if(line.startsWith("USERLIST:")) { 
                        onlineUsers.clear();
                        String[] users = line.substring(9).split(",");
                        for(String user : users) {
                            if(!user.trim().equals("")) onlineUsers.add(user.trim());
                        }
                        updateMemberList(o.db.getRoomMembers(roomnum));
                    } else { 
                        chatArea.append(line + "\n");
                        if (line.contains("님이 입장하셨습니다") || line.contains("님이 퇴장하셨습니다")) { 
                            try {
                                String nick = line.substring(line.indexOf("] ") + 2, line.indexOf("님"));
                                if(line.contains("입장")) onlineUsers.add(nick);
                                else onlineUsers.remove(nick);
                                updateMemberList(o.db.getRoomMembers(roomnum));
                            } catch (Exception ex) {}
                        }
                        chatArea.setCaretPosition(chatArea.getDocument().getLength());
                    }
                }
            } catch (Exception e) { chatArea.append("서버 연결 종료\n"); }
        }
    }
    
    class WorkListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if(!workStarted) {
                lastInputTime = System.currentTimeMillis();
                workStarted = true; working = true;
                workBtn.setText("일시정지");
                if(out != null) out.println("WORK_START");
            } else if(working) {
                working = false;
                workBtn.setText("작업 재개");
                if(out != null) out.println("WORK_PAUSE");
            } else {
                lastInputTime = System.currentTimeMillis();
                working = true;
                workBtn.setText("일시정지");
                if(out != null) out.println("WORK_RESUME");
            }
        }
    }
    
    class SendListener implements ActionListener { 
        @Override
        public void actionPerformed(ActionEvent e) { sendMessage(); }
    }
}
