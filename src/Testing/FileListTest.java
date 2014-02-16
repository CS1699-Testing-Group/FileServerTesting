package Testing;

import static org.junit.Assert.*;

import java.awt.List;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;
import Main.*;


public class FileListTest {

	@Before
	public void setUp() throws Exception {

		
	}

	@After
	public void tearDown() throws Exception {
	}

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
		assert(sf.getGroup() == "TestGroup");
		assert(sf.getPath() == "TestPath");
		assert(sf.getOwner() == "TestOwner");
	}
	
	@Test
	public void testRemoveFile()
	{
		FileList f = new FileList();
		ShareFile sf = new ShareFile("Test","Test","TestPath");
		f.list.add(sf);
		f.removeFile("TestPath");
		assert(f.list.size() == 0);
				
	}
	
	@Test
	public void testCheckFile()
	{
		FileList f = new FileList();
		ShareFile sf = new ShareFile("Test","Test","TestPath");
		f.list.add(sf);
		assert(f.checkFile("TestPath"));
	}
	
	@Test
	public void testGetFiles()
	{
		FileList f = new FileList();
		
		ShareFile sfa = new ShareFile("TestA","TestA","z");
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
		
		assert(sfl.get(0).equals(sfa));
		assert(sfl.get(0).path == "a");
		
		assert(sfl.get(1).equals(sfb));
		assert(sfl.get(1).path == "b");
		
		assert(sfl.get(2).equals(sfc));
		assert(sfl.get(2).path == "c");
		
		assert(sfl.get(3).equals(sfd));
		assert(sfl.get(3).path == "d");
	}
	

}
