package Testing;
//Test by MDS
import cucumber.api.java.en.*;
import static org.junit.Assert.*;

import org.mockito.*;

import Main.FileClient;
import Main.Token;
import Main.UserToken;

public class FileDownloadFailStepDefinitions{
	
	Boolean confirm = false;
	String file_source = "";
	String file_dest = "";
	String group = "";
	
	@Mock
	FileClient m_fc; 
	
	@Given("I am going to attempt to download a file that does not exist")
	public void start_bad_download_process(){
		m_fc = Mockito.mock(FileClient.class);
	}
	
	@When("The file I want to download is said to be at the directory (.+)$")
	public void choose_bad_file_source(String mySource){
		file_source = mySource;
	}
	
	@And("I want to attempt to download the file to the directory (.+)$")
	public void choose_bad_file_dest(String myDestination){
		file_dest = myDestination;
	}
	
	@Then("I shall receive an invalid code from the server")
	public void confirm_bad_download(){
		UserToken token = new Token();
		Mockito.when(m_fc.download(file_source, file_dest, token)).thenReturn(false);
		confirm = m_fc.download(file_source, file_dest, token);
		assertTrue(confirm.equals(false));
	}
	
}