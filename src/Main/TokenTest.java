import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.*;
import java.util.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

public class TokenTest {
	
	public static String GS_publicKey = "305c300d06092a864886f70d0101010500034b003048024100914754aec76a53097bb052be8154be1e493f803809b706daf3f09030047e05083e8c45181f45c6741b30232bff46b4d00868aeb9b8e30f6aaf2a3dfb248771d90203010001";
	public static String GS_privateKey = "30820154020100300d06092a864886f70d01010105000482013e3082013a020100024100914754aec76a53097bb052be8154be1e493f803809b706daf3f09030047e05083e8c45181f45c6741b30232bff46b4d00868aeb9b8e30f6aaf2a3dfb248771d9020301000102405e7402f1b38344438920655ba89861172dc654659aa35d98a33b0773c0a7f23aa404e18f1c9ce241d706803469b53034fdfb269f4f80b175241791a0380dead1022100c8531a9437672ef099f010484c6a88295adcf8d401ad9230c79c53a15b3b678d022100b9a7c294b0ff532abec703c919eb741dd90cb822b597bd7d73543a26e01bea7d0220637918c6a6a83f1fcc60efc4e6e5338dcd87d2ab7bd5d3b51339a631869afdf502206009e66060c753d072ec248b2d3b5dcfeaede77b1d1127d6f38808a4ff9db1490221009ec47cf98186c862af9c4a9124bf1b2102ef5e275d81493909b9214b5605542d";
	
	public static void main(String[] args) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		ArrayList<String> groups = new ArrayList<String>();
		groups.add("a");
		groups.add("c");
		groups.add("b");
		//Token token = new Token("Harrison","ADMIN",groups);
		Token token = new Token(); //placeholder due to error
		
		String TOKEN = token.stringify();
		System.out.println("Stringified token: "+TOKEN);//string representation of token object
		byte[] byte_TOKEN = token.byteify(TOKEN);
		String s_byte_TOKEN = Hex.toHexString(byte_TOKEN); //byte array of TOKEN
		System.out.println("Byte array of token: "+s_byte_TOKEN);
		
		//GENERATE SIGNATURE BYTE ARRAY
		
		//STEP 1: HASH TOKEN, hashedToken
		MessageDigest TokenMessageDigest = MessageDigest.getInstance("SHA1", "BC");
		TokenMessageDigest.update(byte_TOKEN);
		byte hashedToken[] = TokenMessageDigest.digest();
		String s_hashed_TOKEN = Hex.toHexString(hashedToken);
		System.out.println("Hashed byte array of token: "+s_hashed_TOKEN);
		
		//STEP 2: SIGN HASHED TOKEN with server's private key, sigBytes

        byte[] privatebytes = Hex.decode(GS_privateKey); //generate private key bytes using string value of key
        PKCS8EncodedKeySpec keySpecPrivate = new PKCS8EncodedKeySpec(privatebytes);
        KeyFactory keyFactoryPrivate = KeyFactory.getInstance("RSA");
        PrivateKey private_key_final = keyFactoryPrivate.generatePrivate(keySpecPrivate); //generate private key from private key bytes
		
        Signature signature = Signature.getInstance("SHA1withRSA", "BC");
	    signature.initSign(private_key_final, new SecureRandom());
	    signature.update(hashedToken);
	    byte[] sigBytes = signature.sign();
	    
	    String s_signature = Hex.toHexString(sigBytes);
	    System.out.println("Signature byte array: "+s_signature);
	    
	    //STEP 3: CONCATENATE TOKEN WITH SIGNATURE
	    
	    byte[] token_final = new byte[sigBytes.length+byte_TOKEN.length];
		for(int i = 0; i < byte_TOKEN.length; i++){
			token_final[i] = byte_TOKEN[i];
		}
		for(int j = byte_TOKEN.length, k = 0; j < token_final.length;j++,k++){
			token_final[j] = sigBytes[k];
		}
		
		String s_final_token = Hex.toHexString(token_final);
		System.out.println("FINAL bytes of token. Should be byte_TOKEN||SigBytes: ");
		System.out.println(s_final_token);
		
		
		//STEP 4: AT FILE SERVER, NEED TO VERIFY SIGNATURE MATCHES TOKEN
			
			//need to break the token byte object into two parts
			
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
	    System.out.println("Signature is verified: "+signature2.verify(sigBytes));
		
	    
	    
	    //putting the token back together....
	    
	    
	    
		String s_Token = new String(TOKEN_from_token_final);
		System.out.println("String of token from token_final is "+s_Token);
		
		String[] s_Token_array =s_Token.split("_");
		String issuer = s_Token_array[1];
		String subject = s_Token_array[2];
		ArrayList<String> s_Token_groups = new ArrayList<String>();
		int i = 3;
		while(!s_Token_array[i].equals("*")){
			s_Token_groups.add(s_Token_array[i]);
			i++;
		}
		
		System.out.println("Issuer is "+issuer);
		System.out.println("Subject is "+subject);
		System.out.println("Groups are "+s_Token_groups);
		/*
		System.out.println("\nGENERATING KEYS");
		
		KeyPair keys = GenerateASymmetricKeys(512);
		byte[] publicKey = keys.getPublic().getEncoded();
		byte[] privateKey = keys.getPrivate().getEncoded();
		System.out.println("\nActual Public key------");
        String s1 = Hex.toHexString(publicKey);
        System.out.println(s1);
        System.out.println("Actual private key--------");
        String s2 = Hex.toHexString(privateKey);
        System.out.println(s2);
        
        System.out.println("END GENERATING KEYS");
		*/
		
	}
	/*public static KeyPair GenerateASymmetricKeys(int keySizeInBits)throws Exception {
		KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA","BC"); 
        keyGenerator.initialize(keySizeInBits); 
        KeyPair keys = keyGenerator.generateKeyPair(); 
        return keys;
	}*/

}
