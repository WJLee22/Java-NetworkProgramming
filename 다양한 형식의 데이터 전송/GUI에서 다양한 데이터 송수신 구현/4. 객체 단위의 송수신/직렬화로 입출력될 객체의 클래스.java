import java.io.Serializable;

public class TestMsg implements Serializable{
//  직렬화로 입출력될 객체의 클래스. Serializable implements하여 구현.
//  직렬화 하는 이유: 객체를 바이트 단위로 입출력할때, 복잡한 구조의 객체를
//	바이트의 흐름속에서 객체의 상태를 그대로 유지하면서 하나의 흐름으로 전송하고
//	그 객체를 서버에서 온전히 복원할 수 있도록 직렬화가 유용

	String msg; //서버로 전달될 메시지. 
	
	public TestMsg(String msg) {
		this.msg=msg;
	}
	
	@Override
	public String toString() { //toString 오버라이드하여, 메시지 읽을때 양 사이드에 [] 추가하여 읽음.
		return "[" +msg +"]";
	}
}
