import java.awt.BorderLayout;
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

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class FileReceiver extends JFrame {

	private int port;
	private ServerSocket serverSocket;
	private JTextArea t_display;

	public FileReceiver(int port) {

		super("File Receiver");

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

		// 클라이언트측으로부터 지속적으로 데이터를 전달받는 메서드
		private void receiveFile(Socket cSocket) {

			
			try {
				DataInputStream in = new DataInputStream(new BufferedInputStream(cSocket.getInputStream()));
				
				String fileName = in.readUTF(); //바이트스트림이지만 유니코드 형식으로 문자열을 전달받음. sender 로부터 받는 첫번째 메시지는 파일 이름이다.
				File file = new File(fileName); //전달받은 파일 이름으로 파일객체 생성. 파일 객체만 생성할뿐 실제 파일은 생성되지 않음.
				
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file)); //파일 객체와 연결된 파일출력스트림 생성 +버퍼.
				
				
				byte[] buffer = new byte[1024];
				int nRead;
				while ((nRead = in.read(buffer)) != -1) { //입력 스트림으로 전달되는 데이터를 1KB씩, 즉 1KB블럭단위로 파일의 끝까지 or 연결끊길때까지 읽어들임.
					bos.write(buffer, 0, nRead); //buffer 바이트 배열의 0번째, 즉 처음부터 nRead, 즉 sender로부터 받은 데이터의 크기만큼을 읽어서 파일에 write.
				}
				bos.close(); //파일출력스트림 닫기. 하나의 파일을 잘 수신했음을 의미.

				printDisplay("수신을 완료했습니다: " + file.getName());
				printDisplay("클라이언트가 연결을 종료했습니다.");

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
			receiveFile(clientSocket);
		}
		
		
	}
	
	public static void main(String[] args) {
		FileReceiver server = new FileReceiver(54321);
		server.startServer(); // 서버소켓 생성하면서 서버시작.
	}

}
