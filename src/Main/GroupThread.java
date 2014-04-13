/* This thread does all the work. It communicates with the client through Envelopes.
 * 
 */
import java.lang.Thread;
import java.net.Socket;
import java.io.*;
import java.util.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec; 
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
///
public class GroupThread extends Thread 
{
	public static String GS_publicKey = "305c300d06092a864886f70d0101010500034b003048024100914754aec76a53097bb052be8154be1e493f803809b706daf3f09030047e05083e8c45181f45c6741b30232bff46b4d00868aeb9b8e30f6aaf2a3dfb248771d90203010001";
	public static String GS_privateKey = "30820154020100300d06092a864886f70d01010105000482013e3082013a020100024100914754aec76a53097bb052be8154be1e493f803809b706daf3f09030047e05083e8c45181f45c6741b30232bff46b4d00868aeb9b8e30f6aaf2a3dfb248771d9020301000102405e7402f1b38344438920655ba89861172dc654659aa35d98a33b0773c0a7f23aa404e18f1c9ce241d706803469b53034fdfb269f4f80b175241791a0380dead1022100c8531a9437672ef099f010484c6a88295adcf8d401ad9230c79c53a15b3b678d022100b9a7c294b0ff532abec703c919eb741dd90cb822b597bd7d73543a26e01bea7d0220637918c6a6a83f1fcc60efc4e6e5338dcd87d2ab7bd5d3b51339a631869afdf502206009e66060c753d072ec248b2d3b5dcfeaede77b1d1127d6f38808a4ff9db1490221009ec47cf98186c862af9c4a9124bf1b2102ef5e275d81493909b9214b5605542d";
	private final Socket socket;
	private GroupServer my_gs;
	PrivateKey private_key_final;
	SecretKey secretAESKey;
	SecretKey hash_secretAESKey;
	Key AESKey; //SSH Session Key
	Key hash_AESKey; //separate key for hashing
	IvParameterSpec iv;
	byte[] AESkeyByteArray;
	byte[] hash_AESkeyByteArray;
	
	public GroupThread(Socket _socket, GroupServer _gs)
	{
		socket = _socket;
		my_gs = _gs;
	}
	
