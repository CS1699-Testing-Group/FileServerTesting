package Main;
// group client that deals with signals from main program
public class GroupClientEnd {
	
	
	GroupClient groupClient = new GroupClient();
	
	public boolean startGroupClientEnd(String server, int port, UserToken token){
		
		
		if (groupClient.connect(server, port)){
			
			return true;
			
		}else{
			
			return false;
		}
		
		
	}
	
		
}

