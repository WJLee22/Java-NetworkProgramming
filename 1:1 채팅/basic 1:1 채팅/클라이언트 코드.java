

import java.awt.BorderLayout;
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
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class P2PHsTalk extends JFrame {

	private JTextField t_input;
	private JTextArea t_display;
	private JButton b_connect;
	private JButton b_disconnect;
	private JButton b_send;
	private JButton b_exit;
	private String serverAddress;
	private int serverPort;
	private Socket socket;
	private Writer out; 
	private Reader in; 


	public P2PHsTalk(String serverAddress, int serverPort) {
		super("P2P Handshake Talk");

		this.serverAddress = serverAddress;
		this.serverPort = serverPort;

		buildGUI();

		setBounds(300, 300, 500, 400);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}

	private void connectToServer()  {

		try {
			socket = new Socket(serverAddress, serverPort);
			out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8")); 
			in = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));
	


		} catch (UnknownHostException e) { 
			System.err.println("알 수 없는 서버> " + e.getMessage());
		} 
		catch (IOException e) {
			System.err.println("클라이언트 연결 오류> " + e.getMessage());
		}

	}

	// 서버와의 연결 종료

	private void disconnect() {

		try {
			socket.close();


		} catch (IOException e) {
			System.err.println("클라이언트 닫기 오류> " + e.getMessage());
			System.exit(-1);
		}
	

	}

	private void sendMesssage() {

		String msg = t_input.getText();

		if (msg.isEmpty())// 아무것도 입력하지않고 보내려고한다면 그냥 return.
			return;

		
			try {
				((BufferedWriter)out).write(msg+"\n");
				out.flush();
				
				t_display.append("나: " + msg + "\n"); 
			} catch (IOException e) {
				System.err.println("클라이언트 일반 전송 오류> " + e.getMessage());
				System.exit(-1);
			} 
		

			t_input.setText("");
			
			
	}
	
	//클라이언트가 서버에게 메시지를 보내고나서, 서버가 나에게 반향하는 메시지를 수신.
	private void receiveMessage() {
		
		try {
			String inMsg=((BufferedReader)in).readLine();
			t_display.append("서버:\t"+inMsg+"\n");
		} catch (IOException e) {
			System.err.println("클라이언트 일반 수신 오류> " + e.getMessage());
		}
		
	}
	
	
	
	private void buildGUI() {
		
		JPanel p_input = new JPanel(new GridLayout(2, 1));
		p_input.add(createInputPanel());
		p_input.add(createControlPanel());
		add(createDisplayPanel(), BorderLayout.CENTER);
		add(p_input, BorderLayout.SOUTH);
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


		t_input.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					sendMesssage();// 텍스트필드에 엔터 입력시, sendMesssage 호출하여 텍스트필드에 입력한 문자열을 서버측 소켓으로전송
					receiveMessage(); // 내가 메시지를 보내야지만 내가 메시지를 받을수있게됨. 이것이 한계.
				}
			}

		});

		b_send.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				sendMesssage();// 보내기 버튼 클릭시, sendMesssage 호출하여 텍스트필드에 입력한 문자열을 서버측 소켓으로전송
				receiveMessage();  // 내가 메시지를 보내야지만 내가 메시지를 받을수있게됨. 이것이 한계.
			}
		});

		inputPanel.add(t_input, BorderLayout.CENTER);
		inputPanel.add(b_send, BorderLayout.EAST);
		
		t_input.setEnabled(false);
		b_send.setEnabled(false);
		return inputPanel;
	}

	// control 패널
	private JPanel createControlPanel() {
		JPanel controlPanel = new JPanel(new GridLayout(1,0));
		b_connect = new JButton("접속하기");
		b_disconnect = new JButton("접속 끊기");
		b_disconnect.setEnabled(false);
		b_exit = new JButton("종료하기");

		b_connect.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				connectToServer(); // 접속하기버튼 클릭시 서버에 접속요청.
				b_send.setEnabled(true);
				b_connect.setEnabled(false);
				b_exit.setEnabled(false);
				b_disconnect.setEnabled(true);
				t_input.setEnabled(true);

			}
		});

		b_disconnect.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				disconnect(); // 접속끊기버튼 클릭시 서버와 연결종료.
				
				b_send.setEnabled(false);
				b_connect.setEnabled(true);
				b_exit.setEnabled(true);
				b_disconnect.setEnabled(false);
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

		P2PHsTalk client = new P2PHsTalk(serverAddress, serverPort);
	}

}
