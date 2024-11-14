import java.io.Serializable;

import javax.swing.ImageIcon;

public class ChatMsg implements Serializable{
	//클라이언트와 서버간에 주고받는 채팅 메시지에 그저 보내고자하는 내용만 담는게아니라, 서버에게 알려줄 정보들을 모드값으로 코드화해서 전달.
	public static final int MODE_LOGIN = 0x1;
	public static final int MODE_LOGOUT = 0x2; //기존에 클라이언트가 연결을 끊기위해서 소켓을 그냥 닫아버리는 처리를 했었음. 이제는 로그아웃 메시지를 보내서 서버에서 해당 클라이언트(헨들러객체)를 제거하는 방식으로 안전하게 처리.
	public static final int MODE_TX_STRING = 0x10;
	public static final int MODE_TX_FILE = 0x20;
	public static final int MODE_TX_IMAGE = 0x40;
	
	String userID;  // 이 필드들은 default 접근지정자로 선언되어 있으므로, 같은 패키지내에서 모두 접근 가능. 별도의 getter/setter 메소드가 필요없이 손쉽게. 
	int mode; 		
	String message;
	ImageIcon image;
	long size;
	
	public ChatMsg(String userID, int mode, String message, ImageIcon image, long size) {
		this.userID = userID;
		this.mode = mode;
		this.message = message;
		this.image = image;
		this.size = size;
	}
	
	public ChatMsg(String UserID, int code, String message, ImageIcon image) {
		this(UserID, code, message, image, 0);
	}
	
	public ChatMsg(String userID, int code) {
		this(userID, code, null, null);
    }
	
	public ChatMsg(String userID, int code, String message) {
		this(userID, code, message, null);
	}
	
	public ChatMsg(String userID, int code, ImageIcon image) {
		this(userID, code, null, image);
	}
	
	public ChatMsg(String userID, int code, String filename,long size) {
		this(userID, code, filename, null, size);
	}

}
