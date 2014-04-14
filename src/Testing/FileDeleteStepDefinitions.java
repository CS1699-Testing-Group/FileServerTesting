package Testing;
//Test by MDS
import cucumber.api.java.en.*;
import static org.junit.Assert.*;

import org.mockito.*;

import Main.FileClient;
import Main.Token;
import Main.UserToken;

public class FileDeleteStepDefinitions{
	
	Boolean confirm = false;
	String file_name;

	
	@Mock
	FileClient m_fc; 
	
	@Given("I am going to delete a file from the file server")
	public void a_user_shares_a_file(){
		m_fc = Mockito.mock(FileClient.class);
	}
	
	@When("I give the file server the name of an existing file (.+)$")
	public void set_file_location(String myFile){
		file_name = myFile;
	}
	
	@Then("The file shall be removed from the file server")
	public void group_server_received_file_check(){
		UserToken token = new Token();
		Mockito.when(m_fc.delete(file_name, token)).thenReturn(true);
		confirm = m_fc.delete(file_name, token);
		assertTrue(confirm.equals(true));
	}
	
}