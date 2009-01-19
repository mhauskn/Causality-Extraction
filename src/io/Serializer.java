package io;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Serializes and deserializes an object of type T
 */
public class Serializer <T> {
	/**
	 * Deserializes a given object of type T from a specified file
	 */
	@SuppressWarnings("unchecked")
	public T deserialize (String fileName) {
		try {
			FileInputStream fis = new FileInputStream(fileName);
			ObjectInputStream ois = new ObjectInputStream(fis);
			T myDeserializedObject = (T) ois.readObject();
			ois.close();
			return myDeserializedObject;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Serializes the given object to the given fileName
	 */
	public void serialize (T toSer, String fileName) {
		try {
			FileOutputStream fos = new FileOutputStream(fileName);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(toSer);
			oos.flush();
			oos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
