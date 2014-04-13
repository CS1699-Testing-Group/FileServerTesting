/* File worker thread handles the business of uploading, downloading, and removing files for clients with valid tokens */

import java.lang.Thread;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.bouncycastle.util.encoders.Hex;

import java.security.*;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SealedObject;

public class FileThread extends Thread
{
	public String FS_publicKey = "305c300d06092a864886f70d0101010500034b00304802410097b35e02af768bf98da14984bbace13ed5f27bb765c9664e647a46c0dd1a540f56f627b4a9e86663cdab6ba3388ead5864f19b9f3f5eaba75313b2dd1a4e5b270203010001";
	public String FS_privateKey = "30820155020100300d06092a864886f70d01010105000482013f3082013b02010002410097b35e02af768bf98da14984bbace13ed5f27bb765c9664e647a46c0dd1a540f56f627b4a9e86663cdab6ba3388ead5864f19b9f3f5eaba75313b2dd1a4e5b27020301000102400fa9eea81a54044a054cc51996835852b3a6b10d93ce02e94f48aceb9728f3cae4596a0271594b6cd64aa617cbabd38cec130b25eb720266a51f4e27f4dd1329022100eb671964284b18a121c1ea02ba88d552a22f39547c552b962c1e9f51c08aa663022100a4f962612d12a9b8c54732c562d63e97458a89997709bba9d11f187d80fe616d022100c75c8e6d7b8e75f268d3806b052d4374f334095a9addcac728e05a4f7340393f02210081e7f675fade15536fd50ebfab82752afd118824963dcdce7ce3758f6e41e1490220605924a407c4ce7eb4defdad889d63fcbec3fef92540efc4704db6cf2efb00a1";
	PrivateKey private_key_final;
	SecretKey secretAESKey;
	SecretKey hash_secretAESKey;
	Key AESKey; //SSH Session Key
	Key hash_AESKey;
	IvParameterSpec iv;
	byte[] AESkeyByteArray;
	byte[] hash_AESkeyByteArray;
	FileClient fileClient = new FileClient();
	
	private final Socket socket;

	public FileThread(Socket _socket)
	{
		socket = _socket;
	}
	

