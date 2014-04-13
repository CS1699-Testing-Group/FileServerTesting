/* FileClient provides all the client functionality regarding the file server */

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
 
import java.security.PublicKey;
import java.security.Key;

import javax.crypto.SealedObject;

import java.security.*;


public class FileClient extends Client implements FileClientInterface {

	PublicKey public_key_final;
	IvParameterSpec iv;
	Key AESkey;
	Key hash_AESkey;
	public String groupname;
	public int fileKeyVersion;
	//copied from file server
	//used to send Encrypted AES Key to File Thread from user
    public Boolean sendEncAESKey(byte[] encAESKey){
        Envelope getEncAESKey = null, env = null;
        getEncAESKey = new Envelope("GETENCAESKEY");
        getEncAESKey.addObject(encAESKey);
         
        try{
            output.writeObject(getEncAESKey);
            env = (Envelope)input.readObject();
            if(env.getMessage().equals("OK"))
            {
                return(true);
            }
        }catch(Exception e){
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
        return false;
    }

    public Boolean verifyAddress(String address, String port){
    	Envelope send = null, env = null;
    	send = new Envelope("VERIFYADDRESS");
    	send.addObject(address);
    	send.addObject(port);

    	try{
            output.writeObject(send);
			env = (Envelope)input.readObject();
            if (env.getMessage().equals("OK")){
            	return true;
            }else{
            	return false;
            }
        }catch(Exception e){
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
        

    }
	//Used to request IV from server thread
    public String requestIV(){
        Envelope getIV = null, env = null;
        getIV = new Envelope("GETIV");
        
         
        try{
            output.writeObject(getIV);
             
            env = (Envelope)input.readObject();
             
            if(env.getMessage().equals("OK"))
            {
                return (String)env.getObjContents().get(0);
            }
        }catch(Exception e){
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
        return null;
    }


	//Used first to request public key from group server thread
	public String requestPublicKey(){
		try{
		Envelope getPubKey = new Envelope("GETPUBKEY");
		Envelope env = null;
		String token = "test";
		
		getPubKey.addObject(token);
		
		
		output.writeObject(getPubKey);
		 
		env = (Envelope)input.readObject();
		
		if(env.getMessage().equals("OK"))
		{ 
			return (String)env.getObjContents().get(0); //This cast creates compiler warnings. Sorry.
		}
		}catch(Exception e){
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
		return null;
		
	}
    
    
    //end copied from file server
	
	public boolean delete(String filename, UserToken token) {
		String remotePath;
		if (filename.charAt(0)=='/') {
			remotePath = filename.substring(1);
		}
		else {
			remotePath = filename;
		}
		Envelope env = new Envelope("DELETEF"); //Success
	    env.addObject(remotePath);
	    env.addObject(token);
	    try {
			output.writeObject(encEnvelope(env));
			
			challengeAccepted((Envelope)input.readObject());
			
		    env = (Envelope)input.readObject();
		    if(env.getMessage().equals("ENCENV"))
		    {
		    	env = decEnvelope(env);
		    }
		    
		    //env = encMessage(env);
			
			if (env.getMessage().compareTo("OK")==0) {
				System.out.printf("File %s deleted successfully\n", filename);				
			}
			else {
				System.out.printf("Error deleting file %s (%s)\n", filename, env.getMessage());
				return false;
			}			
		} catch (Exception e) {
			e.printStackTrace();
		}	
		return true;
	}

	public boolean download(String sourceFile, String destFile, UserToken token) {
				
				if (sourceFile.charAt(0)=='/') {
					sourceFile = sourceFile.substring(1);
				}
		
				File file = new File("downloadingfile.txt");
			    try {
			    				
				
				    if (!file.exists()) {
				    	file.createNewFile();
					    FileOutputStream fos = new FileOutputStream(file);
					    
					    Envelope env = new Envelope("DOWNLOADF"); //Success
					    env.addObject(sourceFile);
					    env.addObject(token);
					    output.writeObject(encEnvelope(env));
					    
					    challengeAccepted((Envelope)input.readObject());
						
					    env = (Envelope)input.readObject();
					    if(env.getMessage().equals("ENCENV"))
					    {
					    	env = decEnvelope(env);
					    }
					    //env = encMessage(env);
					    
						while (env.getMessage().compareTo("CHUNK")==0) { 
								fos.write((byte[])env.getObjContents().get(0), 0, (Integer)env.getObjContents().get(1));
								System.out.printf(".");
								env = new Envelope("DOWNLOADF"); //Success
								env = encMessage(env);
								//output.writeObject(env);
								//env = (Envelope)input.readObject();									
						}										
						fos.close();
						
					    if(env.getMessage().compareTo("EOF")==0) {
					    		fileKeyVersion = (Integer) env.getObjContents().get(0);
					    		groupname = (String)env.getObjContents().get(1);
					    		//System.out.println("KEY IS OF VERISON "+fileKeyVersion);
					    		//System.out.println("GROUP USED TO ENCRYP WAS "+groupname);
								//System.out.println("END OF FILE!!!");
								fos.close();
								System.out.printf("\nTransfer successful file %s\n", sourceFile);
								env = new Envelope("OK"); //Success
								//output.writeObject(env);
								try{
									Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
									cipher.init(Cipher.ENCRYPT_MODE, AESkey,iv);
									SealedObject so = new SealedObject(env, cipher);
									Envelope outMsg = new Envelope("ENCENV");
									Envelope response = null;
									outMsg.addObject(so);
									output.writeObject(outMsg);
								}catch(Exception e){
									System.out.println("ERROR: " + e.getMessage());
									e.printStackTrace(System.err);
									return false;
								}
								
						}
						else {
								System.out.printf("Error reading file %s (%s)\n", sourceFile, env.getMessage());
								file.delete();
								return false;								
						}
				    }    
					 
				    else {
						System.out.printf("Error couldn't create file %s\n", destFile);
						return false;
				    }
								
			
			    } catch (IOException e1) {
			    	
			    	System.out.printf("Error couldn't create file %s\n", destFile);
			    	return false;
			    
					
				}
			    catch (Exception e) {
					e.printStackTrace();
				}
			    
			   try{ 
			    //System.out.println("I just downloaded an encrypted file.");
				//System.out.println("READING THE KEY I NEED FROM THE FILE ON THE USER SIDE");
				String fileserver_groupname = groupname;//GROUP NAME
				int fileserver_keynumber = fileKeyVersion;//DECRYPTION KEY NUMBER
				String fileserver_correctKey = "";
				String fileserver_correctIV = "";
				boolean fileserver_found_correctKey = false;
				
				BufferedReader READin = new BufferedReader(new FileReader("GroupKeys.txt"));
				String READline;
				while((READline = READin.readLine())!=null){ //read file line by line and split by underscore
					String lineArray[] = READline.split("_");
					int line_keyNumber = Integer.parseInt(lineArray[0]);
					String line_groupname = lineArray[1];
					
					if(line_groupname.equals(fileserver_groupname) && line_keyNumber == fileserver_keynumber){ //IF KEY MATCHES PARAMETERS
						fileserver_found_correctKey = true;
						fileserver_correctKey = lineArray[2];
						fileserver_correctIV = lineArray[3];
					}
				}
				if(fileserver_found_correctKey == true){
					//System.out.print("Found the needed key: ");
					//System.out.println(fileserver_correctKey); //WE CAN DECRYPT
				}else{
					System.out.println("You don't have the key to decrypt this file, please obtain a new token");//ERROR OUT
					return false;
					
				}
				
				//NEED TO GENERATE THE AES KEY FROM THE STRING IN THE FILE
				byte[] DECRYPTaesBytes = Hex.decode(fileserver_correctKey); //generate public key bytes using string value of key
				byte[] DECRYPTIVBytes = Hex.decode(fileserver_correctIV);
				SecretKeySpec DECRYPTkey_from_file = new SecretKeySpec(DECRYPTaesBytes, "AES");       
				
				//DECRYPT FILE CONTENTS WITH key_from_file
				Cipher DECRYPTcipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
				System.out.println("Starting decryption..");
				DECRYPTcipher.init(Cipher.DECRYPT_MODE, DECRYPTkey_from_file, new IvParameterSpec(DECRYPTIVBytes)); //encrypt using key
				
				File toDecrypt = new File("downloadingfile.txt");
				FileInputStream toDecryptfis = new FileInputStream(toDecrypt);
				
				File Decryptednewfile = new File(destFile);
				Decryptednewfile.createNewFile();
				FileOutputStream Decryptedfos = new FileOutputStream(Decryptednewfile);
				
				CBCDecrypt(DECRYPTcipher,toDecryptfis,Decryptedfos);
				
				System.out.print("Finished Decryption");
				//DECRYPTED FILE IS DECRYPTED.TXT
	   } catch (IOException e1) {
	    	
	    	System.out.printf("Error couldn't create file %s\n", destFile);
	    	return false;
	    
			
		}
	    catch (Exception e) {
			e.printStackTrace();
		}
				 return true;
	}

	@SuppressWarnings("unchecked")
	public List<String> listFiles(UserToken token) {
		 try
		 {
			 Envelope message = null, e = null;
			 //Tell the server to return the member list
			 message = new Envelope("LFILES");
			 message.addObject(token); //Add requester's token
			 output.writeObject((encEnvelope(message)));
			 
				
			challengeAccepted((Envelope)input.readObject());
			 
			e = (Envelope)input.readObject();
			if(e.getMessage().equals("ENCENV"))
			{
				e = decEnvelope(e);
			}
			 
			 //e = encMessage(message);
			 
			 
			 //If server indicates success, return the member list
			 if(e.getMessage().equals("OK"))
			 { 
				return (List<String>)e.getObjContents().get(0); //This cast creates compiler warnings. Sorry.
			 }
				
			 return null;
			 
		 }
		 catch(Exception e)
			{
				System.err.println("Error: " + e.getMessage());
				e.printStackTrace(System.err);
				return null;
			}
	}

	public boolean upload(String sourceFile, String destFile, String group,UserToken token) {
		Security.addProvider(new BouncyCastleProvider());

		if (destFile.charAt(0)!='/') {
			 destFile = "/" + destFile;
		 }
		//NEED TO ENCRYPT FILE BEFORE UPLOADING IT
		//ENCRYPTION ON UPLOAD - USE KEY NUMBER AND GROUPNAME BASED ON MOST RECENT KEY
		System.out.println("I want to upload this file, it needs to be encrypted.");
		System.out.println("READING MOST RECENT KEY I NEED FROM GROUPKEYS.TXT ON THE USER SIDE");
		String toUpload_groupname = group;//FILE FLAGGED FOR THIS GROUP
		String toUpload_correctKey = "";
		String toUpload_correctIV = "";
		String s_toUpload_lineKeyNumber = "";
		boolean toUpload_found_correctKey = false;
		
		try{
		BufferedReader in = new BufferedReader(new FileReader("GroupKeys.txt"));
		String line;
		int maxKeyNumber = -1;
			
		while((line = in.readLine())!=null){ //read file line by line and split by underscore
			String lineArray[] = line.split("_");
			int line_keyNumber = Integer.parseInt(lineArray[0]);
			String s_line_keyNumber = lineArray[0];
			String line_groupname = lineArray[1];
			System.out.println(line_groupname+" is the line's groupname from the file");
			if(line_groupname.equals(toUpload_groupname) && line_keyNumber > maxKeyNumber){
				toUpload_found_correctKey = true;
				toUpload_correctKey = lineArray[2];
				toUpload_correctIV = lineArray[3];
				s_toUpload_lineKeyNumber = lineArray[0];
				System.out.println("We have a key, "+ toUpload_correctKey+" with the correct IV!");
				}
			}
			if(toUpload_found_correctKey == true){//YAY WE HAVE THE KEY
					System.out.println("Actual key-------");
					System.out.println(toUpload_correctKey);
			}else{
				System.out.println("You don't have the key to decrypt this file, please obtain a new token");
				return false;
			}
				
				//NEED TO GENERATE THE AES KEY FROM THE STRING FOUND IN GROUPKEYS.BIN
			byte[] aesBytes = Hex.decode(toUpload_correctKey); //generate public key bytes using string value of key
			byte[] IVBytes = Hex.decode(toUpload_correctIV);
			SecretKeySpec key_from_file = new SecretKeySpec(aesBytes, "AES");    
				
				//ENCRYPT CONTENTS OF FILE 		
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
			System.out.println("Starting encryption..");
			cipher.init(Cipher.ENCRYPT_MODE, key_from_file, new IvParameterSpec(IVBytes)); //encrypt using key
				
			File newfile = new File(sourceFile);
			FileInputStream fis = new FileInputStream(newfile);
				
			File file = new File("newsourcefile");
			file.createNewFile();
			FileOutputStream fos = new FileOutputStream(file);
				
			CBCEncrypt(cipher,fis,fos);
				
			System.out.println("Finished encryption.");
				//END OF ENCRYPTING THE FILE, IT IS READY FOR UPLOADING (ENCRYPTED.TXT)
			fis.close();
			fos.close();
		}catch(Exception e1){
			System.err.println("Error: " + e1.getMessage());
			e1.printStackTrace(System.err);
			return false;
		}
		
		try{
			 
			 Envelope message = null, env = null;
			 //Tell the server to return the member list
			 message = new Envelope("UPLOADF");
			 message.addObject(destFile);
			 message.addObject(group);
			 message.addObject(token); //Add requester's token
			 message.addObject(s_toUpload_lineKeyNumber);
			
			 output.writeObject(encEnvelope(message));
			
			 challengeAccepted((Envelope)input.readObject());
			 
			 FileInputStream fis2 = new FileInputStream("newsourcefile");

			 env = (Envelope)input.readObject();
			 if(env.getMessage().equals("ENCENV"))
			 {
			 	env = decEnvelope(env);
			 }
			 
			 //env = (Envelope)input.readObject();
			 //env = encMessage(message);
			 System.out.println(env.getMessage());
			 //If server indicates success, return the member list
			 if(env.getMessage().equals("READY"))
			 { 
				System.out.printf("Meta data upload successful\n");
				
			}
			 else {
				
				 System.out.printf("Upload failed: %s\n", env.getMessage());
				 return false;
			 }
			 
		 	
			 do {
				 byte[] buf = new byte[4096];
				 	if (env.getMessage().compareTo("READY")!=0) {
				 		System.out.printf("Server error: %s\n", env.getMessage());
				 		return false;
				 	}
				 	message = new Envelope("CHUNK");
					int n = fis2.read(buf); //can throw an IOException
					if (n > 0) {
						System.out.printf(".");
					} else if (n < 0) {
						System.out.println("Read error");
						return false;
					}
					
					message.addObject(buf);
					message.addObject(new Integer(n));
					
					//output.writeObject(message);
					
					
					//env = (Envelope)input.readObject();
					env = encMessage(message);
										
			 }
			 while (fis2.available()>0);		 
					 
			 //If server indicates success, return the member list
			 if(env.getMessage().compareTo("READY")==0)
			 { 
				
				message = new Envelope("EOF");
				//output.writeObject(message);
				env = encMessage(message);
				
				//env = (Envelope)input.readObject();
				if(env.getMessage().compareTo("OK")==0) {
					System.out.printf("\nFile data upload successful\n");
				}
				else {
					
					 System.out.printf("\nUpload failed: %s\n", env.getMessage());
					 return false;
				 }
				
			}
			 else {
				
				 System.out.printf("Upload failed: %s\n", env.getMessage());
				 return false;
			 }
			 
		 }catch(Exception e1)
			{
				System.err.println("Error: " + e1.getMessage());
				e1.printStackTrace(System.err);
				return false;
				}
		 return true;
	}
	
	//Used first to request public key from File server thread
	public String requestPublicKey(UserToken token){
		Envelope getPubKey = null, env = null;
		getPubKey = new Envelope("GETPUBKEY");
		getPubKey.addObject(token);
		
		try{
		output.writeObject(getPubKey);
		 
		env = (Envelope)input.readObject();
		
		if(env.getMessage().equals("OK"))
		{ 
			//duplicate code from FileClientSubInterface but needed for functions in FileClient
			String FS_pubKey = (String)env.getObjContents().get(0);
			byte[] publicbytes = Hex.decode(FS_pubKey); //generate public key bytes using string value of key
	        X509EncodedKeySpec keySpecPublic = new X509EncodedKeySpec(publicbytes);
	        KeyFactory keyFactoryPublic = KeyFactory.getInstance("RSA");
	        public_key_final = keyFactoryPublic.generatePublic(keySpecPublic); //generate public key from private key bytes
			
			return (String)env.getObjContents().get(0); //This cast creates compiler warnings. Sorry.
		}
		}catch(Exception e){
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
		return null;	
	}
	//Used to request IV from server thread
	public String requestIV(UserToken token){
		Envelope getIV = null, env = null;
		getIV = new Envelope("GETIV");
		getIV.addObject(token);
		
		try{
			output.writeObject(getIV);
			
			env = (Envelope)input.readObject();
			
			if(env.getMessage().equals("OK"))
			{
				String myIV = (String)env.getObjContents().get(0);
				byte[] myIVbytes = Hex.decode(myIV); //get bytes from sent iv string
				iv = new IvParameterSpec(myIVbytes); //get new iv from the byte array
				System.out.println("FILE CLIENT: Native IV: " + iv + "\n String IV: " + myIV );
				return (String)env.getObjContents().get(0);
			}
		}catch(Exception e){
			System.out.println("ERROR: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
		return null;
	}
	
	
	//used to send Encrypted AES Key to File Thread from user
	public Boolean sendEncAESKey(){
		Envelope getEncAESKey = null, env = null;
		getEncAESKey = new Envelope("GETENCAESKEY");

		byte[] AESkeyByteArray;   
        
        //generate AES-128 key
        int keySize = 128;
		KeyGenerator AESkg = null;
		
		try {											//Create generator instance for AES / BouncyCastle
			AESkg = KeyGenerator.getInstance("AES", "BC");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		}
		
		AESkg.init(keySize);							//initialize AES-128 key
		AESkey = AESkg.generateKey();						//creates the key
		
		
		//need to encrypt AESkey with FS pub key
		AESkeyByteArray = AESkey.getEncoded();
		
		byte[] cipherText = null;
		Cipher cipher = null;
		
		try{
		cipher = Cipher.getInstance("RSA", "BC");
		cipher.init(Cipher.ENCRYPT_MODE, public_key_final);
		cipherText = cipher.doFinal(AESkeyByteArray);
		
		}catch(Exception e){
			e.printStackTrace(System.err);
		}
		
		try{
			getEncAESKey.addObject(cipherText);
			output.writeObject(getEncAESKey);
			env = (Envelope)input.readObject();
			if(env.getMessage().equals("OK"))
			{
				return(true);
			}
		}catch(Exception e){
			System.out.println("ERROR: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
		return false;		
	}
	
	public Boolean HASH_sendEncAESKey(){
		Envelope getEncAESKey = null, env = null;
		getEncAESKey = new Envelope("HASHGETENCAESKEY");

		byte[] AESkeyByteArray;   
        
        //generate AES-128 key
        int keySize = 128;
		KeyGenerator AESkg = null;
		
		try {											//Create generator instance for AES / BouncyCastle
			AESkg = KeyGenerator.getInstance("AES", "BC");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		}
		
		AESkg.init(keySize);							//initialize AES-128 key
		hash_AESkey = AESkg.generateKey();						//creates the key
		
		
		//need to encrypt AESkey with FS pub key
		AESkeyByteArray = hash_AESkey.getEncoded();
		
		byte[] cipherText = null;
		Cipher cipher = null;
		
		try{
		cipher = Cipher.getInstance("RSA", "BC");
		cipher.init(Cipher.ENCRYPT_MODE, public_key_final);
		cipherText = cipher.doFinal(AESkeyByteArray);
		
		}catch(Exception e){
			e.printStackTrace(System.err);
		}
		
		try{
			getEncAESKey.addObject(cipherText);
			output.writeObject(getEncAESKey);
			env = (Envelope)input.readObject();
			if(env.getMessage().equals("OK"))
			{
				return(true);
			}
		}catch(Exception e){
			System.out.println("ERROR: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
		return false;		
	}
	
	public Envelope encMessage(Envelope msg)
	{
		try{
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
			
			cipher.init(Cipher.ENCRYPT_MODE, AESkey, iv);
			SealedObject so = new SealedObject(msg, cipher);
			Envelope outMsg = new Envelope("ENCENV");
			Envelope response = null;
			outMsg.addObject(so);
			output.writeObject(outMsg);
			response = (Envelope)input.readObject();
			if(response.getMessage().equals("ENCENV"))
			{
				SealedObject inObj = (SealedObject)response.getObjContents().get(0);
				String algorithm = inObj.getAlgorithm();
				Cipher eCipher = Cipher.getInstance(algorithm);
				eCipher.init(Cipher.DECRYPT_MODE, AESkey, iv);
				
				return (Envelope)inObj.getObject(eCipher);
			}else{
				return response;
			}
		}catch(Exception e){
			System.out.println("ERROR: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}
	
	private Envelope encEnvelope(Envelope message)
	{
		try{
			Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
			c.init(Cipher.ENCRYPT_MODE, AESkey, iv);
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
		SealedObject so = (SealedObject)message.getObjContents().get(0);
		
		if(message.getObjContents().size() >1){
			byte[] byte_hash_SealedObject = (byte[]) message.getObjContents().get(1);
			///HASH so AND COMPARE TO byte_hash_SealedObject
			byte[] so_bytes = toByteArray(so);
			MessageDigest hashedSealedObject = MessageDigest.getInstance("SHA1", "BC");
			hashedSealedObject.update(so_bytes);
			byte[] byte_hash_so_bytes = hashedSealedObject.digest();
		
			if(Arrays.equals(byte_hash_so_bytes,byte_hash_SealedObject)){
				
			}else{
				Envelope badvelope = new Envelope("Someone Tampered With This");
				return badvelope;
			}
		}
		///
		
		try{
			String algorithm = so.getAlgorithm();
			Cipher c = Cipher.getInstance(algorithm);
			c.init(Cipher.DECRYPT_MODE, AESkey,iv);
			return (Envelope)so.getObject(c);
			
		}catch(Exception e){
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
		return null;
		
	}

	private void challengeAccepted(Envelope message) throws IOException, NoSuchAlgorithmException, NoSuchProviderException
	{
		int challenge;
		int receivedChallenge;
		Envelope env;
		
		
		if(message.getMessage().equals("ENCENV"))
		{
			message = decEnvelope(message);
		}
		else
		{
			System.out.println("Message was not encrypted. Closing connection to prevent replay attacks.");
			System.exit(0);
		}
		
		//System.out.println("Envelope tag is: " + message.getMessage());
		if(message.getMessage().equals("CHALLENGE"))
		{
			receivedChallenge = (Integer)message.getObjContents().get(0);
			//System.out.println("Got challenge: " + receivedChallenge);
			
			if(receivedChallenge == Integer.MAX_VALUE)
			{
				//System.out.println("TEST MESSAGE: Sending challenge back to server.");
				challenge = Integer.MIN_VALUE;
				env = new Envelope("CHALLENGE");
				env.addObject(challenge);
				output.writeObject(encEnvelope(env));
			}
			else
			{
				//System.out.println("TEST MESSAGE: Sending challenge back to server.");
				challenge = ++receivedChallenge;
				env = new Envelope("CHALLENGE");
				env.addObject(challenge);
				output.writeObject(encEnvelope(env));
			}
		}
		else
		{
			System.out.println("Invalid Challenge Message. Exiting...");
			System.exit(1);
		}
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
	public static void CBCEncrypt(Cipher encryptCipher,FileInputStream fis, FileOutputStream fos)throws IOException,ShortBufferException,IllegalBlockSizeException,BadPaddingException{
	       byte[] buffer = new byte[16];
	       int noBytes = 0;
	       byte[] cipherBlock =new byte[encryptCipher.getOutputSize(buffer.length)];
	       int cipherBytes;
	       while((noBytes = fis.read(buffer))!=-1){
	           cipherBytes =encryptCipher.update(buffer, 0, noBytes, cipherBlock);
	           fos.write(cipherBlock, 0, cipherBytes);
	       }
	       cipherBytes = encryptCipher.doFinal(cipherBlock,0);
	       fos.write(cipherBlock,0,cipherBytes);
	 
	       fos.close();
	       fis.close();
	    }
		
		public static void CBCDecrypt(Cipher DecryptCipher,FileInputStream in, FileOutputStream out)throws ShortBufferException,IllegalBlockSizeException,BadPaddingException,DataLengthException,IllegalStateException,InvalidCipherTextException,IOException{
			byte[] buffer = new byte[16];
		    int noBytes = 0;
		    byte[] cipherBlock = new byte[DecryptCipher.getOutputSize(buffer.length)];
		    int cipherBytes;
		    
		    while((noBytes = in.read(buffer))!=-1){
		           cipherBytes = DecryptCipher.update(buffer, 0, noBytes, cipherBlock);
		           out.write(cipherBlock, 0, cipherBytes);
		    }
		    cipherBytes = DecryptCipher.doFinal(cipherBlock,0);
		    out.write(cipherBlock,0,cipherBytes);
		 
		    out.close();
		    in.close();
		}

	
}

