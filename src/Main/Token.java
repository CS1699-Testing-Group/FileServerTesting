import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bouncycastle.util.encoders.Hex;

public class Token implements UserToken, java.io.Serializable{		
	private String Issuer;
	private String Subject;
	private List<String> Groups;
	private String IPAddress;
	private String PortNum;

	public Token(String issuer,String subject,List<String> groups, String ipAddress, String portI){
		Issuer = issuer;
		Subject = subject;
		Groups = groups;
		IPAddress = ipAddress;	
		PortNum = portI;

	}
	public Token(){
		Issuer = null;
		Subject = null;
		Groups = null;
		IPAddress = null;
	}
	@Override
	public String getIssuer() {
		return this.Issuer;
	}

	public String getIPAddress() {
		return this.IPAddress;
	}

	@Override
	public String getSubject() {
		return this.Subject;
	}

	@Override
	public List<String> getGroups() {
		return this.Groups;
	}
	public String getPort(){
		return this.PortNum;
	}
	
	public void addGroup(String groupToAdd){
		this.Groups.add(groupToAdd);
	}
	
	//create a string representation of the token. 
	//Underscore(_) used as a delimeter between all fields.
	//Groups sorted alphabetically prior to stringification
	public String stringify(){
		StringBuilder sb = new StringBuilder();
		sb.append("TOKEN:");
		sb.append("_");
		sb.append(this.Issuer);
		sb.append("_");
		sb.append(this.Subject);
		sb.append("_");
		sb.append(this.IPAddress);
		sb.append("_");
		sb.append(this.PortNum);
		Collections.sort(this.Groups);
		sb.append("_");
		for(int i = 0; i < this.Groups.size();i++){
			sb.append(Groups.get(i));
			sb.append("_");
		}
		sb.append("*");
		return sb.toString();
	}
	
	public byte[] byteify(String stringified){
		byte[] byteified = new byte[stringified.length()];
		try {
			byteified = stringified.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return byteified;
	}
	
	public void tokify(byte[] byte_token){
		String s_Token = new String(byte_token);
		//System.out.println("String of token from token_final is "+s_Token);
		
		String[] s_Token_array =s_Token.split("_");
		String issuer = s_Token_array[1];
		String subject = s_Token_array[2];
		String ipAddressF = s_Token_array[3];
		String portNumber = s_Token_array[4];
		ArrayList<String> s_Token_groups = new ArrayList<String>();
		
		int i = 5;
		while(!s_Token_array[i].equals("*")){
			s_Token_groups.add(s_Token_array[i]);
			i++;
		}
		
		//System.out.println("Issuer is "+issuer);
		//System.out.println("Subject is "+subject);
		//System.out.println("Groups are "+s_Token_groups);
		
		this.Issuer = issuer;
		this.Subject = subject;
		this.Groups = s_Token_groups;
		this.IPAddress = ipAddressF;
		this.PortNum = portNumber;
	}

}
