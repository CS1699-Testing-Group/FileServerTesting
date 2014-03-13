Feature: 
	As a registered user of the group server
	I want to upload a file to the file server
	So that others in my respective group may download it

  @FileServer
  Scenario: Upload a file to the file server
    Given I am going to upload a file to the file server
    When The file is located at the directory myfile/thisfile
    And The file destination is located at the directory testersfile/thisfile
    And I am a member of the group Testers
    Then The group server shall confirm it received the file successfully
    
  @FileServer
  Scenario: Download a file from the file server
	Given I am going to download a file from my group
	When The file I want to download is located at the directory testersfile/thisfile
	And I want to download the file to the directory myfile/thisfile
	Then I shall be able to confirm the file is downloaded to my computer
	
  @FileServer
  Scenario: Delete a file from the file server
  	Given I am going to delete a file from the file server
  	When I give the file server the name of an existing file myfile
  	Then The file shall be removed from the file server