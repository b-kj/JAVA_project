package JAVA_project;

import java.sql.*;
import java.util.*;
import javax.swing.JOptionPane;

public class DB {
	Connection con = null;
	Statement stmt = null;
	String url = "jdbc:mysql://localhost:3306/usedb?serverTimezone=Asia/Seoul&useSSL=false";	//dbstudy 스키마
	String user = "root";
	String passwd = "bkj03010301!";		
	
	DB() {	//데이터베이스에 연결한다.
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			con = DriverManager.getConnection(url, user, passwd);
			stmt = con.createStatement();
			System.out.println("MySQL 서버 연동 성공");
		} catch(Exception e) {
			System.out.println("MySQL 서버 연동 실패 > " + e.toString());
		}
	}

	/* 로그인 정보를 확인 */
	boolean logincheck(String _i, String _p) {
		boolean flag = false;
		
		String id = _i;
		String pw = _p;
		
		try {
			String checkingStr = "SELECT password FROM user WHERE id='" + id + "'";
			ResultSet result = stmt.executeQuery(checkingStr);
			
			int count = 0;
			while(result.next()) {
				if(pw.equals(result.getString("password"))) {
					flag = true;
					System.out.println("로그인 성공");
				}
				
				else {
					flag = false;
					System.out.println("로그인 실패");
				}
				count++;
			}
		} catch(Exception e) {
			flag = false;
			System.out.println("로그인 실패 > " + e.toString());
		}
		
		return flag;
	}
	
	public String getNick(String id) {
	    String nick = "";

	    try {
	        String sql = "SELECT nickname FROM user WHERE id='" + id + "'";
	        ResultSet rs = stmt.executeQuery(sql);

	        if (rs.next()) {
	            nick = rs.getString("nickname");
	            
	        }
	    } catch (Exception e) {
	        System.out.println("닉네임 조회 실패 > " + e.toString());
	    }

	    return nick;
	}
	
	boolean joinCheck(String _i, String _p, String _n) { //회원가입 체크
		boolean flag = false;
		
		String id = _i;
		String pw = _p;
		String nk = _n;
			
		try {
			String insertStr = "INSERT INTO user VALUES('" + id + "', '" + pw + "', '" + nk + "')";
			stmt.executeUpdate(insertStr);
				
			flag = true;
			System.out.println("회원가입 성공");
		} catch(Exception e) {
			flag = false;
			System.out.println("회원가입 실패 > " + e.toString());
		}
			
		return flag;
	}
	
	public ArrayList<String> RoomList(String loginId) {
	    ArrayList<String> list = new ArrayList<>();

	    try {
	        String sql =
	            "SELECT c.roomnum, c.name FROM room c JOIN member m ON c.roomnum = m.room WHERE m.user_id = '" + loginId + "'";

	        ResultSet rs = stmt.executeQuery(sql);

	        while (rs.next()) {
	            int roomnum = rs.getInt("roomnum");
	            String roomName = rs.getString("name");

	            list.add(roomnum + " - " + roomName);
	        }

	    } catch (Exception e) {
	        System.out.println("방 목록 조회 실패 > " + e.toString());
	    }

	    return list;
	}
	
	void createRoom(String roomName, String loginId) {
	    try {
	        // 방 생성
	        String sql =
	            "INSERT INTO room(name) VALUES('" + roomName + "')";

	        stmt.executeUpdate(sql);

	        // 마지막 생성된 roomnum 가져오기
	        String findSql =
	            "SELECT MAX(roomnum) AS roomnum FROM room";

	        ResultSet rs = stmt.executeQuery(findSql);

	        int roomnum = 0;

	        if (rs.next()) {
	            roomnum = rs.getInt("roomnum");
	        }

	        // 생성자를 member 테이블에 추가
	        String memberSql =
	            "INSERT INTO member(room, user_id) VALUES(" +  roomnum + ", '" + loginId + "')";

	        stmt.executeUpdate(memberSql);

	        System.out.println("방 생성 완료");

	    } catch(Exception e) {
	        System.out.println("방 생성 실패 > " + e.toString());
	    }
	}
	
	public boolean joinRoom(int roomId, String loginId) {
	    boolean flag = false;

	    try {
	        // 먼저 해당 방이 존재하는지 확인
	        String checkSql =
	            "SELECT * FROM room WHERE roomnum = " + roomId;

	        ResultSet rs = stmt.executeQuery(checkSql);

	        if (!rs.next()) {
	            System.out.println("참여 실패 > 존재하지 않는 방");
	            return false;
	        }

	        // 이미 참여 중인지 확인
	        String dupSql =
	            "SELECT * FROM member WHERE room = " + roomId + " AND user_id = '" + loginId + "'";

	        rs = stmt.executeQuery(dupSql);

	        if (rs.next()) {
	            System.out.println("이미 참여 중인 방입니다.");
                JOptionPane.showMessageDialog(null,"이미 참여 중인 방입니다.");
	            return false;
	        }

	        // member 테이블에 추가
	        String insertSql =
	            "INSERT INTO member(room, user_id) VALUES(" + roomId + ", '" + loginId + "')";

	        stmt.executeUpdate(insertSql);

	        flag = true;
	        System.out.println("방 참여 성공");

	    } catch (Exception e) {
	        flag = false;
	        System.out.println("방 참여 실패 > " + e.toString());
	    }

	    return flag;
	}
	// 메시지 저장
	public void saveMessage(int room, String nickname, String tx) {
	    try {
	        String sql =
	            "INSERT INTO chat(room, nickname, tx) VALUES('" +  room + "', '" + nickname + "', '" + tx + "')";

	        stmt.executeUpdate(sql);

	    } catch (Exception e) {
	        System.out.println("메시지 저장 실패 > " + e.toString());
	    }
	}

	// 이전 대화 불러오기
	public ArrayList<String> getChatHistory(int room) {
	    ArrayList<String> list = new ArrayList<>();

	    try {
	        String sql =
	            "SELECT nickname, tx FROM chat WHERE room = '" + room + "' ORDER BY num ASC";

	        ResultSet rs = stmt.executeQuery(sql);

	        while (rs.next()) {
	            String nickname = rs.getString("nickname");
	            String tx = rs.getString("tx");

	            list.add("[" + nickname + "] " + tx);
	        }

	    } catch (Exception e) {
	        System.out.println("채팅 내역 조회 실패 > " + e.toString());
	    }

	    return list;
	}
}
