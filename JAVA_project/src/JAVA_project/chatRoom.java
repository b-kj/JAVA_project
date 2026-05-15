package JAVA_project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class chatRoom extends JFrame {

    /* GUI */
    JTextArea chatArea = new JTextArea();
    JTextField inputField = new JTextField();
    JButton sendBtn = new JButton("전송");

    /* Socket */
    Socket socket;
    BufferedReader in;
    PrintWriter out;

    /* 정보 */
    Operator o;
    int roomnum;
    String roomName;

    /* 생성자 */
    public chatRoom(int roomnum, String roomName, Operator o) {
        this.roomnum = roomnum;
        this.roomName = roomName;
        this.o = o;

        /* 채팅창 설정 */
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);

        JScrollPane scrollPane = new JScrollPane(chatArea);

        /* 입력 패널 */
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendBtn, BorderLayout.EAST);

        /* 레이아웃 */
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        /* 창 설정 */
        setTitle("채팅방 - " + roomName);
        setSize(500, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        /* 이전 대화 불러오기 */
        loadChatHistory();

        /* 서버 연결 */
        connectToServer();

        /* 버튼/엔터 이벤트 */
        SendListener sl = new SendListener();
        sendBtn.addActionListener(sl);
        inputField.addActionListener(sl);

        /* 창 닫을 때 소켓 종료 */
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });

        setVisible(true);
    }

    /* DB에서 이전 대화 불러오기 */
    private void loadChatHistory() {
        ArrayList<String> history = o.db.getChatHistory(roomnum);

        if (!history.isEmpty()) {

            for (String msg : history) {
                chatArea.append(msg + "\n");
            }

        }
    }

    /* 서버 연결 */
    private void connectToServer() {
        try {
            socket = new Socket("127.0.0.1", 12345);

            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            out = new PrintWriter(
                    socket.getOutputStream(), true);

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

        // 내 화면에 즉시 출력
        chatArea.append("[" + o.loginNick + "] " + message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());

        // 서버로 전송
        if (out != null) {
            out.println(message);
        }

        // DB 저장
        o.db.saveMessage(roomnum, o.loginNick, message);

        // 입력창 초기화
        inputField.setText("");
        inputField.requestFocus();
    }

    /* 연결 종료 */
    private void disconnect() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            System.out.println("소켓 종료 실패 > " + e.toString());
        }
    }

    /* 서버 메시지 수신 */
    class Receiver implements Runnable {
        @Override
        public void run() {
            try {
                String line;

                while ((line = in.readLine()) != null) {

                    /* 서버가 방 이름 요구 */
                    if (line.equals("SUBMIT_ROOM")) {
                        out.println(roomName);
                    }

                    /* 서버가 닉네임 요구 */
                    else if (line.equals("SUBMIT_NAME")) {
                        out.println(o.loginNick);
                    }

                    /* 일반 메시지 */
                    else {
                        chatArea.append(line + "\n");
                        chatArea.setCaretPosition(
                                chatArea.getDocument().getLength());
                    }
                }

            } catch (Exception e) {
                chatArea.append("서버와 연결이 종료되었습니다.\n");
            }
        }
    }

    /* 전송 버튼 리스너 */
    class SendListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            sendMessage();
        }
    }
}