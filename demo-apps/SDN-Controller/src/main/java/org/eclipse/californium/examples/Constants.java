package org.eclipse.californium.examples;

public class Constants {
	public enum Actions{
		FORWARD(1),
	    DROP(2),
	    MODIFY(3),
	    DECREMENT(4),
	    INCREMENT(5),
	    CONTINUE(6),
	    TO_UPPER_L(7);
	    
	    private final int id;
		Actions(int id) { this.id = id; }
	    public int getValue() { return id; }
	};
	public enum Fields{
		NO_FIELD(1),
	    LINK_SRC_ADDR(2),          //802.15.4 source address
	    LINK_DST_ADDR(3),          //802.15.4 destination address
	    MH_SRC_ADDR(4),            //Mesh Header Originator Address field
	    MH_DST_ADDR(5),            //Mesh Header Final Address field
	    MH_HL(6),                  //Mesh Header Hop Limit field
	    SICSLO_DISPATCH(7),        //6LoWPAN Dispatch Type field(s)
	    SICSLO_BRDCST_HDR(8),      //6LoWPAN Broadcast Header
	    SICSLO_FRAG1_HDR(9),       //6LoWPAN Fragmentation Header (first fragment)
	    SICSLO_FRAGN_HDR(10),       //6LoWPAN Fragmentation Header (subsequent fragments)
	    SICSLO_IPHC(11),            //6LoWPAN IPv6 Header Compression
	    SICSLO_NHC(12),             //6LoWPAN IPv6 Next Header Compression
	    SICSLO_IPV6(13),            //6LoWPAN IPv6 Uncompressed IPv6 addresses
	    IP_SRC_ADDR(14),            //IPv6 packet Source Address field
	    IP_DST_ADDR(15),            //IPv6 packet Destination Address field
	    IP_HL(16),                  //IPv6 packet Hop Limit field
	    IP_TC(17),                  //IPv6 packet Traffic Class field
	    IP_FL(18),                  //IPv6 packet Flow Label field
	    IP_NH(19),                  //IPv6 packet Next Header field
	    IP_PAYLOAD(20),             //Payload of the IPv6 packet
	    NODE_STATE(21);              //State array
		
		private final int id;
		Fields(int id) { this.id = id; }
	    public int getValue() { return id; }
	};
	public enum Operators{
		EQUAL(1),
	    NOT_EQUAL(2),
	    GREATER(3),
	    LESS(4),
	    GREATER_OR_EQUAL(5),
	    LESS_OR_EQUAL(6);
		
		private final int id;
		Operators(int id) { this.id = id; }
	    public int getValue() { return id; }
	};
}
