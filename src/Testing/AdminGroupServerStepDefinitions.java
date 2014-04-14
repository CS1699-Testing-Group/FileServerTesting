package Testing;

import Main.UserList;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.When;

public class AdminGroupServerStepDefinitions {

	private UserList userlist;

	@Given("I am the administrator")
	public void set_up_with_admin() {
		userlist = new UserList();
		userlist.addUser("Owner");
		userlist.createGroup("Owner", "ADMIN");
		userlist.addOwnership("Owner", "ADMIN");
	}

	@When("I create a new user (.*)")
	public void create_user(String username) {
		userlist.addUser(username);
	}

	@When("I delete user (.*)")
	public void delete_user(String username) {
		userlist.deleteUser(username);
	}

}
