package JAVA_project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class chatRoom extends JFrame {

    /* GUI */
    JTextArea chatArea = new JTextArea();
    JTextField inputField = new JTextField();
    JButton sendBtn = new JButton("전송");
    JButton workBtn = new JButton("작업 시작");

    /* 멤버 목록 */
    DefaultListModel<String> memberModel = new DefaultListModel<>();
    JList<String> memberList =  new JList<>(memberModel);

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

    /*생성자 */
    public chatRoom(int roomnum, String roomName, Operator o) {
        this.roomnum = roomnum;
        this.roomName = roomName;
        this.o = o;

        /* 채팅창 설정 */
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        JScrollPane chatScroll = new JScrollPane(chatArea);

        /* 멤버 목록 설정 */
        JScrollPane memberScroll = new JScrollPane(memberList);
        memberScroll.setPreferredSize(new Dimension(0, 100));
        memberScroll.setBorder(BorderFactory.createTitledBorder("참여 목록"));

        /* 입력 패널 */
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(workBtn, BorderLayout.WEST);
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendBtn,BorderLayout.EAST);

       /* 추가: 상단 버튼 패널 (일정 / 투두) */
        JPanel topBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton scheduleBtn = new JButton("일정 관리");
        JButton todoBtn = new JButton("투두 리스트");
        topBtnPanel.add(scheduleBtn);
        topBtnPanel.add(todoBtn);

        /* 추가: 버튼 패널과 멤버 목록을 하나로 묶음 */
        JPanel topCombinedPanel = new JPanel(new BorderLayout());
        topCombinedPanel.add(topBtnPanel, BorderLayout.NORTH);
        topCombinedPanel.add(memberScroll, BorderLayout.CENTER);

        /* 메인 레이아웃 적용 */
        setLayout(new BorderLayout());
        add(chatScroll, BorderLayout.CENTER);
        add(topCombinedPanel, BorderLayout.NORTH); 
        add(bottomPanel, BorderLayout.SOUTH);

        /* 일정/투두 프레임 띄우는 이벤트 연결  */
        scheduleBtn.addActionListener(e -> new ScheduleFrame(roomnum, o.db));
        todoBtn.addActionListener(e -> new TodoFrame(roomnum, o.db));

        /* 창 설정 */
        setTitle("[ " + roomnum + " ] " + roomName);
        setSize(700, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        /* 이전 채팅 불러오기 */
        loadChatHistory();

        /* 멤버 목록 불러오기 */
        ArrayList<String> members = o.db.getRoomMembers(roomnum);
        updateMemberList(members);

        /* 서버 연결 */
        connectToServer();

        /* 이벤트 연결 */
        SendListener sl = new SendListener();
        sendBtn.addActionListener(sl);
        inputField.addActionListener(sl);
        workBtn.addActionListener(new WorkListener());
        
        /* 창 종료 시 소켓 종료 */
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });
        setVisible(true);
        
        new javax.swing.Timer(1000, e -> checkIdle()).start();
        new javax.swing.Timer(1000, e -> refreshWorkTime()).start();
    }

    /* 이전 채팅 불러오기 */
    private void loadChatHistory() {
        ArrayList<String> history = o.db.getChatHistory(roomnum);
        for (String msg : history) {
            chatArea.append(msg + "\n");
        }
    }

    /* 서버 연결 */ 
    private void connectToServer() { 
        try { 
            socket = new Socket("127.0.0.1", 12345); 
            
            in = new BufferedReader(new InputStreamReader(socket.getInputStream())); 
            out = new PrintWriter(socket.getOutputStream(), true); 
            
            /* 수신 스레드 시작 */
            new Thread(new Receiver()).start();
            } catch (Exception e) { 
                chatArea.append("서버 연결 실패: " + e.getMessage() + "\n"); 
            } 
    }

    /* 메시지 전송 */
    private void sendMessage() {
        lastInputTime = System.currentTimeMillis();
        String message = inputField.getText().trim();

        if (message.equals("")) {
            return;
        }

        /* 내 화면 즉시 출력 */
        chatArea.append("[" + o.loginNick + "] " + message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());

        /* 서버 전송 */
        if (out != null) {
            out.println(message);
        }

        /* DB 저장 */
        o.db.saveMessage(roomnum, o.loginNick, message);

        /* 입력창 초기화 */
        inputField.setText("");
        inputField.requestFocus();
    }
    
    private void checkIdle() {
        if(!working)
            return;
        
        long idle = System.currentTimeMillis() - lastInputTime;

        if(idle >= 10000 && working) { // 타이머 멈춤 확인 용 숫자(10초) 마지막에 300000(5분)으로 변경
                    working = false;
                    workBtn.setText("작업 재개");

                    if(out != null)
                        out.println("WORK_PAUSE");
                    
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(chatRoom.this, "입력이 없어 타이머가 중지되었습니다.");
                    });
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
        if(changed) {
            updateMemberList(o.db.getRoomMembers(roomnum));
        }
    }
    
    /* 연결 종료 */
    private void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (Exception e) {
            System.out.println("소켓 종료 실패 > " + e.toString());
        }
    }


    private void updateMemberList(ArrayList<String> allMembers) { //멤버 목록 갱신
        memberModel.clear();

        for (String nick : allMembers) {
            if (onlineUsers.contains(nick)) {
                String status = userStatus.getOrDefault(nick,"REST");
                long sec = userTime.getOrDefault(nick, 0L);
                String time = String.format("%02d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60);
                String text;

                if(status.equals("WORKING")) 
                    text = nick + " ● 작업중 (" + time + ")";
                else 
                    text = nick + " ● 쉬는중 (" + time + ")";
                memberModel.addElement(text);
            }
            else {
                memberModel.addElement(nick + " ○ 오프라인");
            }
        }
    }

    // 서버 메시지 수신
    class Receiver implements Runnable {

        @Override
        public void run() {
            try {

                String line;

                while ((line = in.readLine()) != null) {
                    if (line.equals("SUBMIT_ROOM")) { // 방 이름 요구 
                        out.println(roomName);
                    }
                    else if (line.equals("SUBMIT_NAME")) { // 닉네임 요구
                        out.println(o.loginNick);
                    }
                    else if(line.startsWith("STATUS:")) { // 상태 요구

                        userStatus.clear();
                        userTime.clear();

                        String data = line.substring(7);
                        String[] users = data.split(",");

                        for(String user : users) {
                            if(user.trim().equals(""))
                                continue;

                            String[] info = user.split("\\|");

                            if(info.length >= 3) {
                                userStatus.put(info[0], info[1]);
                                long sec = Long.parseLong(info[2]);
                                userTime.put(info[0], sec);
                                lastUpdateTime.put(info[0], System.currentTimeMillis());
                            }
                        }
                        updateMemberList(o.db.getRoomMembers(roomnum));
                        continue;
                    }
                    else if(line.startsWith("USERLIST:")) { //멤버 요구

                        onlineUsers.clear();

                        String data = line.substring("USERLIST:".length());
                        String[] users = data.split(",");

                        for(String user : users) {
                            if(!user.trim().equals("")) {
                                onlineUsers.add(user.trim());
                            }
                        }

                        updateMemberList(o.db.getRoomMembers(roomnum));
                        continue;
                    }
                    
                    else { 
                        chatArea.append(line + "\n");

                        if (line.contains("님이 입장하셨습니다")) { //입장
                            try {
                                String nick = line.substring(line.indexOf("] ") + 2, line.indexOf("님"));
                                onlineUsers.add(nick);
                                updateMemberList(o.db.getRoomMembers(roomnum));
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                        else if (line.contains("님이 퇴장하셨습니다")) { //퇴장
                            try {
                                String nick = line.substring(line.indexOf("] ") + 2, line.indexOf("님"));
                                onlineUsers.remove(nick);
                                updateMemberList(o.db.getRoomMembers(roomnum));
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                        chatArea.setCaretPosition(chatArea.getDocument().getLength());
                    }
                }

            } catch (Exception e) {
                chatArea.append("서버 연결 종료\n");
            }
        }
    }
    
    class WorkListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {

            if(!workStarted) {
                lastInputTime = System.currentTimeMillis();
                workStarted = true;
                working = true;
                workBtn.setText("일시정지");

                if(out != null)
                    out.println("WORK_START");
            }
            else if(working) {
                working = false;
                workBtn.setText("작업 재개");

                if(out != null)
                    out.println("WORK_PAUSE");
            }
            else {
                lastInputTime = System.currentTimeMillis();
                working = true;
                workBtn.setText("일시정지");
                if(out != null) out.println("WORK_RESUME");
            }
        }
    }
    
    class SendListener implements ActionListener { //전송

        @Override
        public void actionPerformed(
                ActionEvent e) {
            sendMessage();
        }
    }
}
