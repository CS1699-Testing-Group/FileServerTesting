import java.util.Scanner;

public class MainUserInterface
{
	public static void main(String[] args)
	{
		Scanner scanner = new Scanner(System.in);

		GroupClient groupClient = new GroupClient();
		FileClient fileClient = new FileClient();

		//Variables for user input
		String chosenStr;
		int chosenInt;

		//Variables to validate user input
		boolean validToken = false;
		boolean validChoice = true;

		//User parameters
		String username = null;
		UserToken userToken = null;

		//Networking paramaters for file server
		String fsIP = "";
		int fsPort = 0; //default port
		String fsCustomPort = "";
		
		//Networking parameters for the group server
		String gsIP = "";
		int gsPort = 0;
		String gsCustomPort = "";
		
		//Dual networking parameters
		String ipOptionStr;
		int ipOptionInt = 0;
		
		boolean loopIP = false;

		while(true){ //loop overall program until user chooses to quit

			System.out.println("Please enter a number.");
			System.out.println("\t1: Login with Username");
			System.out.println("\t2: Quit");
			System.out.print("\nEntry: ");

			chosenStr = scanner.nextLine();
			try{	//make sure input is valid
				validChoice = true;
				chosenInt = Integer.parseInt(chosenStr);
			}catch(Exception e){
				validChoice = false;
				chosenInt = 0;
			}

			if(validChoice == true)
			{
				if(chosenInt == 1)
				{
					System.out.println("Enter your username");
					System.out.print("Username: ");
					username = scanner.nextLine();

					//ip stuff here for group client
					do{
						System.out.println("Where is the Group Server you would like to connect to for authentication?");
						System.out.println("\t0: Default (localhost, 8765)");
						System.out.println("\t1: Custom");
					
						ipOptionStr = scanner.nextLine();
						ipOptionInt = Integer.parseInt(ipOptionStr);
						if(ipOptionInt == 0)//default localhost ip
						{
							gsIP = "localhost";
							gsPort = 8765;
							loopIP = false;
						}
						else if(ipOptionInt == 1)//custom ip
						{
							System.out.print("Please enter the IP address: ");
							gsIP = scanner.nextLine();
							System.out.print("Please enter the port: ");
							gsCustomPort = scanner.nextLine();
							gsPort = Integer.parseInt(gsCustomPort);
							loopIP = false;
						}
						else
						{
							loopIP = true;
						}
					}while(loopIP == true);
					
					//end
					groupClient.connect(gsIP, gsPort);
					if(groupClient.isConnected())
					{
						userToken = groupClient.getToken(username);
						if(userToken != null)	//Valid login
						{
							validToken = true;
							groupClient.disconnect();
						}
						else	//Invalid login
						{
							System.out.println("Invalid Username.");
							groupClient.disconnect();
						}
					}
					else
					{
						System.out.println("Group Server Not Connecting...");
					}
				}
				else if(chosenInt == 2) //user chose to quit
				{
					System.out.println("Shutting down...");
					System.exit(0);
				}
				else
				{
					System.out.println("Invalid Choice. Please enter a valid command.");
				}

				while(validToken) //User checks out. Token issued. User may continue...
				{
					System.out.println("Please enter a number.");
					System.out.println("\t1: Connect to the Group Server");
					System.out.println("\t2: Connect to the File server");
					System.out.println("\t3: Quit");
					System.out.print("\nEntry: ");

					validChoice = true;
					chosenStr = scanner.nextLine();
					try{
						validChoice = true;
						chosenInt = Integer.parseInt(chosenStr);
					}catch(Exception e){
						validChoice = false;
						chosenInt = 0;
					}
					if(validChoice == true)
					{

						if(chosenInt == 1)//Group server
						{
							CallGroupServer(gsIP, gsPort, userToken);
						}
						else if(chosenInt == 2)//File Server
						{
							do{
								groupClient.connect(gsIP, gsPort);
								if(groupClient.isConnected())
								{
									userToken = groupClient.getToken(username);
									if(userToken != null)	//Valid login
									{
										validToken = true;
										groupClient.disconnect();
									}
									else	//Invalid login
									{
										System.out.println("Invalid Username.");
										groupClient.disconnect();
									}
								}
								System.out.println("Where is the server you would like to connect to?");
								System.out.println("\t0: Default (localhost, 4321)");
								System.out.println("\t1: Custom");
							
								ipOptionStr = scanner.nextLine();
								ipOptionInt = Integer.parseInt(ipOptionStr);
								if(ipOptionInt == 0)//default localhost ip
								{
									CallFileServer("localhost", 4321, userToken);
									loopIP = false;
								}
								else if(ipOptionInt == 1)//custom ip
								{
									System.out.print("Please enter the IP address: ");
									fsIP = scanner.nextLine();
									System.out.print("Please enter the port: ");
									fsCustomPort = scanner.nextLine();
									fsPort = Integer.parseInt(fsCustomPort);
									
									CallFileServer(fsIP, fsPort, userToken);
									loopIP = false;
								}
								else
								{
									loopIP = true;
								}
							}while(loopIP == true);
						}
						else if(chosenInt == 3)//Quit
						{
							System.out.println("Shutting down...");
							System.exit(0);
						}
						else //invalid entry
						{
							System.out.println("Invalid Choice. Please enter a valid command.");
						}
					}
					else //second validChoice != true
					{
						System.out.println("Invalid Choice. Please enter a valid command.");
					}
				}
			}
			else{ 	//if initial valid choice != true
				System.out.println("Invalid Choice. Please enter a valid command.");
			}
		}
	}
	public static void CallGroupServer(String ip, int port, UserToken userToken)
	{
		GroupClientSubInterface groupClientSubInterface = new GroupClientSubInterface();
		groupClientSubInterface.startGroupInterface(ip, port, userToken);
	}
	public static void CallFileServer(String ip, int port, UserToken userToken)
	{
		FileClientSubInterface fileClientSubInterface = new FileClientSubInterface();
		fileClientSubInterface.startFileInterface(ip, port, userToken);
	}
	
}