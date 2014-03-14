Feature:
	As an administrator of the group server
	I want the ability to create and delete users
	So that I have control over who is in the system
	
  @Admin	
  Scenario: Creating a user
  	Given I am the administrator of the Group Server
    When I create a new user Mary
    Then user Mary shows up in the user list
    
  @Admin
  Scenario: Deleting a user
  	Given I am the administrator of the Group Server
  	And user Mary exists
  	When I delete user Mary
  	Then user Mary should not show up in the user list  	
   
  @Admin
  Scenario: Deleting a user who is the only member of a group should remove them from all groups
  	Given I am logged into the server with a registered username John
	And group Johnsgroup exists
	And user John is in the group Johnsgroup
	When I delete user John
	Then group Johnsgroup should not contain user John