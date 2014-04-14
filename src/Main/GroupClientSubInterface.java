package Main;
import java.util.Scanner;
import java.util.List;
import java.net.Socket;


public class GroupClientSubInterface
{

	GroupClient groupClient = new GroupClient();

	public void startGroupInterface(String server, int port, UserToken token)
	{
		if(groupClient.connect(server, port))
		{

			Scanner scan = new Scanner(System.in);
			boolean quit = false;
			String chosenStr;
			int chosenInt;

			String thisUser = "";
			String thisGroup = "";

			//for group member listing
			List<String> userList;

			System.out.println("\nWelcome to the Group client!");

			while(!quit)
			{
				System.out.println("Please select a number corresponding to the following options...");
				System.out.println("------User Options------");
				System.out.println("\t1: Create a new user");
				System.out.println("\t2: Delete an existing user");
				System.out.println("------Group Options------");
				System.out.println("\t3: Create a new group");
				System.out.println("\t4: Delete an existing group");
				System.out.println("\t5: Add user to an existing group");
				System.out.println("\t6: Delete user from existing group");
				System.out.println("\t7: List a group's members");

				System.out.println("------Other Options------");
				System.out.println("\t8: Quit the Group Server");

				System.out.print("Enter a number: ");

				chosenStr = scan.nextLine();
				chosenInt = Integer.parseInt(chosenStr);


				if(chosenInt == 1) //create new user
				{
					System.out.print("Please enter a username: ");
					
					thisUser = scan.nextLine();
					
					if(groupClient.createUser(thisUser, token))
					{
						System.out.println("User creation successful!");
					}
					else
					{
						System.out.println("User creation failed.");
					}
					
				}
				else if(chosenInt == 2) //delete existing user
				{
					System.out.print("Please enter a username: ");
					
					thisUser = scan.nextLine();
					
					if(groupClient.deleteUser(thisUser, token))
					{
						System.out.println("User deleted!");
					}
					else
					{
						System.out.println("User deletion failed");
					}
				}
				else if(chosenInt == 3) //create new group
				{
					System.out.print("Please enter a Group Name: ");
					
					thisGroup = scan.nextLine();
					
					if(groupClient.createGroup(thisGroup, token))
					{
						System.out.println("Group Created!");
					}
					else
					{
						System.out.println("Group creation failed.");
					}
					
				}
				else if(chosenInt == 4) //delete existing group
				{
					System.out.print("Please enter a Group Name: ");
					
					thisGroup = scan.nextLine();
					
					if(groupClient.deleteGroup(thisGroup, token))
					{
						System.out.println("Group Deleted!");
					}
					else
					{
						System.out.println("Group deletion failed.");
					}
					
				}
				else if(chosenInt == 5) //add user to group
				{
					System.out.print("Please enter a Username: ");
					thisUser = scan.nextLine();
					
					System.out.print("Please enter a group: ");
					thisGroup = scan.nextLine();
					
					if(groupClient.addUserToGroup(thisUser, thisGroup, token))
					{
						System.out.println("User added successfully");
					}
					else
					{
						System.out.println("Adding the user failed.");
					}
				}
				else if(chosenInt == 6) //delete user from group
				{
					System.out.print("Please enter a Username: ");
					thisUser = scan.nextLine();
					
					System.out.print("Please enter a group: ");
					thisGroup = scan.nextLine();
					
					if(groupClient.deleteUserFromGroup(thisUser, thisGroup, token))
					{
						System.out.println("User removed successfully");
					}
					else
					{
						System.out.println("Removing the user failed.");
					}
				}
				else if(chosenInt == 7) //list members
				{
					System.out.print("Please enter the group's name: ");
					thisGroup = scan.nextLine();
					
					userList = groupClient.listMembers(thisGroup, token);
					
					if(userList != null)
					{
						if(!userList.isEmpty())
						{
							//System.out.println("userList size = " + userList.size());
							
							for(int i = 0; i < userList.size(); i++)
							{	
								//System.out.print("index(" + i + ") = ");
								System.out.println(userList.get(i));
							}
							
							/*
							for(String str: userList)
							{
								System.out.println(str);
							}
							*/
						}
					}
					else	
					{
						System.out.println("Cannot list members of this group.");
					}
				}
				else if(chosenInt == 8) //quit group client
				{
					System.out.println("Exiting Group Client...");
					quit = true;
					groupClient.disconnect();
				}
				else
				{
					System.out.println("Invalid Entry. Please enter a valid command.");
				}
			}
		}
		else
		{
			System.out.println("Error Connecting to the Group Server.");
		}
	}

}