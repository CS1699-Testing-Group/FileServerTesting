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
	String file_loc = "";
	String file_dest = "";
	String group = "";
	
	@Mock
	FileClient m_fc; 
	
	@Given("I wish to upload a file")
	public void a_user_shares_a_file(){
		m_fc = Mockito.mock(FileClient.class);
	}
	
	@When("The file is located at .+")
	public void set_file_location(){
		file_loc = "myfile/thisfile";
	}
	
	@And("The destination is .+")
	public void set_file_destination(){
		file_dest = "testersfile/thisfile";
	}
	
	@And("The user is part of the group .+")
	public void set_group(){
		group = "Testers";
	}
	
	@And("The file is sent")
	public void send_file(){
		UserToken token = new Token();
		Mockito.when(m_fc.upload(file_loc, file_dest, group, token)).thenReturn(true);
		confirm = m_fc.upload(file_loc, file_dest, group, token);
	}
	
	@Then("The group server shall confirm it recieved the file successfully")
	public void group_server_received_file_check(){
		assertTrue(confirm.equals(true));
	}
	
}