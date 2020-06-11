package tp;
 
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
 
public class TestFTP {
 
	/**
         * @param args
         */
	public static void main(String[] args) {
		
    
		 JFrame f=new JFrame("Client FTP");
		   f.setSize(300,500);
		   f.getContentPane().setLayout(null);
		   f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		   f.setResizable(false);
		   f.getContentPane().setLayout(null);
		   
		   JTextField t1=new JTextField("127.0.0.1");
		   t1.setBounds(10,50,250,50);
		   t1.setFont(new Font("Arial Rounded MT Bold", Font.PLAIN, 17));
		   f.add(t1);
		   
		   JTextField t2=new JTextField("21");
		   t2.setBounds(10,120,250,50);
		   t2.setFont(new Font("Arial Rounded MT Bold", Font.PLAIN, 17));
		   f.add(t2);
		   
		   JTextField t3=new JTextField("AkelChaima");
		   t3.setBounds(10,190,250,50);
		   t3.setFont(new Font("Arial Rounded MT Bold", Font.PLAIN, 17));
		   f.add(t3);
		   
		   JPasswordField t4=new JPasswordField("AkelChaima RSD1");
		   t4.setBounds(10,250,250,50);
		   t4.setFont(new Font("Arial Rounded MT Bold", Font.PLAIN, 17));
		   f.add(t4);
		   
		   JButton b1=new JButton("Connecter");
		   b1.setBounds(30, 400, 200, 30);
			b1.setFont(new Font("Yu Mincho Light", Font.BOLD, 15));
			b1.setForeground(new Color(0, 0, 255));

		   b1.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				FtpConnection clientFtp = new FtpConnection(true,0);
				try {
					clientFtp.connect(t1.getText(), Integer.parseInt(t2.getText()));
					clientFtp.login(t3.getText(), t4.getText().toString());
					

					f.hide();
					
					
					
					JFrame f2=new JFrame("Execution Des Commandes Possibles");
					f2.setSize(500, 500);
				    f2.setResizable(false);
					f2.getContentPane().setLayout(null);
					
					JTextArea l1=new JTextArea();
					l1.setBounds(10,50,450,200);
					l1.setFont(new Font("Arial Rounded MT Bold", Font.PLAIN, 12));
					f2.add(l1);
			
					
					JTextField t1=new JTextField();
					t1.setBounds(10, 20, 200, 20);
					t1.setFont(new Font("Arial Rounded MT Bold", Font.PLAIN, 15));
					f2.add(t1);
					
					JButton b1=new JButton("PWD");
					b1.setBounds(50, 400, 100, 30);
					b1.setFont(new Font("Yu Mincho Light", Font.BOLD, 12));
					f2.add(b1);
					b1.addActionListener(new ActionListener() {
						
						@Override
						public void actionPerformed(ActionEvent e) {
							// TODO Auto-generated method stub
							
							try {
								l1.setText("");
								clientFtp.getCurrentDirectory();
								l1.setText("Le repértoire courant : "+clientFtp.getCurrentDirectory());
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
					});
					
					JButton b2=new JButton("QUIT");
					b2.setBounds(150, 400, 100, 30);
					b2.setFont(new Font("Yu Mincho Light", Font.BOLD, 12));
					f2.add(b2);
					b2.addActionListener(new ActionListener() {
						
						@Override
						public void actionPerformed(ActionEvent e) {
							// TODO Auto-generated method stub
							
							try {
								l1.setText("");
								clientFtp.logout();
						System.out.println("**** FIN ***");
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							f2.hide();
							f.setVisible(true);
						}
					});
					
					
					JButton b3=new JButton("CWD");
					b3.setBounds(250, 400, 100, 30);
					b3.setFont(new Font("Yu Mincho Light", Font.BOLD, 12));
					f2.add(b3);
					b3.addActionListener(new ActionListener() {
						
						@Override
						public void actionPerformed(ActionEvent e) {
							// TODO Auto-generated method stub
							
							try {
								l1.setText("");
								clientFtp.changeDirectory(t1.getText());
								l1.setText("Aller à "+t1.getText());
								t1.setText("");
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
					});
					
					JButton b4=new JButton("LIST");
					b4.setBounds(350, 400, 100, 30);
					b4.setFont(new Font("Yu Mincho Light", Font.BOLD, 12));
					f2.add(b4);
					b4.addActionListener(new ActionListener() {
						
						@Override
						public void actionPerformed(ActionEvent e) {
							// TODO Auto-generated method stub
							
							try {
								l1.setText("");
								clientFtp.listFiles();
								l1.setText("Contenu de répertoire courant: " +clientFtp.listFiles());

							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
					});
					
					
					JButton b5=new JButton("Download");
					b5.setBounds(50, 300, 100, 30);
					b5.setFont(new Font("Yu Mincho Light", Font.BOLD, 12));
					f2.add(b5);
					b5.addActionListener(new ActionListener() {
						
						@Override
						public void actionPerformed(ActionEvent e) {
							// TODO Auto-generated method stub
							
							try {
								l1.setText("");
								clientFtp.downloadFile(t1.getText());
								long s = clientFtp.getFileSize(t1.getText());
								l1.setText("Server Reply = \n 226: Transfert du fichier " 
								+t1.getText()+" est terminé avec succès, "
								+ "Sa taille = " +clientFtp.getFileSize(t1.getText()));
								t1.setText("");
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
					});
					
					
					JButton b6=new JButton("Store");
					b6.setBounds(150, 300, 100, 30);
					b6.setFont(new Font("Yu Mincho Light", Font.BOLD, 12));
					f2.add(b6);
					b6.addActionListener(new ActionListener() {
						
						@Override
						public void actionPerformed(ActionEvent e) {
							// TODO Auto-generated method stub
							
							try {
								l1.setText("");
								clientFtp.uploadFile(t1.getText());
								l1.setText("Server Reply = \n 226: Transfert du fichier " 
								+t1.getText()+" est terminé avec succès, "
										+ "Sa taille = " +clientFtp.getFileSize(t1.getText()));
							
								t1.setText("");
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
					});
					
					JButton b7=new JButton("Delete");
					b7.setBounds(250, 300, 100, 30);
					b7.setFont(new Font("Yu Mincho Light", Font.BOLD, 12));
					f2.add(b7);
					b7.addActionListener(new ActionListener() {
						
						@Override
						public void actionPerformed(ActionEvent e) {
							// TODO Auto-generated method stub
							
							try {
								l1.setText("");
								clientFtp.deleteFile(t1.getText());
								l1.setText("250: Requête exécutée : Fichier suprimé avec succès");
								
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								l1.setText("Requête non exécutée : Fichier indisponible");
								e1.printStackTrace();
							}
						}
					});
					
					JButton b8=new JButton("ls -la");
					b8.setBounds(350, 300, 100, 30);
					b8.setFont(new Font("Yu Mincho Light", Font.BOLD, 12));
					f2.add(b8);
					b8.addActionListener(new ActionListener() {
						
						@Override
						public void actionPerformed(ActionEvent e) {
							// TODO Auto-generated method stub
							try {
								clientFtp.listSubdirectories(t1.getText());
								l1.setText("");
								l1.setText("le Contenu du dossier : "+clientFtp.listSubdirectories(t1.getText())+"\n Server Reply = \n 227: Mode passif \n "
									                	+" 200: Action demandée accomplie avec succès \n"
									                	+ " 150: Statut du fichier ok ; Ouverture de la connexion en cours\n");
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
								l1.setText("Server Reply = \n 550: Requête non exécutée,Fichier indisponible \n ");

							}
						}
					});
					
					
					f2.setVisible(true);
					
					
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}


		});
		   f.add(b1);
		   f.setVisible(true);
	}
 
}