package JAVA_project;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 12345;
    
    private static class UserState { //유저 상태
        boolean working;
        long workedSeconds;
        long startTime;
    }

    private static class Room { //방안의 정보
        Set<PrintWriter> clients = new HashSet<>();
        Set<String> users = new HashSet<>();
        Map<String, UserState> states = new HashMap<>();
    }

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
    

    // 1인 1 스레드
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

                // 클라이언트에게 방 이름 요청 chatRoom.java가 자동으로 roomName을 전송한다.
                out.println("SUBMIT_ROOM");
                roomName = in.readLine();

                if (roomName == null || roomName.trim().equals("")) {
                    return;
                }

                // 2. 클라이언트에게 닉네임 요청 chatRoom.java가 자동으로 o.loginNick을 전송한다.
                out.println("SUBMIT_NAME");
                name = in.readLine();

                if (name == null || name.trim().equals("")) {
                    return;
                }

                //방 생성 또는 기존 방 가져오기
                Room room;

                synchronized (rooms) {
                    rooms.putIfAbsent(roomName, new Room());
                    room = rooms.get(roomName);

                    // 현재 사용자 추가
                    room.clients.add(out);
                    room.users.add(name);
                    
                    sendUserList(room);
                    sendStatus(room);
                }

                //입장 메시지 전송
                broadcast(roomName, "[시스템] " + name + "님이 입장하셨습니다. ", null);

                // 메시지 수신 및 전달
                String message;

                while ((message = in.readLine()) != null) {
                	Room r = rooms.get(roomName);
                	
                	if(message.equals("WORK_START")) {
                	    UserState state = new UserState();
                	    state.working = true;
                	    state.startTime = System.currentTimeMillis();
                	    r.states.put(name, state);
                	    sendStatus(r);
                	    continue;
                	}
                	if(message.equals("WORK_PAUSE")) {
                	    UserState state = r.states.get(name);

                	    if(state != null && state.working) {
                	        state.workedSeconds += (System.currentTimeMillis() - state.startTime) /1000;
                	        state.working = false;
                	    }
                	    sendStatus(r);
                	    continue;
                	}
                	if(message.equals("WORK_RESUME")) {
                	    UserState state = r.states.get(name);

                	    if(state != null) {
                	        state.working = true;
                	        state.startTime =System.currentTimeMillis();
                	    }
                	    sendStatus(r);
                	    continue;
                	}
                	broadcast(roomName,"[" + name + "] " + message, out);
                }

            } catch (IOException e) {
                System.out.println("클라이언트 연결 오류: " + e.getMessage());

            } finally {
                disconnect();
            }
        }

        // 연결 종료
        private void disconnect() {
            if (roomName != null && out != null) {
                int remainingUsers = 0;

                synchronized (rooms) {
                    Room room = rooms.get(roomName);

                    if (room != null) {
                        // 현재 사용자 제거
                        room.clients.remove(out);
                        room.users.remove(name);
                        
                        sendUserList(room);
                        sendStatus(room);

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

        // 특정 방의 모든 사용자에게 메시지 전송
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
    
    private static void sendUserList(Room room) {
        StringBuilder sb = new StringBuilder("USERLIST:");

        for(String user : room.users) {
            sb.append(user).append(",");
        }

        String msg = sb.toString();
        System.out.println(msg);

        for(PrintWriter pw : room.clients) {
            pw.println(msg);
        }
    }
    
    private static void sendStatus(Room room) {
        StringBuilder sb = new StringBuilder("STATUS:");
        long now = System.currentTimeMillis();

        for(String user : room.users) {
            UserState state = room.states.get(user);

            if(state == null) 
                sb.append(user).append("|REST|0,");
            else {
                long sec = state.workedSeconds;
                if(state.working) 
                    sec += (now - state.startTime) /1000;
                sb.append(user).append("|").append(state.working ? "WORKING" : "REST").append("|").append(sec).append(",");
            }
        }
        String msg = sb.toString();
        
        for(PrintWriter pw : room.clients)
            pw.println(msg);
    }
}