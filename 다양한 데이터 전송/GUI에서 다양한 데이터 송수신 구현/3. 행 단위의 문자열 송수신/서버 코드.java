import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class ByteServerGUI extends JFrame {

	private int port;
	private ServerSocket serverSocket;
	private JTextArea t_display;

	public ByteServerGUI(int port) {

		super("ByteServer GUI");

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
				t_display.append("클라이언트가 연결되었습니다.\n");

				receiveMessages(clientSocket);// 생성한 클라이언트 소켓이 연결된 클라이언트측 소켓으로부터 지속적으로 데이터를 전달받아서 처리하도록 지시.
			}

		} catch (IOException e) {
			System.err.println("서버 소켓 종료: " + e.getMessage());
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

		BufferedReader in;

		try {
			in = new BufferedReader(new InputStreamReader(cSocket.getInputStream()));	
			String message;

			while ((message = in.readLine())!=null) {
				//클라이언트가 아무런 출력을 하지 않으면 readLine()은 블록 상태에 들어가고, 데이터가 들어올 때까지 대기
				//클라이언트가 연결을 종료하면 readLine()이 null을 반환한다.

				printDisplay("클라이언트 메시지: " + message);
			}
			t_display.append("클라이언트가 연결을 종료했습니다.\n");

		} catch (IOException e) {
			System.err.println("서버 읽기 오류> " + e.getMessage());
			// System.exit(-1);
		} finally {

			try {
				cSocket.close(); // 클라이언트측 소켓이 연결을 끊었으니, 이제 이 서버측 클라이언트 소켓은 쓸모가없어짐 -> so, 닫아준다.
			} catch (IOException e) {

				System.err.println("서버 읽기 오류> " + e.getMessage());
				// System.exit(-1);

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
					System.exit(0);
				} catch (IOException e1) {
					System.err.println("서버닫기 오류> " + e1.getMessage());
					// System.exit(-1);
				}
			}
		});

		controlPanel.add(b_exit, BorderLayout.CENTER);

		return controlPanel;
	}

	public static void main(String[] args) {
		ByteServerGUI server = new ByteServerGUI(54321);
		server.startServer(); // 서버소켓 생성하면서 서버시작.
	}

}
