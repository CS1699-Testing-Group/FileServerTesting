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

//import com.sun.xml.internal.bind.v2.schemagen.xmlschema.List;

import Main.UserList;
import Main.UserList.User;

//this test class validates updation of two Hashtables that serve as a database for group membership in the groupserver system
public class UserListTest {
	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
	UserList userList;
	
	//initialize the mockeduserList object. Is used below.
	@Mock
	UserList mockedUserList = Mockito.mock(UserList.class);
	
	//creates a new UserList object that will be used for all tests
	//initializes the print streams which are used to compare System.out.println
	//string values with the error messages I expected.
	@Before 
	public void setUp() throws Exception {
		userList = new UserList();
		MockitoAnnotations.initMocks(mockedUserList);
		System.setOut(new PrintStream(outContent));
	    System.setErr(new PrintStream(errContent));
	}
	
	//tear down the print streams
	@After
	public void tearDown() throws Exception {
		System.setOut(null);
	    System.setErr(null);
	}
	
	//tests functionality of checking the system for a username that doesn't exist
	//The list does not have any users, so it should return false 
	//TEST BY: HSB
	@Test 
	public void CheckUserTest1(){
		assertFalse(userList.checkUser("John"));
	}
	//tests functionality of checking the system for a username that exists
	//The list has one user John, so it should return true.
	//TEST BY: HSB
	@Test 
	public void CheckUserTest2(){
		userList.addUser("John");
		assertTrue(userList.checkUser("John"));
	}
	
	//tests adding of a user to the system. USES a mocked instance of the USER class
	//to subvert any dependency issues because the User class is not even used.
	//Adding a user John should work, so I expect the check to be true.
	//TEST BY:HSB
	@Test 
	public void AddUserTest(){
		boolean first = userList.checkUser("John");
		if(!first){
			userList.list.put("John", Mockito.mock(Main.UserList.User.class));
			assertTrue(userList.checkUser("John"));
		}
	}
	//test creation and existence of a group on the fresh List
	//I expect that after creating a group, it should exist (return true)
	//TEST BY:HSB
	@Test 
	public void TestCreateGroup(){
		userList.createGroup("John", "JohnsGroup");
		assertTrue(userList.groupExists("JohnsGroup"));
	}
	
	//tests an error given the creation of an already existing group
	//If a group already exists, creating one with the name prints out, 
	//"Already a group with this name", so we check to see if the printstream
	//reflects this
	//TEST BY: HSB
	@Test 
	public void TestAlreadyCreatedGroup(){
		userList.createGroup("John", "JohnsGroup");
		userList.createGroup("John", "JohnsGroup");
		assertEquals("Already a group with this name",outContent.toString());
	}
	
	//creating a group should add the creator to the list of group members.
	//Getting group members of a group we just created should return the creator in the 
	//ArrayList as the only member, so it shouldn't be empty
	//TEST BY: HSB
	@Test 
	public void GetCreatorGroupTest(){
		userList.createGroup("John", "JohnsGroup");
		assertFalse(userList.getGroupMembers("JohnsGroup").isEmpty());
	}
	
	//A group with no members should return an empty arrayList of members.
	//Removing John from the group where he only exists should leave an empty group,
	//which we assert as true
	//TEST BY: HSB
	@Test
	public void GetNullGroupMembersTest(){
		userList.createGroup("John", "JohnsGroup");
		userList.removeMemberFromGroup("John", "JohnsGroup");
		assertTrue(userList.getGroupMembers("JohnsGroup").isEmpty());
	}
	
	//tests adding one user to a created group
	//The array list from getting group members should only contain John because he 
	//created it. We assert that the first element of the group's members is John
	//TEST BY: HSB
	@Test 
	public void AddOneUserToGroupTest(){
		userList.addUser("John");
		userList.createGroup("John", "JohnsGroup");
		userList.addGroup("John", "JohnsGroup");
		
		ArrayList<String> UsersInGroup = userList.getGroupMembers("JohnsGroup");
		assertEquals(UsersInGroup.get(0),"John");
	}
	
	//adding user to group he/she is already in should cause an error message
	//The error message "User is already in the group" is printed if the user is 
	//already in said group. We expect the System.out.println from the server
	//to match the error message. 
	//TEST BY: HSB
	@Test 
	public void AddExistingUserToSameGroupTest(){
		userList.addUser("John");
		userList.createGroup("John", "JohnsGroup");
		userList.addGroup("John", "JohnsGroup");
		assertEquals("User is already in the group",outContent.toString());
	}
	
	//adding multiple users to a group should populate that groups list with those names
	//We assert that getting the group members of said group should equal the expected 
	//list of members explicitly created
	//TEST BY: HSB
	@Test 
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
	
	//tests the deleteUser method, which should remove said user from the global
	//list of users. Checking a user that doesn't exist in the list should return false.
	//TEST BY: HSB
	@Test 
	public void DeleteUserTest1(){
		boolean first = userList.checkUser("John");
		if(!first){
			userList.addUser("John");
		}
		userList.deleteUser("John");
		assertFalse(userList.checkUser("John"));
	}
	
	//Deleting a user should remove him/her from any groups he/she belongs to. We expect
	//Johns group list to be empty once we remove him from his only group.
	//TEST BY: HSB
	@Test 
	public void DeleteUserTest(){
		userList.addUser("John");
		userList.createGroup("John", "JohnsGroup");
		userList.deleteUser("John");
		ArrayList<String> expected = new ArrayList<String>();
		assertEquals(expected,userList.getGroupMembers("JohnsGroup"));
	}

	//checks if a user who creates a group, then they become owner of that group
	//We assert that the expected array of groups matches the output of the method 
	//getUserOwnership(USER) - only JohnsGroup
	//TEST BY: HSB
	@Test 
	public void OwnershipTest(){
		userList.addUser("John");
		userList.createGroup("John", "JohnsGroup");
		ArrayList<String> actual = userList.getUserOwnership("John");
		ArrayList<String> expected = new ArrayList<String>();
		expected.add("JohnsGroup");
		assertEquals(expected,actual);
		//This fails because the code we wrote for our project is wrong.
	}
	
	//tests dependency between User(getOwnership) method and userList(getUserOwnership)
	//methods. I use a stub to auto-generate a response from the User class, which the 
	//UserList class depends on for the execution of the getUserOwnership method
	//TEST BY: HSB
	@Test 
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
	
	//tests the functionality of the addowner method
	//Adding a user as an owner of a group populates their database entry to reflect this
	//which is replayed back through the getUserOwnership method.
	//TEST BY : HSB
	@Test 
	public void AddOwnershipTest(){
		userList.addUser("John");
		userList.createGroup("John", "JohnsGroup");
		userList.addOwnership("John", "JohnsGroup");
		
		String actual = userList.getUserOwnership("John").get(0);
		String expected = "JohnsGroup";
		
		assertEquals(actual,expected);
	}
	
	
	
	
}
