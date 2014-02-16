package Testing;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import com.sun.xml.internal.bind.v2.schemagen.xmlschema.List;

import Main.Token;
import Main.UserList;
import Main.UserList.User;
import Main.UserToken;

//this test class validates updation of two Hashtables that serve as a database for group membership in the groupserver system
public class UserListTest {
	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
	UserList userList;
	
	@Mock
	UserList mockedUserList = Mockito.mock(UserList.class);
	
	@Before //the setup creates a new, empty instance of the group server database system
	public void setUp() throws Exception {
		userList = new UserList();
		MockitoAnnotations.initMocks(mockedUserList);
		System.setOut(new PrintStream(outContent));
	    System.setErr(new PrintStream(errContent));
	}

	@After
	public void tearDown() throws Exception {
		System.setOut(null);
	    System.setErr(null);
	}
	
	@Test //tests functionality of checking the system for a username that doesn't exist
	public void CheckUserTest1(){
		assertFalse(userList.checkUser("John"));
	}
	@Test //tests functionality of checking the system for a username that exists
	public void CheckUserTest2(){
		userList.addUser("John");
		assertTrue(userList.checkUser("John"));
	}
	
	@Test //tests adding of a user to the system. USES a mocked instance of the USER class because it is not important.
	public void AddUserTest(){
		boolean first = userList.checkUser("John");
		if(!first){
			userList.list.put("John", Mockito.mock(Main.UserList.User.class));
			assertTrue(userList.checkUser("John"));
		}
	}
	@Test //test creation and existence of a group on the fresh List
	public void TestCreateGroup(){
		userList.createGroup("John", "JohnsGroup");
		assertTrue(userList.groupExists("JohnsGroup"));
	}
	
	@Test //tests an error given the creation of an already existing group
	public void TestAlreadyCreatedGroup(){
		userList.createGroup("John", "JohnsGroup");
		userList.createGroup("John", "JohnsGroup");
		assertEquals("Already a group with this name\n",outContent.toString());
	}
	
	@Test //creating a group should add the creator to the list of group members. we assert isEmpty is false. -HSB
	public void GetCreatorGroupTest(){
		userList.createGroup("John", "JohnsGroup");
		assertFalse(userList.getGroupMembers("JohnsGroup").isEmpty());
	}
	
	@Test //A group with no members should return an empty arrayList of members. we assert that isEmpty is true. -HSB
	public void GetNullGroupMembersTest(){
		userList.createGroup("John", "JohnsGroup");
		userList.removeMemberFromGroup("John", "JohnsGroup");
		assertTrue(userList.getGroupMembers("JohnsGroup").isEmpty());
	}
	
	@Test //tests adding one user to a created group. -HSB
	public void AddOneUserToGroupTest(){
		userList.addUser("John");
		userList.createGroup("John", "JohnsGroup");
		userList.addGroup("John", "JohnsGroup");
		
		ArrayList<String> UsersInGroup = userList.getGroupMembers("JohnsGroup");
		assertEquals(UsersInGroup.get(0),"John");
	}
	
	@Test //adding user to group he/she is already in should cause an error message -HSB
	public void AddExistingUserToSameGroupTest(){
		userList.addUser("John");
		userList.createGroup("John", "JohnsGroup");
		userList.addGroup("John", "JohnsGroup");
		assertEquals("User is already in the group\n",outContent.toString());
	}
	
	@Test //adding multiple users to a group and check to see if it works -HSB
	public void AddUsersToSameGroupTest(){
		userList.addUser("John");
		userList.addUser("Mary");
		userList.addUser("Mike");
		userList.createGroup("John", "JohnsGroup");
		userList.addGroup("John", "JohnsGroup");
		userList.addGroup("Mike","JohnsGroup");
		userList.addGroup("Mary", "JohnsGroup");
		ArrayList<String>expected = new ArrayList<String>();
		expected.add("John");
		expected.add("Mike");
		expected.add("Mary");
		
		assertEquals(expected,userList.getGroupMembers("JohnsGroup"));
	}
	
	
	@Test //tests the deleteUser method, which should remove said user from the userList
	public void DeleteUserTest1(){
		boolean first = userList.checkUser("John");
		if(!first){
			userList.addUser("John");
		}
		userList.deleteUser("John");
		assertFalse(userList.checkUser("John"));
	}
	
	@Test //simple test to see if deleting a user removes him from a group to which he/she belongs -HSB
	public void DeleteUserTest(){
		userList.addUser("John");
		userList.createGroup("John", "JohnsGroup");
		userList.deleteUser("John");
		ArrayList<String> expected = new ArrayList<String>();
		assertEquals(expected,userList.getGroupMembers("JohnsGroup"));
	}

	@Test //checks if a user who creates a group becomes the owner of that group. uses GetUserOwnership method
			//to compare arrayList values. -HSB
	public void OwnershipTest(){
		userList.addUser("John");
		userList.createGroup("John", "JohnsGroup");
		ArrayList<String> actual = userList.getUserOwnership("John");
		ArrayList<String> expected = new ArrayList<String>();
		expected.add("JohnsGroup");
		assertEquals(expected,actual);
		//This fails because the code we wrote for our project is wrong. yay for finding errors in OUR code!!
	}
	
	@Test //tests dependency between User(getOwnership) method and userList(getUserOwnership) method using mocking and stubs
	public void DependencyTest(){
		User mockeduser = Mockito.mock(UserList.User.class); //mocked version of the user class
		ArrayList<String> expectedList = new ArrayList<String>(); //set up the arraylist we want to see
		expectedList.add("JohnsGroup");
		
		//STUB
		Mockito.when(mockeduser.getOwnership()).thenReturn(expectedList);//calling getownership in the 
																		//User sub-class will return the pre-set list
		
		userList.addUser("John");
		userList.createGroup("John", "JohnsGroup");
		userList.addOwnership("John", "JohnsGroup");
		ArrayList<String> actual = userList.getUserOwnership("John");//call from UserList class returns the pre-set stubbed list
																	//that we specified above
		ArrayList<String> expected = new ArrayList<String>();
		expected.add("JohnsGroup");
		
		assertEquals(expected,actual);
	}
	
	@Test //tests the functionality of the addowner method, which should flag a user as owner of a group.
	public void AddOwnershipTest(){
		userList.addUser("John");
		userList.createGroup("John", "JohnsGroup");
		userList.addOwnership("John", "JohnsGroup");
		
		String actual = userList.getUserOwnership("John").get(0);
		String expected = "JohnsGroup";
		
		assertEquals(actual,expected);
	}
	
	
	
	
}
