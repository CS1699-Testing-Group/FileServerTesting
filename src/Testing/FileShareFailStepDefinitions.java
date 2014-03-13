package Testing;
//Test by MDS
import cucumber.api.java.en.*;
import static org.junit.Assert.*;

import org.mockito.*;

import Main.FileClient;
import Main.UserToken;
import Main.Token;

public class FileShareFailStepDefinitions{
	
	Boolean confirm = false;
	String file_loc;
	String file_dest;
	String group;
	
	@Mock
	FileClient m_fc; 
	
	@Given("I am going to attempt to upload a file that does not exist")
	public void a_user_shares_a_bad_file(){
		m_fc = Mockito.mock(FileClient.class);
	}
	
	@When("The bad file directory is given as (.+)$")
	public void set_bad_file_location(String myLocation){
		file_loc = myLocation;
	}
	
	@And("The bad file destination is located at the directory (.+)$")
	public void set_bad_file_destination(String myDestination){
		file_dest = myDestination;
	}
	
	@And("I am a member of group (.+)$")
	public void set_my_group(String myGroup){
		group = myGroup;
	}
	
	@Then("The file server shall fail the upload since the file does not exist")
	public void file_server_received_bad_file_check(){
		UserToken token = new Token();
		Mockito.when(m_fc.upload(file_loc, file_dest, group, token)).thenReturn(false);
		confirm = m_fc.upload(file_loc, file_dest, group, token);
		assertTrue(confirm.equals(false));
	}
	
}