package br.usp.ime.compmus.pushloop.connections;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import br.usp.ime.compmus.pushloop.util.MobileDevice;

public class Packet {

	private static Random random = new Random();
	private static int extraFloats = 0;
	
	private static ArrayList<Float> floatsList = new ArrayList<Float>();
	
	/**
	 * Create a packet to be sent during the tests.
	 * @param extraFloats
	 */
	public Packet(int extraFloats) {
		Packet.extraFloats = extraFloats;
		
		generateFloatList();
	}

	private void generateFloatList() {
		
		if (extraFloats <= 0) {
			
			floatsList.clear();
		} else if (extraFloats > floatsList.size()) {
			
			for (int i = floatsList.size(); i < extraFloats; i++) {
				floatsList.add(random.nextFloat());
			}
		} else {			
			for (int i = floatsList.size()-1; i >= extraFloats; i--) {
				
				floatsList.remove(i);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public List<Object> getListContents() {
		
		List<Object> packet;
		
		try {
			packet = (List<Object>) floatsList.clone();
		} catch (ClassCastException e) {
			packet = new ArrayList<Object>();
		}
		packet.add(0, random.nextInt());
		packet.add(MobileDevice.getRandomId());
		
		return packet;
	}
	
	public String getJsonContents() {
		
		StringBuilder packet = new StringBuilder();
		String floatsString = floatsList.toString();
		
		packet.append("{");
		packet.append(random.nextInt());
		
		if (extraFloats > 0) {
			
			packet.append(",");
			packet.append(floatsString.substring(1, floatsString.length()-1));
		}
		packet.append(",");
		packet.append(MobileDevice.getRandomId());		
		packet.append("}");
		
		return packet.toString();
	}
	
}