	public void run()
	{
		boolean proceed = true;
		
		int challenge;
		//generate new challenge for new thread
		SecureRandom sRandom = new SecureRandom();
		challenge = sRandom.nextInt();
		
		try
		{
			Security.addProvider(new BouncyCastleProvider());
			//Announces connection and opens object streams
			System.out.println("*** New connection from " + socket.getInetAddress() + ":" + socket.getPort() + "***");
			final ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
			final ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
			
			byte[] privateBytes = Hex.decode(GS_privateKey); //generate priv key bytes using string value of key
	        KeyFactory keyFactoryPrivate = KeyFactory.getInstance("RSA");
	        private_key_final = keyFactoryPrivate.generatePrivate(new PKCS8EncodedKeySpec(privateBytes)); //generate private key from private key bytes 
			
			
			do
			{
				//Envelope message = (Envelope)input.readObject();
				Envelope response;
				
				//
				Envelope env = (Envelope)input.readObject();
				System.out.println("Request received: " + env.getMessage());
				Envelope message;
				if(env.getMessage().equals("ENCENV"))
				{
					message = decEnvelope(env);
				}
				else
				{
					message = env;
				}
					
				//
				
				if(message.getMessage().equals("GET")){//Client wants a token
					String username = (String)message.getObjContents().get(0); //Get the username
					String password = (String)message.getObjContents().get(1); // get password
					String timeStamp = (String)message.getObjContents().get(2); //timestamp
					String address = (String)message.getObjContents().get(3); //address
					String port = (String)message.getObjContents().get(4);

					if(username == null || password == null || address == null || port == null){
						response = new Envelope("FAIL");
						response.addObject(null);
						output.writeObject(response);
					}
					else{
						
						byte[]yourToken = createToken(username, password, timeStamp, address, port); //Create a token
						//Respond to the client. On error, the client will receive a null token						
						response = new Envelope("OK");
						response.addObject(yourToken);

						byte[] decodedUsername = Hex.decode(username);
						Cipher cipher = Cipher.getInstance("RSA", "BC");
						cipher.init(Cipher.DECRYPT_MODE, private_key_final);
						//first decode the timestamp and verify too much time has not passed
						byte[] decodedFinalUsername = cipher.doFinal(decodedUsername);
						
						String s_decodedFinalUsername = new String(decodedFinalUsername,"UTF8");
						//System.out.println(s_decodedFinalUsername + " is the decoded username");
						//System.out.println("User is in "+my_gs.userList.getUserGroups(s_decodedFinalUsername).size()+" groups");
						ArrayList<String> userKeys = new ArrayList<String>();
						for(int i = 0; i < my_gs.userList.getUserGroups(s_decodedFinalUsername).size();i++){
							String currentgroup = my_gs.userList.getUserGroups(s_decodedFinalUsername).get(i);
							System.out.println(username+"belongs to "+currentgroup);
							 
							for(int j = 0; j < my_gs.userList.groupKeyTable.get(currentgroup).size();j++){
								userKeys.add(my_gs.userList.groupKeyTable.get(currentgroup).get(j));
								//System.out.println("Adding key "+my_gs.userList.groupKeyTable.get(currentgroup).get(j));
							}
						}
						System.out.println("Just added all user groups to the array");
						response.addObject(userKeys);
						System.out.println("added to response");
						output.writeObject(response);
					}
				}
				else if(message.getMessage().equals("CUSER")){ //Client wants to create a user

					//send challenge request to proceed
					if(!challengeClient(input, output, challenge))
					{
						//challenge failed
						proceed = false;
						socket.close();
						
					}
					else //challenge success
					{
						//increment challenge 
						if(challenge == Integer.MAX_VALUE)
						{
							challenge = Integer.MIN_VALUE;
						}
						else
						{
							challenge += 1;
						}
						
						if(message.getObjContents().size() < 2){
							response = new Envelope("FAIL");
						}
						else{
							response = new Envelope("FAIL");
						
							if(message.getObjContents().get(0) != null){
								if(message.getObjContents().get(1) != null){ //expects 2 objects in envelope
									String username = (String)message.getObjContents().get(0); //Extract the username
									UserToken yourToken = (UserToken)message.getObjContents().get(1); //Extract the token
									// added password with temp for now
									String password = (String)message.getObjContents().get(2);
									if(createUser(username, yourToken, password)){
										response = new Envelope("OK"); //Success
									}
								}
							}
							output.writeObject(encEnvelope(response)); //
						}
					}
				}
				else if(message.getMessage().equals("DUSER")){//Client wants to delete a user
					
					//send challenge request to proceed
					if(!challengeClient(input, output, challenge))
					{
						//challenge failed
						proceed = false;
						socket.close();
						
					}
					else //challenge success
					{
					
						//increment challenge 
						if(challenge == Integer.MAX_VALUE)
						{
							challenge = Integer.MIN_VALUE;
						}
						else
						{
							challenge += 1;
						}
						
						if(message.getObjContents().size() < 2){
							response = new Envelope("FAIL");
						}
						else{
							response = new Envelope("FAIL");
						
							if(message.getObjContents().get(0) != null){
								if(message.getObjContents().get(1) != null){
									String username = (String)message.getObjContents().get(0); //Extract the username
									UserToken yourToken = (UserToken)message.getObjContents().get(1); //Extract the token
								
									if(deleteUser(username, yourToken)){
										response = new Envelope("OK"); //Success
									}
								}
							}
						}
						output.writeObject(encEnvelope(response));
					}
				}
			
				else if(message.getMessage().equals("CGROUP")) //Client wants to create a group
				{
					
					//increment challenge 
					if(challenge == Integer.MAX_VALUE)
					{
						challenge = Integer.MIN_VALUE;
					}
					else
					{
						challenge += 1;
					}
					
					//send challenge request to proceed
					if(!challengeClient(input, output, challenge))
					{
						//challenge failed
						proceed = false;
						socket.close();
						
					}
					else //challenge success
					{
					
						if(message.getObjContents().size() < 2)
						{
							response = new Envelope("FAIL");
						}
						else
						{
							response = new Envelope("FAIL");

							if(message.getObjContents().get(0) != null)
							{
								if(message.getObjContents().get(1) != null)
								{
									String groupAdded = (String)message.getObjContents().get(0); //Extract the group name
									UserToken yourToken = (UserToken)message.getObjContents().get(1); //Extract the token

									if(createGroup(groupAdded, yourToken))
									{
										response = new Envelope("OK"); //Success
									}
								}
							}
						}
						output.writeObject(encEnvelope(response));
					}
				}
				else if(message.getMessage().equals("DGROUP")){//Client wants to delete a group
					//send challenge request to proceed
					if(!challengeClient(input, output, challenge))
					{
						//challenge failed
						proceed = false;
						socket.close();
						
					}
					else //challenge success
					{
						
						//increment challenge 
						if(challenge == Integer.MAX_VALUE)
						{
							challenge = Integer.MIN_VALUE;
						}
						else
						{
							challenge += 1;
						}
						
						if(message.getObjContents().size() < 2){
							response = new Envelope("FAIL");
						}
						else{
							response = new Envelope("FAIL");

							if(message.getObjContents().get(0) != null){
								if(message.getObjContents().get(1) != null){
									String groupDeleted = (String)message.getObjContents().get(0); //Extract the group name
									UserToken yourToken = (UserToken)message.getObjContents().get(1); //Extract the token
								
									if(deleteGroup(groupDeleted, yourToken)){
										response = new Envelope("OK"); //Success
									}
								}
							}
						}
					output.writeObject(encEnvelope(response));
					}
				}
				else if(message.getMessage().equals("LMEMBERS")){//Client wants a list of members in a group
					
					//send challenge request to proceed
					if(!challengeClient(input, output, challenge))
					{
						//challenge failed
						proceed = false;
						socket.close();
						
					}
					else //challenge success
					{
					
						//increment challenge 
						if(challenge == Integer.MAX_VALUE)
						{
							challenge = Integer.MIN_VALUE;
						}
						else
						{
							challenge += 1;
						}
						
						if(message.getObjContents().size()<2){
							response = new Envelope("FAIL");
						}
						response = new Envelope("FAIL");
						if(message.getObjContents().get(0) != null){
							if(message.getObjContents().get(1) != null){
								String groupName = (String)message.getObjContents().get(0); //Get the groupName
								UserToken yourToken = (UserToken)message.getObjContents().get(1); //Extract the token
					
								ArrayList<String> members = getMembers(groupName, yourToken); // getMembers returns the members in a group
				
								if(!members.isEmpty()){
									response = new Envelope("OK");
									response.addObject(members);
								}
								output.writeObject(encEnvelope(response));
							}
						}
					}
					
				
				}
				else if(message.getMessage().equals("AUSERTOGROUP")){//Client wants to add user to a group
					
					//send challenge request to proceed
					if(!challengeClient(input, output, challenge))
					{
						//challenge failed
						proceed = false;
						socket.close();
						
					}
					else //challenge success
					{
					
						//increment challenge 
						if(challenge == Integer.MAX_VALUE)
						{
							challenge = Integer.MIN_VALUE;
						}
						else
						{
							challenge += 1;
						}
						
						if(message.getObjContents().size() < 3){
							response = new Envelope("FAIL");
						}
						else{
							response = new Envelope("FAIL");

							if(message.getObjContents().get(0) != null){
								if(message.getObjContents().get(1) != null){
									if(message.getObjContents().get(2) != null){
									
										String username = (String)message.getObjContents().get(0); //Extract the username
										String group = (String)message.getObjContents().get(1); //Extract the group name
										UserToken yourToken = (UserToken)message.getObjContents().get(2); //Extract the token

										if(addUserToGroup(username, group, yourToken)){
											response = new Envelope("OK"); //Success
										}
									}
								}
							}
						}
					output.writeObject(encEnvelope(response));
					}
				}
				else if(message.getMessage().equals("RUSERFROMGROUP")){//Client wants to remove user from a group
					
					//send challenge request to proceed
					if(!challengeClient(input, output, challenge))
					{
						//challenge failed
						proceed = false;
						socket.close();
						
					}
					else //challenge success
					{
				
						//increment challenge 
						if(challenge == Integer.MAX_VALUE)
						{
							challenge = Integer.MIN_VALUE;
						}
						else
						{
							challenge += 1;
						}
						
						if(message.getObjContents().size() < 3){
							response = new Envelope("FAIL");
						}
						else{
							response = new Envelope("FAIL");

							if(message.getObjContents().get(0) != null){
								if(message.getObjContents().get(1) != null){
									if(message.getObjContents().get(2) != null){
										String username = (String)message.getObjContents().get(0); //Extract the username
										String group = (String)message.getObjContents().get(1); //Extract the group name
										UserToken yourToken = (UserToken)message.getObjContents().get(2); //Extract the token

										if(removeUserFromGroup(username, group, yourToken)){
											response = new Envelope("OK"); //Success
										}
									
									}
								}
							}
						}
					output.writeObject(encEnvelope(response));
					}
				}
				
				
				else if(message.getMessage().equals("DISCONNECT")) //Client wants to disconnect
				{
					socket.close(); //Close the socket
					proceed = false; //End this communication loop
				}
				else if(message.getMessage().equals("GETPUBKEY"))	//used to send pub key to user when first connecting to group server
				{
					response = new Envelope("OK");
					response.addObject(GS_publicKey);
					output.writeObject(response);
				}
				//used to send a new IV to user when first connecting to the file server thread 
				else if(message.getMessage().equals("GETIV"))
				{
					SecureRandom sr = new SecureRandom();
					byte[] ivByteArray = new byte[16];
					sr.nextBytes(ivByteArray);
					
					iv = new IvParameterSpec(ivByteArray);
					String myIV = Hex.toHexString(ivByteArray);
					response = new Envelope("OK");
					System.out.println("Native IV: " + iv + "\n String IV: " + myIV );
					response.addObject(myIV); //send string representation of IV
					output.writeObject(response);
				}
						///////////////////////
						///// T7 Section //////
						///////////////////////
				else if(message.getMessage().equals("SIPADDRESS"))
				{
					//get ip address

					String ipAddress = (String)message.getObjContents().get(0);
					response = new Envelope("OK");
					output.writeObject(encEnvelope(response));
				}
						///////////////////////
						///// T7 Section //////
						///////////////////////
                else if(message.getMessage().equals("GETENCAESKEY"))
				{
					byte[] AESencKeyByteArray = (byte[])message.getObjContents().get(0);
					response = new Envelope("OK");
					output.writeObject(response);
					//Decrypt AES Key byte array
					byte[] text = null;
					Cipher decCipher = null;
					
					//Decrypt Encrypted AES Key Byte Array locally
					decCipher = Cipher.getInstance("RSA", "BC");
					decCipher.init(Cipher.DECRYPT_MODE, private_key_final);
					AESkeyByteArray = decCipher.doFinal(AESencKeyByteArray); //decrypt and store aes key byte array
					
					//convert AES Key byte array to AES SecretKey, then Key
					SecretKeySpec sks = new SecretKeySpec(AESkeyByteArray, "AES");
					secretAESKey = sks;
					AESKey = secretAESKey;
				}
                else if(message.getMessage().equals("HASHGETENCAESKEY"))
                {
                	
                	byte[] AESencKeyByteArray = (byte[])message.getObjContents().get(0);
					response = new Envelope("OK");
					output.writeObject(response);
					//Decrypt AES Key byte array
					byte[] text = null;
					Cipher decCipher = null;
					
					//Decrypt Encrypted AES Key Byte Array locally
					decCipher = Cipher.getInstance("RSA", "BC");
					decCipher.init(Cipher.DECRYPT_MODE, private_key_final);
					hash_AESkeyByteArray = decCipher.doFinal(AESencKeyByteArray); //decrypt and store aes key byte array
					
					//convert AES Key byte array to AES SecretKey, then Key
					SecretKeySpec sks = new SecretKeySpec(hash_AESkeyByteArray, "AES");
					hash_secretAESKey = sks;
                	hash_AESKey = hash_secretAESKey;
                }
				
				
				else
				{
					response = new Envelope("FAIL"); //Server does not understand client request
					output.writeObject(response);
				}
			}while(proceed);	
		}
		catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}
	

