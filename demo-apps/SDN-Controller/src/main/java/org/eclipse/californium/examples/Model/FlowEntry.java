package org.eclipse.californium.examples.Model;

import java.util.ArrayList;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.builder.ArrayBuilder;

public class FlowEntry {
	private int priority;
	private int count;
	private int ttl;
	ArrayList<Rule> rules;
	ArrayList<Action> actions;
	
	public FlowEntry(){
		
	}
	
	public FlowEntry(int priority, int ttl){
		this.priority = priority;
		this.ttl = ttl;
		rules = new ArrayList<Rule>();
		actions = new ArrayList<Action>();
	}
	
	public FlowEntry(int priority, int ttl, ArrayList<Rule> rules, ArrayList<Action> actions) {
		this.priority = priority;
		this.ttl = ttl;
		this.rules = rules;
		this.actions = actions;
		this.count = 0;
	}
	
	public void addRule(Rule r){
		rules.add(r);
	}

	public void addAction(Action a){
		actions.add(a);
	}
	
	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getTtl() {
		return ttl;
	}

	public void setTtl(int ttl) {
		this.ttl = ttl;
	}

	public ArrayList<Rule> getRules() {
		return rules;
	}

	public void setRules(ArrayList<Rule> rules) {
		this.rules = rules;
	}

	public ArrayList<Action> getActions() {
		return actions;
	}

	public void setActions(ArrayList<Action> actions) {
		this.actions = actions;
	}

	public void toCbor(ArrayBuilder<CborBuilder> array) {
		ArrayBuilder<ArrayBuilder<ArrayBuilder<CborBuilder>>> support = array.addArray()
			.add(this.priority)
			.add(this.ttl)
			.add(this.count)
			.addArray();
		for(Rule rule: rules){
			rule.toCbor(support);
		}
		support = support.end()
				.addArray();
		for(Action action: actions){
			action.toCbor(support);
		}
		support.end().end();
	}
	
	
	
}
