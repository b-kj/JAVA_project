package JAVA_project;

public class Operator {
	DB db = null;
	Login mf = null;
	Join jf = null;
	Room rm = null;

    // 현재 로그인한 사용자 정보
    String loginId = null;
    String loginNick = null;
	
	public static void main(String[] args) {
		Operator opt = new Operator();
		opt.db = new DB();
		opt.mf = new Login(opt);
		opt.jf = new Join(opt);
	}
}
