package JAVA_project;

public class Operator {
	DB db = null;
	GUI mf = null;
	
	public static void main(String[] args) {
		Operator opt = new Operator();
		opt.db = new DB();
		opt.mf = new GUI(opt);
	}
}
