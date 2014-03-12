Feature: 
	As a registered user of the group server
	I want to create a group
	So that I can share files with my friends

  @GroupServer
  Scenario: CreateGroup
    Given I am logged in with username john
    When I want to create a group called johnsgroup
    Then johnsgroup should be added to the database