	public void run()
	{
		Security.addProvider(new BouncyCastleProvider());
		boolean proceed = true;
		
		int challenge;
		//generate new challenge for new thread
		SecureRandom sRandom = new SecureRandom();
		challenge = sRandom.nextInt();
		
		try
		{
			System.out.println("*** New connection from " + socket.getInetAddress() + ":" + socket.getPort() + "***");
			final ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
			final ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
			Envelope response;

			fileClient.disconnect();
			//get private key from string for decryption
			byte[] privateBytes = Hex.decode(FS_privateKey); //generate public key bytes using string value of key
	        //X509EncodedKeySpec keySpecPrivate = new X509EncodedKeySpec(privateBytes);
	        KeyFactory keyFactoryPrivate = KeyFactory.getInstance("RSA");
	        private_key_final = keyFactoryPrivate.generatePrivate(new PKCS8EncodedKeySpec(privateBytes)); //generate private key from private key bytes 
			

			do
			{
				Envelope env = (Envelope)input.readObject();
				System.out.println("Request received: " + env.getMessage());
				Envelope e;
				if(env.getMessage().equals("ENCENV"))
				{
					e = decEnvelope(env);
				}
				else
				{
					e = env;
				}
					
				if(e.getMessage().equals("VERIFYADDRESS")){
					String address = (String)e.getObjContents().get(0);
					String port = (String)e.getObjContents().get(1);
					String test = socket.getLocalAddress().toString();
					int testPort = socket.getLocalPort();
					String testingPort = Integer.toString(testPort);
					if (test.equals(address) && testingPort.equals(port)){
						response = new Envelope("OK");
						output.writeObject(response);
					}else{
						response = new Envelope("BAD-ADDRESS");
						output.writeObject(response);
					}
				}
				// Handler to list files that this user is allowed to see
				if(e.getMessage().equals("LFILES"))
				{
					
					//send challenge request to proceed
					if(!challengeClient(input, output, challenge))
					{
						System.out.println("Challenge failed");
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
					
						if(e.getObjContents().size() < 1)
						{
							response = new Envelope("FAIL-BADCONTENTS");
							System.out.println("Bad contents");
						}
						else
						{
							if(e.getObjContents().get(0) == null) {
								response = new Envelope("FAIL-BADTOKEN");
								System.out.println("Bad token");
							}
							else
							{
							
								System.out.println("it worked, listing files");
								UserToken yourToken = (UserToken)e.getObjContents().get(0); //Extract token

							
								List<String> userFiles = new ArrayList<String>();
								List<ShareFile> Files = FileServer.fileList.getFiles();
								if (Files != null)
								{
									for (ShareFile f: Files)
									{
										if (yourToken.getGroups().contains(f.getGroup()))
										{
											userFiles.add(f.getPath() + "\t(" + f.getOwner() + "/" + f.getGroup() + ")");
										}
									}
								}

								response = new Envelope("OK"); //Success
								response.addObject(userFiles);
							}
						}
						System.out.println("Got the files, encrypting to new envelope");
						output.writeObject(encEnvelope(response));
					}
				}
				if(e.getMessage().equals("UPLOADF"))
				{
					
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
					

						if(e.getObjContents().size() < 3)
						{
							response = new Envelope("FAIL-BADCONTENTS");
						}
						else
						{
							if(e.getObjContents().get(0) == null) {
								response = new Envelope("FAIL-BADPATH");
							}
							if(e.getObjContents().get(1) == null) {
								response = new Envelope("FAIL-BADGROUP");
							}
							if(e.getObjContents().get(2) == null) {
								response = new Envelope("FAIL-BADTOKEN");
							}
							if(e.getObjContents().get(3) == null){
								response = new Envelope("NO KEY!");
							}
							else {
								String remotePath = (String)e.getObjContents().get(0);
								String group = (String)e.getObjContents().get(1);
								UserToken yourToken = (UserToken)e.getObjContents().get(2); //Extract token
								String keyVersion = (String)e.getObjContents().get(3);
								int int_keyVersion = Integer.parseInt(keyVersion); 
								
								if (FileServer.fileList.checkFile(remotePath)) {
									System.out.printf("Error: file already exists at %s\n", remotePath);
									response = new Envelope("FAIL-FILEEXISTS"); //Success
								}
								else if (!yourToken.getGroups().contains(group)) {
									System.out.printf("Error: user missing valid token for group %s\n", group);
									response = new Envelope("FAIL-UNAUTHORIZED"); //Success
								}
								else  {
									File file = new File("shared_files/"+remotePath.replace('/', '_'));
									file.createNewFile();
									FileOutputStream fos = new FileOutputStream(file);
									System.out.printf("Successfully created file %s\n", remotePath.replace('/', '_'));

									response = new Envelope("READY"); //Success
								
									output.writeObject(encEnvelope(response));
									e = decEnvelope((Envelope)input.readObject());
								
								
								
									while (e.getMessage().compareTo("CHUNK")==0) {
										fos.write((byte[])e.getObjContents().get(0), 0, (Integer)e.getObjContents().get(1));
										response = new Envelope("READY"); //Success
										output.writeObject(encEnvelope(response));
										e = decEnvelope((Envelope)input.readObject());
									}

									if(e.getMessage().compareTo("EOF")==0) {
										System.out.printf("Transfer successful file %s\n", remotePath);
										FileServer.fileList.addFile(yourToken.getSubject(), group, remotePath, int_keyVersion);
										response = new Envelope("OK"); //Success
									}	
									else {
										System.out.printf("Error reading file %s from client\n", remotePath);
										response = new Envelope("ERROR-TRANSFER"); //Success
									}
									fos.close();
								}
							}
						}

						output.writeObject(encEnvelope(response));
					}
				}
				else if (e.getMessage().compareTo("DOWNLOADF")==0) {
					int download_key_verison;
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
					

						String remotePath = (String)e.getObjContents().get(0);
						Token t = (Token)e.getObjContents().get(1);
						ShareFile sf = FileServer.fileList.getFile("/"+remotePath);						
						if (sf == null) {
							System.out.printf("Error: File %s doesn't exist\n", remotePath);
							e = new Envelope("ERROR_FILEMISSING");
							output.writeObject(encEnvelope(e));

						}
						else if (!t.getGroups().contains(sf.getGroup())){
							System.out.printf("Error user %s doesn't have permission\n", t.getSubject());
							e = new Envelope("ERROR_PERMISSION");
							output.writeObject(encEnvelope(e));
						}
						else {

							try
							{
								int fileKeyVersion = sf.getKeyVersion();
								String group = sf.getGroup();
								
								System.out.println("Key version used to decrypt was..."+fileKeyVersion);
								File f = new File("shared_files/_"+remotePath.replace('/', '_'));
								if (!f.exists()) {
									System.out.printf("Error file %s missing from disk\n", "_"+remotePath.replace('/', '_'));
									e = new Envelope("ERROR_NOTONDISK");
									output.writeObject(encEnvelope(e));

								}
								else {
									FileInputStream fis = new FileInputStream(f);

									do {
										byte[] buf = new byte[4096];
										if (e.getMessage().compareTo("DOWNLOADF")!=0) {
											System.out.printf("Server error: %s\n", e.getMessage());
											break;
										}
										e = new Envelope("CHUNK");
										int n = fis.read(buf); //can throw an IOException
										if (n > 0) {
											System.out.printf(".");
										} else if (n < 0) {
											System.out.println("Read error");

										}


										e.addObject(buf);
										e.addObject(new Integer(n));

										output.writeObject(encEnvelope(e));

										e = decEnvelope((Envelope)input.readObject());


									}
									while (fis.available()>0);

									//If server indicates success, return the member list
									if(e.getMessage().compareTo("DOWNLOADF")==0)
									{

										e = new Envelope("EOF");
										e.addObject(fileKeyVersion);
										e.addObject(group);
										output.writeObject(encEnvelope(e));
										
										e = decEnvelope((Envelope)input.readObject());
										if(e.getMessage().compareTo("OK")==0) {
											System.out.printf("File data upload successful\n");
										}
										else {

											System.out.printf("Upload failed: %s\n", e.getMessage());

										}

									}
									else {

										System.out.printf("Upload failed: %s\n", e.getMessage());

									}
									//added 3/14 4:10pm
									fis.close();
								}
						
							}
							catch(Exception e1)
							{
								System.err.println("Error: " + e.getMessage());
								e1.printStackTrace(System.err);

							}
							
							
							///NOW WE NEED TO DECRYPT THE FILE!
							
							
							
							
							
							
							
							
							
							///
						}
					}
				}
				else if (e.getMessage().compareTo("DELETEF")==0) {
					
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
					

						String remotePath = (String)e.getObjContents().get(0);
						Token t = (Token)e.getObjContents().get(1);
					
						System.out.println("Remote Path is "+remotePath);
						ShareFile sf = FileServer.fileList.getFile("/"+remotePath);
						if (sf == null) {
							System.out.printf("Error: File %s doesn't exist\n", remotePath);
							e = new Envelope("ERROR_DOESNTEXIST");
						}
						else if (!t.getGroups().contains(sf.getGroup())){
							System.out.printf("Error user %s doesn't have permission\n", t.getSubject());
							e = new Envelope("ERROR_PERMISSION");
						}
						else {

							try
							{


								File f = new File("shared_files/"+"_"+remotePath.replace('/', '_'));
								System.out.println("f is "+f.toString());
								if (!f.exists()) {
									System.out.printf("Error file %s missing from disk\n", "_"+remotePath.replace('/', '_'));
									e = new Envelope("ERROR_FILEMISSING");
								}
								else if (f.delete()) {
									System.out.printf("File %s deleted from disk\n", "_"+remotePath.replace('/', '_'));
									FileServer.fileList.removeFile("/"+remotePath);
									e = new Envelope("OK");
								}
								else {
									System.out.printf("Error deleting file %s from disk\n", "_"+remotePath.replace('/', '_'));
									e = new Envelope("ERROR_DELETE");
								}


							}
							catch(Exception e1)
							{
								System.err.println("Error: " + e1.getMessage());
								e1.printStackTrace(System.err);
								e = new Envelope(e1.getMessage());
							}
						}
						output.writeObject(encEnvelope(e));
					}
				}
				else if(e.getMessage().equals("DISCONNECT"))
				{
					socket.close();
					proceed = false;
				}
				//used to send pub key to user when first connecting to file server thread
				else if(e.getMessage().equals("GETPUBKEY"))	
				{
					response = new Envelope("OK");
					response.addObject(FS_publicKey);
					output.writeObject(response);
				}
				//used to send a new IV to user when first connecting to the file server thread
				else if(e.getMessage().equals("GETIV"))
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
				else if(e.getMessage().equals("GETENCAESKEY"))
				{
					byte[] AESencKeyByteArray = (byte[])e.getObjContents().get(0);
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
				else if(e.getMessage().equals("HASHGETENCAESKEY"))
	                {
	                	
	                	byte[] AESencKeyByteArray = (byte[])e.getObjContents().get(0);
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
				

			} while(proceed);
		}
		catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
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
		///
		
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
	
