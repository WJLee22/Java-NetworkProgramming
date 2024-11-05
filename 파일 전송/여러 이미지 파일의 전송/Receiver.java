import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;

public class ImgReceiver extends JFrame {

	private int port;
	private ServerSocket serverSocket;
	private JTextPane t_display;
	private DefaultStyledDocument document; //문서객체
	private Thread acceptThread=null;
	
	public ImgReceiver(int port) {

		super("Image Receiver");
		
		buildGUI();

		setBounds(900, 300, 500, 400);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		
		this.port = port;
		//startServer(); startServer 메서드를 실행하면 서버가 시작되고 while 무한루프를 돌기때문에,
		//클라이언트에 대한 연결이 끝날때까지 계속해서 accept(), 작업스레드 생성.. 등등 계속 수행.
		// so, 이 startServer() 메서드가 끝나기전까지는 다른 부분에 대한 사용자 이벤트처리가 제대로 일어나지 않을 수 있음.  
		// 그래서 startServer를 단독으로 호출하는 것이 아니라, accept 스레드로 처리해줘야함.
		
		acceptThread = new Thread(new Runnable() {
			@Override
			public void run() {
				startServer();
			}
		});
		acceptThread.start(); //acceptThread를 실행시키면, startServer() 메서드가 실행되고, 서버가 시작되고, 클라이언트로부터의 연결을 기다리게 됨.
		// 이로써 서버가 실행되는 동안에도 다른 사용자이벤트처리가 가능해짐.
	}

	private void startServer() {
		Socket clientSocket = null;
		try {
			serverSocket = new ServerSocket(port);// 해당 포트와 연결된 서버소켓 객체 생성.
			printDisplay("서버가 시작되었습니다.");

			while (true) {
				clientSocket = serverSocket.accept(); // 클라이언트측 소켓이 이 서버소켓에게 연결 요청을 보냈고 -> 이를 서버소켓이 수락하면서 해당 클라이언트측 소켓과
														// 연결할 별도의 소켓을 생성-반환.
				printDisplay("클라이언트가 연결되었습니다.");

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

	private void buildGUI() {
		JPanel dPanel = createDisplayPanel();
		JPanel cPanel = createControlPanel();

		add(dPanel, BorderLayout.CENTER);
		add(cPanel, BorderLayout.SOUTH);
	}

	// 디스플레이 패널
	private JPanel createDisplayPanel() {
		JPanel dispalyPanel = new JPanel(new BorderLayout());
		
		document = new DefaultStyledDocument();
		t_display = new JTextPane(document);

		t_display.setEditable(false);
		
		JScrollPane scroll = new JScrollPane(t_display);
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
	
	// 클라이언트측으로부터 전달받은 데이터를 textarea에 출력해주는 메서드
	private void printDisplay(String msg) {
		
		int len = t_display.getDocument().getLength();
		
		try {
			document.insertString(len, msg + "\n", null);

		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		t_display.setCaretPosition(len);																							
	}
	
	private void printDisplay(ImageIcon icon) { //출력할 이미지를 받아서 textPane에 출력.(textPane의 문서 객체에 이미지를 추가함으로써)
		t_display.setCaretPosition(t_display.getDocument().getLength()); //현재 커서위치를 맨 마지막으로 이동.
		
		if(icon.getIconWidth()> 400) { 
			Image img = icon.getImage(); //이미지아이콘으로부터 이미지를 추출.
			Image changeImg = img.getScaledInstance(400, -1, Image.SCALE_SMOOTH); //이미지의 크기를 400픽셀로 조정. 종횡비 유지. 이미지의 품질을 유지하면서 크기를 조정.
			icon = new ImageIcon(changeImg); //이미지아이콘을 새로운 이미지로 설정.
		}
		t_display.insertIcon(icon); //이미지를 textPane에 추가. 근데, textPane의 크기가 이미지의 크기보다 작으면 이미지가 잘림. so, 위의 if문을 통해 이미지의 크기를 조정해줌.
		
		printDisplay(""); //이 다음에 출력되는 내용들이 현재이미지의 아래에 출력되도록 빈줄을 하나 출력함.
	
	}
	
	private class ClientHandler extends Thread{
		private Socket clientSocket; 
		
		public ClientHandler(Socket clientSocket) {
			this.clientSocket=clientSocket; 
		}

		// 클라이언트측으로부터 지속적으로 데이터를 전달받는 메서드
		private void receiveImage(Socket cSocket) {
		
			try {
				DataInputStream in = new DataInputStream(new BufferedInputStream(cSocket.getInputStream()));
				
				while(true) { //하나의 파일이름을 수신받고 - 파일크기를 수신받고 - 파일데이터를 수신받고 - 다시 파일이름을 수신받고... 하나의 파일만을 전송받는게 아니라 여러 파일을 전송받는 프로그램이므로 무한루프를 돌면서 계속 수신. 
				String fileName = in.readUTF(); //바이트스트림이지만 유니코드 형식으로 파일명 문자열을 전달받음. sender 로부터 받는 첫번째 메시지는 파일 이름이다.
				
				Long size = (Long)in.readLong(); //sender로부터 받는 두번째 메시지는 파일의 크기.
				
				File file = new File(fileName); //전달받은 파일 이름으로 파일객체 생성. 파일 객체만 생성할뿐 실제 파일은 생성되지 않음.
				
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file)); //파일 객체와 연결된 파일출력스트림 생성 +버퍼.
				
				//1024 바이트의 한 블럭씩 파일을 읽어들이기 위한 바이트 배열 생성. 스트림이 닫힐때까지 반복해서 한 블럭씩 읽어들임.
				byte[] buffer = new byte[1024];
				int nRead;
				while (size > 0) { //현재 수신되어지는 파일의 크기가 존재하는 한 계속해서 파일을 읽어들임.
					nRead = in.read(buffer); //nRead는 실제로 읽어들인 바이트의 크기.
					size -= nRead; //전체 파일크기에서 읽어들인 바이트의 크기를 빼줌 -> 파일의 끝에 도달하면 size는 0이 됨 -> while문 종료 -> 하나의 파일 수신 완료.
					
					bos.write(buffer, 0, nRead); //파일출력스트림에다가 읽어들인 바이트를 쓰기.
					
				}
				bos.close(); //파일출력스트림 닫기. 하나의 파일을 잘 수신했음을 의미.

				printDisplay("수신을 완료했습니다: " + file.getName());
				printDisplay("클라이언트가 연결을 종료했습니다.");
				
				ImageIcon icon = new ImageIcon(fileName); //수신받은 파일을 이미지아이콘으로 생성.
				printDisplay(icon); //이미지아이콘을 textPane에 출력.
				}
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
		
		@Override
		public void run() { 
			receiveImage(clientSocket);
		}
		
		
	}
	
	public static void main(String[] args) {
		ImgReceiver server = new ImgReceiver(54321);
	}

}
