package org.eclipse.californium.examples.Model;

import org.eclipse.californium.examples.Model.Constants.*;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.builder.ArrayBuilder;

public class Rule {
	private Fields field;
	private int offset;
	private int size;
	private Operators operator;
	private byte[] value;
	
	public Rule(){
		
	}
	
	public Rule(Fields field, int offset, int size, Operators operator, byte[] value) {
		this.field = field;
		this.offset = offset;
		this.size = size;
		this.operator = operator;
		this.value = value;
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

	public Operators getOperator() {
		return operator;
	}

	public void setOperator(Operators operator) {
		this.operator = operator;
	}

	public byte[] getValue() {
		return value;
	}

	public void setValue(byte[] value) {
		this.value = value;
	}

	public void toCbor(ArrayBuilder<ArrayBuilder<ArrayBuilder<CborBuilder>>> support) {
		support.addArray()
			.add(this.field.getValue())
			.add(this.operator.getValue())
			.add(this.offset)
			.add(this.size)
			.add(this.value)
			.end();		
	}
	
	
}
