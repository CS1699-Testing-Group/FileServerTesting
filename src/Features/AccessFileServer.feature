Feature: 
	As a registered user of the group server
	I want to upload a file to the file server
	So that others in my respective group may download it

  @FileServer
  Scenario: FileShare
    Given I am going to upload a file to the file server
    When The file is located at the directory myfile/thisfile
    And The file destination is located at the directory testersfile/thisfile
    And I am a member of the group Testers
    Then The group server shall confirm it recieved the file successfully
    
  @FileServer
  Scenario: FileDownload
	Given I am going to download a file from my group
	When The file I want to download is testersfile/thisfile
	And I want to download the file to myfile/thisfile
	Then I shall be able to confirm the file is downloaded to my computer