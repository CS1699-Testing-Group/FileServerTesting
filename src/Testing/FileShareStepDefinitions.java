package Testing;
//Test by MDS
import cucumber.api.java.en.*;
import static org.junit.Assert.*;

import org.mockito.*;

import Main.FileClient;
import Main.UserToken;
import Main.Token;

public class FileShareStepDefinitions{
	
	Boolean confirm = false;
	String file_loc;
	String file_dest;
	String group;
	
	@Mock
	FileClient m_fc; 
	
	@Given("I am going to upload a file to the file server")
	public void a_user_shares_a_file(){
		m_fc = Mockito.mock(FileClient.class);
	}
	
	@When("The file is located at the directory (.+)$")
	public void set_file_location(String myLocation){
		file_loc = myLocation;
	}
	
	@And("The file destination is located at the directory (.+)$")
	public void set_file_destination(String myDestination){
		file_dest = myDestination;
	}
	
	@And("I am a member of the group (.+)$")
	public void set_group(String myGroup){
		group = myGroup;
	}
	
	@Then("The group server shall confirm it received the file successfully")
	public void group_server_received_file_check(){
		UserToken token = new Token();
		Mockito.when(m_fc.upload(file_loc, file_dest, group, token)).thenReturn(true);
		confirm = m_fc.upload(file_loc, file_dest, group, token);
		assertTrue(confirm.equals(true));
	}
	
}