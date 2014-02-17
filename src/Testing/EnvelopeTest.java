package Testing;
import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import Main.Envelope;

public class EnvelopeTest {
	
	int i = 0;
	
	@Test	//Tests envelope to make sure received value is equal to entered value. 
			//Test by MDS
	public void testEnvelopeGetMessage(){
		String testString = "hello";
		String getString;
		Envelope env = new Envelope(testString);
		getString = env.getMessage();
		assertTrue(getString.equals(testString));
	}
	
	@Test	//Tests adding objects to an envelope. Makes sure that added Objects are actually added. 
			//Tested by MDS
	public void testEnvelopeAddObjects(){
		String testString = "hello";
		Object obj = new Object();
		Envelope env = new Envelope(testString);
		env.addObject(obj);
		assertNotNull(env.getObjContents());
	}
	
	@Test	//Tests adding objects to the envelope and counts an exact number of those objects. Compares envelope's
			//count to actual count.																
			//Tested by MDS
	public void testEnvelopeAddObjectsSize(){
		String testString = "hello";
		Envelope env = new Envelope(testString);
		Object obj1 = new Object();
		Object obj2 = new Object();
		Object obj3 = new Object();
		Object obj4 = new Object();
		Object obj5 = new Object();
		
		env.addObject(obj1); i++;
		env.addObject(obj2); i++;
		env.addObject(obj3); i++;
		env.addObject(obj4); i++;
		env.addObject(obj5); i++;
		
		assertTrue (i == env.getObjContents().size());
	}
	
	@Test	//Tests returning elements from empty envelope's object list to make sure it returns no non-added elements
			//Tested by MDS
	public void testEnvelopeReturnNull(){
		String testString = "hello";
		Envelope env = new Envelope(testString);
		assertTrue(env.getObjContents().isEmpty());
	}
	
	
}