	private ArrayList<String> getMembers(String groupName, UserToken yourToken) {
		// RETURNS ARRAY OF MEMBERS IN GROUPNAME 
		String requester = yourToken.getSubject();
		
		if(my_gs.userList.checkUser(requester)){
			ArrayList<String> ownedGroups = my_gs.userList.getUserOwnership(yourToken.getSubject());
			ArrayList<String> members = new ArrayList<String>();
			if(ownedGroups.contains(groupName)){ //if token Subject is owner of the group
				members = my_gs.userList.getGroupMembers(groupName);
			}
			return members;
		}else{
			return null;
		}
	
	}

	//method to remove a user from a group
	private boolean removeUserFromGroup(String username, String groupname,UserToken yourToken) throws Exception {
		
		ArrayList<String> ownedGroups = my_gs.userList.getUserOwnership(yourToken.getSubject());
		String requester = yourToken.getSubject();
		if(my_gs.userList.checkUser(requester)){
			if(my_gs.userList.groupExists(groupname)){
				if(ownedGroups.contains(groupname)){ //if token SUBJECT is the owner of the GROUPNAME
					if(my_gs.userList.checkUser(username)){ //if user exists
						my_gs.userList.removeMemberFromGroup(username, groupname); //he can remove USERNAME from the GROUPNAME
						
						///SECURITY THREAT 6 FROM PART 4 CODE HERE
						String groupToDelete = groupname;
						my_gs.userList.removeMemberFromGroup(username, groupToDelete);//remove USERNAME, GROUPNAME 1, 2, 3, 4...
						ArrayList<String> currentKeyList = my_gs.userList.groupKeyTable.get(groupToDelete);
						System.out.println("GENERATING KEY FOR DELETED GROUP");
						Key key = GenerateSymmetricKey(128);
						byte[] IV = new byte[16];
						Random random = new SecureRandom();
						random.nextBytes(IV);
						byte[] key_bytes = key.getEncoded();
						System.out.print("Actual key: ");
						String s1 = (my_gs.userList.groupKeyTable.get(groupToDelete).size())+"_"+groupToDelete+"_"+Hex.toHexString(key_bytes)+"_"+Hex.toHexString(IV);
						
						System.out.println(s1);
						System.out.println("END GENERATING KEY");
						// KEY GENERATION COMPLETED
						
						
						//ADD KEY TO HASHTABLE
						System.out.println("ADDING KEY TO GROUP SERVER HASHTABLE(Deleted Member)");
						
						
						currentKeyList.add(s1);
						my_gs.userList.groupKeyTable.put(groupToDelete, currentKeyList);
						/////
						
						
						
						return true;
					}else{
						return false; //user doesn't exist
					}
				}else{
					return false;
				}
			}else{
				return false;// not an owner of the group
			}
		}else{
			return false;
		}
		
	}

