package Main;
/* This thread does all the work. It communicates with the client through Envelopes.
 * 
 */
import java.lang.Thread;
import java.net.Socket;
import java.io.*;
import java.util.*;

public class GroupThread extends Thread 
{
	private final Socket socket;
	private GroupServer my_gs;
	
	public GroupThread(Socket _socket, GroupServer _gs)
	{
		socket = _socket;
		my_gs = _gs;
	}
	
	public void run()
	{
		boolean proceed = true;

		try
		{
			//Announces connection and opens object streams
			System.out.println("*** New connection from " + socket.getInetAddress() + ":" + socket.getPort() + "***");
			final ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
			final ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
			
			do
			{
				Envelope message = (Envelope)input.readObject();
				System.out.println("Request received: " + message.getMessage());
				Envelope response;
				
				if(message.getMessage().equals("GET")){//Client wants a token
					String username = (String)message.getObjContents().get(0); //Get the username
					if(username == null){
						response = new Envelope("FAIL");
						response.addObject(null);
						output.writeObject(response);
					}
					else{
						UserToken yourToken = createToken(username); //Create a token
						//Respond to the client. On error, the client will receive a null token						
						response = new Envelope("OK");
						response.addObject(yourToken);
						output.writeObject(response);
					}
				}
				else if(message.getMessage().equals("CUSER")){ //Client wants to create a user

					if(message.getObjContents().size() < 2){
						response = new Envelope("FAIL");
					}
					else{
						response = new Envelope("FAIL");
						
						if(message.getObjContents().get(0) != null){
							if(message.getObjContents().get(1) != null){ //expects 2 objects in envelope
								String username = (String)message.getObjContents().get(0); //Extract the username
								UserToken yourToken = (UserToken)message.getObjContents().get(1); //Extract the token
								
								if(createUser(username, yourToken)){
									response = new Envelope("OK"); //Success
								}
							}
						}
					}
					output.writeObject(response);
				}
				else if(message.getMessage().equals("DUSER")){//Client wants to delete a user
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
					
					output.writeObject(response);
				}
			
				else if(message.getMessage().equals("CGROUP")) //Client wants to create a group
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

					output.writeObject(response);
				}
				else if(message.getMessage().equals("DGROUP")){//Client wants to delete a group
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

					output.writeObject(response);
				}
				else if(message.getMessage().equals("LMEMBERS")){//Client wants a list of members in a group
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
							
							output.writeObject(response);
						}
					}
					
				
				}
				else if(message.getMessage().equals("AUSERTOGROUP")){//Client wants to add user to a group
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

					output.writeObject(response);
				}
				else if(message.getMessage().equals("RUSERFROMGROUP")){//Client wants to remove user from a group
				
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

					output.writeObject(response);
				}
				else if(message.getMessage().equals("DISCONNECT")) //Client wants to disconnect
				{
					socket.close(); //Close the socket
					proceed = false; //End this communication loop
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
	private boolean removeUserFromGroup(String username, String groupname,UserToken yourToken) {
		
		ArrayList<String> ownedGroups = my_gs.userList.getUserOwnership(yourToken.getSubject());
		String requester = yourToken.getSubject();
		if(my_gs.userList.checkUser(requester)){
			if(my_gs.userList.groupExists(groupname)){
				if(ownedGroups.contains(groupname)){ //if token SUBJECT is the owner of the GROUPNAME
					if(my_gs.userList.checkUser(username)){ //if user exists
						my_gs.userList.removeMemberFromGroup(username, groupname); //he can remove USERNAME from the GROUPNAME
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
	private UserToken createToken(String username) {
		//Check that user exists
		if(my_gs.userList.checkUser(username)){
			//Issue a new token with server's name, user's name, and user's groups
			UserToken yourToken = new Token(my_gs.name, username, my_gs.userList.getUserGroups(username));
			return yourToken;
		}
		else
		{
			return null;
		}
	}
	
	//Method to create a user
	private boolean createUser(String username, UserToken yourToken){
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
					my_gs.userList.addUser(username);
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
	private boolean createGroup(String group,UserToken yourToken){
		
		String requester = yourToken.getSubject();
		
		//Check if requester exists
				if(my_gs.userList.checkUser(requester)){
					if(!my_gs.userList.groupExists(group)){
						my_gs.userList.addOwnership(requester, group);
						my_gs.userList.addGroup(requester, group);
						my_gs.userList.createGroup(requester,group);
						return true;
					}
					return false;
				}else{
					return false;
				}
		
		
	}
	
	//Method to delete a user
	private boolean deleteUser(String username, UserToken yourToken){
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
						my_gs.userList.removeMemberFromGroup(username, deleteFromGroups.get(index));//remove USERNAME, GROUPNAME 1, 2, 3, 4...
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
						deleteGroup(deleteOwnedGroup.get(index), new Token(my_gs.name, username, deleteOwnedGroup));
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
	
}
