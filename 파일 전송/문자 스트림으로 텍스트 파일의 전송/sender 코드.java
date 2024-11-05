

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class TextFileSender extends JFrame {

	private JTextField t_input;
	private JTextArea t_display;
	private JButton b_connect, b_disconnect, b_send, b_exit;
	
	private String serverAddress;
	private int serverPort;
	private Socket socket;
	
	private Writer out; 
	//private Reader in; 

	public TextFileSender(String serverAddress, int serverPort) {
		super("TextFile Sender");

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
			//PrintWriter 내부적으로 버퍼를 갖고있음. so, BufferedWriter를 체이닝하여 필터링 스트림으로 추가해줄 필요x.
			out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8"), true);//버퍼 자동 flush 
			//in = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));
		

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

		((PrintWriter)out).println(msg);
	
		
		t_display.append("나: " + msg + "\n"); 
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
		//파일이 존재하면 출력스트림을 통해서 파일이름을 먼저 전송하며-> receiver쪽에서 해당 이름의 빈파일을 먼저 생성할 수 있도록하기위함.
		((PrintWriter)out).println(filename);
		//1. 먼저 파일이름을 행단위 문자열로 전달.
		//2. 이후, 텍스트 파일의 내용들을 전달.
		
		BufferedReader br=null;
		try {
			br=new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8")); //파일에서 바이트 단위로 데이터를 읽어오는 바이트 스트림을 문자 스트림으로 필터링. 
			
			String line;
			while((line=br.readLine())!=null){ //파일의 끝에 도달할때까지.
				((PrintWriter)out).println(line); //해당 파일에서 읽어들인 내용들을 출력스트림으로 출력 ->receiver에게 보냄.
			}
			out.close(); //파일의 끝까지 잘 읽었으니, 스트림 닫아줌.-> 이 스트림이 만들어진곳은 connectToServer(). 즉 sender 측에 연결할때임.
			// 즉, 연결을 끊고 다시 연결을 요청해야 out 스트림이 다시 생성됨. 
			// 왜 이런 제약조건을 만든거지? -> 클라이언트입장에서, 보낸내용이 끝났다라는것을 서버가 알아들을수있도록 하는방법이 기존에코서버에서는 없었음.
			// 이를 보완하기위해 이런 제약을 둔것임. 일단 임시로.
			t_display.append(">> 전송을 완료했습니다: " + filename + "\n"));
			t_input.setText("");
		} catch (UnsupportedEncodingException e) { //지원되지않는 인코딩인경우
			t_display.append(">> 인코딩 형식을 알 수 없습니다: " + e.getMessage() + "\n");
			return;
		} catch (FileNotFoundException e) {//해당하는 파일이 존재하지않는 경우(위에서 이미 사전에 검사했지만, 혹시모르니.)
			t_display.append(">> 파일이 존재하지 않습니다: " + e.getMessage() + "\n");
			return;
		} //(파일데이터=바이트 스트림)=> 행단위로 파일 데이터 읽어오도록 필터링.
		catch (IOException e) {
			t_display.append(">> 파일을 읽을 수 없습니다: " + e.getMessage() + "\n");
			return;
		} finally {
            try {
                if(br!=null) br.close(); //파일스트림을 닫음.
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

		TextFileSender client = new TextFileSender(serverAddress, serverPort);
	}

}
