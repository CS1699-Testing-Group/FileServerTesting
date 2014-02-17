package Testing;

import static org.junit.Assert.*;

import java.awt.List;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;
import Main.*;


public class FileListTest {

	@Before
	public void setUp() throws Exception {

		
	}

	@After
	public void tearDown() throws Exception {
	}
    /*
     * Tests adding a file to the list
     * ADM
     */
	@Test
	public void testAddFile() 
	{
		FileList f = new FileList();
		f.addFile("TestOwner", "TestGroup", "TestPath");
		if(f.list.size() != 1)
		{
			fail();
		}
		ShareFile sf = f.list.get(0);
		assertEquals(sf.getGroup(),"TestGroup");
		assertEquals(sf.getPath() , "TestPath");
		assertEquals(sf.getOwner() ,"TestOwner");
	}
	/*
	 * Tests removing a file from the list
	 * ADM
	 */
	@Test
	public void testRemoveFile()
	{
		FileList f = new FileList();
		ShareFile sf = new ShareFile("Test","Test","TestPath");
		f.list.add(sf);
		f.removeFile("TestPath");
		assertEquals(f.list.size(), 0);
				
	}
	/*
	 * Tests checking a file in the list
	 * ADM
	 */
	@Test
	public void testCheckFile()
	{
		FileList f = new FileList();
		ShareFile sf = new ShareFile("Test","Test","TestPath");
		f.list.add(sf);
		assertEquals(f.checkFile("TestPath"), true);
	}
	/*
	 * Test listing file in sorted order
	 * ADM
	 */
	@Test
	public void testGetFiles()
	{
		FileList f = new FileList();
		
		ShareFile sfa = new ShareFile("TestA","TestA","a");
		ShareFile sfb = new ShareFile("TestB","TestB","b");
		ShareFile sfc = new ShareFile("TestC","TestC","c");
		ShareFile sfd = new ShareFile("TestD","TestD","d");
		
		//Unsorted add order
		f.list.add(sfc);
		f.list.add(sfb);
		f.list.add(sfd);
		f.list.add(sfa);
		
		ArrayList<ShareFile> sfl = f.getFiles();
		
		if(sfl.size() != 4) fail();
		
		assertEquals(sfl.get(0),sfa);

		assertEquals(sfl.get(1),sfb);

		assertEquals(sfl.get(2),sfc);

		assertEquals(sfl.get(3),sfd);

	}
	/*
	 * Tests getting file from a file path
	 * ADM
	 */
	@Test
	public void testGetFile()
	{
		String s = "testPath";
		ShareFile sf = new ShareFile("Test","Test",s);
		FileList f = new FileList();
		f.list.add(sf);
		ShareFile ret = f.getFile(s);

		assertEquals(sf,ret);
	}
	
	

}
