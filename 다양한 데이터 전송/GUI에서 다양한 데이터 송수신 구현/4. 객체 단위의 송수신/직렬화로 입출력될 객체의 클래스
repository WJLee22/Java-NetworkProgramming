import java.io.Serializable;

public class TestMsg implements Serializable{
// 직렬화로 입출력될 객체의 클래스. Serializable implements하여 구현.
	
	String msg; //서버로 전달될 메시지. 
	
	public TestMsg(String msg) {
		this.msg=msg;
	}
	
	@Override
	public String toString() { //toString 오버라이드하여, 메시지 읽을때 양 사이드에 [] 추가하여 읽음.
		return "[" +msg +"]";
	}
}
