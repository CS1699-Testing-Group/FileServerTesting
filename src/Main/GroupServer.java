/* Group server. Server loads the users from UserList.bin.
 * If user list does not exists, it creates a new list and makes the user the server administrator.
 * On exit, the server saves the user list to file. 
 */

/*
 * TODO: This file will need to be modified to save state related to
 *       groups that are created in the system
 *
 */

import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.security.Key;
import java.security.SecureRandom;
import java.util.*;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

public class GroupServer extends Server {

	public static final int SERVER_PORT = 8765;
	public UserList userList;
	public String GS_publicKey = "305c300d06092a864886f70d0101010500034b003048024100914754aec76a53097bb052be8154be1e493f803809b706daf3f09030047e05083e8c45181f45c6741b30232bff46b4d00868aeb9b8e30f6aaf2a3dfb248771d90203010001";
	public String GS_privateKey = "30820154020100300d06092a864886f70d01010105000482013e3082013a020100024100914754aec76a53097bb052be8154be1e493f803809b706daf3f09030047e05083e8c45181f45c6741b30232bff46b4d00868aeb9b8e30f6aaf2a3dfb248771d9020301000102405e7402f1b38344438920655ba89861172dc654659aa35d98a33b0773c0a7f23aa404e18f1c9ce241d706803469b53034fdfb269f4f80b175241791a0380dead1022100c8531a9437672ef099f010484c6a88295adcf8d401ad9230c79c53a15b3b678d022100b9a7c294b0ff532abec703c919eb741dd90cb822b597bd7d73543a26e01bea7d0220637918c6a6a83f1fcc60efc4e6e5338dcd87d2ab7bd5d3b51339a631869afdf502206009e66060c753d072ec248b2d3b5dcfeaede77b1d1127d6f38808a4ff9db1490221009ec47cf98186c862af9c4a9124bf1b2102ef5e275d81493909b9214b5605542d";

	
	public GroupServer() {
		super(SERVER_PORT, "ALPHA");
	}
	
	public GroupServer(int _port) {
		super(_port, "ALPHA");
	}
	
	public void start() {
		// Overwrote server.start() because if no user file exists, initial admin account needs to be created
		
		String userFile = "UserList.bin";
		Scanner console = new Scanner(System.in);
		ObjectInputStream userStream;
		ObjectInputStream groupStream;
		
		//This runs a thread that saves the lists on program exit
		Runtime runtime = Runtime.getRuntime();
		runtime.addShutdownHook(new ShutDownListener(this));
		
		//Open user file to get user list
		try
		{
			FileInputStream fis = new FileInputStream(userFile);
			userStream = new ObjectInputStream(fis);
			userList = (UserList)userStream.readObject();
		}
		catch(FileNotFoundException e)
		{
			// get name and password for admin
			System.out.println("UserList File Does Not Exist. Creating UserList...");
			System.out.println("No users currently exist. Your account will be the administrator.");
			System.out.print("Enter your username: ");
			String username = console.next();
			System.out.print("Enter your password: ");
			String password = console.next();
			
			//Create a new list, add current user to the ADMIN group along with password. They now own the ADMIN group.
			userList = new UserList();
			userList.addUser(username, password);
			userList.addGroup(username, "ADMIN");
			userList.addOwnership(username, "ADMIN");
			//System.out.println("GENERATING KEY FOR ADMIN GROUP");
			Key key;
			
			try {
				key = GenerateSymmetricKey(128);
			
			byte[] IV = new byte[16];
			Random random = new SecureRandom();
			random.nextBytes(IV);
			byte[] key_bytes = key.getEncoded();
			//System.out.print("Actual key: ");
			String s1 = "0_"+"ADMIN"+"_"+Hex.toHexString(key_bytes)+"_"+Hex.toHexString(IV);
			
			//System.out.println(s1);
			//System.out.println("END GENERATING KEY");
			// KEY GENERATION COMPLETED
			
			
			//ADD KEY TO HASHTABLE
			//System.out.println("ADDING KEY TO GROUP SERVER HASHTABLE(Deleted Member)");
			
			ArrayList<String> adminList = new ArrayList<String>();
			adminList.add(s1);
			userList.groupKeyTable.put("ADMIN", adminList);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		catch(IOException e)
		{
			System.out.println("Error reading from UserList file");
			System.exit(-1);
		}
		catch(ClassNotFoundException e)
		{
			System.out.println("Error reading from UserList file");
			System.exit(-1);
		}
		
		//Autosave Daemon. Saves lists every 5 minutes
		AutoSave aSave = new AutoSave(this);
		aSave.setDaemon(true);
		aSave.start();
		
		//This block listens for connections and creates threads on new connections
		try
		{
			
			final ServerSocket serverSock = new ServerSocket(port);
			
			Socket sock = null;
			GroupThread thread = null;
			
			while(true)
			{
				sock = serverSock.accept();
				thread = new GroupThread(sock, this);
				thread.start();
			}
		}
		catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}

	}

	public static SecretKey GenerateSymmetricKey(int keySizeInBits)throws Exception {
		KeyGenerator keyGenerator = KeyGenerator.getInstance("AES", "BC");
		SecretKey secretkey = keyGenerator.generateKey();
		return secretkey;
	}
	
}

//This thread saves the user list
class ShutDownListener extends Thread
{
	public GroupServer my_gs;
	
	public ShutDownListener (GroupServer _gs) {
		my_gs = _gs;
	}
	
	public void run()
	{
		System.out.println("Shutting down server");
		ObjectOutputStream outStream;
		try
		{
			outStream = new ObjectOutputStream(new FileOutputStream("UserList.bin"));
			outStream.writeObject(my_gs.userList);
		}
		catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}
}

class AutoSave extends Thread
{
	public GroupServer my_gs;
	
	public AutoSave (GroupServer _gs) {
		my_gs = _gs;
	}
	
	public void run()
	{
		do
		{
			try
			{
				Thread.sleep(300000); //Save group and user lists every 5 minutes
				System.out.println("Autosave group and user lists...");
				ObjectOutputStream outStream;
				try
				{
					outStream = new ObjectOutputStream(new FileOutputStream("UserList.bin"));
					outStream.writeObject(my_gs.userList);
				}
				catch(Exception e)
				{
					System.err.println("Error: " + e.getMessage());
					e.printStackTrace(System.err);
				}
			}
			catch(Exception e)
			{
				System.out.println("Autosave Interrupted");
			}
		}while(true);
	}

}

