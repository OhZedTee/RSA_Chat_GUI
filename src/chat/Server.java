package chat;

import java.io.*;
import java.math.BigInteger;
import java.net.*;

//GUI gems
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class Server extends JFrame{
	
	private static final long serialVersionUID = 1L;
	
	/*Static Names*/
	private static String name;
	private static String otherName;	
	private static boolean isActive = false; 
	//private static boolean isSending = false; //false == receiving turn ? true == sending turn
	private static boolean isDisconnected = false;
	
	/*Use booleans to switch between modes! Create a switch */
	
	public static final int WIDTH = 800;
	public static final int HEIGHT = 600;
	
	
	//Panels
	private JPanel top = new JPanel();
	private JPanel bottom = new JPanel();
	private JPanel middle = new JPanel();
	
	//Top Fields
	private JLabel jl_Name = new JLabel("Name");
	private JButton btn_Status = new JButton("Connect");
	private JTextField tf_Name = new JTextField(20);
	
	//Bottom
	private JButton btn_Send = new JButton("Send");
	private JButton btn_Receive = new JButton("Receive");
	private JTextField tf_Message = new JTextField(150);
	private JPanel buttons = new JPanel();
	
	//Middle
	private JTextArea ta_Msg = new JTextArea();
	
	private JFrame window = new JFrame();
	
	
	/****Messenger Globals****/
	private RSA encryptionSystem = null;
	private ServerSocket server = null;
	private Socket socket = null;
	//private BufferedReader keyboardReader = null;
	private OutputStream out = null;
	private PrintWriter write = null;
	private InputStream in = null;
	private  BufferedReader receiveReader = null;
	private String receiveMsg = "", sendMsg = "";

	
	public Server()
	{
		super("Server");
		window.setSize(WIDTH, HEIGHT);
		window.setTitle("Server");
		window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		window.setLayout(new BorderLayout());
		//getContentPane().setLayout(new GridLayout(1, 3));

		// handle each exception as is relevant
    	try {
    	    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    	} catch (ClassNotFoundException e) {
    	    System.err.println("Could not find class name for system look and feel");
    	} catch (InstantiationException e) {
    	    System.err.println("Could not instantiate class for system look and feel");
    	} catch (IllegalAccessException e) {
    	    System.err.println("Could not access system look and feel.");
    	} catch (UnsupportedLookAndFeelException e) {
    	    System.err.println("Platform's native look and feel not supported.");
    	}
		
    	
    	//GUI 
    	top.setLayout(new GridLayout(1,3));
    	jl_Name.setFont(new Font("Comic Sans", Font.BOLD, 18));
    	btn_Status.setForeground(Color.RED);
    	top.add(jl_Name);
    	top.add(tf_Name);
    	top.add(btn_Status);
    	
    	middle.setLayout(new GridLayout(1,1));
    	ta_Msg.setEditable(false);
		//ta_Msg.setLineWrap(true);
		middle.add(ta_Msg);
		
        btn_Receive.setBorderPainted(false);
        btn_Receive.setBackground(Color.red);
        btn_Receive.setBorder(null);
        btn_Receive.setFocusable(false);
        btn_Receive.setMargin(new Insets(0, 0, 0, 0));
        btn_Send.setPreferredSize(new Dimension(150,20));
        btn_Receive.setPreferredSize(new Dimension(100,20));
		
		
		bottom.setLayout(new GridLayout(1,3));
		bottom.add(tf_Message);
		buttons.add(btn_Send);
		buttons.add(btn_Receive);
		bottom.add(buttons);
		
		btn_Send.setEnabled(false);
		btn_Receive.setEnabled(false);
		
		btn_Receive.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent arg0) {
				//Once text sent, wait for response!
				if (isActive && btn_Receive.isEnabled())
				{
					while(true) //Connected and receiving turn
					{
						try
						{
							if ((receiveMsg = receiveReader.readLine()) != null)
							{
								String message = "";
								BigInteger decMsg;
							
								decMsg = encryptionSystem.decrypt(receiveMsg);
								message = encryptionSystem.convertByteArrToString(decMsg.toByteArray());
							
								if (message.equalsIgnoreCase("q!"))
								{
									ta_Msg.setText("Your Friend has left the chat! Press the Exit Button to Quit now\n");
									btn_Status.setText("Disconnected!");
									btn_Status.setForeground(Color.RED);
									isActive = false;
									isDisconnected = true;
									btn_Receive.setEnabled(false);
									btn_Send.setEnabled(false);
								
									try {
										server.close();
										socket.close();
									} catch (IOException e) {
										e.printStackTrace();
									}
								
									window.setDefaultCloseOperation(EXIT_ON_CLOSE);

								}
								else
								{
									ta_Msg.setText(ta_Msg.getText() + String.format("$%s: ", otherName) +  message + "\n");									
									btn_Receive.setEnabled(false);
									btn_Send.setEnabled(true);									
								}
								
								break;
							}
						} 
						catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				
				else if (!isActive) //Not connected
				{
					if (!isDisconnected) 
						ta_Msg.setText("Please enter your name and connect first!\n");
					else
						ta_Msg.setText("You have disconnected, please exit the app\n");
				}
			}
		});
		
		btn_Send.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				if(isActive && btn_Send.isEnabled())
				{
				
					String encMsg = "";
					sendMsg = tf_Message.getText();
					tf_Message.setText("");
					ta_Msg.setText(ta_Msg.getText() + String.format("$%s: ", name) + sendMsg + "\n");
					
					encMsg = encryptionSystem.encrypt(sendMsg);		
					write.println(encMsg);
					write.flush();
				
					if (sendMsg.equalsIgnoreCase("q!"))
					{
						btn_Status.setText("Disconnected!");
						btn_Status.setForeground(Color.RED);
						ta_Msg.setText("You have quit the chat! Press the Exit Button to Quit now\n");
						isActive = false;
						isDisconnected = true;
						btn_Send.setEnabled(false);
						btn_Receive.setEnabled(false);
						
						try {
							server.close();
							socket.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						window.setDefaultCloseOperation(EXIT_ON_CLOSE);
					}
					else
					{
						btn_Send.setEnabled(false);
						btn_Receive.setEnabled(true);
					}
					
				}
			
				else if (!isActive) //Not connected
				{
					if (!isDisconnected) 
						ta_Msg.setText("Please enter your name and connect first!\n");
					else
						ta_Msg.setText("You have disconnected, please exit the app\n");
				}
			}
		});
		
		btn_Status.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (!tf_Name.getText().isEmpty() && !isActive && !isDisconnected)
				{
					name = tf_Name.getText();
					tf_Name.setEditable(false);
					isActive = true;
					
					try {
						server = new ServerSocket(5050);
						socket = server.accept();
						out = socket.getOutputStream();
						in = socket.getInputStream();
					} catch (IOException e) {
						e.printStackTrace();
					}
					encryptionSystem = new RSA (1024);
					
					write = new PrintWriter(out, true);
					receiveReader = new BufferedReader(new InputStreamReader(in));
					
					//Share public key and modulus with other user!
					while(true)
					{
						try
						{
							if ((receiveMsg = receiveReader.readLine()) != null)
							{
								if (receiveMsg.startsWith("public-"))
								{
									encryptionSystem.setGivenPublicKey(new BigInteger(receiveMsg.substring(7)));
									write.println("public-" + encryptionSystem.getPublicKey());
									write.flush();
									receiveMsg = "";
									encryptionSystem.setReceivedPublic(true);
								}
								
								else if (receiveMsg.startsWith("modulus-"))
								{
									encryptionSystem.setGivenModulus(new BigInteger(receiveMsg.substring(8)));
									write.println("modulus-" + encryptionSystem.getModulus());
									write.flush();
									receiveMsg = "";
									encryptionSystem.setReceivedModulus(true);
								}
								
								else if (receiveMsg.startsWith("name-"))
								{
									otherName = receiveMsg.substring(5);
									receiveMsg = "";
									write.println("name-" + name);
									write.flush();
								}
							}
							if (encryptionSystem.isReceivedModulus() && encryptionSystem.isReceivedPublic() && otherName != null)
							{
								break;
							}
						}
					
						catch (SocketException se)
						{
							continue;
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				
					receiveMsg = "";
					sendMsg = "";
					System.out.println("public: " + encryptionSystem.getGivenPublicKey());
					System.out.println("modulus: " + encryptionSystem.getGivenModulus());
					ta_Msg.setText("Connected to " + otherName + "\nHave Fun Chatting securely over a 1024-bit RSA Encryption\n");
					
					
					btn_Status.setForeground(Color.GREEN);
					btn_Status.setText("Connected!");
					btn_Receive.setEnabled(true);
				}
				
				else if (isActive)
				{
					//Already connected
					ta_Msg.setText(ta_Msg.getText() + "\n\nAlready connected!\n");
				}
				else if (isDisconnected)
				{
					//Need to quit
					ta_Msg.setText("You have disconnected, please exit the app\n");
				}
				else
				{
					//Didn't enter a name
					ta_Msg.setText("Please enter your name in order to connect!\n");
				}
			}
		});
		
		window.add(top, BorderLayout.NORTH);
		window.add(middle, BorderLayout.CENTER);
		window.add(bottom, BorderLayout.SOUTH);
		window.setVisible(true);
	}
	
	
	public static void main (String[] args) throws Exception
	{
		new Server();		
	}
}