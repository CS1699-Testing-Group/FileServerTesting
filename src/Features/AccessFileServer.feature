Feature:
	As a registered user of the group server
	I want to manage files on the file server
	So that I may access files and others may access my files
	
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
  	
  @FileServer
  Scenario: Upload a non-existent file to the file server, possibly on accident
  	Given I am going to attempt to upload a file that does not exist
  	When The bad file directory is given as myfile/does_not_exist
  	And The file destination is located at the directory testersfile/thisfile
  	And I am a member of group Testers
  	Then The file server shall fail the upload since the file does not exist

  @FileServer	
  Scenario: Attempt to download an invalid file from the file server, possibly on accident
  	Given I am going to attempt to download a file that does not exist
  	When The file I want to download is said to be at the directory testersfile/does_not_exist
  	And I want to attempt to download the file to the directory myfile/thisfile
  	Then I shall receive an invalid code from the server
  	
  @FileServer
  Scenario: Delete a file from the file server
  	Given I am going to delete a nonexistent file from the file server
  	When I give the file server the name of a bad file myfile
  	Then The server will warn me of the error
  	