package org.openlcb.implementations;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openlcb.AbstractConnection;
import org.openlcb.Connection;
import org.openlcb.DatagramAcknowledgedMessage;
import org.openlcb.DatagramMessage;
import org.openlcb.DatagramRejectedMessage;
import org.openlcb.InterfaceTestBase;
import org.openlcb.Message;
import org.openlcb.NodeID;
import org.openlcb.Utilities;
import org.openlcb.can.CanFrame;
import org.openlcb.can.GridConnect;
import org.openlcb.can.MessageBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Tests of MemoryCOnfigurationService using the OlcbInterface output concept and proper mocks.
 *
 * @author  Balazs Racz   Copyright 2016
 * @version $Revision: -1 $
 */
public class MemoryConfigurationServiceInterfaceTest extends InterfaceTestBase {

    NodeID hereID = iface.getNodeId();
    NodeID farID = new NodeID(new byte[]{1,2,3,4,5,7});

    public void testCtorViaSetup() {
    }

    public void testSimpleWrite() {
        int space = 0xFD;
        long address = 0x12345678;
        byte[] data = new byte[]{1,2};

        MemoryConfigurationService.McsWriteHandler hnd = mock(MemoryConfigurationService
                .McsWriteHandler.class);

        iface.getMemoryConfigurationService().requestWrite(farID, space, address, data, hnd);
        verifyNoMoreInteractions(hnd);

        expectMessageAndNoMore(new DatagramMessage(hereID, farID, new int[]{
                0x20, 0x01, 0x12, 0x34, 0x56, 0x78, 1, 2}));

        // datagram reply comes back
        sendMessage(new DatagramAcknowledgedMessage(farID, hereID));

        verify(hnd).handleSuccess();
        verifyNoMoreInteractions(hnd);
    }

    public void testSimpleWriteError() {
        int space = 0xFD;
        long address = 0x12345678;
        byte[] data = new byte[]{1,2};

        MemoryConfigurationService.McsWriteHandler hnd = mock(MemoryConfigurationService
                .McsWriteHandler.class);

        iface.getMemoryConfigurationService().requestWrite(farID, space, address, data, hnd);
        verifyNoMoreInteractions(hnd);

        expectMessageAndNoMore(new DatagramMessage(hereID, farID, new int[]{
                0x20, 0x01, 0x12, 0x34, 0x56, 0x78, 1, 2}));

        // datagram reply comes back
        sendMessage(new DatagramRejectedMessage(farID, hereID, 0x1999));

        verify(hnd).handleFailure(0x1999);
        verifyNoMoreInteractions(hnd);
    }

    public void testDelayedWrite() {
        int space = 0xFD;
        long address = 0x12345678;
        byte[] data = new byte[]{1,2};

        MemoryConfigurationService.McsWriteHandler hnd = mock(MemoryConfigurationService
                .McsWriteHandler.class);

        iface.getMemoryConfigurationService().requestWrite(farID, space, address, data, hnd);
        verifyNoMoreInteractions(hnd);

        expectMessageAndNoMore(new DatagramMessage(hereID, farID, new int[]{
                0x20, 0x01, 0x12, 0x34, 0x56, 0x78, 1, 2}));

        // datagram reply comes back
        sendMessage(new DatagramAcknowledgedMessage(farID, hereID, 0x80));
        verifyNoMoreInteractions(hnd);

        // Incoming reply datagram
        sendMessage(new DatagramMessage(farID, hereID, new int[]{0x20, 0x11, 0x12, 0x34, 0x56,
                0x78}));
        // which gets ack-ed.
        expectMessageAndNoMore(new DatagramAcknowledgedMessage(hereID, farID));
        verify(hnd).handleSuccess();
        verifyNoMoreInteractions(hnd);
    }

    public void testDelayedWriteError() {
        int space = 0xFD;
        long address = 0x12345678;
        byte[] data = new byte[]{1,2};

        MemoryConfigurationService.McsWriteHandler hnd = mock(MemoryConfigurationService
                .McsWriteHandler.class);

        iface.getMemoryConfigurationService().requestWrite(farID, space, address, data, hnd);
        verifyNoMoreInteractions(hnd);

        expectMessageAndNoMore(new DatagramMessage(hereID, farID, new int[]{
                0x20, 0x01, 0x12, 0x34, 0x56, 0x78, 1, 2}));

        // datagram reply comes back
        sendMessage(new DatagramAcknowledgedMessage(farID, hereID, 0x80));
        verifyNoMoreInteractions(hnd);

        // Incoming reply datagram
        sendMessage(new DatagramMessage(farID, hereID, new int[]{0x20, 0x19, 0x12, 0x34, 0x56,
                0x78, 0x19, 0x99}));
        // which gets ack-ed.
        expectMessageAndNoMore(new DatagramAcknowledgedMessage(hereID, farID));
        verify(hnd).handleFailure(0x1999);
        verifyNoMoreInteractions(hnd);
    }

