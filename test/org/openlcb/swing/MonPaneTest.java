package org.openlcb.swing;

import org.openlcb.*;
import org.openlcb.implementations.*;

import org.junit.*;

import javax.swing.*;
/**
 * Simulate nine nodes interacting on a single gather/scatter
 * "link", and feed them to monitor.
 * <ul>
 * <li>Nodes 1,2,3 send Event A to 8,9
 * <li>Node 4 sends Event B to node 7
 * <li>Node 5 sends Event C to node 6
 * </ul>
 *
 * @author  Bob Jacobsen   Copyright 2009
 * @version $Revision$
 */
public class MonPaneTest {

    NodeID id1 = new NodeID(new byte[]{0,0,0,0,0,1});
    NodeID id2 = new NodeID(new byte[]{0,0,0,0,0,2});
    NodeID id3 = new NodeID(new byte[]{0,0,0,0,0,3});
    NodeID id4 = new NodeID(new byte[]{0,0,0,0,0,4});
    NodeID id5 = new NodeID(new byte[]{0,0,0,0,0,5});
    NodeID id6 = new NodeID(new byte[]{0,0,0,0,0,6});
    NodeID id7 = new NodeID(new byte[]{0,0,0,0,0,7});
    NodeID id8 = new NodeID(new byte[]{0,0,0,0,0,8});
    NodeID id9 = new NodeID(new byte[]{0,0,0,0,0,9});

    EventID eventA = new EventID(new byte[]{1,0,0,0,0,0,1,0});
    EventID eventB = new EventID(new byte[]{1,0,0,0,0,0,2,0});
    EventID eventC = new EventID(new byte[]{1,0,0,0,0,0,3,0});
    
    SingleProducerNode node1;
    SingleProducerNode node2;
    SingleProducerNode node3;
    SingleProducerNode node4;
    SingleProducerNode node5;
    SingleConsumerNode node6;
    SingleConsumerNode node7;
    SingleConsumerNode node8;
    SingleConsumerNode node9;
    
    ScatterGather sg;
    JFrame f; 
   
    @Before 
    public void setUp() throws Exception {
        // Test is really popping a window before doing all else
        f = new JFrame();
        f.setTitle("MonPane Test");
        MonPane m = new MonPane();
        f.add( m );
        m.initComponents();
        f.pack();
        f.setVisible(true);
        
        // and rerun simulations

        sg = new ScatterGather();

        sg.register(m.getConnection());
        
        node1 = new SingleProducerNode(id1, sg.getConnection(), eventA);
        sg.register(node1);
        
        node2 = new SingleProducerNode(id2, sg.getConnection(), eventA);
        sg.register(node2);
        
        node3 = new SingleProducerNode(id3, sg.getConnection(), eventA);
        sg.register(node3);
        
        node4 = new SingleProducerNode(id4, sg.getConnection(), eventB);
        sg.register(node4);
        
        node5 = new SingleProducerNode(id5, sg.getConnection(), eventC);
        sg.register(node5);
        
        node6 = new SingleConsumerNode(id6, sg.getConnection(), eventC);
        sg.register(node6);
        
        node7 = new SingleConsumerNode(id7, sg.getConnection(), eventB);
        sg.register(node7);
        
        node8 = new SingleConsumerNode(id8, sg.getConnection(), eventA);
        sg.register(node8);
        
        node9 = new SingleConsumerNode(id9, sg.getConnection(), eventA);
        sg.register(node9);
        
    }
   
    @After 
    public void tearDown() {
        f.setVisible(false);
	f.dispose();
        sg = null;
        node1 = null;
        node2 = null;
        node3 = null;
        node4 = null;
        node5 = null;
        node6 = null;
        node7 = null;
        node8 = null;
        node9 = null;
    }
        
    void initAll() {
        node1.initialize();
        node2.initialize();
        node3.initialize();
        node4.initialize();
        node5.initialize();
        node6.initialize();
        node7.initialize();
        node8.initialize();
        node9.initialize();
    }
   
    @Test 
    public void testMessagesInOrder() {
        initAll();
        
        node1.send();  
        node4.send();  
        node5.send(); 
        node2.send();  
        node3.send();  
    }
}
