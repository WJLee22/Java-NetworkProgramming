
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class ObjServerGUI extends JFrame {

	private int port;
	private ServerSocket serverSocket=null;
	private JTextArea t_display;

	public ObjServerGUI(int port) {

		super("ObjServer GUI");

		this.port = port;

		buildGUI();

		setBounds(900, 300, 500, 400);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);

	}

	private void startServer() {
		Socket clientSocket = null;
		try {
			serverSocket = new ServerSocket(port);// 해당 포트와 연결된 서버소켓 객체 생성.
			t_display.append("서버가 시작되었습니다.\n");

			while (true) {
				clientSocket = serverSocket.accept(); // 클라이언트측 소켓이 이 서버소켓에게 연결 요청을 보냈고 -> 이를 서버소켓이 수락하면서 해당 클라이언트측 소켓과
														// 연결할 별도의 소켓을 생성-반환.
				printDisplay("클라이언트가 연결되었습니다.");

				//receiveMessages(clientSocket);// 생성한 클라이언트 소켓이 연결된 클라이언트측 소켓으로부터 지속적으로 데이터를 전달받아서 처리하도록 지시.
				
				//그러나 main 스레드만으로(즉, 단일 스레드 환경에서는) 하나의 클라이언트 소켓이 receiveMessages를 통해 클라이언트측과 통신하는 동안에는, 또 다른 클라이언트들로부터 연결 요청을 accept 할 수가 없다.   
				// 즉, 이 클라이언트 소켓이 통신을 모두 끝마쳐야만 연결 요청을 한 다음 클라이언트와 통신이 가능하다는 한계가 존재한다.
				// so, accept 되어 생성된 클라이언트 소켓이 클라이언트측과 통신하는 동작인 receiveMessages를 추가적인 작업 스레드에서 진행하도록 변경하여,   
				// 서버가 동시에 여러 클라이언트로부터 통신이 가능해지도록 변경구현.
			
				ClientHandler cHandler= new ClientHandler(clientSocket);
				cHandler.start(); //작업 스레드 구동.(내부적으로 receiveMessages 동작실행)
			}

		} catch (IOException e) {
			System.err.println("서버 소켓 종료: " + e.getMessage()); //서버 소켓 생성중, 해당 포트가 이미 사용중이어서 소켓 생성이 불가한경우 예외처리.=> 근데 왜 종료버튼 누르면 이게 출력되지??
			System.exit(0);
		} finally {
			try {
				if (clientSocket != null)
					clientSocket.close(); // 클라이언트 소켓 닫기. startServer메서드가 여러 클라이언트와의 연결을 처리하는 동안, 만약 startServer가 예외로 인해
											// 종료되거나 루프가 끝날 때, clientSocket 변수가 이전 클라이언트 소켓을 가리키고 있을 수 있다. 이 경우떄문에
											// clientSocket을 안전하게 닫아주는 작업을 혹시모르니 해준다.
			} catch (IOException e) {

				System.err.println("서버닫기 오류> " + e.getMessage());
				// System.exit(-1);
			}
		}

	}

	// 클라이언트측으로부터 전달받은 데이터를 textarea에 출력해주는 메서드
	private void printDisplay(String msg) {

		t_display.append(msg + "\n");
		t_display.setCaretPosition(t_display.getDocument().getLength());// t_display에 출력이 누적되먼서, t_display를 자동으로 스크롤
																		// 다운되도록. Caret의 위치를 t_display의 가장 끝 아래부분으로 강제
																		// 이동시키도록 설정하도록하는 구문.
	}

	// 클라이언트측으로부터 지속적으로 데이터를 전달받는 메서드
	private void receiveMessages(Socket cSocket) {

		ObjectInputStream in=null;

		try {
			in = new ObjectInputStream(new BufferedInputStream(cSocket.getInputStream())); 
			
			String message; //String 형으로 데이터를 받을 예정.(ObjectOutputStream 으로 받은 객체의 필드 문자열을 읽어와야함)
			
			while (true) {
			
				message= ((TestMsg)in.readObject()).toString();
				//readInt을 통해 스트림으로 들어온 바이트를 정수형으로 변환하여 읽음. 
				
				printDisplay("클라이언트 메시지: " + message);
			}


		} catch (IOException e) {
			
			printDisplay("클라이언트가 연결을 종료했습니다.");
		} catch (ClassNotFoundException e) {  //ObjectOutputStream으로부터 얻어온 객체를 TestMsg타입으로 캐스팅할때 발생하는 캐스팅오류 예외처리.
			System.err.println("객체 전달 오류> " + e.getMessage());
			//System.exit(-1);
		}
		
		
		finally {

			try {
				in.close();
				cSocket.close(); // 클라이언트측 소켓이 연결을 끊었으니, 이제 이 서버측 클라이언트 소켓은 쓸모가없어짐 -> so, 닫아준다.
			} catch (IOException e) {

				System.err.println("서버 읽기 오류> " + e.getMessage());
				System.exit(-1);

			}
		}
	}

	private void buildGUI() {
		JPanel dPanel = createDisplayPanel();
		JPanel cPanel = createControlPanel();

		add(dPanel, BorderLayout.CENTER);
		add(cPanel, BorderLayout.SOUTH);
	}

	// 디스플레이 패널
	private JPanel createDisplayPanel() {
		JPanel dispalyPanel = new JPanel(new BorderLayout());
		t_display = new JTextArea();
		JScrollPane scroll = new JScrollPane(t_display);
		t_display.setEditable(false);

		dispalyPanel.add(scroll);

		return dispalyPanel;
	}

	// control 패널
	private JPanel createControlPanel() {
		JPanel controlPanel = new JPanel(new BorderLayout());

		JButton b_exit = new JButton("종료");

		b_exit.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {// 종료버튼이 눌려지면 서버소켓을 닫아준다. 이로써 프로그램 종료.
				try {
					serverSocket.close();
					System.exit(-1);
				} catch (IOException e1) {
					System.err.println("서버닫기 오류> " + e1.getMessage());
					// System.exit(-1);
				}
			}
		});

		controlPanel.add(b_exit, BorderLayout.CENTER);

		return controlPanel;
	}

	
	//클라이언트와 통신하는 작업을 별도의 작업 스레드에서 처리하도록하자.
	private class ClientHandler extends Thread{
		private Socket clientSocket; //클라이언트와 통신하는 주체인 클라이언트 소켓
		
		public ClientHandler(Socket clientSocket) {
			this.clientSocket=clientSocket; //작업 스레드 생성시, 매개변수 값으로 클라이언트 소켓 넘겨받음.
		}

		@Override
		public void run() { //작업 스레드 실행 -> 작업 스레드를 통해 클라이언트와 통신 시작. (여러 작업 스레드: 동시에 여러 클라이언트와 통신이 가능해짐) 
			receiveMessages(clientSocket);
		}
		
		
	}
	
	
	public static void main(String[] args) {
		ObjServerGUI server = new ObjServerGUI(54321);
		server.startServer(); // 서버소켓 생성하면서 서버시작.
	}

}
