
import java.awt.BorderLayout;
import java.awt.FlowLayout;
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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class CalcClientGUI extends JFrame {

	private JTextField t_operand1;
	private JTextField t_operator;
	private JTextField t_operand2;
	private JLabel l_equal;
	private JTextField t_result;
	private JButton b_calc;
	
	private JTextArea t_display;
	private JButton b_connect;
	private JButton b_disconnect;

	private JButton b_exit;
	private String serverAddress;
	private int serverPort;
	private Socket socket;
	private Writer out; 
	private Reader in; 
	private boolean Connected = false; 

	public CalcClientGUI(String serverAddress, int serverPort) {
		super("CalcClient GUI");

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
			out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8")); 
			in = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));
			
			Connected = true;


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
			Connected = false;

			b_calc.setEnabled(false);
			b_connect.setEnabled(true);
			b_exit.setEnabled(true);
			b_disconnect.setEnabled(false);

		} catch (IOException e) {
			System.err.println("클라이언트 닫기 오류> " + e.getMessage());
			System.exit(-1);
		}
	

	}

	private void sendMesssage() {

		if (!Connected)
			return; 

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
	
	//클라이언트가 서버에게 메시지를 보내고나서, 서버가 나에게 반향하는 메시지를 수신.
	private void receiveMessage() {
		
		try {
			String inMsg=((BufferedReader)in).readLine();
			t_display.append("서버:\t"+inMsg+"\n");
		} catch (IOException e) {
			System.err.println("클라이언트 일반 수신 오류> " + e.getMessage());
			System.exit(-1);
		}
		
	}
	
	
	private void buildGUI() {
		
		JPanel calcPanel = createCalcPanel();
		JPanel controlPanel = createControlPanel();
		int topMargin = 10;	
		calcPanel.setBorder(BorderFactory.createEmptyBorder(topMargin, 0, 0, 0)); // 패널에 topMargin 부여
		add(calcPanel, BorderLayout.NORTH);
		add(controlPanel, BorderLayout.SOUTH);
	}


	// 수식 패널
	private JPanel createCalcPanel() {
		JPanel calcPanel = new JPanel(new FlowLayout());
		t_operand1 = new JTextField(5);
		t_operator = new JTextField(3);
		t_operand2 = new JTextField(5);
		l_equal = new JLabel("=");
		t_result   = new JTextField(5);
		b_calc = new JButton("계산");
		
		b_calc.setEnabled(false);
		t_result.setEnabled(false);

//		t_input.addKeyListener(new KeyAdapter() {
//
//			@Override
//			public void keyPressed(KeyEvent e) {
//				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
//					sendMesssage();// 텍스트필드에 엔터 입력시, sendMesssage 호출하여 텍스트필드에 입력한 문자열을 서버측 소켓으로전송
//					receiveMessage();
//				}
//			}
//
//		});

		b_calc.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				sendMesssage();// 계산 버튼 클릭시, sendMesssage 호출하여 텍스트필드에 입력한 문자열을 서버측 소켓으로전송
				receiveMessage();
			}
		});
		
		calcPanel.add(t_operand1);
		calcPanel.add(t_operator);
		calcPanel.add(t_operand2);
		calcPanel.add(l_equal);
		calcPanel.add(t_result);
		calcPanel.add(b_calc);

		return calcPanel;
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
				b_calc.setEnabled(true);
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

		CalcClientGUI client = new CalcClientGUI(serverAddress, serverPort);
	}

}