    public void testSimpleRead() {
        int space = 0xFD;
        long address = 0x12345678;
        int length = 4;

        MemoryConfigurationService.McsReadHandler hnd = mock(MemoryConfigurationService
                .McsReadHandler.class);

        iface.getMemoryConfigurationService().requestRead(farID, space, address, length, hnd);

        // should have sent datagram
        expectMessageAndNoMore(new DatagramMessage(hereID, farID, new int[]{
                0x20, 0x41, 0x12, 0x34, 0x56, 0x78, 4}));

        // datagram reply comes back
        sendMessage(new DatagramAcknowledgedMessage(farID, hereID, 0x80));
        verifyNoMoreInteractions(hnd);

        // Response datagram comes and gets acked.
        sendMessageAndExpectResult(new DatagramMessage(farID, hereID, new int[]{
                        0x20, 0x51, 0x12, 0x34, 0x56, 0x78, 0xaa}),
                new DatagramAcknowledgedMessage(hereID, farID));

        verify(hnd).handleReadData(farID, space, address, new byte[]{(byte) 0xaa});
        verifyNoMoreInteractions(hnd);
    }

    public void testTwoSimpleReadsInSequence() {
        int space = 0xFD;
        long address = 0x12345678;
        int length = 4;
        MemoryConfigurationService.McsReadHandler hnd = mock(MemoryConfigurationService
                .McsReadHandler.class);

        // start of 1st pass
        {
            iface.getMemoryConfigurationService().requestRead(farID, space, address, length, hnd);

            // should have sent datagram
            expectMessageAndNoMore(new DatagramMessage(hereID, farID, new int[]{
                    0x20, 0x41, 0x12, 0x34, 0x56, 0x78, 4}));

            // datagram reply comes back
            sendMessage(new DatagramAcknowledgedMessage(farID, hereID, 0x80));
            verifyNoMoreInteractions(hnd);

            // now return data
            // Response datagram comes and gets acked.
            sendMessageAndExpectResult(new DatagramMessage(farID, hereID, new int[]{
                            0x20, 0x51, 0x12, 0x34, 0x56, 0x78, 0xaa}),
                    new DatagramAcknowledgedMessage(hereID, farID));

            verify(hnd).handleReadData(farID, space, address, new byte[]{(byte) 0xaa});
            verifyNoMoreInteractions(hnd);
        }

        {
            iface.getMemoryConfigurationService().requestRead(farID, space, address+1, length, hnd);

            // should have sent datagram
            expectMessageAndNoMore(new DatagramMessage(hereID, farID, new int[]{
                    0x20, 0x41, 0x12, 0x34, 0x56, 0x79, 4}));

            // datagram reply comes back
            sendMessage(new DatagramAcknowledgedMessage(farID, hereID, 0x80));
            verifyNoMoreInteractions(hnd);

            // now return data
            // Response datagram comes and gets acked.
            sendMessageAndExpectResult(new DatagramMessage(farID, hereID, new int[]{
                            0x20, 0x51, 0x12, 0x34, 0x56, 0x79, 0xaa}),
                    new DatagramAcknowledgedMessage(hereID, farID));

            verify(hnd).handleReadData(farID, space, address+1, new byte[]{(byte) 0xaa});
            verifyNoMoreInteractions(hnd);
        }
    }

