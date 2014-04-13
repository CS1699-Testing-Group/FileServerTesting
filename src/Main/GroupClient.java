/* Implements the GroupClient Interface */

import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SealedObject;
import javax.crypto.spec.IvParameterSpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

public class GroupClient extends Client implements GroupClientInterface {


	PublicKey public_key_final;
	IvParameterSpec iv;
	Key AESkey;
	Key hash_AESkey;
	
	//copied from file server
	//used to send Encrypted AES Key to GROUP Thread from user
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
    
    public Boolean hashSendEncAESKey(byte[] encAESKey){
        Envelope getEncAESKey = null, env = null;
        getEncAESKey = new Envelope("HASHGETENCAESKEY");
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
 public byte[] getToken(byte[] username, byte[] password)

	 {
	 		return null;
	 }
	 public byte[] getToken(byte[] username, byte[] password, byte[] timeStamp, String address, String port)
	 { 
		try
		{
			byte[] token = null;
			Envelope message = null, response = null;
		 		 	
			//Tell the server to return a token.
			message = new Envelope("GET");
			String usernameConverted = Hex.toHexString(username);
			String passwordConverted = Hex.toHexString(password);
			String timeStampConverted = Hex.toHexString(timeStamp);
			message.addObject(usernameConverted); //Add user name string 0
			message.addObject(passwordConverted); //1
			message.addObject(timeStampConverted); //time stamp 2
			message.addObject(address); //3
			message.addObject(port);
			output.writeObject(message);
		
			//Get the response from the server
			response = (Envelope)input.readObject();
			
			//Successful response
			if(response.getMessage().equals("OK"))
			{
				//If there is a token in the Envelope, return it 
				ArrayList<Object> temp = null;
				temp = response.getObjContents();
				
				if(temp.size() == 1)
				{
					token = (byte[])temp.get(0);
					return token;
				}else if(temp.size() == 2){/// IF WE GET A KEY BUNDLE WE NEED TO SAVE IT TO THE FILE
					token = (byte[])temp.get(0);
					@SuppressWarnings("unchecked")
					ArrayList<String>groupKeys = (ArrayList<String>)temp.get(1);
					//System.out.println("I am a user");
						//ADD KEY TO LOCAL FILE AT THE NEXT LINE UPON TOKEN RECEPTION
						//When a user recieves a token, he/she also recieves an envelope full of all the keys 
						//System.out.println("Adding keys to groupkeys.txt from the group server envelope");
						BufferedWriter out = new BufferedWriter(new FileWriter("GroupKeys.txt",true));
						
						//KEYS IS THE ARRAYLIST FOUND IN THE ENVELOPE
						//keys = (ArrayList<String>)ENVELOPE.OBJECT(0);
						for(int i = 0; i < groupKeys.size();i++){
							//System.out.println("Adding key "+groupKeys.get(i));
							out.write(groupKeys.get(i)); //write all keys to the file line by line 
							out.newLine();
						}
						out.close();
						return token;
				}
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

	 public boolean createUser(String username, UserToken token)
	 {
	 	return false;
	 }
	 public boolean createUser(String username, UserToken token, String password)
	 {
		 try
			{
				Envelope message = null, response = null;
				//Tell the server to create a user
				message = new Envelope("CUSER");
				message.addObject(username); //Add user name string
				message.addObject(token); //Add the requester's token
				message.addObject(password);
				output.writeObject(encEnvelope(message));
			
				//get challenge and return challenge + 1
				challengeAccepted((Envelope)input.readObject());
				
				response = decEnvelope((Envelope)input.readObject());
				
				//If server indicates success, return true
				if(response.getMessage().equals("OK"))
				{
					return true;
				}
				
				return false;
			}
			catch(Exception e)
			{
				System.err.println("Error: " + e.getMessage());
				e.printStackTrace(System.err);
				return false;
			}
	 }
	 
	 public boolean deleteUser(String username, UserToken token)
	 {
		 try
			{
				Envelope message = null, response = null;
			 
				//Tell the server to delete a user
				message = new Envelope("DUSER");
				message.addObject(username); //Add user name
				message.addObject(token);  //Add requester's token
				output.writeObject(encEnvelope(message));
			
				//get challenge and return challenge + 1
				challengeAccepted((Envelope)input.readObject());
				
				response = decEnvelope((Envelope)input.readObject());
				
				//If server indicates success, return true
				if(response.getMessage().equals("OK"))
				{
					return true;
				}
				
				return false;
			}
			catch(Exception e)
			{
				System.err.println("Error: " + e.getMessage());
				e.printStackTrace(System.err);
				return false;
			}
	 }
	 
	 public boolean createGroup(String groupname, UserToken token)
	 {
		 try
			{
				Envelope message = null, response = null;
				//Tell the server to create a group
				message = new Envelope("CGROUP");
				message.addObject(groupname); //Add the group name string
				message.addObject(token); //Add the requester's token
				output.writeObject(encEnvelope(message)); 
			
				//get challenge and return challenge + 1
				challengeAccepted((Envelope)input.readObject());
				
				response = decEnvelope((Envelope)input.readObject());
				
				//If server indicates success, return true
				if(response.getMessage().equals("OK"))
				{
					return true;
				}
				
				return false;
			}
			catch(Exception e)
			{
				System.err.println("Error: " + e.getMessage());
				e.printStackTrace(System.err);
				return false;
			}
	 }
	 
	 public boolean deleteGroup(String groupname, UserToken token)
	 {
		 try
			{
				Envelope message = null, response = null;
				//Tell the server to delete a group
				message = new Envelope("DGROUP");
				message.addObject(groupname); //Add group name string
				message.addObject(token); //Add requester's token
				output.writeObject(encEnvelope(message)); 
			
				//get challenge and return challenge + 1
				challengeAccepted((Envelope)input.readObject());
				
				response = decEnvelope((Envelope)input.readObject());
				//If server indicates success, return true
				if(response.getMessage().equals("OK"))
				{
					return true;
				}
				
				return false;
			}
			catch(Exception e)
			{
				System.err.println("Error: " + e.getMessage());
				e.printStackTrace(System.err);
				return false;
			}
	 }
	 
	 @SuppressWarnings("unchecked")
	public List<String> listMembers(String group, UserToken token)
	 {
		 try
		 {
			 Envelope message = null, response = null;
			 //Tell the server to return the member list
			 message = new Envelope("LMEMBERS");
			 message.addObject(group); //Add group name string
			 message.addObject(token); //Add requester's token
			 output.writeObject(encEnvelope(message)); 
			 
			//get challenge and return challenge + 1
			challengeAccepted((Envelope)input.readObject());
			 
			 response = decEnvelope((Envelope)input.readObject());
			 
			 //If server indicates success, return the member list
			 if(response.getMessage().equals("OK"))
			 { 
				return (List<String>)response.getObjContents().get(0); //This cast creates compiler warnings. Sorry.
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
	 
	 public boolean addUserToGroup(String username, String groupname, UserToken token)
	 {
		 try
			{
				Envelope message = null, response = null;
				//Tell the server to add a user to the group
				message = new Envelope("AUSERTOGROUP");
				message.addObject(username); //Add user name string
				message.addObject(groupname); //Add group name string
				message.addObject(token); //Add requester's token
				output.writeObject(encEnvelope(message)); 
			
				//get challenge and return challenge + 1
				challengeAccepted((Envelope)input.readObject());
				
				response = decEnvelope((Envelope)input.readObject());
				//If server indicates success, return true
				if(response.getMessage().equals("OK"))
				{
					return true;
				}
				
				return false;
			}
			catch(Exception e)
			{
				System.err.println("Error: " + e.getMessage());
				e.printStackTrace(System.err);
				return false;
			}
	 }
	 
	 public boolean deleteUserFromGroup(String username, String groupname, UserToken token)
	 {
		 try
			{
				Envelope message = null, response = null;
				//Tell the server to remove a user from the group
				message = new Envelope("RUSERFROMGROUP");
				message.addObject(username); //Add user name string
				message.addObject(groupname); //Add group name string
				message.addObject(token); //Add requester's token
				output.writeObject(encEnvelope(message));
			
				//get challenge and return challenge + 1
				challengeAccepted((Envelope)input.readObject());
				
				response = decEnvelope((Envelope)input.readObject());
				//If server indicates success, return true
				if(response.getMessage().equals("OK"))
				{
					return true;
				}
				
				return false;
			}
			catch(Exception e)
			{
				System.err.println("Error: " + e.getMessage());
				e.printStackTrace(System.err);
				return false;
			}
	 }
	 
	 //copied
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
					//System.out.println("FILE CLIENT: Native IV: " + iv + "\n String IV: " + myIV );
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
				//System.out.println("Hashed byte array of token: "+s_byte_hash_SealedObject);
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
		
		
	 //end copied
		
		//Get challenge from server and respond + 1
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

}