	//Method to create tokens
private byte[] createToken(String username, String password, String timeStamp, String address, String port) {
	try{
		byte[] decodedUsername = Hex.decode(username);
		byte[] decodedPass = Hex.decode(password);
		byte[] decodedTimeStamp = Hex.decode(timeStamp);
		
		Cipher cipher = Cipher.getInstance("RSA", "BC");
		cipher.init(Cipher.DECRYPT_MODE, private_key_final);
		//first decode the timestamp and verify too much time has not passed
		byte[] decodedFinalTimeStamp = cipher.doFinal(decodedTimeStamp);
		
		String strTimeStamp = new String(decodedFinalTimeStamp, "UTF8");
		int intTimeStamp = Integer.parseInt(strTimeStamp); // time string to int
		int myMinutes = (int)TimeUnit.MINUTES.toMinutes(System.currentTimeMillis()); //server time
		boolean timeStampPass = false;
		
		//need to check to see if time stamp is within +- 5000000 increments (500 seconds or 8.33 minutes) from when it was sent
		//this gives a fair amount of time from client to server for the request.
		
		if(intTimeStamp >= 999500000 ) //edge case. Systime is near max and is about to loop
		{
			if((myMinutes >= (intTimeStamp - 500000)) || (myMinutes <= (500000 - (1000000000 - intTimeStamp))))
			{
				timeStampPass = true;
			}
		}
		else if(intTimeStamp <= 500000) //edge case. Systime is near min and has just looped
		{
			if(((myMinutes <= intTimeStamp  + 500000)) || myMinutes >= ((1000000000 - intTimeStamp) - 500000))
			{
				timeStampPass = true;
			}
		}
		else //standard case. Check to see if time is +- 500000 increments
		{
			if((myMinutes <= (intTimeStamp + 500000)) || myMinutes >= (intTimeStamp - 500000))
			{
				timeStampPass = true;
			}
		}
		//get curr time
		//compare to strTimeStamp
		
		
		
		if(timeStampPass == true)
		{
			System.out.println("Time Stamp is Valid");
			byte[] decodedFinalUsername = cipher.doFinal(decodedUsername);
			byte[] decodedFinalPass = cipher.doFinal(decodedPass);
		
			username = new String(decodedFinalUsername, "UTF8");
			password = new String(decodedFinalPass, "UTF8");
			//System.out.println("In final thread" + username + " " + password);
			if(my_gs.userList.checkUser(username)){//Check that user exists	
				byte test[] = my_gs.userList.returnHash(username);
				byte[] chePassword = new String(password).getBytes("UTF-8");
				Security.addProvider(new BouncyCastleProvider());
				MessageDigest messageDigest = MessageDigest.getInstance("SHA1", "BC");
				messageDigest.update(chePassword);
				byte hashedPass[] = messageDigest.digest();

				if (Arrays.equals(test, hashedPass)){
					//create new token object to give to the user
					//new token is a byte array that contains the token string || signature
					// TOKEN || {H[TOKEN]}Ks-1
					// to verify, check H[TOKEN] == {{H[TOKEN]}Ks-1}Ks
				
					//OLD: UserToken yourToken = new Token(my_gs.name, username, my_gs.userList.getUserGroups(username));
					Token token = new Token(my_gs.name, username, my_gs.userList.getUserGroups(username), address, port);
					//System.out.println("group size in token is "+ my_gs.userList.getUserGroups(username));
					String TOKEN = token.stringify();
				
					//System.out.println("Stringified token: "+TOKEN);//string representation of token object
					byte[] byte_TOKEN = token.byteify(TOKEN);
					String s_byte_TOKEN = Hex.toHexString(byte_TOKEN); //byte array of TOKEN
					//System.out.println("Byte array of token: "+s_byte_TOKEN);
				
					//GENERATE SIGNATURE BYTE ARRAY
				
					//STEP 1: HASH TOKEN, hashedToken
					MessageDigest TokenMessageDigest = MessageDigest.getInstance("SHA1", "BC");
					TokenMessageDigest.update(byte_TOKEN);
					byte hashedToken[] = TokenMessageDigest.digest();
					String s_hashed_TOKEN = Hex.toHexString(hashedToken);
					//System.out.println("Hashed byte array of token: "+s_hashed_TOKEN);
				
					//STEP 2: SIGN HASHED TOKEN with server's private key, sigBytes

					byte[] privatebytes = Hex.decode(my_gs.GS_privateKey); //generate private key bytes using string value of key
					PKCS8EncodedKeySpec keySpecPrivate = new PKCS8EncodedKeySpec(privatebytes);
					KeyFactory keyFactoryPrivate = KeyFactory.getInstance("RSA");
					PrivateKey private_key_final = keyFactoryPrivate.generatePrivate(keySpecPrivate); //generate private key from private key bytes
				
					Signature signature = Signature.getInstance("SHA1withRSA", "BC");
					signature.initSign(private_key_final, new SecureRandom());
					signature.update(hashedToken);
			    	byte[] sigBytes = signature.sign();
			    
			    	String s_signature = Hex.toHexString(sigBytes);
			    	// System.out.println("Signature byte array: "+s_signature);
			    	
			    	//STEP 3: CONCATENATE TOKEN WITH SIGNATURE
			    
			    	byte[] token_final = new byte[sigBytes.length+byte_TOKEN.length];
			    	for(int i = 0; i < byte_TOKEN.length; i++){
			    		token_final[i] = byte_TOKEN[i];
			    	}
			    	for(int j = byte_TOKEN.length, k = 0; j < token_final.length;j++,k++){
			    		token_final[j] = sigBytes[k];
			    	}
				
			    	String s_final_token = Hex.toHexString(token_final);
			    	//System.out.println("FINAL bytes of token. Should be byte_TOKEN||SigBytes: ");
			    	//System.out.println(s_final_token);
				
			    	return token_final;
				}
	
				else{
					return null;
				}
		
			}
		}
		else
		{
			System.out.println("Time Stamp Failed Authenticity Check");
			return null;
		}
	}catch (Exception e){
		e.printStackTrace(System.err);
	}
	
return null;

}
	