    public void testManyReadsInlinePrint() {
        final int space = 0xFD;
        final long address = 0x12345678;
        final int length = 64;
        final int count = 10;
        final boolean debug = false;
        final MemoryConfigurationService.McsReadHandler hnd = new MemoryConfigurationService
                .McsReadHandler() {

            @Override
            public void handleFailure(int errorCode) { }

            @Override
            public void handleReadData(NodeID dest, int space, long caddress, byte[] data) {
                if(debug)System.err.println("Read data callback for offset: " + caddress);
                if (address + count*length > caddress + 64) {
                    if(debug)System.err.println("Sending further read request for offset: " + (caddress +
                            64 - address));
                    iface.getMemoryConfigurationService().requestRead(farID, space, caddress +
                            64, length, this);
                }
            }
        };

        // Triggers initial fetch.
        iface.getMemoryConfigurationService().requestRead(farID, space, address, length, hnd);

        for (int ofs = 0; ofs < count * 64; ofs += 64) {
            if(debug)System.err.println("Iteration: " + ofs);
            int[] payload = new int[]{0x20, 0x41, 0, 0, 0, 0, 64};
            DatagramUtils.renderLong(payload, 2, address + ofs);
            expectMessageAndNoMore(new DatagramMessage(hereID, farID, payload));
            sendMessage(new DatagramAcknowledgedMessage(farID, hereID, 0x80));

            payload = new int[6+64];
            payload[0] = 0x20;
            payload[1] = 0x51;
            DatagramUtils.renderLong(payload, 2, address + ofs);
            for (int i = 6; i < payload.length; ++i) {
                payload[i] = (ofs + i - 6) & 0xff;
            }
            // Response datagram comes and gets acked.
            sendMessageAndExpectResult(new DatagramMessage(farID, hereID, payload),
                    new DatagramAcknowledgedMessage(hereID, farID));
        }
    }

