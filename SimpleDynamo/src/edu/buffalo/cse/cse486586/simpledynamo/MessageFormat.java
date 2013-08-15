package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MessageFormat implements Serializable{
	
	
	private static final long serialVersionUID = 1L;
	int destPort;
	int originPort;
	String key;
	String value;
	String messageType; // messageType = iREQ /jREQ/ iACK / jACK /
	String deviceHash;
	String deviceSuccessorHash;
	String deviceSuccessor;
	String devicePredecessorHash;
	String devID;
	String cordinatorID;
	String devicePredecessor;
	String keyvalue;
	ArrayList<ArrayList<String>> keyvaluePair;
	// devID = 5554 / 5556 / 5558 / 5560 / 5562
	//List<String> ring;
	//HashMap<String, Integer> map;

	
}
