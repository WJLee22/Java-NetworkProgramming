


import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class FileSender extends JFrame {

	private JTextField t_input;
	private JTextArea t_display;
	private JButton b_connect, b_disconnect, b_send, b_exit;
	
	private String serverAddress;
	private int serverPort;
	private Socket socket;
	
	private OutputStream out; 
	//private Reader in; 

	public FileSender(String serverAddress, int serverPort) {
		super("File Sender");

		this.serverAddress = serverAddress;
		this.serverPort = serverPort;

		buildGUI();

		setBounds(300, 300, 500, 400);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}
	
	private void printDisplay(String msg) {

		t_display.append(msg + "\n");
		t_display.setCaretPosition(t_display.getDocument().getLength());
																																
	}

	private void connectToServer() {

		try {
			socket = new Socket(serverAddress, serverPort);

			out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		

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
			out.close();
			socket.close();

			b_send.setEnabled(false);
			b_connect.setEnabled(true);
			b_exit.setEnabled(true);
			b_disconnect.setEnabled(false);

		} catch (IOException e) {
			System.err.println("클라이언트 닫기 오류> " + e.getMessage());
			System.exit(-1);
		}
	

	}

	private void sendMesssage() {

		String msg = t_input.getText();

		if (msg.equals(""))// 아무것도 입력하지않고 보내려고한다면 그냥 return.
			return;

		try {
			((DataOutputStream)out).writeUTF(msg + "\n");	
			out.flush();
			t_display.append("나: " + msg + "\n"); 
		} catch (IOException e) {
			System.err.println("클라이언트 일반 전송 오류> " + e.getMessage());
			System.exit(-1);
		}
	
		t_input.setText("");
			
	}
	
	private void sendFile() {
		String filename = t_input.getText().strip(); //앞뒤 공백제거.->공백으로인한 에러방지.
		if(filename.isEmpty()) return;
		
		File file = new File(filename); //해당 파일명의 파일을 파일객체로써 표현. 
		//파일 입출력은 아니고, 그저 파일에 대한 정보를 유지할 수 있는 객체.
		
		if(!file.exists()) { //파일이 존재하지않는다면
			t_display.append(">> 파일이 존재하지 않습니다: " + filename + "\n");
			return;
		}
		
		
		BufferedInputStream bis=null;
		try {
			((DataOutputStream)out).writeUTF(file.getName()); // DataOutputStream은 바이트 스트림이지만, writeUTF() 메서드를 사용하면 문자열을 UTF-8로 인코딩하여 출력할 수 있음.
			//writeUTF() 메서드는 문자열을 UTF-8 형식으로 변환하여 바이트 스트림에 쓰는 메서드.
			bis=new BufferedInputStream(new FileInputStream(file));
			
//			String line;
//			while((line=bis.readLine())!=null){ //파일의 끝에 도달할때까지.
//				((PrintWriter)out).println(line); //해당 파일에서 읽어들인 내용들을 출력스트림으로 출력 ->receiver에게 보냄.
//			}
			
			byte[] buffer = new byte[1024]; //한 블럭에 대한 1KB크기의 바이트 배열을 만들어놓고.
			int nRead;
			while((nRead = bis.read(buffer)) != -1) //bis스트림이 연결된 파일로부터 buffer 크기만큼의 데이터를 buffer 배열로 읽어들임.즉 1KB씩 읽어들임. 파일의끝에 도달해서 더이상 읽을 데이터가 없을떄까지.
			{
				out.write(buffer, 0, nRead); //읽어들인 데이터를 출력스트림에 쓰기. "이 buffer의 0번 위치에서부터 nRead 크기 만큼의 데이터를 출력스트림에 쓰기"			
				// 파일이 딱 1024의 배수만큼의 크기가 아닐수도 있기때문에, 파일의 마지막 블럭의 경우 1024 바이트 만큼의 크기가 아닐수있으므로, nRead를 사용하여 실제로 읽어들인 바이트 수만큼만 출력스트림에 쓰도록함.
			}
						
			out.close(); //이를통해 receiver는, 한 파일의 전송이 끝났음을 알게됨.
			
			printDisplay(">> 전송을 완료했습니다: " + filename);
			t_input.setText("");
		}  catch (FileNotFoundException e) {//해당하는 파일이 존재하지않는 경우(위에서 이미 사전에 검사했지만, 혹시모르니.)
			printDisplay(">> 파일이 존재하지 않습니다: " + e.getMessage());
			return;
		} //(파일데이터=바이트 스트림)=> 행단위로 파일 데이터 읽어오도록 필터링.
		catch (IOException e) {
			printDisplay(">> 파일을 읽을 수 없습니다: " + e.getMessage());
			return;
		} finally {
            try {
                if(bis!=null) bis.close(); //파일스트림을 닫음.
            } catch (IOException e) {
            	printDisplay(">> 파일을 닫을 수 없습니다: " + e.getMessage());
            	return;
            }
        }
	}
	
	//클라이언트가 서버에게 메시지를 보내고나서, 서버가 나에게 반향하는 메시지를 수신.
//	private void receiveMessage() {
//		
//		try {
//			String inMsg=((BufferedReader)in).readLine();
//			t_display.append("서버:\t"+inMsg+"\n");
//		} catch (IOException e) {
//			System.err.println("클라이언트 일반 수신 오류> " + e.getMessage());
//			System.exit(-1);
//		}
//		
//	}
	
	
	
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
		
		ActionListener listener=new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				//sendMesssage();
				sendFile();
			}
		};
		
		t_input = new JTextField(30);
		b_send = new JButton("보내기");
		b_send.setEnabled(false);

		t_input.addActionListener(listener);
		b_send.addActionListener(listener);

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
				b_send.setEnabled(true);
				b_connect.setEnabled(false);
				b_exit.setEnabled(false);
				b_disconnect.setEnabled(true);
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

		FileSender client = new FileSender(serverAddress, serverPort);
	}

}