    public void testManyReadsInline() {
        final int space = 0xFD;
        final long address = 0x12345678;
        final int length = 64;
        final boolean debug = false;
        final MemoryConfigurationService.McsReadHandler hnd = mock(MemoryConfigurationService
                .McsReadHandler.class, new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                throw new AssertionFailedError("Not stubbed invocation: " + invocationOnMock.toString());
            }
        });
        int count = 10;

        // Triggers initial fetch.
        iface.getMemoryConfigurationService().requestRead(farID, space, address, length, hnd);

        for (int ofs = 0; ofs < count * 64; ofs += 64) {
            if(debug)System.err.println("Iteration: " + ofs);
            int[] payload = new int[]{0x20, 0x41, 0, 0, 0, 0, 64};
            DatagramUtils.renderLong(payload, 2, address + ofs);
            expectMessageAndNoMore(new DatagramMessage(hereID, farID, payload));
            sendMessage(new DatagramAcknowledgedMessage(farID, hereID, 0x80));
            //verifyNoMoreInteractions(hnd);

            payload = new int[6+64];
            payload[0] = 0x20;
            payload[1] = 0x51;
            DatagramUtils.renderLong(payload, 2, address + ofs);
            for (int i = 6; i < payload.length; ++i) {
                payload[i] = (ofs + i - 6) & 0xff;
            }

            if (ofs + 64 < count*64) {
                final int fofs = ofs;
                doAnswer(new Answer() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        if(debug)System.err.println("Sending further read request for offset: " + (fofs + 64));

                        iface.getMemoryConfigurationService().requestRead(farID, space, address +
                                fofs + 64, length, hnd);
                        return null;
                    }
                }).when(hnd).handleReadData(eq(farID), eq(space), eq(address + ofs), any(byte[]
                        .class));
            } else {
                doNothing().when(hnd).handleReadData(eq(farID), eq(space), eq(address + ofs), any
                        (byte[].class));
            }
            // Response datagram comes and gets acked.
            sendMessageAndExpectResult(new DatagramMessage(farID, hereID, payload),
                    new DatagramAcknowledgedMessage(hereID, farID));
        }
    }

    public void testSimpleReadFails() {
        int space = 0xFD;
        long address = 0x12345678;
        int length = 4;

        MemoryConfigurationService.McsReadHandler hnd = mock(MemoryConfigurationService
                .McsReadHandler.class);

        iface.getMemoryConfigurationService().requestRead(farID, space, address, length, hnd);

        // should have sent datagram
        expectMessageAndNoMore(new DatagramMessage(hereID, farID, new int[]{
                0x20, 0x41, 0x12, 0x34, 0x56, 0x78, 4}));

        // datagram reply comes back
        sendMessage(new DatagramAcknowledgedMessage(farID, hereID, 0x80));
        verifyNoMoreInteractions(hnd);

        // Response datagram comes and gets acked.
        sendMessageAndExpectResult(new DatagramMessage(farID, hereID, new int[]{
                        0x20, 0x59, 0x12, 0x34, 0x56, 0x78, 0x10, 0x37}),
                new DatagramAcknowledgedMessage(hereID, farID));

        verify(hnd).handleFailure(0x1037);
        verifyNoMoreInteractions(hnd);
    }

    public void testSimpleReadFromSpace1() {
        int space = 0x1;
        long address = 0x12345678;
        int length = 4;

        MemoryConfigurationService.McsReadHandler hnd = mock(MemoryConfigurationService
                .McsReadHandler.class);

        iface.getMemoryConfigurationService().requestRead(farID, space, address, length, hnd);

        // should have sent datagram
        expectMessageAndNoMore(new DatagramMessage(hereID, farID, new int[]{
                0x20, 0x40, 0x12, 0x34, 0x56, 0x78, 1, 4}));

        // datagram reply comes back
        sendMessage(new DatagramAcknowledgedMessage(farID, hereID, 0x80));
        verifyNoMoreInteractions(hnd);

        // Response datagram comes and gets acked.
        sendMessageAndExpectResult(new DatagramMessage(farID, hereID, new int[]{
                        0x20, 0x50, 0x12, 0x34, 0x56, 0x78, 1, 0xaa}),
                new DatagramAcknowledgedMessage(hereID, farID));

        verify(hnd).handleReadData(farID, space, address, new byte[]{(byte) 0xaa});
        verifyNoMoreInteractions(hnd);
    }

    public void testSimpleReadFromSpaceFB() {
        int space = 0xFB;
        long address = 0x12345678;
        int length = 4;

        MemoryConfigurationService.McsReadHandler hnd = mock(MemoryConfigurationService
                .McsReadHandler.class);

        iface.getMemoryConfigurationService().requestRead(farID, space, address, length, hnd);

        // should have sent datagram
        expectMessageAndNoMore(new DatagramMessage(hereID, farID, new int[]{
                0x20, 0x40, 0x12, 0x34, 0x56, 0x78, 0xFB, 4}));

        // datagram reply comes back
        sendMessage(new DatagramAcknowledgedMessage(farID, hereID, 0x80));
        verifyNoMoreInteractions(hnd);

        // Response datagram comes and gets acked.
        sendMessageAndExpectResult(new DatagramMessage(farID, hereID, new int[]{
                        0x20, 0x50, 0x12, 0x34, 0x56, 0x78, 0xFB, 0xaa}),
                new DatagramAcknowledgedMessage(hereID, farID));

        verify(hnd).handleReadData(farID, space, address, new byte[]{(byte) 0xaa});
        verifyNoMoreInteractions(hnd);
    }

    public void testGetSpaceId() {
        boolean debugFrames = false;

        Message msg = new DatagramMessage(farID, hereID, new int[]{
                0x20, 0x50, 0x12, 0x34, 0x56, 0x78, 0xFB, 0xaa});
        MessageBuilder d = new MessageBuilder(aliasMap);
        List<? extends CanFrame> actualFrames = d.processMessage(msg);
        StringBuilder b = new StringBuilder();
        for (CanFrame f : actualFrames) {
            b.append(GridConnect.format(f));
        }
        if (debugFrames) System.err.println("Input frames: " + b);

        List<Message> parsedMessages = new ArrayList<>();
        List<CanFrame> parsedFrames = GridConnect.parse(b.toString());
        for (CanFrame f : parsedFrames) {
            List<Message> l = d.processFrame(f);
            if (l != null) {
                parsedMessages.addAll(l);
            }
        }
        assertEquals(1, parsedMessages.size());
        assertTrue(parsedMessages.get(0) instanceof DatagramMessage);
        DatagramMessage dg = (DatagramMessage) parsedMessages.get(0);
        assertEquals("20 50 12 34 56 78 FB AA", Utilities.toHexSpaceString(dg.getData()));

        assertEquals(0xFB, dg.getData()[6]);

        assertEquals(0xFB, MemoryConfigurationService.getSpaceFromPayload(dg.getData()));
    }

    public void testTwoSimpleReadsInParallel() {
        int space = 0xFD;
        long address = 0x12345678;
        int length = 4;
        MemoryConfigurationService.McsReadHandler hnd1 = mock(MemoryConfigurationService
                .McsReadHandler.class);
        MemoryConfigurationService.McsReadHandler hnd2 = mock(MemoryConfigurationService
                .McsReadHandler.class);

        iface.getMemoryConfigurationService().requestRead(farID, space, address, 4, hnd1);
        iface.getMemoryConfigurationService().requestRead(farID, space, address & ~0xF, 2,
                hnd2);

        // should have sent datagram
        expectMessageAndNoMore(new DatagramMessage(hereID, farID, new int[]{
                0x20, 0x41, 0x12, 0x34, 0x56, 0x78, 4}));

        // datagram reply comes back
        sendMessage(new DatagramAcknowledgedMessage(farID, hereID, 0x80));
        verifyNoMoreInteractions(hnd1);
        verifyNoMoreInteractions(hnd2);

        // now return data
        // Response datagram comes and gets acked.
        sendMessageAndExpectResult(new DatagramMessage(farID, hereID, new int[]{
                        0x20, 0x51, 0x12, 0x34, 0x56, 0x78, 0xaa}),
                new DatagramAcknowledgedMessage(hereID, farID));

        verify(hnd1).handleReadData(farID, space, address, new byte[]{(byte) 0xaa});
        verifyNoMoreInteractions(hnd1);

        // The first ACK will trigger sending the second message.
        expectMessageAndNoMore(new DatagramMessage(hereID, farID, new int[]{
                0x20, 0x41, 0x12, 0x34, 0x56, 0x70, 2}));
        sendMessage(new DatagramAcknowledgedMessage(farID, hereID, 0x80));

        sendMessageAndExpectResult(new DatagramMessage(farID, hereID, new int[]{
                        0x20, 0x51, 0x12, 0x34, 0x56, 0x70, 0xbb}),
                new DatagramAcknowledgedMessage(hereID, farID));

        verify(hnd2).handleReadData(farID, space, address & ~0xF, new byte[]{(byte) 0xbb});
        verifyNoMoreInteractions(hnd2);
    }


