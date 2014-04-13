import java.util.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.*;

import java.io.*;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.InvalidKeyException; 
import java.security.Key; 
import java.security.KeyFactory; 
import java.security.MessageDigest; 
import java.security.NoSuchAlgorithmException; 
import java.security.NoSuchProviderException; 
import java.security.PublicKey; 
import java.security.Security;
import java.security.Signature; 
import java.security.SignatureException; 
import java.security.spec.InvalidKeySpecException; 
import java.security.spec.X509EncodedKeySpec; 
import java.util.Scanner; 
import java.util.List; 
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher; 
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator; 
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec; 
  




import org.bouncycastle.jce.provider.BouncyCastleProvider; 
import org.bouncycastle.util.encoders.Hex; 

public class MainUserInterface 
{
	
	public static String GS_publicKey = "305c300d06092a864886f70d0101010500034b003048024100914754aec76a53097bb052be8154be1e493f803809b706daf3f09030047e05083e8c45181f45c6741b30232bff46b4d00868aeb9b8e30f6aaf2a3dfb248771d90203010001";
	public static void main(String[] args) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, NoSuchPaddingException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException 
	{
		Security.addProvider(new BouncyCastleProvider());
        

		boolean pubKeyVerified = false;
		String publicKey = "";

		Scanner scanner = new Scanner(System.in);

		GroupClient groupClient = new GroupClient();
		FileClient fileClient = new FileClient();

		//Variables for user input
		String chosenStr;
		int chosenInt;

		//Variables to validate user input
		boolean validToken = false;
		boolean validChoice = true;

		//User parameters
		String username = null;
		byte[] userToken = null;

		//Networking paramaters for file server
		String fsIP = "";
		int fsPort = 0; //default port
		String fsCustomPort = "";
		
		//Networking parameters for the group server
		String gsIP = "";
		int gsPort = 0;
		String gsCustomPort = "";
		
		//Dual networking parameters
		String ipOptionStr;
		int ipOptionInt = 0;
		
		boolean loopIP = false;

String checkPassword ="";

		while(true){ //loop overall program until user chooses to quit

			System.out.println("Please enter a number.");
			System.out.println("\t1: Login with Username");
			System.out.println("\t2: Quit");
			System.out.print("\nEntry: ");

			chosenStr = scanner.nextLine();
			try{	//make sure input is valid
				validChoice = true;
				chosenInt = Integer.parseInt(chosenStr);
			}catch(Exception e){
				validChoice = false;
				chosenInt = 0;
			}

			if(validChoice == true)
			{
				if(chosenInt == 1)
				{

					
						byte[] publicbytes = Hex.decode(GS_publicKey); //generate public key bytes using string value of key
				        X509EncodedKeySpec keySpecPublic = new X509EncodedKeySpec(publicbytes);
				        KeyFactory keyFactoryPublic = KeyFactory.getInstance("RSA");
				        PublicKey public_key_final = keyFactoryPublic.generatePublic(keySpecPublic); //generate public key from private key bytes   

				        
						
					System.out.println("Enter your username");
					System.out.print("Username: ");
					
					username = scanner.nextLine();
					byte[] byteUsername = username.getBytes("UTF8");
					Cipher c = Cipher.getInstance("RSA", "BC");
					c.init(Cipher.ENCRYPT_MODE, public_key_final);
					byte[] cipherText = c.doFinal(byteUsername);
					
					System.out.println("Enter your password");
					System.out.print("password: ");
					
					checkPassword = scanner.nextLine();
					byte[] bytePass = checkPassword.getBytes("UTF8");
					Cipher cPass = Cipher.getInstance("RSA", "BC");
					cPass.init(Cipher.ENCRYPT_MODE, public_key_final);
					byte[] cipherTextPass = cPass.doFinal(bytePass);
					
					int time = (int)TimeUnit.MINUTES.toMinutes(System.currentTimeMillis());	//current sys time
					byte[] myTime = (Integer.toString(time)).getBytes("UTF8");	//convert time in seconds int -> String -> byte array
					Cipher cTime = Cipher.getInstance("RSA", "BC");
					cTime.init(Cipher.ENCRYPT_MODE, public_key_final);
					byte[] cipherTimeStamp = cTime.doFinal(myTime);
					
					
					
					//ip stuff here for group client
					do{

						System.out.println("Where is the Group Server you would like to connect to for authentication?");
						System.out.println("\t0: Default (localhost, 8765)");
						System.out.println("\t1: Custom");
					
						ipOptionStr = scanner.nextLine();
						ipOptionInt = Integer.parseInt(ipOptionStr);

					
							if(ipOptionInt == 0)//default localhost ip
							{
								gsIP = "localhost";
								gsPort = 8765;
								loopIP = false;
							}
							else if(ipOptionInt == 1)//custom ip
							{
								System.out.print("Please enter the IP address: ");
								gsIP = scanner.nextLine();
								System.out.print("Please enter the port: ");
								gsCustomPort = scanner.nextLine();
								gsPort = Integer.parseInt(gsCustomPort);
								loopIP = false;
							}
							else
							{
								loopIP = true;
							}
						
					}while(loopIP == true);
					
					//end
					groupClient.connect(gsIP, gsPort);
					if(groupClient.isConnected())
					{
					if (pubKeyVerified == false){
							if (groupClient.requestPublicKey() != null){
								String trust ="";
								publicKey = groupClient.requestPublicKey();
								System.out.println("\nThis Group Server's Public Key is: " + publicKey + "\n");
								System.out.println("\nWould you like to trust this server?");
								System.out.println("Enter 'Yes' or 'No'");
								trust = scanner.nextLine();
									if (trust.equals("Yes")){
										pubKeyVerified = true;
										System.out.println("Adding to list of trusted servers...");
									}else if(trust.equals("No")){
											System.out.println("Exiting...");
											System.exit(0);
									}else{
										System.out.println("Invalid input...");
										System.exit(0);
									}


							
							}else{
								pubKeyVerified = false;
							}
						}else{
							System.out.println("The public key from the group server has already been verified");
							System.out.println("Continuing...");

						}
					}

					

					if(groupClient.isConnected())
					{
						///////////////////////
						///// T7 Section //////
						///////////////////////
						String ipAddress = "";
						System.out.println("Please enter the IP address of the File Server you will connect to, add '/' in front of the IP Address: ");
						ipAddress = scanner.nextLine();
						String port = "";
						System.out.println("Please enter the port: ");
						port = scanner.nextLine();
						

						userToken = groupClient.getToken(cipherText, cipherTextPass, cipherTimeStamp, ipAddress, port);
						///////////////////////
						///// T7 Section //////
						///////////////////////
						if(userToken != null)	//Valid login
						{
							validToken = true;
							groupClient.disconnect();
						}
						else	//Invalid login
						{
							System.out.println("Invalid Username/Password combination");
							groupClient.disconnect();
						}
					}
					else
					{
						System.out.println("Group Server Not Connecting...");
					}
					
				}
				else if(chosenInt == 2) //user chose to quit
				{
					System.out.println("Shutting down...");
					System.exit(0);
				}
				else
				{
					System.out.println("Invalid Choice. Please enter a valid command.");
				}
				
				while(validToken) //User checks out. Token issued. User may continue...
				{
					System.out.println("Please enter a number.");
					System.out.println("\t1: Connect to the Group Server");
					System.out.println("\t2: Connect to the File server");
					System.out.println("\t3: Quit");
					System.out.print("\nEntry: ");

					validChoice = true;
					chosenStr = scanner.nextLine();
					try{
						validChoice = true;
						chosenInt = Integer.parseInt(chosenStr);
					}catch(Exception e){
						validChoice = false;
						chosenInt = 0;
					}
					if(validChoice == true)
					{

						if(chosenInt == 1)//Group server
						{
							CallGroupServer(gsIP, gsPort, userToken);
						}
						else if(chosenInt == 2)//File Server
						{
							do{
								groupClient.connect(gsIP, gsPort);
								if(groupClient.isConnected())
								{
									//userToken = groupClient.getToken(username, checkPassword);
									if(userToken != null)	//Valid login
									{
										validToken = true;
										groupClient.disconnect();
									}
									else	//Invalid login
									{
										System.out.println("Invalid Username.");
										groupClient.disconnect();
									}
								}
								System.out.println("Where is the server you would like to connect to?");
								System.out.println("\t0: Default (localhost, 4321)");
								System.out.println("\t1: Custom");
							
								ipOptionStr = scanner.nextLine();
								ipOptionInt = Integer.parseInt(ipOptionStr);
								if(ipOptionInt == 0)//default localhost ip
								{
									CallFileServer("localhost", 4321, userToken);
									loopIP = false;
								}
								else if(ipOptionInt == 1)//custom ip
								{
									System.out.print("Please enter the IP address: ");
									fsIP = scanner.nextLine();
									System.out.print("Please enter the port: ");
									fsCustomPort = scanner.nextLine();
									fsPort = Integer.parseInt(fsCustomPort);
									
									CallFileServer(fsIP, fsPort, userToken);
									loopIP = false;
								}
								else
								{
									loopIP = true;
								}
							}while(loopIP == true);
						}
						else if(chosenInt == 3)//Quit
						{
							System.out.println("Shutting down...");
							System.exit(0);
						}
						else //invalid entry
						{
							System.out.println("Invalid Choice. Please enter a valid command.");
						}
					}
					
					else //second validChoice != true
					{
						System.out.println("Invalid Choice. Please enter a valid command.");
					}
				}
				
			}
			else{ 	//if initial valid choice != true
				System.out.println("Invalid Choice. Please enter a valid command.");
			}
				
		}
			
	}
	public static void CallGroupServer(String ip, int port, byte[] userToken) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException
	{
		GroupClientSubInterface groupClientSubInterface = new GroupClientSubInterface();
		groupClientSubInterface.startGroupInterface(ip, port, userToken);
	}
	public static void CallFileServer(String ip, int port, byte[] userToken) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException
	{
		FileClientSubInterface fileClientSubInterface = new FileClientSubInterface();
		fileClientSubInterface.startFileInterface(ip, port,userToken);
	}
	
}