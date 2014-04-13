/* This list represents the users on the server */
import java.util.*;
import java.security.*;
import javax.crypto.*;
import java.io.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
	public class UserList implements java.io.Serializable {
	
		/**
		 * 
		 */
		private static final long serialVersionUID = 7600343803563417992L;
		private Hashtable<String, User> list = new Hashtable<String, User>();
		private Hashtable<String, List<String>> group_list = new Hashtable<String, List<String>>();//(GROUPNAME,LISTofMEMBERS)
		public Hashtable<String,ArrayList<String>> groupKeyTable = new Hashtable<String,ArrayList<String>>(); //key storage on GS side

		

		public synchronized void createGroup(String requester,String groupname){
			if(!group_list.containsKey(groupname)){
				System.out.println("Creating a group");
				List<String>newGroup = new ArrayList<String>();
				newGroup.add(requester);
				group_list.put(groupname, newGroup);
				
				System.out.println("Group was created");
				Enumeration<String> enumKey = group_list.keys();
				while(enumKey.hasMoreElements()){ //remove username from all groups in group_list
					String key = enumKey.nextElement();
				    List<String> val = group_list.get(key);
				    System.out.println("one group is "+ key +" with value "+val);
				}	
			}else{
				System.out.println("Already a group with this name...");
				//group list already has name
			}
		}
		public synchronized void addUser(String username, String password)
		{
			
			byte doHash[] = null;
		try { // to create array of bytes from input
			doHash = new String(password).getBytes("UTF8");
		}
		catch (UnsupportedEncodingException e) {
			e.printStackTrace(System.err);
			
		}
		try { // to get hash of byte array

                Security.addProvider(new BouncyCastleProvider());
			MessageDigest messageDigest = MessageDigest.getInstance("SHA1", "BC");
			messageDigest.update(doHash);
			byte sol[] = messageDigest.digest();
			 User newUser = new User(password, sol);
			
			list.put(username, newUser);
			
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
			
		}
			
		}
		public synchronized boolean checkPassword(String username, String password)
		{
			if(!list.isEmpty()){
				
				if(list.containsKey(username)){
					
					 User check = list.get(username);
					 
					 if (check.userPassword.equals(password)){
					 	return true;
					 }
					
				}else{
					return false;
				}
			}else{
				return false;
			}
			return false;
		}

		public synchronized byte[] returnHash(String username)
	{
	
	if(!list.isEmpty()){
				
				if(list.containsKey(username)){
					
					 User check = list.get(username);
					 
					 return check.pw;
					
				}else{
					return null;
				}
			}else{
				return null;
			}	

		
	}
		
		public synchronized boolean groupExists(String groupname){
			
			if(!group_list.isEmpty()){
				if(group_list.containsKey(groupname)){
					return true;
				}else{
					return false;
				}
			}
			return false;
		}
		public synchronized void deleteUser(String username)
		{
			list.remove(username); //user is removed from main hashtable
			
			Enumeration<String> enumKey = group_list.keys();
			while(enumKey.hasMoreElements()){ //remove username from all groups in group_list
				String key = enumKey.nextElement();
			    List<String> val = group_list.get(key);
			    
			    if(val.contains(username)){
			    	val.remove(username);
			    }
			}	
		}
		
		public ArrayList<String> getGroupMembers(String groupName) {
			System.out.println("Getting group members....");
			ArrayList<String> members = new ArrayList<String>();
			
			if(!group_list.isEmpty()){
				System.out.println("Group isn't empty");
				if(group_list.containsKey(groupName)){
					System.out.print("Group exists in list");
					for(int i = 0; i < group_list.get(groupName).size();i++){
						System.out.println("Adding "+group_list.get(groupName).get(i)+ " to the printed list");
						members.add(group_list.get(groupName).get(i));
					}
				}
			}
			
			return members;
		}
		
		public synchronized void deleteGroup(String groupname){
			
			if(!group_list.isEmpty()){
				if(group_list.containsKey(groupname)){ //group exists
					//we need to remove all users from USER from said group
					Enumeration<String> enumKey = group_list.keys();
					while(enumKey.hasMoreElements()){ //iterate through groups of group_list
						String key = enumKey.nextElement();
					
						if(key.equals(groupname)){ //if current enumeration group == group we are deleting
							List<String> val = group_list.get(key);//list of members of group we want to delete
						
							for(int i = 0; i < val.size();i++){ //for each member of the group we are deleting...
								removeMemberFromGroup(val.get(i),key); //remove from both hashtables 
							}
						}
					}
				
				group_list.remove(groupname);//delete entry in group_list hashtable for group once we are done with it
				
				}else{
				//group doesn't even exist...
				}
			}
		}
		
		public synchronized void removeMemberFromGroup(String username, String groupName){
			if(!group_list.isEmpty()){
				if(group_list.get(groupName).contains(username)){ //if list of members of GROUPNAME contains USERNAME
					group_list.get(groupName).remove(username);//remove the USERNAME from the list
				}
			}
			
			if(!list.isEmpty()){
				list.get(username).removeGroup(groupName);//remove groupname from user's list as well
			}
		}
		
		public synchronized boolean checkUser(String username)
		{
			if(!list.isEmpty()){
				if(list.containsKey(username)){
					return true;
				}else{
					return false;
				}
			}else{
				return false;
			}
		}
		
		public synchronized ArrayList<String> getUserGroups(String username)
		{
			if(checkUser(username) == true){
				return list.get(username).getGroups();
			}else{
				return new ArrayList<String>();
			}
		}
		
		public synchronized ArrayList<String> getUserOwnership(String username)
		{
			return list.get(username).getOwnership();
		}
		
		public synchronized void addGroup(String user, String groupname)
		{
			if(!list.isEmpty()){
				list.get(user).groups.add(groupname); //add group to user's list of groups they belong to
				System.out.println("User added to the group "+groupname);
			}
			
			if(!group_list.isEmpty()){
				System.out.println("Group list isn't empty");
			/////--------GROUP_LIST--------////////
				if(group_list.containsKey(groupname)){ //if the group is already alive and kicking
					System.out.println("Group list contains the group name" + group_list.contains(groupname));
					if(group_list.get(groupname).contains(user)){
					System.out.println("User already in group...");
					//do nothing, user already part of the group
					}else{
						System.out.println("User isn't in the group and it exists");
						System.out.println("Adding user "+user+" to group "+groupname);
						group_list.get(groupname).add(user); //add user to the list of members of GROUPNAME
					}
				}else{
					
				}
			
			}
			////---------GROUP_LIST--------/////////
			
		}

		
		public synchronized void addOwnership(String user, String groupname)
		{
			list.get(user).addOwnership(groupname);
		}
		
		public synchronized void removeOwnership(String user, String groupname)
		{
			list.get(user).removeOwnership(groupname);
		}
		
	
	class User implements java.io.Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = -6699986336399821598L;
		private ArrayList<String> groups;
		private ArrayList<String> ownership;
		private String userPassword;

		private byte pw[];

		public User(String password, byte pw1[])
		{
			groups = new ArrayList<String>();
			ownership = new ArrayList<String>();
			userPassword = password;
			pw = pw1;
		}
		
		public ArrayList<String> getGroups()
		{
			return groups;
		}
		
		public ArrayList<String> getOwnership()
		{
			return ownership;
		}
		
		public void addGroup(String group)
		{
			groups.add(group);
		}
		
		public void removeGroup(String group)
		{
			if(!groups.isEmpty())
			{
				if(groups.contains(group))
				{
					//TODO also remove them from the group_list group
					
					groups.remove(groups.indexOf(group));
					
				}
			}
		}
		
		public void addOwnership(String group)
		{
			ownership.add(group);
		}
		
		public void removeOwnership(String group)
		{
			if(!ownership.isEmpty())
			{
				if(ownership.contains(group))
				{
					ownership.remove(ownership.indexOf(group));
				}
			}
		}
		
	}
	
}	
