import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class ScoreServerGUI extends JFrame {

	private int port;
	private ServerSocket serverSocket;
	private JTextArea t_display;
	private ScoreManager sMgr = null; // 성적관리를 위한 객체.

	public ScoreServerGUI(int port) {

		super("ScoreServer GUI");

		this.port = port;

		buildGUI();

		setBounds(900, 300, 500, 400);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);

		sMgr = new ScoreManager("score.txt");// 프레임 화면 구성이 모두끝나고나서, 서버소켓을 생성하고 클라이언트로부터 접속을받을 수 있는 준비를 하기전에
												// scoreManager 객체를 먼저 생성을해서, 현재 경로에있는 score.txt 라는 파일로부터 데이터읽어들이기.

		startServer(); // 서버시작.
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

		BufferedReader in;
		BufferedWriter out;
		try {
			in = new BufferedReader(new InputStreamReader(cSocket.getInputStream(), "UTF-8"));
			out = new BufferedWriter(new OutputStreamWriter(cSocket.getOutputStream(), "UTF-8"));

			String name; // 입력 스트림으로부터 읽어들일 이름데이터

			while ((name = in.readLine()) != null) {
				String grade = sMgr.get(name); // 읽어들인 이름에 해당하는 학점 반환.

				if (grade == null) // 해쉬맵에 해당 이름을 가진 학생에대한 성적 정보가 없음.
					grade = "성적 정보가 없음";

				String message = name + ": " + grade + " (" + cSocket.getInetAddress().getHostAddress() + ")";
				printDisplay(message);
				out.write(grade + "\n"); // 클라이언트에게 수신받은 데이터를 가공해서, 다시 클라이언트에게 반향.
				out.flush();
			}
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

	private class ClientHandler extends Thread {
		private Socket clientSocket;

		public ClientHandler(Socket clientSocket) {
			this.clientSocket = clientSocket;
		}

		@Override
		public void run() {
			receiveMessages(clientSocket);
		}
	}

	private class ScoreManager {
		private HashMap<String, String> map = new HashMap<String, String>(); // HashMap을 통해 학생 이름과 학점정보를 저장관리.

		ScoreManager(String fileName) {
			try {
				Scanner reader = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(fileName),"UTF-8")));// FileReader는 지정된 파일을 읽어들이는 클래스. Scanner는
																		// FileReader를 감싸서 파일에서 데이터를 읽을 수 있게 해줌.
				// !!!! Windows 환경에서 UTF-8 로 인코딩되어진 txt파일을 처리하고자한다면 
				// 단순히 FileReader 만 쓰면안되고, 위처럼 FileInputStream을 InputStreamReader과 연결하여 파일로부터 읽은 바이트 데이터를 -> 문자스트림으로 변환할때 
				//"UTF-8" 인코딩 타입으로 지정하여, UTF-8 형식의 txt 파일 입력받을 수 있도록처리.
				//읽어들인 바이트를 UTF-8 형식의 문자로 변환하는 것임.
				//효율적인 처리를위해, 거기에 버퍼스트림 연결하여 처리.
				// 파일로부터 읽어들인 바이트 데이터-> 문자스트림으로 변환(utf-8로 인코딩해서) -> 버퍼링해서 -> Scanner 객체에게 전달.

				// 파일이 잘 읽어들여졌다면
				while (reader.hasNext()) {
					String name = reader.next();// 공백 기준으로 읽어들임
					String grade = reader.next();

					map.put(name, grade);// 읽어들인 이름과 성적을 맵에 저장
				}
				reader.close();

				printDisplay("데이터 파일 불러오기");// 데이터 파일 불러오는 처리가 끝났음을 알림.
			} catch (FileNotFoundException e) { // 해당 파일이 존재하지않는다면
				System.err.println("데이터 파일이 없습니다: " + e.getMessage());
				System.exit(-1);
			} catch (UnsupportedEncodingException e) {//인코딩과정에서 발생하는 예외처리
				System.err.println("파일의 인코딩 형식을 확인하세요: " + e.getMessage());
				System.exit(-1);
			}

		}

		String get(String name) { // 지정된 이름에 따른 학점을 반환해주는 메서드.
			return map.get(name);
		}
	}

	public static void main(String[] args) {
		ScoreServerGUI server = new ScoreServerGUI(54321);

	}

}
