import java.util.Scanner;
import java.util.List;

public class FileClientSubInterface
 {
	FileClient fileClient = new FileClient();
	
	public void startFileInterface(String server, int port, UserToken token)
	{
		if(fileClient.connect(server, port))
		{
			Scanner scan = new Scanner(System.in);
			
			boolean quit = false;
			String chosenStr;
			int chosenInt;
			
			List<String> userList;
			
			String sourceFile = "";
			String destFile = "";
			String groupName = "";
			
			System.out.println("Welcome to the File client!");
			
			while(!quit)
			{
				System.out.println("Please select a number corresponding to the following options...");
				System.out.println("------File Options------");
				System.out.println("\t1: List files you have access to");
				System.out.println("\t2: Upload a file");
				System.out.println("\t3: Download a file");
				System.out.println("\t4: Delete a file");
				System.out.println("------Other Options------");
				System.out.println("\t5: Quit the File Server");
				
				System.out.print("Enter a number: ");
				
				chosenStr = scan.nextLine();
				chosenInt = Integer.parseInt(chosenStr);
				
				if(chosenInt == 1) //list files user has access to
				{
					userList = fileClient.listFiles(token);
					if(userList != null)
					{
						if(!userList.isEmpty())
						{
							for(int i=0; i < userList.size(); i++)
							{
								System.out.println(userList.get(i));
							}
						}
					}
					else
					{
						System.out.println("You seem to have no files available.");
					}
					
				}
				else if(chosenInt == 2) //upload file
				{
					System.out.print("Please enter the name of the file to be uploaded: ");
					sourceFile = scan.nextLine();
					System.out.print("Please enter what the file is to be called at the destination: ");
					destFile = scan.nextLine();
					System.out.println("Please enter the Group Name you wish to share the file with: ");
					groupName = scan.nextLine();
					
					if(fileClient.upload(sourceFile, destFile, groupName, token))
					{
						System.out.println("Upload request accepted!");
					}
					else
					{
						System.out.println("Upload request failed.");
					}
					
				}
				else if(chosenInt == 3)//download file
				{
					System.out.println("Please enter the name of the file to download: ");
					sourceFile = scan.nextLine();
					System.out.println("Please enter what the file is to be called at the destination: ");
					destFile = scan.nextLine();
					
					if(fileClient.download(sourceFile, destFile, token))
					{
						System.out.println("Download request accepted!");
					}
					else
					{
						System.out.println("Download request failed.");
					}
				}
				else if(chosenInt == 4)//delete file
				{
					System.out.println("Please enter the name of the file to delete: ");
					sourceFile = scan.nextLine();
					
					if(fileClient.delete(sourceFile, token))
					{
						System.out.println("Delete request accepted!");
					}
					else
					{
						System.out.println("Delete request failed.");
					}
				}
				else if(chosenInt == 5)//quit
				{
					System.out.println("Exiting File Client");
					quit = true;
					fileClient.disconnect();
				}
				else
				{
					System.out.println("Invalid Entry. Please enter a valid command.");
				}
			}
		}
		else
		{
			System.out.println("Error Connecting to the File Server.");
		}
	}
	
}


