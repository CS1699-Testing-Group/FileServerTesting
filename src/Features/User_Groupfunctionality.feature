Feature: 
	As a registered user of the group server
	I want to enjoy group server functionality
	So that I can share files with my friends in a secure way

  @UseGroup
  Scenario: CreateGroup
	Given I am logged into the server with a registered username John
    When I create a group called Johnsgroup
    Then Johnsgroup is added to the database
    
  @UseGroup
  Scenario: Trying to create a group with already existing name
  	Given I am logged into the server with a registered username John
    When I create a group called Johnsgroup
    And I create a group called Johnsgroup
    Then an error message should display:Already a group with this name
    
  @UseGroup
  Scenario: Granting a user ownership of a group
   Given I am logged into the server with a registered username John
   When I create a group called Johnsgroup
   And I grant John ownership of Johnsgroup
   Then John should be the owner of Johnsgroup
   
   @UseGroup
   Scenario: Adding a user to owned group
   	Given I am logged into the server with a registered username John
   	When I create a group called Johnsgroup
   	And I grant John ownership of Johnsgroup
   	Then I can add a friend to Johnsgroup
   
   @UseGroup
   Scenario: Removing a user from an owned group
   	Given I am logged into the server with a registered username John
   	And group Johnsgroup exists
   	And John is the owner of the group Johnsgroup
   	And Mary is in the group Johnsgroup
   	When I remove Mary from the group Johnsgroup
   	Then Mary should not be in the list of members for group Johnsgroup
   	
   @UseGroup
   Scenario: Deleting a group that exists
	Given I am logged into the server with a registered username John
   	And group Johnsgroup exists
   	And John is the owner of the group Johnsgroup
   	When I delete a group Johnsgroup
   	Then group Johnsgroup is deleted from the database
   	
   @UseGroup
   Scenario: Deleting a group that does not exist
   	Given group Johnsgroup does not exist
   	When I delete a group Johnsgroup
   	Then an error message should display:Group doesn't exist

   
   	

  