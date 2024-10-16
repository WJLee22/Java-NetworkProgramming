
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ScoreClientGUI extends JFrame {

	private JTextField t_name;
	private JLabel l_grade;
	private JButton b_connect;
	private JButton b_disconnect;
	private JButton b_exit;
	private String serverAddress;
	private int serverPort;
	private Socket socket;
	private Writer out;
	private Reader in;

	public ScoreClientGUI(String serverAddress, int serverPort) {
		super("ScoreClient GUI");

		this.serverAddress = serverAddress;
		this.serverPort = serverPort;

		buildGUI();

		setBounds(300, 300, 500, 400);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}

	private void connectToServer() {

		try {
			socket = new Socket(serverAddress, serverPort);
			out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
			in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

		} catch (UnknownHostException e) {
			System.err.println("알 수 없는 서버> " + e.getMessage());
		} catch (IOException e) {
			System.err.println("클라이언트 연결 오류> " + e.getMessage());
		}

	}

	// 서버와의 연결 종료

	private void disconnect() {

		try {
			out.close();
			socket.close();

			b_connect.setEnabled(true);
			b_exit.setEnabled(true);
			b_disconnect.setEnabled(false);

		} catch (IOException e) {
			System.err.println("클라이언트 닫기 오류> " + e.getMessage());
			System.exit(-1);
		}

	}

	private void sendMesssage() {

		String msg = t_name.getText().trim();// 메시지 전송시 문자열 앞뒤에 불필요한 공백이 존재할 수 있으니 공백제거.
		// 공백이 있으면, 서버에서 메시지 수신시 파일에서 해당 이름 검색시 못 찾게됨

		if (msg.isEmpty())// 아무것도 입력하지않고 보내려고한다면 그냥 return.
			return;

		try {
			((BufferedWriter) out).write(msg + "\n");
			out.flush();
		} catch (IOException e) {
			System.err.println("클라이언트 일반 전송 오류> " + e.getMessage());
			System.exit(-1);
		}

		// receiveMessage(); // 출력데이터 없이 보내기버튼 클릭시, readLine이 서버로부터 데이터를 수신할 때까지 무한
		// 대기상태에 빠지게되는 문제 해결을 위해
		// sendMesssage 내부에서 receiveMessage 호출하도록 수정.
		// 기존 코드의 경우, 텍스트입력없어서 return 되더라도 반환후 receiveMessage을 호출하는 로직이었기에, 서버는 당연히 데이터
		// 값을 전달받지못했고
		// 그에 따라 클라이언트도 서버로부터 반향받지 못하는 것인데, receiveMessage에서 readLine은 입력값 받을떄까지 무한정
		// 대기하게되는 심각한 오류 발생하게됨.
	}

	// 클라이언트가 서버에게 메시지를 보내고나서, 서버가 나에게 반향하는 메시지를 수신.
	private void receiveMessage() {

		try {
			String grade = ((BufferedReader) in).readLine();
			l_grade.setText("학점 : " + grade);
		} catch (IOException e) {
			System.err.println("클라이언트 일반 수신 오류> " + e.getMessage());
			System.exit(-1);
		}

	}

	private void buildGUI() {

		JPanel iPanel = createInputPanel();
		JPanel cPanel = createControlPanel();

		add(iPanel, BorderLayout.CENTER);
		add(cPanel, BorderLayout.SOUTH);
	}

	// input 패널
	private JPanel createInputPanel() {
		JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		t_name = new JTextField(10);

		t_name.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					sendMesssage();// 텍스트필드에 엔터 입력시, sendMesssage 호출하여 텍스트필드에 입력한 문자열을 서버측 소켓으로전송
					receiveMessage();
				}
			}

		});

		t_name.setEnabled(false); // 서버에 접속하기전에 이름을 입력받아서 이벤트가 발생되는 경우를 방지.

		l_grade = new JLabel("학점 확인");

		inputPanel.add(new JLabel("이름: "));
		inputPanel.add(t_name);
		inputPanel.add(l_grade);

		return inputPanel;
	}

	// control 패널
	private JPanel createControlPanel() {
		JPanel controlPanel = new JPanel(new GridLayout());
		b_connect = new JButton("접속하기");
		b_disconnect = new JButton("접속 끊기");
		b_disconnect.setEnabled(false);
		b_exit = new JButton("종료하기");

		b_connect.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				connectToServer(); // 접속하기버튼 클릭시 서버에 접속요청.
				t_name.setEnabled(true);
				b_connect.setEnabled(false);
				b_exit.setEnabled(false);
				b_disconnect.setEnabled(true);
			}
		});

		b_disconnect.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				disconnect(); // 접속끊기버튼 클릭시 서버와 연결종료.

			}
		});

		b_exit.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0); // 프로그램 정상 종료.
			}
		});

		controlPanel.add(b_connect);
		controlPanel.add(b_disconnect);
		controlPanel.add(b_exit);

		return controlPanel;
	}

	public static void main(String[] args) {
		String serverAddress = "localhost"; // 연결하고자하는 서버의 주소는 로컬호스트. 즉, 내 컴퓨터.
		int serverPort = 54321;

		ScoreClientGUI client = new ScoreClientGUI(serverAddress, serverPort);
	}

}
