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

import javax.crypto.SealedObject;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;

import java.security.*;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;


public class FileClientSubInterface
 {
	public String GS_publicKey = "305c300d06092a864886f70d0101010500034b003048024100914754aec76a53097bb052be8154be1e493f803809b706daf3f09030047e05083e8c45181f45c6741b30232bff46b4d00868aeb9b8e30f6aaf2a3dfb248771d90203010001";
	FileClient fileClient = new FileClient();
	boolean trusted = false;

	public void startFileInterface(String server, int port, byte[] byte_token) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException
	{

		if(fileClient.connect(server, port))
		{

			Security.addProvider(new BouncyCastleProvider());

			Token token = verifyToken(byte_token); //returns a re-made token if verified, returns null if invalid token

			
			
			if (fileClient.verifyAddress(token.getIPAddress(), token.getPort())){
				System.out.println("IP ADDRESS & Port is VERFIED...Continuing...");

			}else{
				System.out.println("IP ADDRESS/Port IS NOT VERIFIED! Ending...");
				fileClient.disconnect();
				System.exit(0);
			}


			Scanner scan = new Scanner(System.in);
			
			boolean quit = false;
			boolean pubKeyShown = false;
			String chosenStr;
			int chosenInt;
			
			List<String> userList;
			
			String sourceFile = "";
			String destFile = "";
			String groupName = "";
			
			String FS_publicKey = "";
			IvParameterSpec iv;
			PublicKey public_key_final;
			Key AESkey = null;
			
			System.out.println("Welcome to the File client!");
			
			while(!quit)
			{
				
				if(trusted == false)	//test to see if key has been printed yet
				{
					if(fileClient.requestPublicKey(token) != null)//get key from FileThread
					{
						FS_publicKey = fileClient.requestPublicKey(token);
						System.out.println("\nThis File Server's Public Key is: " + FS_publicKey + "\n");
						System.out.println("\nWould you like to trust this server?");
						System.out.println("Enter 'Yes' or 'No'");
						Scanner scanner = new Scanner(System.in);
						String trust = scanner.nextLine();
							if (trust.equals("Yes")){
								trusted = true;
								System.out.println("Adding to list of trusted servers...");
							}else if(trust.equals("No")){
									System.out.println("Exiting...");
									System.exit(0);
							}else{
								System.out.println("Invalid input...");
								System.exit(0);
							}
						
						String myIV = fileClient.requestIV(token); ////get string version of IV from server
						byte[] myIVbytes = myIV.getBytes(); //get bytes from sent iv string
						iv = new IvParameterSpec(myIVbytes); //get new iv from the byte array
						
			
						//send pub key encrypted aes key to server
						if(fileClient.sendEncAESKey())
						{
							System.out.println("\nEncrypted AES Key send to server successfully\nSSH may now begin\n");
						}
						else
						{
							System.out.println("ERROR: Key not received successfully");
						}
					//end aes creation/sending
						if(fileClient.HASH_sendEncAESKey())
						{
							System.out.println("\nEncrypted AES Key for HASH sent to server successfully");
						}
						else
						{
							System.out.println("ERROR: HASH Key not received successfully");
						}
						
						
						//pubKeyShown = true;
					}
					else	//ERROR - Key does not exist on file server.
					{
						System.out.println("ERROR - File Server's Public Key is INVALID");
						fileClient.disconnect();
						
					}
				}else{
					System.out.println("The public key from the group server has already been verified");
					System.out.println("Continuing...");
				}
				
				System.out.println("Please select a number corresponding to the following options...");
				System.out.println("------File Options------");
				System.out.println("\t1: List files you have access to");
				System.out.println("\t2: Upload a file");
				System.out.println("\t3: Download a file");
				System.out.println("\t4: Delete a file");
				System.out.println("------Other Options------");
				System.out.println("\t5: Quit the File Server");
				
				System.out.print("Enter a number: ");
				
				chosenStr = scan.nextLine();
				chosenInt = Integer.parseInt(chosenStr);
				
				if(chosenInt == 1) //list files user has access to
				{
					userList = fileClient.listFiles(token);
					if(userList != null)
					{
						if(!userList.isEmpty())
						{
							for(int i=0; i < userList.size(); i++)
							{
								System.out.println(userList.get(i));
							}
						}
					}
					else
					{
						System.out.println("You seem to have no files available.");
					}
					
				}
				else if(chosenInt == 2) //upload file
				{
					System.out.print("Please enter the name of the file to be uploaded: ");
					sourceFile = scan.nextLine();
					System.out.print("Please enter what the file is to be called at the destination: ");
					destFile = scan.nextLine();
					System.out.println("Please enter the Group Name you wish to share the file with: ");
					groupName = scan.nextLine();
					
					if(fileClient.upload(sourceFile, destFile, groupName, token))
					{
						System.out.println("Upload request accepted!");
					}
					else
					{
						System.out.println("Upload request failed.");
					}
					
				}
				else if(chosenInt == 3)//download file
				{
					System.out.println("Please enter the name of the file to download: ");
					sourceFile = scan.nextLine();
					System.out.println("Please enter what the file is to be called at the destination: ");
					destFile = scan.nextLine();
					
					if(fileClient.download(sourceFile, destFile, token))
					{
						System.out.println("Download request accepted!");
					}
					else
					{
						System.out.println("Download request failed.");
					}
				}
				else if(chosenInt == 4)//delete file
				{
					System.out.println("Please enter the name of the file to delete: ");
					sourceFile = scan.nextLine();
					
					if(fileClient.delete(sourceFile, token))
					{
						System.out.println("Delete request accepted!");
					}
					else
					{
						System.out.println("Delete request failed.");
					}
				}
				else if(chosenInt == 5)//quit
				{
					System.out.println("Exiting File Client");
					quit = true;
					fileClient.disconnect();
				}
				else
				{
					System.out.println("Invalid Entry. Please enter a valid command.");
				}
			}
		}
		else
		{
			System.out.println("Error Connecting to the File Server.");
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


