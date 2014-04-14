package Testing;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import Main.*;

import org.mockito.Mockito;
import org.mockito.Mockito.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FileClientSubInterfaceTest {

	FileClientSubInterface FCSI = new FileClientSubInterface();
	FileClient FC = Mockito.mock(FileClient.class);
	
	//Record outputs
	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
	
	@Before
	public void setUp() throws Exception {
		FCSI.fileClient = FC; 
		System.setOut(new PrintStream(outContent));
		System.setErr(new PrintStream(errContent));
	}

	@After
	public void tearDown() throws Exception 
	{
		
	}
	/*
	 * Tests quitting from FileClientSubInterface by selection option "5" in the menu
	 * ADM
	 */
	@Test
	public void testQuit() 
	{
		
		String inputText = "5\n";
		ByteArrayInputStream in = new ByteArrayInputStream(inputText.getBytes());
		System.setIn(in);
		String o= "";
		
		Mockito.when(FC.connect(o, 0)).thenReturn(true);
		UserToken t = Mockito.mock(UserToken.class);
		
		FCSI.startFileInterface(o, 0, t);
		
		Mockito.verify(FC, Mockito.atLeastOnce()).disconnect();
		assertTrue(outContent.toString().contains("Exiting File Client"));
		
	}
	/*
	 * Tests listing files (1 in the menu) 
	 * ADM
	 */
	@Test
	public void testListFiles() 
	{
		String inputText = "1\n5\n";
		ByteArrayInputStream in = new ByteArrayInputStream(inputText.getBytes());
		System.setIn(in);
		String o= "";
		
		Mockito.when(FC.connect(o, 0)).thenReturn(true);
		UserToken t = Mockito.mock(UserToken.class);
		
		FCSI.startFileInterface(o, 0, t);
		
		assertTrue(outContent.toString().contains("You seem to have no files available."));
		
	}
	/*
	 * Tests file upload (2 in menu)
	 * ADM
	 */
	@Test
	public void fileUploadTest()
	{
		
		String fileName = "TestFileSrc";
		String destFile = "TestFileSrv";
		String groupName = "TestGroup";
		String inputText = "2\n" + fileName + "\n" + destFile  + "\n" + groupName  + "\n5\n";
		UserToken t = Mockito.mock(UserToken.class);
		
		ByteArrayInputStream in = new ByteArrayInputStream(inputText.getBytes());
		System.setIn(in);
		String o= "";
		
		Mockito.when(FC.connect(o, 0)).thenReturn(true); //Stub connection
		Mockito.when(FC.upload(fileName, destFile, groupName, t)).thenReturn(true); //Stub upload
		
		
		FCSI.startFileInterface(o, 0, t);
		
		Mockito.verify(FC, Mockito.atLeastOnce()).upload(fileName, destFile, groupName, t);
		assertTrue(outContent.toString().contains("Upload request accepted!"));
	
	}
	/*
	 * Tests file download (3 in menu)
	 * ADM
	 */
	@Test
	public void fileDownloadTest()
	{
		String fileName = "TestFileSrc";
		String destFile = "TestFileSrv";

		String inputText = "3\n" + fileName + "\n" + destFile   + "\n5\n";
		UserToken t = Mockito.mock(UserToken.class);
		
		ByteArrayInputStream in = new ByteArrayInputStream(inputText.getBytes());
		System.setIn(in);
		String o= "";
		
		Mockito.when(FC.connect(o, 0)).thenReturn(true); //Stub connection
		Mockito.when(FC.download(fileName, destFile, t)).thenReturn(true); //Stub upload
		
		
		FCSI.startFileInterface(o, 0, t);
		
		Mockito.verify(FC, Mockito.atLeastOnce()).download(fileName, destFile, t);
		assertTrue(outContent.toString().contains("Download request accepted!"));
	}
	/*
	 * Tests file deletion (4 in menu)
	 * ADM
	 */
	@Test
	public void testDeleteFile()
	{
		String fileName = "TestFileSrc";

		String inputText = "4\n" + fileName  + "\n5\n";
		UserToken t = Mockito.mock(UserToken.class);
		
		ByteArrayInputStream in = new ByteArrayInputStream(inputText.getBytes());
		System.setIn(in);
		String o= "";
		
		Mockito.when(FC.connect(o, 0)).thenReturn(true); //Stub connection
		Mockito.when(FC.delete(fileName, t)).thenReturn(true); //Stub upload
		
		
		FCSI.startFileInterface(o, 0, t);
		
		Mockito.verify(FC, Mockito.atLeastOnce()).delete(fileName, t);
		assertTrue(outContent.toString().contains("Delete request accepted!"));
	}
	/*
	 * Tests simulated connection error
	 * ADM
	 */
	@Test
	public void testConnError()
	{

		String o= "";
		
		Mockito.when(FC.connect(o, 0)).thenReturn(false);
		UserToken t = Mockito.mock(UserToken.class);
		
		FCSI.startFileInterface(o, 0, t);
		
		assertTrue(outContent.toString().contains("Error Connecting to the File Server."));

	}
	

}
