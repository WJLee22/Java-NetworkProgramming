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
import java.net.InetAddress;
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
	private Vector<ClientHandler>users=new Vector<ClientHandler>(); //현재 서버에 접속한 클라이언트들을 저장해두는 Vector객체.
	private JTextArea t_display;
	private JButton b_connect;
	private JButton b_disconnect;
	private JButton b_exit;
	
	//private BufferedReader in; 
	//private BufferedWriter out;

	
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
		InetAddress inetAddress = null;
		
		try {
			serverSocket = new ServerSocket(port);// 해당 포트와 연결된 서버소켓 객체 생성.
			inetAddress= InetAddress.getLocalHost();
			printDisplay("서버가 시작되었습니다: " + inetAddress.getHostAddress());

			while (acceptThread == Thread.currentThread()) {//현재 acceptThread값이 null값이거나, 다른 스레드가 생성되어서 현재 스레드가 더 이상 이 while문 처리할 필요가 없음.
				clientSocket = serverSocket.accept(); // 클라이언트측 소켓이 이 서버소켓에게 연결 요청을 보냈고 -> 이를 서버소켓이 수락하면서 해당 클라이언트측 소켓과
														// 연결할 별도의 소켓을 생성-반환.
				
				String cAddress = clientSocket.getInetAddress().getHostAddress();
				printDisplay("클라이언트가 연결되었습니다: " +cAddress);

				ClientHandler cHandler = new ClientHandler(clientSocket);
				users.add(cHandler); //새로 연결된 클라이언트를 사용자로써 사용자벡터에 추가.
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

			//b_send.setEnabled(false);
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
		add(createControlPanel(), BorderLayout.SOUTH);
	}

	
//	// input 패널
//	private JPanel createInputPanel() {
//		JPanel inputPanel = new JPanel(new BorderLayout());
//		t_input = new JTextField(30);
//		b_send = new JButton("보내기");
//		b_send.setEnabled(false);
//
//		t_input.addKeyListener(new KeyAdapter() {
//
//			@Override
//			public void keyPressed(KeyEvent e) {
//				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
//					sendMesssage();// 텍스트필드에 엔터 입력시, sendMesssage 호출하여 텍스트필드에 입력한 문자열을 서버측 소켓으로전송
//					//receiveMessage(); // 내가 메시지를 보내야지만 내가 메시지를 받을수있게됨. 이것이 한계.
//				}
//			}
//
//		});
//
//		b_send.addActionListener(new ActionListener() {
//
//			@Override
//			public void actionPerformed(ActionEvent e) {
//
//				sendMesssage();// 보내기 버튼 클릭시, sendMesssage 호출하여 텍스트필드에 입력한 문자열을 서버측 소켓으로전송
//				//receiveMessage();  // 내가 메시지를 보내야지만 내가 메시지를 받을수있게됨. 이것이 한계.
//			}
//		});
//
//		inputPanel.add(t_input, BorderLayout.CENTER);
//		inputPanel.add(b_send, BorderLayout.EAST);
//
//		return inputPanel;
//	}
	
	
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
	

//	private void sendMesssage() {
//
//		String msg = t_input.getText();
//
//		if (msg.equals(""))// 아무것도 입력하지않고 보내려고한다면 그냥 return.
//			return;
//
//		
//			try {
//				((BufferedWriter)out).write(msg+"\n");
//				out.flush();
//			} catch (IOException e) {
//				System.err.println("클라이언트 일반 전송 오류> " + e.getMessage());
//				System.exit(-1);
//			} 
//		
//			t_display.append("나: " + msg + "\n"); 
//			t_input.setText("");
//			
//			
//	}
	
	private class ClientHandler extends Thread{
		private Socket clientSocket; 
		private BufferedWriter out; //이 ClientHandler작업 스레드마다 특정 클라이언트에 별도로 출력하도록, ClientHandler 내부로 out 스트림객체 이동시킴.
		private String uid;
		
		public ClientHandler(Socket clientSocket) {
			this.clientSocket=clientSocket; 
		
		}
		// 클라이언트측으로부터 지속적으로 데이터를 전달받는 메서드
		private void receiveMessages(Socket cSocket) {


			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(cSocket.getInputStream(), "UTF-8"));
				out = new BufferedWriter(new OutputStreamWriter(cSocket.getOutputStream(), "UTF-8"));
				String message;
				
				while ((message = in.readLine()) != null) { //상대 클라이언트로부터 연결이 끊어지지 않는 한 계속해서 메시지를 수신.
					if(message.contains("/uid:")) { //사용자 아이디 메시지라면
						String token []= message.split(":");
						uid=token[1]; // : 다음의 문자열을 추출하여 uid에 저장.
						printDisplay("새 참가자: "+uid);
						printDisplay("현재 참가자 수: "+users.size());
						continue;
					}
					message = uid + ": " + message; //일반 메시지라면
					printDisplay(message);
					broadcating(message); //현재 서버에 접속한 모든 클라이언트에게 메시지 전송.
					
				}				
				
				users.remove(this); //연결이 끊은 클라이언트를 사용자벡터에서 제거.
				printDisplay(uid+" 퇴장. 현재 참가자 수 : " + users.size());

			} catch (IOException e) { //의도적으로, 정상적으로 연결을 끊은 경우가 아니라 연결상태 불량으로 인한 연결 끊김인 경우.
				users.remove(this);
				printDisplay(uid+" 연결 끊김. 현재 참가자 수 : " + users.size());
			} finally {
						
				try {
					cSocket.close(); 
				} catch (IOException e) {

					System.err.println("서버 닫기 오류> " + e.getMessage());
					System.exit(-1);

				}
			}
		}
		
		private void sendMesssage(String msg) { //특정 클라이언트에게 메시지를 보내는 메서드인 sendMesssage를 작업스레드 내부로 이동시킴.
		
					try {
						((BufferedWriter)out).write(msg+"\n");
						out.flush();
					} catch (IOException e) {
						System.err.println("클라이언트 일반 전송 오류> " + e.getMessage());
					} 
				
			}
		
			private void broadcating(String msg) { //현재 서버에 접속한 모든 클라이언트에게 메시지를 보내는 메서드.=> 이것이 다자간 채팅의 핵심.
				for (ClientHandler client : users) {
					client.sendMesssage(msg);
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
