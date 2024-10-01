import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class ByteClientGUI extends JFrame {

	private JTextField t_input;
	private JTextArea t_display;
	private JButton b_connect;
	private JButton b_disconnect;
	private JButton b_send;
	private JButton b_exit;
	private String serverAddress;
	private int serverPort;
	private Socket socket;
	private BufferedWriter out;
	private boolean Connected = false; // 서버와의 연결 상태를 나타내는 논리변수

	public ByteClientGUI(String serverAddress, int serverPort) {
		super("ByteClient GUI");

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
			out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));  
			Connected = true;
			// 접속하기 버튼 클릭후 => 보내기버튼 활성화, 접속하기버튼 비활성화, 종료하기버튼 비활성화, 접속끊기버튼 활성화.
			b_send.setEnabled(true);
			b_connect.setEnabled(false);// 서버에 접속되었으니 다시접속되어지는 일을 수행하지않도록, 접속하기버튼 비활성화.
			b_exit.setEnabled(false);
			b_disconnect.setEnabled(true);

		} catch (IOException e) {
			System.out.println("클라이언트 접속 오류> " + e.getMessage());
			// System.exit(-1);

		}

	}

	// 서버와의 연결 종료

	public void disconnect() {

		try {
			socket.close();// out.close()??
			Connected = false;
			// 접속 끊기버튼 클릭하여 클라이언트 소켓 종료 후 => 보내기버튼 비활성화, 접속하기버튼 활성화, 종료하기버튼 활성화, 접속끊기버튼
			// 비활성화.
			b_send.setEnabled(false);
			b_connect.setEnabled(true);
			b_exit.setEnabled(true);
			b_disconnect.setEnabled(false);

		} catch (IOException e) {

			System.out.println("클라이언트 닫기 오류> " + e.getMessage());
			// System.exit(-1);
		}
		// System.exit(0);

	}

	public void sendMesssage() {

		if (!Connected)
			return; // 서버와 연결되지 않은 상태에서의 메시지 전송을 방지(ex. 보내기버튼은 비활성화여도 엔터키 입력을통한 전송은 활성화상태. 이 경우를
					// boolean 변수로 제어)

		String msg = t_input.getText();

		if (msg.equals(""))// if(msg.isEmpty())
			return;

		try {
			out.write(msg+"\n");//문자열 출력. 서버에서는 readLine()을 통해 개행문자까지를 한 문자열로 인식하기떄문에, 문자 출력 버퍼 스트림에 개행문자 포함해서 출력.

			out.flush(); //  버퍼에 있는 데이터를 강제로 출력-스트림 버퍼 비우기, 데이터 즉시 전송
		}

		catch (NumberFormatException e) {// 문자 -> 정수로 변환하는 과정에서, 정수가 아닌 문자들이 입력된경우 발생하는 NumberFormatException 예외처리.
			System.out.println("클라이언트 쓰기 오류(정수만 입력하세요)> " + e.getMessage());
			// System.exit(-1);
		} catch (IOException e) {

			System.out.println("클라이언트 쓰기 오류> " + e.getMessage());
			// System.exit(-1);
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
		t_input = new JTextField();
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

		ByteClientGUI client = new ByteClientGUI(serverAddress, serverPort);
	}

}