	//Method to create a user
	private boolean createUser(String username, UserToken yourToken, String password){
		String requester = yourToken.getSubject();
		
		//Check if requester exists
		if(my_gs.userList.checkUser(requester)){
			//Get the user's groups
			ArrayList<String> temp = my_gs.userList.getUserGroups(requester);
			//requester needs to be an administrator
			if(temp.contains("ADMIN")){
				//Does user already exist?
				if(my_gs.userList.checkUser(username)){
					return false; //User already exists
				}
				else{
					// added password section
					my_gs.userList.addUser(username, password);
					return true;
				}
			}
			else{
				return false; //requester not an administrator
			}
		}
		else{
			return false; //requester does not exist
		}
	}
	
	//Method to add a user to a group
	private boolean addUserToGroup(String username, String groupname, UserToken yourToken){
		String requester = yourToken.getSubject();

		//Check if requester exists
		if(my_gs.userList.checkUser(requester)){
			System.out.println("Requestor is a user");
			if(my_gs.userList.checkUser(username)){
				System.out.println("user is in the userlist");
			// Check for ownership
				if (my_gs.userList.getUserOwnership(requester).contains(groupname)){
					System.out.println("Requestor is the owner of the group");
					// Is user already in the group?
					if (!my_gs.userList.getUserGroups(username).contains(groupname)){
						System.out.println("User isn't already in the group");
						my_gs.userList.addGroup(username, groupname);
						return true;
					}else{
						System.out.println("user is already in the group");
						return false; //user is already part of the group
					}
				}else{
					System.out.println("Requestor doesn't own group");
					return false; //requestor doesn't own the group
				}
			}else{
				System.out.println("User is not in the userlist");
				return false; //user doesn't exist
			}
		}else{
			return false; //requestor doesn't exist
		}
	}
	
