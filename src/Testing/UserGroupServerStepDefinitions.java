package Testing;
import java.io.ByteArrayOutputStream;

import Main.UserList;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import static org.junit.Assert.*;
import java.io.PrintStream;

public class UserGroupServerStepDefinitions {

	private UserList userlist;
	private String currentUser;
	private String currentGroupname;
	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
	
	
    
    @Given("^I am logged into the server with a registered username (.*)$")
    public void set_up_with_username(String username) {
        userlist = new UserList();
        userlist.addUser(username);
        currentUser = username;
    	streamSetUp();
    }
    
    @Given("(.*) is the owner of the group(.*)")
    public void give_ownership(String username, String groupname){
    	userlist.addOwnership(username, groupname);
    }
    
    @Given("user (.*) exists")
    public void establish_user(String username){
    	userlist.addUser(username);
    }
    
    @Given("group (.*) exists")
    public void establish_group(String groupname){
    	userlist.createGroup(currentUser, groupname);
    	currentGroupname = groupname;
    }
    
    @Given("group (.*) does not exist$")
    public void group_doesnt_exist(String groupname){
    	userlist = new UserList();
    	streamSetUp();
    }
  
    @Given("(.*) is in the group (.*)")
    public void user_exists_in_group(String username, String groupname){
    	if(!userlist.checkUser(username)){
    		userlist.addUser(username);
    	}
    	userlist.addGroup(username, groupname);
    }

    @When("^I create a group called (.*)$")
    public void create_group_request(String groupname) {    	
        userlist.createGroup(currentUser, groupname);
        currentGroupname = groupname;
    }
   
    @When("^I grant (.*) ownership of (.*)$")
    public void grant_ownership(String username, String groupname){
    	userlist.addOwnership(username, groupname);
    }
    @When("I delete a group (.*)")
    public void delete_group(String groupname){
    	userlist.deleteGroup(groupname);
    }
    
    @When("^I remove (.*) from the group (.*)")
    public void remove_user_from_group(String username, String groupname){
    	userlist.removeMemberFromGroup(username, groupname);
    }
  
    @Then("^(.*) is added to the database$")
    public void verify_group_added(String groupname) {
        assertTrue(userlist.groupExists(groupname));
    }
    
    @Then("^an error message should display:(.*)")
    public void error_message_duplicate(String errorMessage){
    	assertEquals(errorMessage,outContent.toString());
    }
    
    @Then("(.*) should be the owner of (.*)")
    public void check_owner(String username,String groupname){
    	boolean flag = false;
    	String match = "";
    	
    	for(int i = 0; i < userlist.getUserOwnership(username).size();i++){
    		if(userlist.getUserOwnership(username).get(i).equals(groupname)){
    			flag = true;
    			match = userlist.getUserOwnership(username).get(i);
    		}
    	}
    	if(flag == false){
    		fail();
    	}else{
    		assertEquals(match,groupname);
    	}
    }
    
    @Then("(.*) should not be in the list of members for group (.*)")
    public void checkRemoved(String username, String groupname){
    	//list of groups user belongs to does not match the group they were deleted from
    	assertFalse(userlist.getUserGroups(username).contains(groupname));
    }
    
    @Then("group (.*) is deleted")
    public void checkDeleted(String groupname){
    	assertFalse(userlist.groupExists(groupname));
    }
        
    @Then("user (.*) shows up")
    public void check_user_exists(String username){
    	assertTrue(userlist.checkUser(username));
    }
    
    @Then("user (.*) should not show up")
    public void check_user_doesnt_exist(String username){
    	assertFalse(userlist.checkUser(username));
    }
    
    @Then("group (.*) should not contain user (.*)")
    public void check_user_deleted_from_group(String groupname,String username){
    	assertFalse(userlist.getUserGroups(username).contains(groupname));
    }
    
    public void streamSetUp(){
    	System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

}
