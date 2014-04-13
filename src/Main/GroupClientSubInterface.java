import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Scanner;
import java.util.List;
import java.net.Socket;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.KeyGenerator;
import javax.crypto.Cipher;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;


public class GroupClientSubInterface
{

	GroupClient groupClient = new GroupClient();
	public String GS_publicKey = "305c300d06092a864886f70d0101010500034b003048024100914754aec76a53097bb052be8154be1e493f803809b706daf3f09030047e05083e8c45181f45c6741b30232bff46b4d00868aeb9b8e30f6aaf2a3dfb248771d90203010001";

	public void startGroupInterface(String server, int port, byte[] byte_token) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException
	{
		System.out.println("test: SERVER IS STARTING!");
		if(groupClient.connect(server, port))
		{
			System.out.println("test: Server has connected!");
			Security.addProvider(new BouncyCastleProvider());
			//need to verify token, and create a new token with parameters if correct
			Token token = verifyToken(byte_token); //returns a re-made token if verified, returns null if invalid token
			
			Scanner scan = new Scanner(System.in);
			boolean quit = false;
			boolean pubKeyShown = false;
			String chosenStr;
			int chosenInt;
			String password = "";
			String thisUser = "";
			String thisGroup = "";

			//for group member listing
			List<String> userList;
			
			IvParameterSpec iv;
			PublicKey public_key_final;
			Key AESkey = null;
			
			
			System.out.println("\nWelcome to the Group client!");

			//copied from file server
			
			
			if(pubKeyShown == false)	//test to see if key has been printed yet
			{
				if(groupClient.requestPublicKey(token) != null)//get key from FileThread
				{
					
					GS_publicKey = groupClient.requestPublicKey(token);
					System.out.println("\nThis Group Server's Public Key is: " + GS_publicKey + "\n");
					String myIV = groupClient.requestIV(token); ////get string version of IV from server
					byte[] myIVbytes = myIV.getBytes(); //get bytes from sent iv string
					iv = new IvParameterSpec(myIVbytes); //get new iv from the byte array
					
		
					//send pub key encrypted aes key to server
					if(groupClient.sendEncAESKey())
					{
						System.out.println("\nEncrypted AES Key sent to server successfully\nSSH may now begin\n");
					}
					else
					{
						System.out.println("ERROR: Key not received successfully");
					}
				//end aes creation/sending
					

					if(groupClient.HASH_sendEncAESKey())
					{
						System.out.println("\nEncrypted AES Key for HASH sent to server successfully");
					}
					else
					{
						System.out.println("ERROR: HASH Key not received successfully");
					}
						//end encrypt and send to server
					
					
					//end NEW AES Key
					
					
					pubKeyShown = true;
				}
				else	//ERROR - Key does not exist on file server.
				{
					System.out.println("ERROR - File Server's Public Key is INVALID");
					groupClient.disconnect();
					pubKeyShown = true;
				}
			}
			//end copied from file server
			
			
			
			while(!quit)
			{
				System.out.println("Please select a number corresponding to the following options...");
				System.out.println("------User Options------");
				System.out.println("\t1: Create a new user");
				System.out.println("\t2: Delete an existing user");
				System.out.println("------Group Options------");
				System.out.println("\t3: Create a new group");
				System.out.println("\t4: Delete an existing group");
				System.out.println("\t5: Add user to an existing group");
				System.out.println("\t6: Delete user from existing group");
				System.out.println("\t7: List a group's members");

				System.out.println("------Other Options------");
				System.out.println("\t8: Quit the Group Server");

				System.out.print("Enter a number: ");

				chosenStr = scan.nextLine();
				chosenInt = Integer.parseInt(chosenStr);


				if(chosenInt == 1) //create new user
				{
					System.out.print("Please enter a username: ");
					
					thisUser = scan.nextLine();
					System.out.print("Please enter a password: ");
					
					password = scan.nextLine();
					
					if(groupClient.createUser(thisUser, token, password))
					{
						System.out.println("User creation successful!");
					}
					else
					{
						System.out.println("User creation failed.");
					}
					
				}
				else if(chosenInt == 2) //delete existing user
				{
					System.out.print("Please enter a username: ");
					
					thisUser = scan.nextLine();
					
					if(groupClient.deleteUser(thisUser, token))
					{
						System.out.println("User deleted!");
					}
					else
					{
						System.out.println("User deletion failed");
					}
				}
				else if(chosenInt == 3) //create new group
				{
					System.out.print("Please enter a Group Name: ");
					
					thisGroup = scan.nextLine();
					
					if(groupClient.createGroup(thisGroup, token))
					{
						System.out.println("Group Created!");
					}
					else
					{
						System.out.println("Group creation failed.");
					}
					
				}
				else if(chosenInt == 4) //delete existing group
				{
					System.out.print("Please enter a Group Name: ");
					
					thisGroup = scan.nextLine();
					
					if(groupClient.deleteGroup(thisGroup, token))
					{
						System.out.println("Group Deleted!");
					}
					else
					{
						System.out.println("Group deletion failed.");
					}
					
				}
				else if(chosenInt == 5) //add user to group
				{
					System.out.print("Please enter a Username: ");
					thisUser = scan.nextLine();
					
					System.out.print("Please enter a group: ");
					thisGroup = scan.nextLine();
					
					if(groupClient.addUserToGroup(thisUser, thisGroup, token))
					{
						System.out.println("User added successfully");
					}
					else
					{
						System.out.println("Adding the user failed.");
					}
				}
				else if(chosenInt == 6) //delete user from group
				{
					System.out.print("Please enter a Username: ");
					thisUser = scan.nextLine();
					
					System.out.print("Please enter a group: ");
					thisGroup = scan.nextLine();
					
					if(groupClient.deleteUserFromGroup(thisUser, thisGroup, token))
					{
						System.out.println("User removed successfully");
					}
					else
					{
						System.out.println("Removing the user failed.");
					}
				}
				else if(chosenInt == 7) //list members
				{
					System.out.print("Please enter the group's name: ");
					thisGroup = scan.nextLine();
					
					userList = groupClient.listMembers(thisGroup, token);
					
					if(userList != null)
					{
						if(!userList.isEmpty())
						{
							//System.out.println("userList size = " + userList.size());
							
							for(int i = 0; i < userList.size(); i++)
							{	
								//System.out.print("index(" + i + ") = ");
								System.out.println(userList.get(i));
							}
							
							/*
							for(String str: userList)
							{
								System.out.println(str);
							}
							*/
						}
					}
					else	
					{
						System.out.println("Cannot list members of this group.");
					}
				}
				else if(chosenInt == 8) //quit group client
				{
					System.out.println("Exiting Group Client...");
					quit = true;
					groupClient.disconnect();
				}
				else
				{
					System.out.println("Invalid Entry. Please enter a valid command.");
				}
			}
		}
		else
		{
			System.out.println("Error Connecting to the Group Server.");
		}
	}
	private Token verifyToken(byte[] token_final) throws SignatureException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, InvalidKeyException {
		boolean verified; 
		
		byte[]signature_from_token_final = new byte[64];
		for(int i = 0;i <64;i++){
			signature_from_token_final[i] = token_final[token_final.length-(64-i)];
		}
		String s_signature2 = Hex.toHexString(signature_from_token_final);
		System.out.println("S_signature is "+s_signature2);
		
		byte[]TOKEN_from_token_final = new byte[token_final.length-64];
		for(int i = 0; i < TOKEN_from_token_final.length;i++){
			TOKEN_from_token_final[i] = token_final[i];
		}
		String s_TOKEN2 = Hex.toHexString(TOKEN_from_token_final);
		System.out.println("TOKEN is "+s_TOKEN2);
		
			//need to verify that H[TOKEN_from_token_final] == sig.verify(signature_from_token_final) with public key
       
		byte[] publicbytes = Hex.decode(GS_publicKey); //generate public key bytes using string value of key
        X509EncodedKeySpec keySpecPublic = new X509EncodedKeySpec(publicbytes);
        KeyFactory keyFactoryPublic = KeyFactory.getInstance("RSA");
        PublicKey public_key_final = keyFactoryPublic.generatePublic(keySpecPublic); //generate public key from private key bytes        
        
        	//need to hash the TOKEN 
        
        MessageDigest final_TokenMessageDigest = MessageDigest.getInstance("SHA1", "BC");
		final_TokenMessageDigest.update(TOKEN_from_token_final);
		byte final_hashedTOKEN[] = final_TokenMessageDigest.digest();
		
			//Update with hashed token
		Signature signature2 = Signature.getInstance("SHA1withRSA", "BC");
        signature2.initVerify(public_key_final);//verify with public_key we just generated
	    signature2.update(final_hashedTOKEN); //update with first part of token_final
	    
	    	//verificiation		
	    verified = signature2.verify(signature_from_token_final);
	    System.out.println("Signature is verified: "+ verified);
	    
	    if(verified == true){
	    	Token token_to_return = new Token();
	    	token_to_return.tokify(TOKEN_from_token_final);
	    	return token_to_return;
	    }else{
	    	return null;
	    }
	}

}