


import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Image;
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
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;

public class ImgSender extends JFrame {

	private JTextField t_input;
	private JTextPane t_display; //문자열과 이미지를 모두 출력할 수 있는 텍스트패널. JTextPane은 JTextArea와 비슷하지만, 이미지를 출력할 수 있음.
	private DefaultStyledDocument document; //문서객체
	private JButton b_connect, b_disconnect, b_send, b_exit, b_select;
	
	private String serverAddress;
	private int serverPort;
	private Socket socket;
	
	private OutputStream out; 
	//private Reader in; 

	public ImgSender(String serverAddress, int serverPort) {
		super("Image Sender");

		this.serverAddress = serverAddress;
		this.serverPort = serverPort;

		buildGUI();

		setBounds(300, 300, 500, 400);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}
	
	private void printDisplay(String msg) {
		int len = t_display.getDocument().getLength(); //textPane의 문서내용의 현재까지의 문자열 길이를 반환.
		try {
			document.insertString(len, msg+"\n", null);//문서객체에 문자열을 추가. len은 현재까지의 문자열 길이. 즉, 현재까지의 문자열 바로 다음에 msg를 추가.
		} catch (BadLocationException e) {	
			e.printStackTrace();
		} 
		t_display.setCaretPosition(len); //현재 커서위치를 맨 마지막으로 이동.
																																
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
		t_input.setText(""); //이미지를 출력하고나서, 이미지 경로 입력창을 비워줌.
	
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
			
			b_select.setEnabled(false);
			b_send.setEnabled(false);
			b_connect.setEnabled(true);
			b_exit.setEnabled(true);
			b_disconnect.setEnabled(false);

		} catch (IOException e) {
			System.err.println("클라이언트 닫기 오류> " + e.getMessage());
			System.exit(-1);
		}
	

	}