	//Method to create a group
	private boolean createGroup(String group,UserToken yourToken) throws Exception{
		
		String requester = yourToken.getSubject();
		
		//Check if requester exists
				if(my_gs.userList.checkUser(requester)){
					if(!my_gs.userList.groupExists(group)){
						my_gs.userList.addOwnership(requester, group);
						my_gs.userList.addGroup(requester, group);
						my_gs.userList.createGroup(requester,group);
						//// THREAT 6 OF PART 4 HERE
						System.out.println("GENERATING KEY FOR NEW GROUP");
						Key key = GenerateSymmetricKey(128);
						byte[] IV = new byte[16];
						Random random = new SecureRandom();
						random.nextBytes(IV);
						byte[] key_bytes = key.getEncoded();
						System.out.print("Actual key: ");
						String s1 = "0_"+group+"_"+Hex.toHexString(key_bytes)+"_"+Hex.toHexString(IV);
						
						System.out.println(s1);
						System.out.println("END GENERATING KEY");
						// KEY GENERATION COMPLETED
						
						
						//ADD KEY TO HASHTABLE
						System.out.println("ADDING KEY TO GROUP SERVER HASHTABLE");
						
						ArrayList<String> newList = new ArrayList<String>();
						newList.add(s1);
						my_gs.userList.groupKeyTable.put(group, newList);
						
						////
						return true;
					}
					return false;
				}else{
					return false;
				}
		
		
	}
	
