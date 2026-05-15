package JAVA_project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class Room extends JFrame {
    JPanel panel = new JPanel(new BorderLayout());

    JButton createBtn = new JButton("방만들기");
    JButton joinBtn = new JButton("참여하기");

    DB db;
    Operator o;
    JList<String> roomListUI;

    public Room(DB db, Operator o) {
        this.db = db;
        this.o = o;

        /* 방 목록 불러오기 */
        ArrayList<String> rooms = db.RoomList(o.loginId);
        roomListUI = new JList<>(rooms.toArray(new String[0]));

        /* 리스트 더블클릭 이벤트 */
        roomListUI.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 더블클릭일 때만 실행
                if (e.getClickCount() == 2) {
                    openSelectedRoom();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(roomListUI);

        /* 버튼 크기 */
        createBtn.setPreferredSize(new Dimension(100, 30));
        joinBtn.setPreferredSize(new Dimension(100, 30));

        /* 버튼 이벤트 연결 */
        ButtonListener bl = new ButtonListener();
        createBtn.addActionListener(bl);
        joinBtn.addActionListener(bl);

        /* 하단 버튼 패널 */
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(createBtn);
        buttonPanel.add(joinBtn);

        /* 패널 구성 */
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(panel);

        /* 창 설정 */
        setTitle("프로젝트 목록");
        setSize(300, 400);
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        /* 목록 새로고침 */
        refreshRoomList();

        /* 화면 갱신 */
        roomListUI.revalidate();
        roomListUI.repaint();
    }

    /* 방 목록 새로고침 */
    private void refreshRoomList() {
        ArrayList<String> rooms = db.RoomList(o.loginId);
        roomListUI.setListData(rooms.toArray(new String[0]));
    }

    /* 선택된 방 열기 */
    private void openSelectedRoom() {
        String selected = roomListUI.getSelectedValue();

        if (selected == null) {
            JOptionPane.showMessageDialog(
                Room.this,
                "방을 선택하세요."
            );
            return;
        }

        try {
            // 형식: "1 - 프로젝트 회의방"
            String[] parts = selected.split(" - ", 2);

            int roomnum = Integer.parseInt(parts[0]);
            String roomName = parts[1];

            new chatRoom(roomnum, roomName, o);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                Room.this,
                "방 정보를 읽을 수 없습니다.\n형식: 방번호 - 방이름"
            );
            System.out.println("방 열기 실패 > " + ex.toString());
        }
    }

    /* 버튼 이벤트 */
    class ButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JButton b = (JButton) e.getSource();

            /* 방만들기 */
            if (b == createBtn) {
                String roomName = JOptionPane.showInputDialog(
                    Room.this,
                    "방 이름을 입력하세요."
                );

                if (roomName != null &&
                    !roomName.trim().equals("")) {

                    db.createRoom(roomName, o.loginId);
                    refreshRoomList();
                }
            }

            /* 참여하기 */
            else if (b == joinBtn) {
                String roomIdStr = JOptionPane.showInputDialog(
                    Room.this,
                    "참여할 방 ID를 입력하세요."
                );

                if (roomIdStr != null &&
                    !roomIdStr.trim().equals("")) {

                    try {
                        int roomId = Integer.parseInt(roomIdStr);

                        db.joinRoom(roomId, o.loginId);
                        refreshRoomList();

                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(
                            Room.this,
                            "숫자로 된 방 ID를 입력하세요."
                        );
                    }
                }
            }
        }
    }
}