/*
    public void testConfigMemoIsRealClass() {
        MemoryConfigurationService.McsConfigMemo m20 =
            new MemoryConfigurationService.McsConfigMemo(farID);
        MemoryConfigurationService.McsConfigMemo m20a =
            new MemoryConfigurationService.McsConfigMemo(farID);
        MemoryConfigurationService.McsConfigMemo m21 =
            new MemoryConfigurationService.McsConfigMemo(hereID);

        Assert.assertTrue(m20.equals(m20));
        Assert.assertTrue(m20.equals(m20a));

        Assert.assertTrue(m20 != null);

        Assert.assertTrue(!m20.equals(m21));

    }

    public void testGetConfig() {
        MemoryConfigurationService.McsConfigMemo memo =
            new MemoryConfigurationService.McsConfigMemo(farID) {
                @Override
                public void handleWriteReply(int code) {
                    flag = true;
                }
                @Override
                public void handleConfigData(NodeID dest, int commands, int lengths, int highSpace, int lowSpace, String name) {
                    flag = true;
                }
            };

        // test executes the callbacks instantly; real connections might not
        Assert.assertTrue(!flag);
        service.request(memo);
        Assert.assertTrue(!flag);

        // should have sent datagram
         Assert.assertEquals(1,messagesReceived.size());
         Assert.assertTrue(messagesReceived.get(0) instanceof DatagramMessage);

        // check format of datagram read
        int[] content = ((DatagramMessage)messagesReceived.get(0)).getData();
        Assert.assertTrue(content.length == 2);
        Assert.assertEquals("datagram type", 0x20, content[0]);
        Assert.assertEquals("read command", 0x80, (content[1]&0xFC));

        // datagram reply comes back
        Message m = new DatagramAcknowledgedMessage(farID, hereID);

        Assert.assertTrue(!flag);
        datagramService.put(m, null);
        Assert.assertTrue(flag);

        // now return data
        flag = false;
        content = new int[]{0x20, 0x81, 0x55, 0x55, 0xEE, 0xFF, 0x80, 'a', 'b', 'c'};
        m = new DatagramMessage(farID, hereID, content);

        Assert.assertTrue(!flag);
        datagramService.put(m, null);
        Assert.assertTrue(flag);

    }

    public void testAddrSpaceMemoIsRealClass() {
        MemoryConfigurationService.McsAddrSpaceMemo m20 =
            new MemoryConfigurationService.McsAddrSpaceMemo(farID,0xFD);
        MemoryConfigurationService.McsAddrSpaceMemo m20a =
            new MemoryConfigurationService.McsAddrSpaceMemo(farID,0xFD);
        MemoryConfigurationService.McsAddrSpaceMemo m22 =
            new MemoryConfigurationService.McsAddrSpaceMemo(farID,0xFE);
        MemoryConfigurationService.McsAddrSpaceMemo m23 =
            new MemoryConfigurationService.McsAddrSpaceMemo(hereID,0xFD);

        Assert.assertTrue(m20.equals(m20));
        Assert.assertTrue(m20.equals(m20a));

        Assert.assertTrue(m20 != null);

        Assert.assertTrue(!m20.equals(m22));
        Assert.assertTrue(!m20.equals(m23));

    }

    public void testGetAddrSpace1() {
        int space = 0xFD;
        MemoryConfigurationService.McsAddrSpaceMemo memo =
            new MemoryConfigurationService.McsAddrSpaceMemo(farID, space) {
                @Override
                public void handleWriteReply(int code) {
                    flag = true;
                }
                @Override
                public void handleAddrSpaceData(NodeID dest, int space, long hiAddress, long lowAddress, int flags, String desc) {
                    flag = true;
                    // check contents
                    Assert.assertTrue("space", space == 0xFD);
                    Assert.assertTrue("hiAddress", hiAddress == 0x12345678L);
                    Assert.assertTrue("lowAddress", lowAddress == 0x00L);
                }
            };

        // test executes the callbacks instantly; real connections might not
        Assert.assertTrue(!flag);
        service.request(memo);
        Assert.assertTrue(!flag);

        // should have sent datagram
         Assert.assertEquals(1,messagesReceived.size());
         Assert.assertTrue(messagesReceived.get(0) instanceof DatagramMessage);

        // check format of datagram read
        int[] content = ((DatagramMessage)messagesReceived.get(0)).getData();
        Assert.assertTrue(content.length == 3);
        Assert.assertEquals("datagram type", 0x20, content[0]);
        Assert.assertEquals("addr space command", 0x84, (content[1]&0xFC));
        Assert.assertEquals("space", space, (content[2]));

        // datagram reply comes back
        Message m = new DatagramAcknowledgedMessage(farID, hereID);

        Assert.assertTrue(!flag);
        datagramService.put(m, null);
        Assert.assertTrue(flag);

        // now return data
        flag = false;
        content = new int[]{0x20, 0x85, space, 0x12, 0x34, 0x56, 0x78, 0x55};
        m = new DatagramMessage(farID, hereID, content);

        Assert.assertTrue(!flag);
        datagramService.put(m, null);
        Assert.assertTrue(flag);

    }
    public void testGetAddrSpace2() {
        int space = 0xFD;
        MemoryConfigurationService.McsAddrSpaceMemo memo =
            new MemoryConfigurationService.McsAddrSpaceMemo(farID, space) {
                @Override
                public void handleWriteReply(int code) {
                    flag = true;
                }
                @Override
                public void handleAddrSpaceData(NodeID dest, int space, long hiAddress, long lowAddress, int flags, String desc) {
                    flag = true;
                    // check contents
                    Assert.assertTrue("space", space == 0xFD);
                    Assert.assertTrue("hiAddress", hiAddress == 0xFFFFFFFFL);
                    Assert.assertTrue("lowAddress", lowAddress == 0x12345678L);
                }
            };

        // test executes the callbacks instantly; real connections might not
        Assert.assertTrue(!flag);
        service.request(memo);
        Assert.assertTrue(!flag);

        // should have sent datagram
         Assert.assertEquals(1,messagesReceived.size());
         Assert.assertTrue(messagesReceived.get(0) instanceof DatagramMessage);

        // check format of datagram read
        int[] content = ((DatagramMessage)messagesReceived.get(0)).getData();
        Assert.assertTrue(content.length == 3);
        Assert.assertEquals("datagram type", 0x20, content[0]);
        Assert.assertEquals("addr space command", 0x84, (content[1]&0xFC));
        Assert.assertEquals("space", space, (content[2]));

        // datagram reply comes back
        Message m = new DatagramAcknowledgedMessage(farID, hereID);

        Assert.assertTrue(!flag);
        datagramService.put(m, null);
        Assert.assertTrue(flag);

        // now return data
        flag = false;
        content = new int[]{0x20, 0x85, space, 0xFF, 0xFF, 0xFF, 0xFF, 0x55, 0x12, 0x34, 0x56, 0x78};
        m = new DatagramMessage(farID, hereID, content);

        Assert.assertTrue(!flag);
        datagramService.put(m, null);
        Assert.assertTrue(flag);

    }
    */
    // from here down is testing infrastructure

    public MemoryConfigurationServiceInterfaceTest(String s) {
        super(s);
        aliasMap.insert(0x987, farID);
        testWithCanFrameRendering = true;
    }

    // Main entry point
    static public void main(String[] args) {
        String[] testCaseName = {MemoryConfigurationServiceInterfaceTest.class.getName()};
        junit.swingui.TestRunner.main(testCaseName);
    }

    // test suite from all defined tests
    public static Test suite() {
        TestSuite suite = new TestSuite(MemoryConfigurationServiceInterfaceTest.class);
        return suite;
    }
}
