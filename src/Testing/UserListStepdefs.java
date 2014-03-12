package Testing;


import Main.UserList;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import static org.junit.Assert.*;

public class UserListStepdefs {

	private UserList userlist;
	private String currentUser;
	
    @Given("^I am logged in with username (.*)$")
    public void set_up_with_username(String username) {
        userlist = new UserList();
        userlist.addUser(username);
        currentUser = username;
    }

    @When("^I want to create a group called (.*)$")
    public void create_group_request(String groupname) {    	
        userlist.createGroup(currentUser, groupname);
    }

    @Then("^(.*) should be added to the database$")
    public void verify_group_added(String groupname) {
        assertTrue(userlist.groupExists(groupname));
    }

}
