package JAVA_project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class chatRoom extends JFrame {

    /* GUI */
    JTextArea chatArea = new JTextArea();
    JTextField inputField = new JTextField();
    JButton sendBtn = new JButton("전송");

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
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendBtn,BorderLayout.EAST);

        /* 메인 레이아웃 */
        setLayout(new BorderLayout());
        add(chatScroll, BorderLayout.CENTER);
        add(memberScroll, BorderLayout.NORTH);
        add(bottomPanel, BorderLayout.SOUTH);

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

        /* 창 종료 시 소켓 종료 */
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });
        setVisible(true);
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
        	
            if (onlineUsers.contains(nick)) { //온라인 업데이트
                memberModel.addElement("<html>" + nick + " <font color='green'>● 온라인</font></html>");
            }
            else { //오프라인 업데이트
                memberModel.addElement("<html>" + nick  + " <font color='gray'>● 오프라인</font></html>");
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

                    else if(line.startsWith("USERLIST:")) { //멤버 요구

                        onlineUsers.clear();

                        String data = line.substring("USERLIST:".length());
                        String[] users = data.split(",");

                        for(String user : users) {
                            if(!user.trim().equals("")) {
                                onlineUsers.add(user.trim());
                            }
                        }

                        updateMemberList(
                            o.db.getRoomMembers(roomnum));

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
  
    class SendListener implements ActionListener { //전송

        @Override
        public void actionPerformed(
                ActionEvent e) {
            sendMessage();
        }
    }
}