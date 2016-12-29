package org.eclipse.californium.examples.Model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.californium.examples.Model.Constants.Fields;
import org.eclipse.californium.examples.Model.Constants.Operators;
import org.eclipse.californium.examples.Model.Constants.TypeOfAction;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.builder.ArrayBuilder;
import co.nstant.in.cbor.model.DataItem;

public class FlowTable {
	private ArrayList<FlowEntry> flowTable;

	public FlowTable(){
		flowTable = new ArrayList<FlowEntry>();
	}
	
	public FlowTable(ArrayList<FlowEntry> flowTable) {

		this.flowTable = flowTable;
	}
	//Returns the index (i.e. the position) taken from the entry inside the Flow Table.
	public int insertFlowEntry(FlowEntry f){
		int index = 0;
		if(f == null)
			return -1;
		for(FlowEntry iter : flowTable){
			if(iter.getPriority() > f.getPriority())
				break;
			else
				index++;
		}
		flowTable.add(index, f);
		return index;
	}

	public ArrayList<FlowEntry> getFlowTable() {
		return flowTable;
	}

	public void setFlowTable(ArrayList<FlowEntry> flowTable) {
		this.flowTable = flowTable;
	}
	
	public byte[] toCbor() throws CborException{
		byte[] encodedBytes;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		CborBuilder builder = new CborBuilder();
		ArrayBuilder<CborBuilder> array = builder.addArray();
		for(FlowEntry iter : flowTable){
			iter.toCbor(array);
		}
		array.end();
		new CborEncoder(baos).encode(builder.build());
		encodedBytes = baos.toByteArray();
		return encodedBytes;
	}
	
	public static String bytesToHex(byte[] in) {
	    final StringBuilder builder = new StringBuilder();
	    for(byte b : in) {
	        builder.append(String.format("%02x", b));
	    }
	    return builder.toString();
	}
	
	public static void main(String [] args){
		byte[] address = {0x00,0x01,0x00,0x01,0x00,0x01,0x00,0x01};
		byte[] cborEncoding = null;
		FlowTable ft = new FlowTable();
		FlowEntry fe = new FlowEntry(10, 500);
		Rule r = new Rule(Fields.MH_DST_ADDR, 0, 64, Operators.EQUAL, address);
		Action a = new Action(TypeOfAction.FORWARD, Fields.NO_FIELD, 0, 64, address);
		fe.addRule(r);
		fe.addAction(a);
		ft.insertFlowEntry(fe);
		try {
			cborEncoding = ft.toCbor();
		} catch (CborException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(bytesToHex(cborEncoding));
		
		ByteArrayInputStream bais = new ByteArrayInputStream(cborEncoding);
        List<DataItem> dataItems = null;
		try {
			dataItems = new CborDecoder(bais).decode();
		} catch (CborException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("CBOR DECODED:" + dataItems.toString());
	}
}
