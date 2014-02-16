package Testing;
import static org.junit.Assert.*;

import java.util.List;
import java.util.ArrayList;

import org.junit.Test;

import Main.Token;

public class TokenTest{
	
	String issuer = "tester";
	String subject = "test_subject";
	List <String> groups = new ArrayList<String>();;
	
	Token token = new Token(issuer, subject, groups);
	
	@Test	//Tests to make sure issuer is same string when returned from token. Tested by MDS
	public void testTokenGetIssuer(){
		String myIssuer = token.getIssuer();
		assertTrue(myIssuer.equals(issuer));
	}
	
	@Test	//Tests to make sure subject is same string when returned from token. Tested by MDS
	public void testTokenGetSubject(){
		String mySubject = token.getSubject();
		assertTrue(mySubject.equals(subject));
	}
	
	@Test	//Test to make sure correct number of items is added to groups. Tested by MDS
	public void testTokenAddGroups(){
		int i = 0;
		token.addGroup("Test1"); i++;
		token.addGroup("Test2"); i++;
		token.addGroup("Test3"); i++;
		token.addGroup("Test4"); i++;
		token.addGroup("Test5"); i++;
		assertTrue(i == token.getGroups().size());
	}
	
}