	private boolean challengeClient(ObjectInputStream in, ObjectOutputStream out, int challenge) throws IOException, ClassNotFoundException, NoSuchAlgorithmException, NoSuchProviderException
	{
		
		//System.out.println("Challenge is: " + challenge);
		
		Envelope message;
		Envelope response;
		Envelope decResponse;
		int receivedChallenge;
		
		message = new Envelope("CHALLENGE");
		message.addObject(challenge);
		out.writeObject(encEnvelope(message)); //send encrypted challenge to client
		response = (Envelope)in.readObject();
		
		if(response.getMessage().equals("ENCENV"))//if envelope is encrypted, decrypt
		{
			response = decEnvelope(response);
		}
		else
		{
			System.out.println("Response was not encrypted. Closing connection to prevent replay attacks.");
			return false;
		}
		
		receivedChallenge = (Integer)response.getObjContents().get(0);
		//System.out.println("Received Challenge + 1 is: " + receivedChallenge);
		
		if(challenge == Integer.MAX_VALUE) //check for rare case of max int being current challenge
		{
			if(receivedChallenge == Integer.MIN_VALUE)
			{
				//System.out.println("TEST MESSAGE: Challenge success!");
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
				//System.out.println("TEST MESSAGE: Challenge success!");
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
	
}


