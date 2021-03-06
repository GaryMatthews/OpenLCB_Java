package org.openlcb.implementations;

import org.openlcb.*;

import org.junit.*;

/**
 * @author  Bob Jacobsen   Copyright 2012
 */
public class DatagramMeteringBufferTest {
    
    NodeID hereID = new NodeID(new byte[]{1,2,3,4,5,6});
    NodeID farID  = new NodeID(new byte[]{1,1,1,1,1,1});
    
    int[] data;

    java.util.ArrayList<Message> repliesReturned1;
    Connection replyConnection1;

    Connection returnConnection;

    java.util.ArrayList<Message> messagesForwarded;
    Connection forwardConnection;
        
    DatagramMeteringBuffer buffer;
    
    DatagramMessage datagram1;
    DatagramMessage datagram2;
    
    DatagramAcknowledgedMessage replyOK;
    DatagramRejectedMessage replyNAKresend;

    @Before
    public void setUp() {

        repliesReturned1 = new java.util.ArrayList<Message>();
        replyConnection1 = new AbstractConnection(){
            public void put(Message msg, Connection sender) {
                repliesReturned1.add(msg);
            }
        };

        messagesForwarded = new java.util.ArrayList<Message>();
        forwardConnection = new AbstractConnection(){
            public void put(Message msg, Connection sender) {
                messagesForwarded.add(msg);
                Assert.assertEquals(returnConnection, sender);
            }
        };

        data = new int[32];
        
        buffer = new DatagramMeteringBuffer(forwardConnection);
        
        returnConnection = buffer.connectionForRepliesFromDownstream();

        data[0] = 1;
        datagram1   = new DatagramMessage(hereID, farID, data);                                        
        data[0] = 2;
        datagram2   = new DatagramMessage(hereID, farID, data);                                        

        replyOK     = new DatagramAcknowledgedMessage(farID, hereID);
        replyNAKresend = new DatagramRejectedMessage(farID, hereID, 0x210);
    }

    @Test
    public void testSend() {
        buffer.put(datagram1, replyConnection1);

        buffer.waitForSendQueue();
    }

    @Test
    public void testSendNonDatagramGoesThrough() {
        Message m = new InitializationCompleteMessage(hereID);
        buffer.put(m, replyConnection1);

        buffer.waitForSendQueue();
        Assert.assertEquals("forwarded messages", 1, messagesForwarded.size());
        Assert.assertTrue(messagesForwarded.get(0).equals(m));        
    }

    @Test
    public void testFirstDatagramSendGoesThrough() {
        buffer.put(datagram1, replyConnection1);

        buffer.waitForSendQueue();
        Assert.assertEquals("forwarded messages", 1, messagesForwarded.size());
        Assert.assertTrue(messagesForwarded.get(0).equals(datagram1));        
    }

    @Test
    public void testSendReplyOK() {
        buffer.put(datagram1, replyConnection1);

        buffer.waitForSendQueue();

        Assert.assertEquals("forwarded messages", 1, messagesForwarded.size());
        Assert.assertTrue(messagesForwarded.get(0).equals(datagram1));

        returnConnection.put(replyOK, null);

        // The metering buffer does not forward messages to upstream.
        Assert.assertEquals("reply messages", 0, repliesReturned1.size());

        // After an OK a NAK will not do anything.
        returnConnection.put(replyNAKresend, null);
        Assert.assertEquals("forwarded messages", 1, messagesForwarded.size());
    }

    @Test
    public void testSendReplyNakRetransmit() {
        buffer.put(datagram1, replyConnection1);

        buffer.waitForSendQueue();

        returnConnection.put(replyNAKresend, null);

        Assert.assertEquals("forwarded messages", 2, messagesForwarded.size());
        Assert.assertTrue(messagesForwarded.get(0).equals(datagram1));
        Assert.assertTrue(messagesForwarded.get(1).equals(datagram1));
    }

    @Test
    public void testSendReplyNakRetransmitreplyOK() {
        buffer.put(datagram1, replyConnection1);

        buffer.waitForSendQueue();

        returnConnection.put(replyNAKresend, null);

        Assert.assertEquals("forwarded messages", 2, messagesForwarded.size());
        Assert.assertTrue(messagesForwarded.get(1).equals(datagram1));        

        returnConnection.put(replyOK, null);

        assertSendReady();
    }

    private void assertSendReady() {
        int pastSendMsg = messagesForwarded.size();
        buffer.put(datagram2, replyConnection1);

        buffer.waitForSendQueue();

        Assert.assertEquals("forwarded new datagram",pastSendMsg + 1, messagesForwarded.size());
        Assert.assertEquals("new forward", datagram2, messagesForwarded.get(messagesForwarded.size() - 1));
    }

    private void assertSendBusy() {
        int pastSendMsg = messagesForwarded.size();
        buffer.put(datagram2, replyConnection1);

        buffer.waitForSendQueue();

        Assert.assertEquals("not forwarded new datagram",pastSendMsg, messagesForwarded.size());
    }

    @Test
    public void testSendReplyOtherNakNoInterfere() {
        buffer.put(datagram1, replyConnection1);

        buffer.waitForSendQueue();

        Message otherReply = new DatagramRejectedMessage(farID, farID, 0x210);
        returnConnection.put(otherReply, null);
        
        Assert.assertEquals("forwarded messages", 1, messagesForwarded.size());

        otherReply = new DatagramAcknowledgedMessage(farID, farID);
        returnConnection.put(otherReply, null);
        
        Assert.assertEquals("forwarded messages", 1, messagesForwarded.size());

        returnConnection.put(replyNAKresend, null);

        Assert.assertEquals("forwarded messages", 2, messagesForwarded.size());
        Assert.assertTrue(messagesForwarded.get(0).equals(datagram1));
        Assert.assertTrue(messagesForwarded.get(1).equals(datagram1));

        returnConnection.put(replyOK, null);

        assertSendReady();
    }

    @Test
    public void testSendTwoNonDatagramGoesThrough() {
        Message m = new InitializationCompleteMessage(hereID);
        buffer.put(m, replyConnection1);
        buffer.put(m, replyConnection1);

        buffer.waitForSendQueue();
        Assert.assertEquals("forwarded messages", 2, messagesForwarded.size());
        Assert.assertTrue(messagesForwarded.get(0).equals(m));        
        Assert.assertTrue(messagesForwarded.get(1).equals(m));        
    }

    @Test
    public void testSendTwoBeforeReply() {
        buffer.put(datagram1, replyConnection1);
        buffer.put(datagram2, replyConnection1);

        buffer.waitForSendQueue();

        Assert.assertEquals("forwarded messages", 1, messagesForwarded.size());
        Assert.assertTrue(messagesForwarded.get(0).equals(datagram1));        
        
        // now send the reply
        
        returnConnection.put(replyOK, null);

        buffer.waitForSendQueue();

        Assert.assertEquals("forwarded messages", 2, messagesForwarded.size());
        Assert.assertTrue(messagesForwarded.get(1).equals(datagram2));        
    }

    @After
    public void tearDown() {
        buffer.dispose(); 
        repliesReturned1 = null; 
        replyConnection1 = null;
        messagesForwarded = null;
        forwardConnection = null;
        data = null;
        buffer = null;
        returnConnection = null;
        datagram1 = null;
        datagram2 = null;                                        
        replyOK = null;
        replyNAKresend = null;
    }
}
