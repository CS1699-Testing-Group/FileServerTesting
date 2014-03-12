Feature: 
	As a registered user of the group server
	I want to upload a file to the file server
	So that others in my respective group may download it

  @FileServer
  Scenario: FileShare
    Given I want to upload a file to the file server
    When The file is located at the directory myfile/thisfile
    And The file destination is located at the directory testersfile/thisfile
    And I am a member of the group Testers
    And I send the file to the file server
    Then The file shall be added to the file server's database
    
  @FileServer
  Scenario: FileDownload
	Given I want to download a file from my group
	When The file I want to download is groupfiles/thisfile
	And I am a member of the group Testers
	Then I shall be able to verify the file is downloaded to my computer