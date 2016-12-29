package org.eclipse.californium.examples.Model;

import org.eclipse.californium.examples.Model.Constants.*;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.builder.ArrayBuilder;


public class Action {
	private TypeOfAction type;
	private Fields field;
	private int offset;
	private int size;
	private byte[] value;
	
	public Action(){
		
	}
	
	public Action(TypeOfAction action, Fields field, int offset, int size, byte[] value) {
		this.type = action;
		this.field = field;
		this.offset = offset;
		this.size = size;
		this.value = value;
	}

	public TypeOfAction getAction() {
		return type;
	}

	public void setAction(TypeOfAction action) {
		this.type = action;
	}

	public Fields getField() {
		return field;
	}

	public void setField(Fields field) {
		this.field = field;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public byte[] getValue() {
		return value;
	}

	public void setValue(byte[] value) {
		this.value = value;
	}

	public void toCbor(ArrayBuilder<ArrayBuilder<ArrayBuilder<CborBuilder>>> support) {
		support.addArray()
			.add(this.type.getValue())
			.add(this.field.getValue())
			.add(this.offset)
			.add(this.size)
			.add(this.value)
			.end();			
	}
	
	
	
}