	//Method to delete a user
	private boolean deleteUser(String username, UserToken yourToken) throws Exception{
		String requester = yourToken.getSubject();
		
		//Does requester exist?
		if(my_gs.userList.checkUser(requester)){
			ArrayList<String> temp = my_gs.userList.getUserGroups(requester);

			//requester needs to be an administer			
			if(temp.contains("ADMIN")){
				
				//Does user exist?
				if(my_gs.userList.checkUser(username)){
					//User needs deleted from the groups they belong
					ArrayList<String> deleteFromGroups = new ArrayList<String>();
					
					//This will produce a hard copy of the list of groups this user belongs
					for(int index = 0; index < my_gs.userList.getUserGroups(username).size(); index++){
						deleteFromGroups.add(my_gs.userList.getUserGroups(username).get(index));//adds all user groups to array
					}
					
					//Delete the user from the groups
					//If user is the owner, removeMember will automatically delete group!
					for(int index = 0; index < deleteFromGroups.size(); index++){
						String groupToDelete = deleteFromGroups.get(index);
						my_gs.userList.removeMemberFromGroup(username, groupToDelete);//remove USERNAME, GROUPNAME 1, 2, 3, 4...
						ArrayList<String> currentKeyList = my_gs.userList.groupKeyTable.get(groupToDelete);
						System.out.println("GENERATING KEY FOR DELETED GROUP");
						Key key = GenerateSymmetricKey(128);
						byte[] IV = new byte[16];
						Random random = new SecureRandom();
						random.nextBytes(IV);
						byte[] key_bytes = key.getEncoded();
						System.out.print("Actual key: ");
						String s1 = (my_gs.userList.groupKeyTable.get(groupToDelete).size())+"_"+groupToDelete+"_"+Hex.toHexString(key_bytes)+"_"+Hex.toHexString(IV);
						
						System.out.println(s1);
						System.out.println("END GENERATING KEY");
						// KEY GENERATION COMPLETED
						
						
						//ADD KEY TO HASHTABLE
						System.out.println("ADDING KEY TO GROUP SERVER HASHTABLE(Deleted Member)");
						
						
						currentKeyList.add(s1);
						my_gs.userList.groupKeyTable.put(groupToDelete, currentKeyList);
						//NEED TO COMPUTE NEW KEYS FOR ALL OF THESE GROUPS
					
					
					}
					
					//If groups are owned, they must be deleted
					ArrayList<String> deleteOwnedGroup = new ArrayList<String>();
					
					//Make a hard copy of the user's ownership list
					for(int index = 0; index < my_gs.userList.getUserOwnership(username).size(); index++){
						deleteOwnedGroup.add(my_gs.userList.getUserOwnership(username).get(index));
					}
					
					//Delete owned groups
					for(int index = 0; index < deleteOwnedGroup.size(); index++){
						//Use the delete group method. Token must be created for this action
						deleteGroup(deleteOwnedGroup.get(index), new Token(my_gs.name, username, deleteOwnedGroup, null, null));
					}
					//Delete the user from the user list
					my_gs.userList.deleteUser(username);
					return true;	
				}
				else{
					return false; //User does not exist	
				}
			}
			else{
				return false; //requester is not an administer
			}
		}
		else{
			return false; //requester does not exist
		}
	}
	
