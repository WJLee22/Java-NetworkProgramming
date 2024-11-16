
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;

public class WithTalk extends JFrame {

	private JTextField t_input, t_userID, t_hostAddr, t_portNum;
	private JTextPane t_display;
	private DefaultStyledDocument document;
	private JButton b_connect, b_disconnect, b_send, b_exit, b_select;
	private String serverAddress;
	private int serverPort;
	private String uid;
	private Socket socket;
	//private Writer out; 
	private ObjectOutputStream out;
	private Thread receiveThread=null;

	public WithTalk(String serverAddress, int serverPort) {
		super("With Talk");

		this.serverAddress = serverAddress;
		this.serverPort = serverPort;

		buildGUI();

		setBounds(300, 300, 550, 400);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}
	
	private void buildGUI() {
		
		JPanel p_input = new JPanel(new GridLayout(3, 1));
		p_input.add(createInputPanel());
		p_input.add(createInfoPanel());
		p_input.add(createControlPanel());
		add(createDisplayPanel(), BorderLayout.CENTER);
		add(p_input, BorderLayout.SOUTH);
	}

	// 디스플레이 패널
	private JPanel createDisplayPanel() {
		JPanel dispalyPanel = new JPanel(new BorderLayout());
		
		document=new DefaultStyledDocument();
		t_display= new JTextPane(document);
		
		t_display.setEditable(false);

		dispalyPanel.add(new JScrollPane(t_display), BorderLayout.CENTER);

		return dispalyPanel;
	}

