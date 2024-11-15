import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class WithChatServer extends JFrame {

	private int port;
	private ServerSocket serverSocket = null;
	private Thread acceptThread=null;
	private Vector<ClientHandler>users=new Vector<ClientHandler>(); //현재 서버에 접속한 클라이언트들을 저장해두는 Vector객체.
	private JTextArea t_display;
	private JButton b_connect, b_disconnect, b_exit;
	
	
	public WithChatServer(int port) {

		super("With ChatServer");

		this.port = port;

		buildGUI();

		setBounds(900, 300, 500, 400);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);

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
	

	private String getLocalAddr() {
		String addr = null;

		try {
			addr = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			System.err.println("알 수 없는 서버주소> " + e.getMessage());
			
		}

		return addr;
	}
	

	private void startServer() {
		Socket clientSocket = null;
		
		try {
			serverSocket = new ServerSocket(port);// 해당 포트와 연결된 서버소켓 객체 생성.
			printDisplay("서버가 시작되었습니다: " + getLocalAddr());

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
		private BufferedOutputStream bos;
		private ObjectOutputStream out; //이 ClientHandler작업 스레드마다 특정 클라이언트에 별도로 출력하도록, ClientHandler 내부로 out 스트림객체 이동시킴.
		private BufferedInputStream bis;
		private String uid; //어떤 사용자가 연결되었는지를 구분하기위해 저장하는 변수.
		
		public ClientHandler(Socket clientSocket) {
			this.clientSocket=clientSocket; 
		}
		// 클라이언트측으로부터 지속적으로 데이터를 전달받는 메서드
		private void receiveMessages(Socket cSocket) {

			try {
				ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(cSocket.getInputStream())); //in 은 이 메서드 내에서만 활용하니깐.
				out = new ObjectOutputStream(new BufferedOutputStream(cSocket.getOutputStream()));
				bis = new BufferedInputStream(cSocket.getInputStream()); //파일데이터를 읽어들이기 위한 입력스트림
				bos= new BufferedOutputStream(cSocket.getOutputStream()); //파일전송을 위한 출력스트림
				String message;
				ChatMsg msg;
				
				while ((msg = (ChatMsg)in.readObject()) != null) { //입력스트림으로부터 채팅 메시지 객체를 읽어옴. 메시지의 모드값에 따라서 서로 다른 동작처리.		
					
					if(msg.mode == ChatMsg.MODE_LOGIN) { //읽어온 메시지의 모드값이 로그인 메시지라면
						uid = msg.userID; //uid에 로그인한 클라이언트의 아이디를 저장.
						
						printDisplay("새 참가자: "+uid);
						printDisplay("현재 참가자 수: "+users.size());
						continue;
					} else if (msg.mode == ChatMsg.MODE_LOGOUT) { // 로그아웃 메시지라면
						
						break;
						
					} else if (msg.mode == ChatMsg.MODE_TX_STRING) { // 일반 메시지라면
						message = uid + ": " + msg.message;
						printDisplay(message);
						broadcasting(msg); // 전달받은 메시지 객체를 그대로 현재 서버에 접속한 모든 클라이언트에게 메시지 전송.
					}else if (msg.mode == ChatMsg.MODE_TX_IMAGE) { // 이미지 메시지라면
						message = uid + ": " + msg.message;
						printDisplay(message);
						broadcasting(msg); // 전달받은 메시지 객체를 그대로 현재 서버에 접속한 모든 클라이언트에게 메시지 전송.
					} else if (msg.mode == ChatMsg.MODE_TX_FILE) { // 파일 메시지라면
						message = uid + ": " + msg.message;
						printDisplay(message);
						//printDisplay(Long.toString(msg.size));
						broadcastingOthers(msg); // 전달받은 메시지 객체를 그대로 현재 서버에 접속한 모든 클라이언트에게 메시지 전송.(※현재 연결중인 클라이언트 제외.)
					}
//					
//					message = uid + ": " + message; //일반 메시지라면
//					printDisplay(message);
//					broadcating(message); //현재 서버에 접속한 모든 클라이언트에게 메시지 전송.
					
				}				
				
				users.remove(this); //연결이 끊은 클라이언트를 사용자벡터에서 제거. 현재 작업스레드를 벡터에서 제거.
				printDisplay(uid+" 퇴장. 현재 참가자 수 : " + users.size());

			} catch (IOException e) { //의도적으로, 정상적으로 연결을 끊은 경우가 아니라 연결상태 불량으로 인한 연결 끊김인 경우.
				users.remove(this);
				printDisplay(uid+" 연결 끊김. 현재 참가자 수 : " + users.size());
			} catch (ClassNotFoundException e) {
				System.err.println("객체 전달 오류> " + e.getMessage());
			}
			
			finally {
						
				try {
					cSocket.close(); 
				} catch (IOException e) {

					System.err.println("서버 닫기 오류> " + e.getMessage());
					System.exit(-1);

				}
			}
		}
		
		private void send(ChatMsg msg) { // 하나의 문자열 채팅메시지를 전달받았을떄, 그 채팅메시지를 객체스트림을통해서 특정 클라이언트에게 전송하는 메서드.

			try {
				out.writeObject(msg);
				out.flush();
				//printDisplay("메시지 전송 완료: " + msg.message);
			} catch (IOException e) {
				System.err.println("클라이언트 일반 전송 오류> " + e.getMessage());
			}
		}
		
		private void sendMesssage(String msg) { // 만약에 어떤 서버가 특정클라이언트에게만 전달할 필요가있다면 각 클라이언트 헨들러가갖고있는 이 sendMessage()로, 전달받은 문자열 메시지를 하나의 채팅메시지 객체로 구성해서 전달하도록.
		
//					try {
//						((BufferedWriter)out).write(msg+"\n");
//						out.flush();
//					} catch (IOException e) {
//						System.err.println("클라이언트 일반 전송 오류> " + e.getMessage());
//					} 

			    send(new ChatMsg(uid, ChatMsg.MODE_TX_STRING, msg));
			}
		
			private void broadcasting(ChatMsg msg) { //현재 서버에 접속한 모든 클라이언트에게 메시지를 보내는 메서드.=> 이것이 다자간 채팅의 핵심.
				for (ClientHandler client : users) {
					client.send(msg);
				}
			}
			
			private void broadcastingOthers(ChatMsg msg) { //현재 이 클라이언트 헨들러 객체가 연결중인 클라이언트를 제외한 나머지 클라이언트들에게 브로드케스팅.
				for (ClientHandler client : users) {
					if(client == this) { 
						//client.send(new ChatMsg(uid, ChatMsg.MODE_TX_STRING, "client == this"));
						continue;
						}; //현재 클라이언트는 제외.
					client.send(msg);
					client.redirectStream(this.bis, msg.size); //this.bis는 현재 클라이언트의 버퍼 입력 스트림.
				}
			}
			// 여기가 문제.
			private void redirectStream(BufferedInputStream bis, long fileSize) { //파일을 전송받은 클라이언트가 다시 파일을 전송하는 메서드.
				printDisplay(Long.toString(fileSize)+"파일사이즈");
				byte[] buffer = new byte[1024];
				int nRead;

				try {
					
					while (fileSize > 0 && (nRead = bis.read(buffer)) != -1) {
						bos.write(buffer, 0, nRead);
						fileSize -= nRead;
						printDisplay(Long.toString(fileSize));
						bos.flush();
					}
					
				} catch (IOException e) {
					System.err.println("파일 전송 오류> " + e.getMessage());
				}
			}

		
		@Override
		public void run() { 
			receiveMessages(clientSocket);
		}
		
		
	}
	
	public static void main(String[] args) {
		WithChatServer server = new WithChatServer(54321);
		//server.startServer(); // 서버소켓 생성하면서 서버시작.
	}

}