	//Method to delete a group
	private boolean deleteGroup(String GroupToDelete, UserToken token) {
		// CODE FOR DELETING A WHOLE GROUP IF IA USER WHO OWNED A GROUP IS DELETED
		String requester = token.getSubject();
		
		if(my_gs.userList.checkUser(requester)){
			ArrayList<String> ownedGroups = my_gs.userList.getUserOwnership(token.getSubject());
			if(ownedGroups.contains(GroupToDelete)){
				my_gs.userList.deleteGroup(GroupToDelete);
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
		
	}
	
	//encrypt envelope
	private Envelope encEnvelope(Envelope message)
	{
		System.out.println("in encEnvelope in File Thread!");
		try{
			Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
			c.init(Cipher.ENCRYPT_MODE, AESKey, iv);
			SealedObject so = new SealedObject(message, c);
			Envelope encMsg = new Envelope("ENCENV");
			//System.out.println("About to cast so to encMsg, returning");//test
			encMsg.addObject(so);
			//System.out.println("Just cast so to encMsg, returning");//test
			
			//HASH OF SEALED OBJECT TO byte_hash_SealedObject
			MessageDigest hashedSealedObject = MessageDigest.getInstance("SHA1", "BC");
			byte[] byte_SealedObject = toByteArray(so);
			hashedSealedObject.update(byte_SealedObject);
			byte byte_hash_SealedObject[] = hashedSealedObject.digest();
			String s_byte_hash_SealedObject = Hex.toHexString(byte_hash_SealedObject);
			System.out.println("Hashed byte array of token: "+s_byte_hash_SealedObject);
			///
			
			encMsg.addObject(byte_hash_SealedObject);
			
			return encMsg;
		}catch(Exception e){
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
		return null;
	}
	//decrypt envelope
	private Envelope decEnvelope(Envelope message) throws IOException, NoSuchAlgorithmException, NoSuchProviderException
	{
		System.out.println("In decenvelope in file thread!");
		SealedObject so = (SealedObject)message.getObjContents().get(0);
		
		if(message.getObjContents().size()> 1){
			byte[] byte_hash_SealedObject = (byte[]) message.getObjContents().get(1);
		
			///HASH so AND COMPARE TO byte_hash_SealedObject
			byte[] so_bytes = toByteArray(so);
			MessageDigest hashedSealedObject = MessageDigest.getInstance("SHA1", "BC");
			hashedSealedObject.update(so_bytes);
			byte[] byte_hash_so_bytes = hashedSealedObject.digest();
		
			if(Arrays.equals(byte_hash_so_bytes,byte_hash_SealedObject)){
			
			}else{
				System.out.println("Bad envelope....");
				System.out.println("byte_hash_SealedObject is "+Hex.toHexString(byte_hash_SealedObject)+ " byte_hash_so_bytes is "+Hex.toHexString(byte_hash_so_bytes));
				Envelope badvelope = new Envelope("Someone Tampered With This");
				return badvelope;
			}
		}
		
		try{
			String algorithm = so.getAlgorithm();
			Cipher c = Cipher.getInstance(algorithm);
			c.init(Cipher.DECRYPT_MODE, AESKey,iv);
			return (Envelope)so.getObject(c);
			
		}catch(Exception e){
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
		return null;
		
	}
	//Used to issue challenge when a user makes a request.
	private boolean challengeClient(ObjectInputStream in, ObjectOutputStream out, int challenge) throws IOException, ClassNotFoundException, NoSuchAlgorithmException, NoSuchProviderException
	{

		//System.out.println("Challenge is: " + challenge);
		
		Envelope myMessage = null;
		Envelope myResponse = null;
		Envelope decMyResponse = null;
		int receivedChallenge;
		
		myMessage = new Envelope("CHALLENGE");
		myMessage.addObject(challenge);
		
		out.writeObject(encEnvelope(myMessage)); //send encrypted challenge to client
		
		myResponse = (Envelope)in.readObject(); //expect challenge + 1 from client
		
		if(myResponse.getMessage().equals("ENCENV"))//if envelope is encrypted, decrypt
		{
			decMyResponse = decEnvelope(myResponse);
		}
		else //should never receive plaintext challenge without shutting down
		{
			System.out.println("Response was not encrypted. Closing connection to prevent replay attacks.");
			return false;
		}
		
		receivedChallenge = (Integer)decMyResponse.getObjContents().get(0); //0th object should be challenge int received from server
		
		//System.out.println("Received Challenge + 1 is: " + receivedChallenge);
		
		if(challenge == Integer.MAX_VALUE) //check for rare case of max int being current challenge
		{
			if(receivedChallenge == Integer.MIN_VALUE)
			{
				return true;
			}
			else
			{
				System.out.println("Challenge Failed");
				return false;
			}
		}
		else //standard challenge
		{
			if(receivedChallenge == (challenge + 1))
			{
				return true;
			}
			else
			{
				System.out.println("Challenge Failed.");
				return false;
			}
		}
		
		//return false;
	}
	public byte[] toByteArray(SealedObject so) throws IOException{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		byte[]serialized;
		try {
		  out = new ObjectOutputStream(bos);   
		  out.writeObject(so);
		  serialized = bos.toByteArray();
		} finally {
		  try {
		    if (out != null) {
		      out.close();
		    }
		  } catch (IOException ex) {
		    // ignore close exception
		  }
		  try {
		    bos.close();
		  } catch (IOException ex) {
		    // ignore close exception
		  }
		}
		return serialized;
		
	}
	public SealedObject toSealedObject(byte[]serialized) throws IOException, ClassNotFoundException{
		ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
		ObjectInput in = null;
		SealedObject so;
		
		try {
		  in = new ObjectInputStream(bis);
		  so = (SealedObject)in.readObject(); 
		} finally {
		  try {
		    bis.close();
		  } catch (IOException ex) {
		    // ignore close exception
		  }
		  try {
		    if (in != null) {
		      in.close();
		    }
		  } catch (IOException ex) {
		    // ignore close exception
		  }
		}
		return so;
	}
	public static SecretKey GenerateSymmetricKey(int keySizeInBits)throws Exception {
		KeyGenerator keyGenerator = KeyGenerator.getInstance("AES", "BC");
		SecretKey secretkey = keyGenerator.generateKey();
		return secretkey;
	}
	
}
