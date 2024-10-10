import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class TimeServerGUI extends JFrame {

	private int port;
	private ServerSocket serverSocket;
	private JTextArea t_display;

	public TimeServerGUI(int port) {

		super("TimeServer GUI");

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

				ClientHandler cHandler = new ClientHandler(clientSocket);
				cHandler.start();
			}

		} catch (IOException e) {
			System.err.println("서버 소켓 종료: " + e.getMessage());
			System.exit(0);
		} finally {
			try {
				if (clientSocket != null)
					clientSocket.close(); 
											
											
			} catch (IOException e) {

				System.err.println("서버닫기 오류> " + e.getMessage());
				// System.exit(-1);
			}
		}

	}

	// 클라이언트측으로부터 전달받은 데이터를 textarea에 출력해주는 메서드
	private void printDisplay(String msg) {

		t_display.append(msg + "\n");
		t_display.setCaretPosition(t_display.getDocument().getLength());
																		
																		
	}

	// 클라이언트측으로부터 지속적으로 데이터를 전달받는 메서드
	private void receiveMessages(Socket cSocket) {

		//BufferedReader in;
		BufferedWriter out;
		try {
			//in = new BufferedReader(new InputStreamReader(cSocket.getInputStream(), "UTF-8"));
			out = new BufferedWriter(new OutputStreamWriter(cSocket.getOutputStream(), "UTF-8"));
			String message;
			//cSocket.getInetAddress().getHostAddress() 는 클라이언트 소켓에 연결된 클라이언트의 IP 주소를 가져오는 부분
			message= cSocket.getInetAddress().getHostAddress() + ": "+ new Date().toString();
			//while ((message = in.readLine()) != null) {
				
				printDisplay("클라이언트 메시지: " + message);
				out.write(message + "\n"); // 클라이언트에게 수신받은 데이터를 가공해서, 다시 클라이언트에게 반향.
				out.flush(); 
			//}
			t_display.append("클라이언트가 연결을 종료했습니다.\n");

		} catch (IOException e) {
			System.err.println("서버 읽기 오류> " + e.getMessage());
			// System.exit(-1);
		} finally {
					
			try {
				cSocket.close(); 
			} catch (IOException e) {

				System.err.println("서버 읽기 오류> " + e.getMessage());


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
	
	
	private class ClientHandler extends Thread{
		private Socket clientSocket; 
		
		public ClientHandler(Socket clientSocket) {
			this.clientSocket=clientSocket; 
		}

		@Override
		public void run() { 
			receiveMessages(clientSocket);
		}
		
		
	}
	
	public static void main(String[] args) {
		TimeServerGUI server = new TimeServerGUI(54321);
		server.startServer(); // 서버소켓 생성하면서 서버시작.
	}

}
