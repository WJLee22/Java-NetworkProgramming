
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class MsgClientGUI extends JFrame {

	private JTextField t_input;
	private JTextArea t_display;
	private JButton b_connect;
	private JButton b_disconnect;
	private JButton b_send;
	private JButton b_exit;
	private String serverAddress;
	private int serverPort;
	private Socket socket;
	private Writer out; //최상위 문자 스트림 클래스 Writer 타입 객체 out
	private boolean Connected = false; // 서버와의 연결 상태를 나타내는 논리변수

	public MsgClientGUI(String serverAddress, int serverPort) {
		super("MsgClient GUI");

		this.serverAddress = serverAddress;
		this.serverPort = serverPort;

		buildGUI();

		setBounds(300, 300, 500, 400);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}

	// 새로운 클라이언트 소켓을만들고 서버에 연결요청
	void connectToServer() {

		try {
			socket = new Socket(serverAddress, serverPort);
			out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8")); 
			// 소켓으로부터 얻은 바이트 스트림을 -> OutputStreamWriter를 통해 문자 스트림으로 변환하고 -> 그것을 BufferedWriter 와 연결하여, 데이터흐름을 버퍼링해서 조절할 수 있도록 출력 스트림 생성.(긴 문자열이더라도 버퍼를통해 효율적으로 송신하기위함-입출력 최소화&입출력 속도향상) 
			//바이트 스트림을 문자 스트림으로 변환할때 2번째 매개변수로, 문자set을 UTF-8로 설정해서 유니코드형식의 인코딩방법을 사용할 수 있도록 지정.
			Connected = true;
			// 접속하기 버튼 클릭후 => 보내기버튼 활성화, 접속하기버튼 비활성화, 종료하기버튼 비활성화, 접속끊기버튼 활성화.
			b_send.setEnabled(true);
			b_connect.setEnabled(false);// 서버에 접속되었으니 다시접속되어지는 일을 수행하지않도록, 접속하기버튼 비활성화.
			b_exit.setEnabled(false);
			b_disconnect.setEnabled(true);

		} catch (UnknownHostException e) { // 소켓 객체가 제대로 생성되지 못했다거나, 해당 host를 제대로 찾지못했을때 
			System.err.println("알 수 없는 서버> " + e.getMessage());
		} 
		catch (IOException e) { // 서버 와의 연결 및 입출력 작업에 문제발생시.
			System.err.println("클라이언트 연결 오류> " + e.getMessage());
		}

	}

	// 서버와의 연결 종료

	public void disconnect() {

		try {
			out.close();
			socket.close();
			Connected = false;
			// 접속 끊기버튼 클릭하여 클라이언트 소켓 종료 후 => 보내기버튼 비활성화, 접속하기버튼 활성화, 종료하기버튼 활성화, 접속끊기버튼
			// 비활성화.
			b_send.setEnabled(false);
			b_connect.setEnabled(true);
			b_exit.setEnabled(true);
			b_disconnect.setEnabled(false);

		} catch (IOException e) {
			System.err.println("클라이언트 닫기 오류> " + e.getMessage());
			System.exit(-1);
		}
	

	}

	public void sendMesssage() {

		if (!Connected)
			return; // 서버와 연결되지 않은 상태에서의 메시지 전송을 방지(ex. 보내기버튼은 비활성화여도 엔터키 입력을통한 전송은 활성화상태. 이 경우를
					// boolean 변수로 제어)

		String msg = t_input.getText();

		if (msg.equals(""))// 아무것도 입력하지않고 보내려고한다면 그냥 return.
			return;

		try {
			((BufferedWriter)out).write(msg+"\n");// 최상위 Writer 타입에서, 실제로 사용하고자하는 BufferedWriter 클래스 타입으로 다운캐스팅.
			//BufferedWriter는 내부적으로 데이터를 임시로 저장할 수 있는 버퍼를 사용.
			//기존에는 정수로 변환하였지만, BufferedWriter 문자 스트림(바이트 대신 문자 처리) 객체이므로 하나의 문자열을 온전하게 전달가능함. 행단위의 전송을 위해서 개행문자를 추가. 서버측에서 행단위로 수신받기때문(개행문자까지를 하나의 행으로 구분함) 
			out.flush(); // BufferedWriter는 버퍼링을 사용하는 버퍼+문자스트림 이기때문에 버퍼가 가득차지않더라도 출력하기위해 flush. 스트림 버퍼 비우기, 데이터 즉시 전송.
			//이 버퍼가 가득 차기 전까지는 실제로 데이터를 출력 스트림으로 전송하지 않음. so, 한 문자열을 제대로 전송하기위해 가득차지않더라도 flush를 통해 출력시킴.
		
		} catch (IOException e) {
			System.out.println("클라이언트 일반 전송 오류> " + e.getMessage());
			
		} finally {
			t_display.append("나: " + msg + "\n"); // 전송된 내용을 t_display에 표시
			t_input.setText("");
		}
	}

	private void buildGUI() {
		JPanel dPanel = createDisplayPanel();
		JPanel icPanel = new JPanel(new GridLayout(2, 1));
		JPanel iPanel = createInputPanel();
		JPanel cPanel = createControlPanel();

		icPanel.add(iPanel);
		icPanel.add(cPanel);

		add(dPanel, BorderLayout.CENTER);
		add(icPanel, BorderLayout.SOUTH);
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

	// input 패널
	private JPanel createInputPanel() {
		JPanel inputPanel = new JPanel(new BorderLayout());
		t_input = new JTextField(30);
		b_send = new JButton("보내기");
		b_send.setEnabled(false);

		t_input.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					sendMesssage();// 텍스트필드에 엔터 입력시, sendMesssage 호출하여 텍스트필드에 입력한 문자열을 서버측 소켓으로전송
				}
			}

		});

		b_send.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				sendMesssage();// 보내기 버튼 클릭시, sendMesssage 호출하여 텍스트필드에 입력한 문자열을 서버측 소켓으로전송
			}
		});

		inputPanel.add(t_input, BorderLayout.CENTER);
		inputPanel.add(b_send, BorderLayout.EAST);

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

		MsgClientGUI client = new MsgClientGUI(serverAddress, serverPort);
	}

}
