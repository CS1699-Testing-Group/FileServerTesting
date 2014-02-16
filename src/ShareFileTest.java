import static org.junit.Assert.*;
import java.util.ArrayList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ShareFileTest{
	String group = "testGroup";
	String owner = "testAdmin";
	String path = "test/path";
	
	String testPathA = "test/path";
	String testPathB = "bad/path";
	
	int i = 1;
	
	ShareFile sf = new ShareFile(owner, group, path);
	
	@Test	//Checks return value of ShareFile's Group. Tested by MDS
	public void testShareFileGetGroup(){
		String myGroup = sf.getGroup();
		assertTrue(myGroup.equals(group));
	}
	
	@Test	//Checks return value of ShareFile's Owner. Tested by MDS
	public void testShareFileGetOwner(){
		String myOwner = sf.getOwner();
		assertTrue(myOwner.equals(owner));
	}
	
	@Test	//Checks return value of ShareFile's File Path. Tested by MDS
	public void testShareFileGetPath(){
		String myPath = sf.getPath();
		assertTrue(myPath.equals(path));
	}
	
	@Test	//Tests the compareTo function of ShareFile. Tested by MDS
	public void testShareFilePathComparison(){
		
		ShareFile goodCompareSF = new ShareFile(owner, group, testPathA);
		assertFalse(i == sf.compareTo(goodCompareSF)); //strings are equal
		ShareFile badCompareSF = new ShareFile(owner,group,testPathB);
		assertTrue(i == sf.compareTo(badCompareSF)); //strings are not equal
	}
	
	
}