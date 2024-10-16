import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class MultiChatServer extends JFrame {

	private int port;
	private ServerSocket serverSocket;
	private Thread acceptThread=null;
	private Vector<ClientHandler>users=new Vector<ClientHandler>();
	private JTextArea t_display;
	private JButton b_connect;
	private JButton b_disconnect;
	private JButton b_exit;
	private JTextField t_input;
	private JButton b_send;
	
	
	private BufferedReader in; 
	private BufferedWriter out;

	
	public MultiChatServer(int port) {

		super("Multi Chat Server");

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
			printDisplay("서버가 시작되었습니다.");

			while (acceptThread == Thread.currentThread()) {//현재 acceptThread값이 null값이거나, 다른 스레드가 생성되어서 현재 스레드가 더 이상 이 while문 처리할 필요가 없음.
				clientSocket = serverSocket.accept(); // 클라이언트측 소켓이 이 서버소켓에게 연결 요청을 보냈고 -> 이를 서버소켓이 수락하면서 해당 클라이언트측 소켓과
														// 연결할 별도의 소켓을 생성-반환.
				printDisplay("클라이언트가 연결되었습니다.");

				ClientHandler cHandler = new ClientHandler(clientSocket);
				cHandler.start();
			}

		}catch (SocketException e) {
		//	System.err.println("서버 소켓 종료: " + e.getMessage());
			printDisplay("서버 소켓 종료");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (clientSocket != null)
					clientSocket.close(); 
				if (serverSocket != null)
					serverSocket.close(); 						
											
			} catch (IOException e) {

				System.err.println("서버닫기 오류> " + e.getMessage());
				System.exit(-1);
			}
		}

	}
	
	private void disconnect() {

		try {
			acceptThread=null;		
			serverSocket.close();

			b_send.setEnabled(false);
			b_connect.setEnabled(true);
			b_exit.setEnabled(true);
			b_disconnect.setEnabled(false);

		} catch (IOException e) {
			System.err.println("서버소켓 닫기 오류> " + e.getMessage());
			System.exit(-1);
		}
	}
	

	// 클라이언트측으로부터 전달받은 데이터를 textarea에 출력해주는 메서드
	private void printDisplay(String msg) {

		t_display.append(msg + "\n");
		t_display.setCaretPosition(t_display.getDocument().getLength());
																		
																		
	}


	private void buildGUI() {

		add(createDisplayPanel(), BorderLayout.CENTER);
		
		JPanel p_input = new JPanel(new GridLayout(2, 1));
		p_input.add(createInputPanel());
		p_input.add(createControlPanel());
		add(p_input, BorderLayout.SOUTH);
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
					//receiveMessage(); // 내가 메시지를 보내야지만 내가 메시지를 받을수있게됨. 이것이 한계.
				}
			}

		});

		b_send.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				sendMesssage();// 보내기 버튼 클릭시, sendMesssage 호출하여 텍스트필드에 입력한 문자열을 서버측 소켓으로전송
				//receiveMessage();  // 내가 메시지를 보내야지만 내가 메시지를 받을수있게됨. 이것이 한계.
			}
		});

		inputPanel.add(t_input, BorderLayout.CENTER);
		inputPanel.add(b_send, BorderLayout.EAST);

		return inputPanel;
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
		JPanel controlPanel = new JPanel(new GridLayout(1,0));
		b_connect = new JButton("서버 시작");
		b_disconnect = new JButton("서버 종료");
		b_disconnect.setEnabled(false);
		b_exit = new JButton("종료");

		b_connect.addActionListener(new ActionListener() {


			@Override
			public void actionPerformed(ActionEvent e) {
				//startServer(); //기존에는 프로그램실행 시 자동 -> 서버 시작 버튼 클릭시 시작되도록 변경.
				
				acceptThread = new Thread(new Runnable() {
					
					@Override
					public void run() {
						startServer(); //작업 스레드에서 서버 시작.
					}
				});
				acceptThread.start(); //Job-Thread is Runnable.
				
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



		b_exit.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent ae) {// 종료버튼이 눌려지면 서버소켓을 닫아준다. 이로써 프로그램 종료.
				try {
					if(serverSocket !=null) serverSocket.close();
				} catch (IOException e) {
					System.err.println("서버닫기 오류> " + e.getMessage());
				}
			}
		});

		controlPanel.add(b_exit, BorderLayout.CENTER);

		return controlPanel;
	}
	

	private void sendMesssage() {

		String msg = t_input.getText();

		if (msg.equals(""))// 아무것도 입력하지않고 보내려고한다면 그냥 return.
			return;

		
			try {
				((BufferedWriter)out).write(msg+"\n");
				out.flush();
			} catch (IOException e) {
				System.err.println("클라이언트 일반 전송 오류> " + e.getMessage());
				System.exit(-1);
			} 
		
			t_display.append("나: " + msg + "\n"); 
			t_input.setText("");
			
			
	}
	
	private class ClientHandler extends Thread{
		private Socket clientSocket; 
		
		public ClientHandler(Socket clientSocket) {
			this.clientSocket=clientSocket; 
			
			t_input.setEnabled(true);
			b_send.setEnabled(true);
		}
		// 클라이언트측으로부터 지속적으로 데이터를 전달받는 메서드
		private void receiveMessages(Socket cSocket) {

			/* BufferedReader in; */
			/* BufferedWriter out; */
			try {
				in = new BufferedReader(new InputStreamReader(cSocket.getInputStream(), "UTF-8"));
				out = new BufferedWriter(new OutputStreamWriter(cSocket.getOutputStream(), "UTF-8"));
				String message;
				
				while ((message = in.readLine()) != null) {

					printDisplay("클라이언트 메시지: " + message);
//					out.write("'" + message + "' ... echo\n"); // 클라이언트에게 수신받은 데이터를 가공해서, 다시 클라이언트에게 반향.
//					out.flush(); 
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
					System.exit(-1);

				}
			}
		}
		@Override
		public void run() { 
			receiveMessages(clientSocket);
		}
		
		
	}
	
	public static void main(String[] args) {
		MultiChatServer server = new MultiChatServer(54321);
		//server.startServer(); // 서버소켓 생성하면서 서버시작.
	}

}
