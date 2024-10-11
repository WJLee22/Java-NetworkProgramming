
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class CalcClientGUI extends JFrame {

	private JTextField t_operand1; 
	private JTextField t_operator; 
	private JTextField t_operand2; 
	private JLabel l_equal; 
	private JTextField t_result; 
	private JButton b_calc; 
	private JButton b_clean; 

	private JButton b_connect; 
	private JButton b_disconnect; 
	private JButton b_exit; 
	
	private String serverAddress; 
	private int serverPort; 
	private Socket socket; 
	
	private ObjectOutputStream out; 
	private DataInputStream in; 
	

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
			out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream())); 
			in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			
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
			in.close();		
			out.close();			
			socket.close();
		
			t_operand1.setEnabled(false);
			t_operator.setEnabled(false);
			t_operand2.setEnabled(false);
			b_calc.setEnabled(false);
			b_clean.setEnabled(false);
			
			b_connect.setEnabled(true);
			b_exit.setEnabled(true);
			b_disconnect.setEnabled(false);
			
		} catch (IOException e) {
			System.err.println("클라이언트 닫기 오류> " + e.getMessage());
			System.exit(-1);
		}
	}

	private void sendMesssage() {
		
		String op1=t_operand1.getText();
		String operator=t_operator.getText();
		String op2=t_operand2.getText();
		
		if (op1.equals("") || operator.equals("") || op2.equals(""))// 피연산자 및 연산자 입력칸에 아무것도 입력하지않고 보내려고한다면 그냥 return.
			return;

		CalcExpr msg = new CalcExpr(Double.parseDouble(op1), operator.charAt(0), Double.parseDouble(op2));

			try {
				out.writeObject(msg);
				out.flush();
			} catch (IOException e) {
				System.err.println("클라이언트 일반 전송 오류> " + e.getMessage());
				System.exit(-1);
			} 
			receiveMessage();
	}
	
	//클라이언트가 서버에게 메시지를 보내고나서, 서버가 나에게 반향하는 메시지를 수신.
	private void receiveMessage() {
		
		try {
			Double inMsg=in.readDouble();// 서버로부터 수식결과 전달받음.
			t_result.setText(String.format("%.2f", inMsg)); //수식결과값을 소수점 2째자리까지 반올림해서 출력.
		} catch (IOException e) {
			System.err.println("클라이언트 일반 수신 오류> " + e.getMessage());
			System.exit(-1);
		}
		
	}
	
	
	private void buildGUI() {
		
		JPanel calcPanel = createCalcPanel();
		JPanel controlPanel = createControlPanel();
		JPanel cleanPanel = createCleanPanel();
		add(calcPanel, BorderLayout.NORTH);
		add(cleanPanel, BorderLayout.CENTER);
		add(controlPanel, BorderLayout.SOUTH);
	}


	// 수식 패널
	private JPanel createCalcPanel() {
		JPanel calcPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		t_operand1 = new JTextField(5); //피연산자 1 텍스트필드
		t_operator = new JTextField(3); //연산자 텍스트필드
		t_operand2 = new JTextField(5); //피연산자 1 텍스트필드
		l_equal = new JLabel("="); // = 레이블
		t_result   = new JTextField(5); // 수식 계산 결과창 텍스트필드
		b_calc = new JButton("계산"); //계산 버튼
		b_clean = new JButton("지우기"); //지우기 버튼
			
		t_operand1.setEnabled(false);
		t_operand1.setHorizontalAlignment(JTextField.RIGHT); //텍스트 필드의 텍스트를 우측 정렬.
		
		t_operator.setEnabled(false); 
		t_operator.setHorizontalAlignment(JTextField.CENTER);
		
		t_operand2.setEnabled(false); 
		t_operand2.setHorizontalAlignment(JTextField.RIGHT);
		
		t_result.setEditable(false); //결과창은 수정불가하도록 설정.
		t_result.setHorizontalAlignment(JTextField.RIGHT);
		
		b_calc.setEnabled(false); //서버에 접속하기 전까지는 계산버튼 클릭불가.
		b_calc.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				sendMesssage();// 계산 버튼 클릭시, sendMesssage 호출하여 텍스트필드에 입력한 문자열을 서버측 소켓으로전송
				
			}
		});
		
		b_clean.setEnabled(false);
		b_clean.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				t_operand1.setText("");
				t_operator.setText("");
				t_operand2.setText("");
				t_result.setText("");
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
	
	private JPanel createCleanPanel() {
		JPanel cleanPanel = new JPanel(new FlowLayout());
		cleanPanel.add(b_clean);
		
		return cleanPanel;
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

				t_operand1.setEnabled(true);
				t_operator.setEnabled(true);				
				t_operand2.setEnabled(true);				
				b_calc.setEnabled(true);
				b_clean.setEnabled(true);
				
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