	// input 패널
	private JPanel createInputPanel() {
		JPanel inputPanel = new JPanel(new BorderLayout());
		t_input = new JTextField(30);
		b_send = new JButton("보내기");
		
		ActionListener actionListener= new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				sendMesssage();// 텍스트필드에 엔터 입력시, sendMesssage 호출하여 텍스트필드에 입력한 문자열을 서버측 소켓으로전송
				//receiveMessage(); // 내가 메시지를 보내야지만 내가 메시지를 받을수있게됨. 이것이 한계.
			}
		};
		t_input.addActionListener(actionListener);
		b_send.addActionListener(actionListener);
		
		b_select = new JButton("선택하기");
		b_select.addActionListener(new ActionListener() {
			JFileChooser chooser = new JFileChooser(); //파일선택기 객체 생성.
			@Override
			public void actionPerformed(ActionEvent e) {
				FileNameExtensionFilter filter = new FileNameExtensionFilter( //파일선택기에 파일필터를 추가.
						"JPG & GIF & PNG Images", "jpg", "gif", "png"); //여러파일들 중에서 이미지파일만 선택할 수 있도록 필터링.
				
				chooser.setFileFilter(filter); //파일선택기에 필터를 추가.
				
				int ret = chooser.showOpenDialog(WithTalk.this); //파일선택기를 열고, 사용자가 파일을 선택하도록 함.
				//인자로 현재 객체의 참조를 전달하여, 파일선택기가 현재 객체에 열리도록 함. 여기서 현재 객체는 J프레임. 
				//즉, 프레임의 중앙에 파일선택 대화상자(다이얼로그)가 열림.
				
				if(ret != JFileChooser.APPROVE_OPTION){//사용자가 파일을 선택하지 않고 닫혔거나 취소버튼을 누른경우.
					JOptionPane.showMessageDialog(WithTalk.this, "파일을 선택하지 않았습니다.", "경고", JOptionPane.WARNING_MESSAGE);
					return; 
				}
				
				t_input.setText(chooser.getSelectedFile().getAbsolutePath()); //제대로된 선택이 일어났다면 즉, 선택한 파일이 존재하면, 선택한 파일의 절대경로명을  t_input에 출력.
				sendFile(); //선택한 파일을 서버로 전송.
			}
		});
		
		inputPanel.add(t_input, BorderLayout.CENTER);

		JPanel p_button = new JPanel(new GridLayout(1,0));
		p_button.add(b_select);
		p_button.add(b_send);
		
		inputPanel.add(p_button, BorderLayout.EAST);
		
		t_input.setEnabled(false);
		b_select.setEnabled(false);
		b_send.setEnabled(false);
		return inputPanel;
	}
	
	//아이디, 서버주소, 포트번호 입력 패널
	private JPanel createInfoPanel() {
		
		JPanel infoPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel l_userID= new JLabel("아이디: ");
		JLabel l_hostAddr= new JLabel("서버주소: ");
		JLabel l_portNum= new JLabel("포트번호: ");
		
		t_userID= new JTextField(8);
		t_hostAddr= new JTextField(13);
		t_portNum= new JTextField(5);
		
		
		try {
			InetAddress local = InetAddress.getLocalHost();
			String addr=local.getHostAddress();// 로컬 호스트의 ip주소 반환.
			String[] part = addr.split("\\.");
			t_userID.setText("guest"+ part[3]); // part[3]= ip주소의 마지막 바이트
		} catch (UnknownHostException e) {
			printDisplay("호스트 주소 오류: " +e.getMessage());
		}
		
		t_hostAddr.setText(serverAddress);
		t_portNum.setText(Integer.toString(serverPort));
		t_portNum.setHorizontalAlignment(JTextField.CENTER);
		
		
		infoPanel.add(l_userID);
		infoPanel.add(t_userID);
		infoPanel.add(l_hostAddr);
		infoPanel.add(t_hostAddr);
		infoPanel.add(l_portNum);
		infoPanel.add(t_portNum);
		
		return infoPanel;
	}
	

	// 서버 접속 관련 제어 담당 control 패널
	private JPanel createControlPanel() {
		JPanel controlPanel = new JPanel(new GridLayout(1,0));
		
		b_connect = new JButton("접속하기");
		
		b_disconnect = new JButton("접속 끊기");
		b_disconnect.setEnabled(false);
		
		b_exit = new JButton("종료하기");

		b_connect.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				
				serverAddress=t_hostAddr.getText(); //접속버튼 클릭시 텍스트필드에 입력된 값으로 서버주소와 포트번호 설정.
				serverPort=Integer.parseInt(t_portNum.getText());
				
				try {
					connectToServer(); // 접속하기버튼 클릭시 서버에 접속요청.
					sendUserID();//서버 접속후, 서버에게 사용자 아이디값(t_userID) 전송. 서버가 이 아이디값을 통해 클라이언트를 식별.
				} catch (UnknownHostException e1) {//connectToServer에서 throw한 예외처리를 여기서 예외처리.
					printDisplay("서버 주소와 포트번호를 확인하세요: " +e1.getMessage());
					return; //예외발생시 버튼객체들 상태변경 없이, 버튼 활성화 상태 그대로 유지하기위해 그냥 반환. 이후 코드진행x.
				} catch (IOException e2) {
					printDisplay("서버와의 연결 오류: " +e2.getMessage());
					return;
				}
				b_select.setEnabled(true);	
				b_connect.setEnabled(false);
				b_disconnect.setEnabled(true);
				b_send.setEnabled(true);
				b_exit.setEnabled(false);
				
				t_hostAddr.setEditable(false);
				t_portNum.setEditable(false);
				t_input.setEnabled(true);
				t_userID.setEditable(false);
			}
		});

		b_disconnect.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				disconnect(); // 접속끊기버튼 클릭시 서버와 연결종료.
				b_select.setEnabled(false);
				b_send.setEnabled(false);
				b_connect.setEnabled(true);
				b_exit.setEnabled(true);
				b_disconnect.setEnabled(false);
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
	
	private void connectToServer() throws UnknownHostException, IOException{//3초 타임아웃후 접속 실패=> 다른 주소로 입력만 받으면 되기때문에, 버튼 true false 활성화상태 그대로 유지하면서 서버주소-포트번호만 변경할 수 있도록 예외처리 throw. 

			socket= new Socket();   
			SocketAddress sa = new InetSocketAddress(serverAddress,serverPort); //사용자가 입력한 서버주소와 포트번호의 서버에 연결.
			socket.connect(sa, 3000); //해당 서버에 연결요청을 3초동안만 진행. 3초가 지나면 연결실패-> 이후에 다른 연결 요청을 받을수있도록.
			
			//socket= new Socket(serverAddress,serverPort); 
			
			out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream())); 
			//ObjectOutputStream 의 경우에는, 대응되는 ObjectInputStream이 정보를 받지 못한다하더라도 not blocking 즉 블로킹되지 않는 특징을 가지고있음. 그냥 "난 보냈어." 하고 끝인 것. 데이터를 전송하는 요청을 하더라도 해당 요청이 완료될 때까지 기다리지 않도록 설계되어 있음. 
			// 반면에 ObjectInputStream은 대응되는 ObjectOutputStream으로부터 일련의 header를 전달받기전까지는 blocking되어 대기하게됨.
			// ObjectInputStream의 readObject() 메서드는 데이터가 도착할 때까지 기다리기 때문에, ObjectOutputStream의 writeObject() 메서드가 호출되지 않으면 계속 대기하게됨.
			// 즉, ObjectOutputStream은 데이터를 전송할 때 블로킹되지 않지만, ObjectInputStream은 데이터를 읽을 때 블로킹될 수 있음
			
			//그래서 in 레퍼런스 변수는 connectToServer() 메소드에서 생성하면 연결하자마자 block 되어 전체가 대기하는 경우가 발생할 위험이있으므로 여기서 생성하지 않고, receiveMessage() 메소드에서 생성하도록 변경.

			receiveThread = new Thread(new Runnable() {
				private ObjectInputStream in; 
				
				
				//클라이언트가 서버에게 메시지를 보내고나서, 서버가 나에게 반향하는 메시지를 수신.
				private void receiveMessage() {
					
					try {
						ChatMsg inMsg = (ChatMsg)in.readObject(); // 서버로부터 채팅 메시지 전달받음.
						
						if(inMsg==null) {//서버측에서 소켓연결을 종료하여 스트림이 닫힌경우.
							disconnect();
							printDisplay("서버 연결 끊김");			         
			                return;
						}
						
						switch(inMsg.mode) { //수신된 메시지의 모드값에 따라 다른 처리.
						case ChatMsg.MODE_TX_STRING: //문자열을 전달받는 모드라면, 서버로부터 전달받은 id 와 문자열 메시지를 화면에 출력.
			                if (inMsg.userID.equals(uid)) {
			                    printDisplay("나: " + inMsg.message);
			                } else {
			                    printDisplay(inMsg.userID + ": " + inMsg.message);
			                }
			                break;
							
						case ChatMsg.MODE_TX_IMAGE:
							if (inMsg.userID.equals(uid)) {
			                    printDisplay("나: " + inMsg.message);
			                } else {
			                    printDisplay(inMsg.userID + ": " + inMsg.message);
			                }
			                printDisplay(inMsg.image);
			                break;
							
						case ChatMsg.MODE_TX_FILE:
							//printDisplay(inMsg.userID +": " + inMsg.message);
							printDisplay(inMsg.userID + "사용자로부터 "+inMsg.message +"파일을 수신했습니다.");

	        				byte[] buffer = inMsg.fileData; //전달받은 파일데이터를 담아둘 버퍼
	        				long size = inMsg.size; //전달받은 파일의 크기.
	        				int nRead = 0; //입력스트림으로부터 읽어들인 바이트량.
	        				String fileName= inMsg.message; //전달받은 파일명.
	        				File file = new File(fileName);
	        				BufferedOutputStream  bos=new BufferedOutputStream(new FileOutputStream(file)); 
	        				try {
	        					bos.write(buffer, 0, buffer.length);
	        					bos.flush();
	        				} catch (IOException e) {
	        					System.err.println("파일 전송 오류> " + e.getMessage());
	        				}
	        				
	        				ImageIcon icon = new ImageIcon(fileName); //수신받은 파일을 이미지아이콘으로 생성.
	        				printDisplay(icon); //이미지아이콘을 textPane에 출력.
	        				
	                        break;
						}
					} catch (IOException e) {
						printDisplay("연결을 종료했습니다.");

					} catch (ClassNotFoundException e) {
						printDisplay("잘못된 객체가 전달되었습니다.");
					}
				}
					
				
				@Override
				public void run() {
					try {
						in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
					}catch (IOException e) {
						printDisplay("입력 스트림이 열리지 않음");
					}
					while (receiveThread == Thread.currentThread()) {//connect 버튼을 한번에 여러번 누르는 경우를 대비해서, receiveThread가 현재 이 구문을 실행중인 스레드와 같을때만.
						receiveMessage();
					}
				}
				
			
			});
			
			receiveThread.start();
	
	}
	// 서버와의 연결 종료
	//이전에는 수신스레드와 클라이언트 소켓을 강제로 닫아버리는 방식으로 연결을 끊었었음.	
	//이제는 로그아웃 메시지를 서버에보내서 서버에게 연결을 종료한다고 미리 알림. 그러고나서 소켓을 닫아주면-> 서버입장에서는, "아 클라이언트가 종료되는구나" 라고 직관적으로 알 수 있음.
	private void disconnect() {
		send(new ChatMsg(uid, ChatMsg.MODE_LOGOUT));
		try {
			receiveThread=null; //이전에는 수신스레드와 클라이언트 소켓을 
			socket.close(); //소켓 연결을 강제로 종료해서 
			
		} catch (IOException e) {
			System.err.println("클라이언트 닫기 오류> " + e.getMessage());
			System.exit(-1);
		}
		
		t_userID.setEditable(true);
		t_hostAddr.setEditable(true);
		t_portNum.setEditable(true);
	}
	
	private void send(ChatMsg msg) { // 기존에는 sendMesssage를 통해서 하나의 행단위의 문자열만 전송했었음. 이제는 다양한 방식의 전송이 필요해서, 일단 하나의 채팅메시지 자체를 출력객체스트림을 통해서 서버로 전송하는 send 메서드 추가.
		try {
			out.writeObject(msg);
			out.flush();
		} catch (IOException e) {
			System.err.println("클라이언트 일반 전송 오류> " + e.getMessage());
		}
	}
	

	private void sendMesssage() { //행단위의 문자열 전송 대신에, send() 메서드를 통해서 하나의 채팅메시지인 ChatMsg 객체를 전송하는 방식으로 변경.

		String message = t_input.getText();
		
		if (message.isEmpty())// 아무것도 입력하지않고 보내려고한다면 그냥 return.
			return;
			
		send(new ChatMsg(uid, ChatMsg.MODE_TX_STRING, message)); //하나의 채팅메시지를 어떤 사용자가 보냈는지, 어떤 모드로 보냈는지, 어떤 메시지를 보냈는지를 ChatMsg 객체로 만들어서 전송.
		
		t_input.setText("");	
	}
	
	private void sendUserID() {//행단위의 문자열로 사용자 ID를 전송하는 것 대신에, send() 메서드를 통해서 하나의 채팅메시지인 ChatMsg 객체를 전송하는 방식으로 변경.
		
		
		//기존에는 , 일반 메시지 내용하고 사용자ID값을 둘다 문자열로 전송했었기에 둘을 구분할 수 없었다는 단점이 있었음. 
		//!!! 이제는 ChatMsg 객체를 통해서, ChatMsg 객체 내부의 필드와 모드값을 통해서, "어떤 정보를 전달하겠다" 구분하여 전송이 가능해짐.
	
		
		uid=t_userID.getText();
		send(new ChatMsg(uid, ChatMsg.MODE_LOGIN)); //서버에게 로그인 모드&사용자 아이디값을 전달. 서버가 이 아이디값을 통해 클라이언트를 식별.

	}

	private void sendImage() { // 이미지 파일 전송뿐만아니라, 전송된 그 이미지가 출력도 되도록.
		String filename = t_input.getText().strip(); //앞뒤 공백제거.->공백으로인한 에러방지.
		if(filename.isEmpty()) return;
		
		File file = new File(filename); //해당 파일명의 파일을 파일객체로써 표현. 
		//파일 입출력은 아니고, 그저 파일에 대한 정보를 유지할 수 있는 객체.
		
		if(!file.exists()) { //파일이 존재하지않는다면
			printDisplay(">> 파일이 존재하지 않습니다: " + filename);
			return;
		}
		
		//파일이 존재한다면
		ImageIcon icon = new ImageIcon(filename);
		send(new ChatMsg(uid, ChatMsg.MODE_TX_IMAGE, file.getName(), icon)); //이미지 전달 모드.
	
		t_input.setText("");
	}
	private void sendFile() {
	    String filename = t_input.getText().strip();
	    if (filename.isEmpty()) return;

	    File file = new File(filename);
	    if (!file.exists()) {
	        printDisplay(">> 파일이 존재하지 않습니다: " + filename);
	        return;
	    }

	    try {
	        byte[] fileData = new byte[(int) file.length()];
	        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
	        bis.read(fileData, 0, fileData.length); 

	        ChatMsg msg = new ChatMsg(uid, ChatMsg.MODE_TX_FILE, file.getName(), file.length(), fileData);
	        send(msg);
	    } catch (IOException e) {
	        printDisplay("파일 전송 오류: " + e.getMessage());
	    }
	    
	    printDisplay("파일 전송 완료: " + filename);
	    t_input.setText("");
	    
		ImageIcon icon = new ImageIcon(filename); //이미지아이콘 객체 생성.
		printDisplay(icon); //Receiver에게 전송한 이미지아이콘을 현재 이 Sender측의 textPane에도 출력.
	}
	
	
	
	
	public static void main(String[] args) {
		String serverAddress = "localhost"; // 연결하고자하는 서버의 주소는 로컬호스트. 즉, 내 컴퓨터.
		int serverPort = 54321;

		WithTalk client = new WithTalk(serverAddress, serverPort);
	}

}