//	private void sendMesssage() {
//
//		String msg = t_input.getText();
//
//		if (msg.equals(""))// 아무것도 입력하지않고 보내려고한다면 그냥 return.
//			return;
//
//		try {
//			((DataOutputStream)out).writeUTF(msg + "\n");	
//			out.flush();
//			t_display.append("나: " + msg + "\n"); 
//		} catch (IOException e) {
//			System.err.println("클라이언트 일반 전송 오류> " + e.getMessage());
//			System.exit(-1);
//		}
//	
//		t_input.setText("");
//			
//	}
	
	private void sendImage() { // 이미지 파일 전송뿐만아니라, 전송된 그 이미지가 출력도 되도록.
		String filename = t_input.getText().strip(); //앞뒤 공백제거.->공백으로인한 에러방지.
		if(filename.isEmpty()) return;
		
		File file = new File(filename); //해당 파일명의 파일을 파일객체로써 표현. 
		//파일 입출력은 아니고, 그저 파일에 대한 정보를 유지할 수 있는 객체.
		
		if(!file.exists()) { //파일이 존재하지않는다면
			printDisplay(">> 파일이 존재하지 않습니다: " + filename);
			return;
		}
		
		
		BufferedInputStream bis=null;
		try {
			((DataOutputStream)out).writeUTF(file.getName()); // DataOutputStream은 바이트 스트림이지만, writeUTF() 메서드를 사용하면 문자열을 UTF-8로 인코딩하여 출력할 수 있음.
			//writeUTF() 메서드는 문자열을 UTF-8 형식으로 변환하여 바이트 스트림에 쓰는 메서드.
			
			((DataOutputStream)out).writeLong(file.length()); //long 정수 타입으로 파일크기를 전달.
			
			bis=new BufferedInputStream(new FileInputStream(file));

			
			byte[] buffer = new byte[1024]; //한 블럭에 대한 1KB크기의 바이트 배열을 만들어놓고.
			int nRead;
			while((nRead = bis.read(buffer)) != -1) //bis스트림이 연결된 파일로부터 buffer 크기만큼의 데이터를 buffer 배열로 읽어들임.즉 1KB씩 읽어들임. 파일의끝에 도달해서 더이상 읽을 데이터가 없을떄까지.
			{
				out.write(buffer, 0, nRead); //읽어들인 데이터를 출력스트림에 쓰기. "이 buffer의 0번 위치에서부터 nRead 크기 만큼의 데이터를 출력스트림에 쓰기"			
				// 파일이 딱 1024의 배수만큼의 크기가 아닐수도 있기때문에, 파일의 마지막 블럭의 경우 1024 바이트 만큼의 크기가 아닐수있으므로, nRead를 사용하여 실제로 읽어들인 바이트 수만큼만 출력스트림에 쓰도록함.
			}
						
			//out.close(); 현재 스트림을 닫으면 여러 파일을 전송할 수 없음.
			out.flush(); //출력스트림을 비워줌으로써 다음 파일을 전송할 수 있도록 함.
			
			
			printDisplay(">> 전송을 완료했습니다: " + filename);
			t_input.setText("");
			
			ImageIcon icon = new ImageIcon(filename); //이미지아이콘 객체 생성.
			printDisplay(icon); //Receiver에게 전송한 이미지아이콘을 현재 이 Sender측의 textPane에도 출력.
			
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
		
		document = new DefaultStyledDocument(); //문서객체 생성. JTextPane에 출력될 내용물을 담고있는 문서객체.
		t_display = new JTextPane(document); //JTextPane은 생성자의 인수로, JTextPane에 출력되어질 내용물을 담고있는 문서객체를 지정해줄 수 있음. 
		
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
				sendImage();
			}
		};
		
		t_input = new JTextField(30);
		t_input.addActionListener(listener);
		
		b_send = new JButton("보내기");
		b_send.addActionListener(listener);
		
		b_select = new JButton("선택하기");
		b_select.addActionListener(new ActionListener() {
			JFileChooser chooser = new JFileChooser(); //파일선택기 객체 생성.
			@Override
			public void actionPerformed(ActionEvent e) {
				FileNameExtensionFilter filter = new FileNameExtensionFilter( //파일선택기에 파일필터를 추가.
						"JPG & GIF & PNG Images", "jpg", "gif", "png"); //여러파일들 중에서 이미지파일만 선택할 수 있도록 필터링.
				
				chooser.setFileFilter(filter); //파일선택기에 필터를 추가.
				
				int ret = chooser.showOpenDialog(ImgSender.this); //파일선택기를 열고, 사용자가 파일을 선택하도록 함.
				//인자로 현재 객체의 참조를 전달하여, 파일선택기가 현재 객체에 열리도록 함. 여기서 현재 객체는 J프레임. 
				//즉, 프레임의 중앙에 파일선택 대화상자(다이얼로그)가 열림.
				
				if(ret != JFileChooser.APPROVE_OPTION){//사용자가 파일을 선택하지 않고 닫혔거나 취소버튼을 누른경우.
					JOptionPane.showMessageDialog(ImgSender.this, "파일을 선택하지 않았습니다.", "경고", JOptionPane.WARNING_MESSAGE);
					return; 
				}
				
				t_input.setText(chooser.getSelectedFile().getAbsolutePath()); //제대로된 선택이 일어났다면 즉, 선택한 파일이 존재하면, 선택한 파일의 절대경로명을  t_input에 출력.
			}
		});

		inputPanel.add(t_input, BorderLayout.CENTER);
		//inputPanel.add(b_send, BorderLayout.EAST);
		JPanel p_buttons = new JPanel(new GridLayout(1, 0));
		p_buttons.add(b_select);
		p_buttons.add(b_send);
		inputPanel.add(p_buttons, BorderLayout.EAST);
		
		t_input.setEnabled(false);
		b_select.setEnabled(false);
		b_send.setEnabled(false);
		
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
				b_select.setEnabled(true);
				b_send.setEnabled(true);
				t_input.setEnabled(true);
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

		ImgSender client = new ImgSender(serverAddress, serverPort);
	}

}
