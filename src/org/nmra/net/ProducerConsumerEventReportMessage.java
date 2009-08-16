package org.nmra.net;

/**
 * Producer Consumer Event Report message implementation
 *
 * @author  Bob Jacobsen   Copyright 2009
 * @version $Revision$
 */
public class ProducerConsumerEventReportMessage extends Message {
    
    public ProducerConsumerEventReportMessage(NodeID source, EventID eventID) {
        super(source);
        this.eventID = eventID;
    }
    
    private EventID eventID;
    
    // because EventID is immutable, can directly return object
    public EventID getEventID() {
        return eventID;
    }
    
    /**
     * Implement message-type-specific
     * processing when this message
     * is received by a node.
     *<p>
     * Default is to do nothing.
     */
     @Override
    public void applyTo(MessageDecoder decoder, Connection sender) {
        decoder.handleProducerConsumerEventReport(this, sender);
    }
    
    public boolean equals(Object o) {
        if (!super.equals(o)) return false;
        ProducerConsumerEventReportMessage p = (ProducerConsumerEventReportMessage) o;
        return eventID.equals(p.eventID);
    } 
}
