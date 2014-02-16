import java.util.List;

public class Token implements UserToken, java.io.Serializable{		
	private String Issuer;
	private String Subject;
	private List<String> Groups;
	
	public Token(String issuer,String subject,List<String> groups){
		Issuer = issuer;
		Subject = subject;
		Groups = groups;
	}
	public Token(){
		Issuer = null;
		Subject = null;
		Groups = null;
	}
	@Override
	public String getIssuer() {
		return this.Issuer;
	}

	@Override
	public String getSubject() {
		return this.Subject;
	}

	@Override
	public List<String> getGroups() {
		return this.Groups;
	}
	
	public void addGroup(String groupToAdd){
		this.Groups.add(groupToAdd);
	}

}
