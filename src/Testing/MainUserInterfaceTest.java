package Testing;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import Main.*;

import java.lang.System.*;
import java.security.Permission;
import java.util.ArrayList;

public class MainUserInterfaceTest {

	//Record outputs
	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();


	 protected static class ExitException extends SecurityException 
	    {
	        public final int status;
	        public ExitException(int status) 
	        {

	            this.status = status;
	        }
	    }

	    private static class NoExitSecurityManager extends SecurityManager 
	    {
	        @Override
	        public void checkPermission(Permission perm) 
	        {
	            // allow anything.
	        }
	        @Override
	        public void checkPermission(Permission perm, Object context) 
	        {
	            // allow anything.
	        }
	        @Override
	        public void checkExit(int status) 
	        {
	            super.checkExit(status);
	            throw new ExitException(status);
	        }
	    }
    
	@Before
	public void setUp() throws Exception 
	{
		System.setOut(new PrintStream(outContent));
		System.setErr(new PrintStream(errContent));
		System.setSecurityManager(new NoExitSecurityManager());

		
	}

	@After
	public void tearDown() throws Exception 
	{
		
		
	}
	/*
	 * Tests that the program exits correctly when the user chooses "2" in the menu
	 * ADM
	 */

	@Test
	public void quitTest() 
	{
		String inputText = "2\n";
		ByteArrayInputStream in = new ByteArrayInputStream(inputText.getBytes());
		System.setIn(in);
		
		 try 
	        {
			 MainUserInterface.main(null);
	        } catch (ExitException e) 
	        {
	            assertEquals("Exit status", 0, e.status);
	        }
		String out = outContent.toString();
		String testOut = out.substring(out.trim().lastIndexOf("\n"));
		assertEquals(testOut.trim(),"Entry: Shutting down...");
	}
	
	/*
	 * Tests several invalid inputs to the menu
	 * ADM
	 */
	
	@Test
	public void invalidTest()
	{
		ArrayList<String> inputText = new ArrayList<>();
		
		inputText.add("3\n2\n"); //Test invalid number
		inputText.add("A\n2\n"); //Test character
		inputText.add("111111\n2\n"); //Test another invalid number
		
		int test = 0;
		for(String is : inputText )
		{
		ByteArrayInputStream in = new ByteArrayInputStream(is.getBytes());
		System.setIn(in);
		
		 try 
	        {
			 MainUserInterface.main(null);
	        } catch (ExitException e) 
	        {
	            //This was already tested (This is a bit of a workaround because I couldn't figure out any other way to stop the while(true) loop)
	        }
		
		String out = outContent.toString();
		String postEntry = out.substring(out.indexOf("Entry: "));
		String testOut = postEntry.substring(0,postEntry.indexOf("\n")).trim();
		
		assertEquals(testOut.trim(),"Entry: Invalid Choice. Please enter a valid command.");
		test ++;
		}
		assertEquals(test, 3);
		
	
	}

}
