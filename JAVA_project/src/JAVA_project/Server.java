package JAVA_project;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 12345;

    /* clients : 현재 이 방에 접속 중인 사용자들의 출력 스트림
     */
    private static class Room {
        Set<PrintWriter> clients = new HashSet<>();
    }

    /*
     * 방 이름 -> Room 객체
     * 예:
     *   "test" -> Room
     *   "project" -> Room
     */
    private static Map<String, Room> rooms = new HashMap<>();

    public static void main(String[] args) {
        System.out.println("서버가 시작되었습니다. (포트: " + PORT + ")");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            while (true) {
                Socket socket = serverSocket.accept();
                new Handler(socket).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * 클라이언트 1명당 1개의 스레드
     */
    private static class Handler extends Thread {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        private String name;      // 닉네임
        private String roomName;  // 방 이름

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                // 입출력 스트림 생성
                in = new BufferedReader( new InputStreamReader(socket.getInputStream()));

                out = new PrintWriter(
                        socket.getOutputStream(), true);

                /*
                 * 1. 클라이언트에게 방 이름 요청
                 * chatRoom.java가 자동으로 roomName을 전송한다.
                 */
                out.println("SUBMIT_ROOM");
                roomName = in.readLine();

                if (roomName == null || roomName.trim().equals("")) {
                    return;
                }

                /*
                 * 2. 클라이언트에게 닉네임 요청
                 * chatRoom.java가 자동으로 o.loginNick을 전송한다.
                 */
                out.println("SUBMIT_NAME");
                name = in.readLine();

                if (name == null || name.trim().equals("")) {
                    return;
                }

                /*
                 * 3. 방 생성 또는 기존 방 가져오기
                 */
                Room room;

                synchronized (rooms) {
                    rooms.putIfAbsent(roomName, new Room());
                    room = rooms.get(roomName);

                    // 현재 사용자 추가
                    room.clients.add(out);
                }

                /*
                 * 4. 입장 메시지 전송
                 */
                broadcast(roomName, "[시스템] " + name + "님이 입장하셨습니다. ", null);

                /*
                 * 5. 메시지 수신 및 전달
                 */
                String message;

                while ((message = in.readLine()) != null) {
                    broadcast(roomName,"[" + name + "] " + message, out);
                }

            } catch (IOException e) {
                System.out.println("클라이언트 연결 오류: " + e.getMessage());

            } finally {
                disconnect();
            }
        }

        /*
         * 연결 종료 처리
         */
        private void disconnect() {
            if (roomName != null && out != null) {
                int remainingUsers = 0;

                synchronized (rooms) {
                    Room room = rooms.get(roomName);

                    if (room != null) {
                        // 현재 사용자 제거
                        room.clients.remove(out);

                        remainingUsers = room.clients.size();

                        // 방에 아무도 없으면 방 제거
                        if (room.clients.isEmpty()) {
                            rooms.remove(roomName);
                        }
                    }
                }

                // 퇴장 메시지 전송
                if (name != null) {
                    broadcast(roomName, "[시스템] " + name + "님이 퇴장하셨습니다.", null );
                }
            }

            // 소켓 닫기
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                System.out.println("소켓 종료 실패: " + e.getMessage());
            }
        }

        /*
         * 특정 방의 모든 사용자에게 메시지 전송
         *
         * excludeWriter:
         *   null이면 모두에게 전송
         *   특정 PrintWriter면 해당 사용자 제외
         */
        private void broadcast( String roomName, String message, PrintWriter excludeWriter) {

            if (roomName == null) {
                return;
            }

            synchronized (rooms) {
                Room room = rooms.get(roomName);

                if (room != null) {
                    for (PrintWriter writer : room.clients) {
                        if (writer != excludeWriter) {
                            writer.println(message);
                        }
                    }
                }
            }
        }
    }
}