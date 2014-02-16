package Testing;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.Socket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import Main.GroupServer;
import Main.GroupThread;
import Main.UserList;

public class GroupThreadTest {

	//Create a group thread from a mocked GroupServer and mocked Socket
	GroupServer m_gs = mock(GroupServer.class);
	Socket m_s = mock(Socket.class);
	GroupThread m_gt = new GroupThread(m_s, m_gs);
	
	@Before
	public void setUp() throws Exception 
	{

		UserList m_ul = mock(UserList.class);
		
		m_gs.userList = m_ul;
		
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void getMembersTest() {
		

	}

}
