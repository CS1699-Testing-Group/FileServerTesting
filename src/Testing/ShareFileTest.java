package Testing;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import Main.ShareFile;
import Main.UserList;
import Main.UserList.User;


public class ShareFileTest{
	
	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
	
	String group = "testGroup";
	String owner = "testAdmin";
	String path = "test/path";
	
	String testPathA = "test/path";
	String testPathB = "bad/path";
	
	int i = 1;
	
	ShareFile sf = new ShareFile(owner, group, path);
	
	@Test	//Checks return value of ShareFile's Group. 
			//Tested by MDS
	public void testShareFileGetGroup(){
		String myGroup = sf.getGroup();
		assertTrue(myGroup.equals(group));
	}
	
	@Test	//Checks return value of ShareFile's Owner. 
			//Tested by MDS
	public void testShareFileGetOwner(){
		String myOwner = sf.getOwner();
		assertTrue(myOwner.equals(owner));
	}
	
	@Test	//Checks return value of ShareFile's File Path is correctly saved. 
			//Tested by MDS
	public void testShareFileGetPath(){
		String myPath = sf.getPath();
		assertTrue(myPath.equals(path));
	}
	
	@Test	//Tests the compareTo function of ShareFile. 
			//Tested by MDS
	public void testShareFilePathComparison(){
		
		ShareFile goodCompareSF = new ShareFile(owner, group, testPathA);
		assertFalse(i == sf.compareTo(goodCompareSF)); //strings are equal
		ShareFile badCompareSF = new ShareFile(owner,group,testPathB);
		assertTrue(i == sf.compareTo(badCompareSF)); //strings are not equal
	}
	
	//Now test values with Mockito to make sure a mocked version of the function will function properly to
	//the tests above
	
	String mockGroup = "mockGroup";
	String mockOwner = "mockOwner";
	String mockPath = "mock/path";
	
	String badString = "Not_a_match";
	
	String mockTestStr = "mockTestStr";
	
	ShareFile shareFile;
	
	@Mock
	ShareFile m_sf = Mockito.mock(ShareFile.class);
	
	@Before
	public void setUp() throws Exception{
		shareFile = new ShareFile(mockOwner, mockGroup, mockPath);
		MockitoAnnotations.initMocks(m_sf);
		System.setOut(new PrintStream(outContent));
	    System.setErr(new PrintStream(errContent));
	}
	
	@After
	public void tearDown() throws Exception{
		System.setOut(null);
	    System.setErr(null);
	}
	
	//Tests mocked version of ShareFile to make sure funcitons work in mocked form
	
	//Tests comparison of Owner via mock 
	//Tested by MDS
	@Test
	public void testMockReturnOwner(){
		String compare = shareFile.getOwner();
		System.out.println(compare);
		assertTrue(compare.equals(mockOwner));
		assertFalse(compare.equals(badString));
		
	}
	//Tests comparison of Group via mock
	//Tested by MDS
	@Test
	public void testMockReturnGroup(){
		String compare = shareFile.getGroup();
		assertTrue(compare.equals(mockGroup));
		assertFalse(compare.equals(badString));
	}
	
	//Tests comparison of Path via mock
	//Tested by MDS
	@Test
	public void testMockReturnPath(){
		String compare = shareFile.getPath();
		assertTrue(compare.equals(mockPath));
		assertFalse(compare.equals(badString));
	}
	
	//Re-running test using STUBS to double check function returns
	//Tests by MDS
	@Test
	public void testMocReturnGroupStub(){
		ShareFile mockedShareFile = Mockito.mock(ShareFile.class);
		//STUB
		Mockito.when(mockedShareFile.getGroup()).thenReturn("Not_a_match");
		String compare = mockedShareFile.getGroup();
		assertTrue(compare.equals(badString));
	}
	
	@Test
	public void testMocReturnOwnerStub(){
		ShareFile mockedShareFile = Mockito.mock(ShareFile.class);
		//STUB
		Mockito.when(mockedShareFile.getOwner()).thenReturn("Not_a_match");
		String compare = mockedShareFile.getOwner();
		assertTrue(compare.equals(badString));
	}
	
	@Test
	public void testMocReturnPathStub(){
		ShareFile mockedShareFile = Mockito.mock(ShareFile.class);
		//STUB
		Mockito.when(mockedShareFile.getPath()).thenReturn("Not_a_match");
		String compare = mockedShareFile.getPath();
		assertTrue(compare.equals(badString));
	}
}
