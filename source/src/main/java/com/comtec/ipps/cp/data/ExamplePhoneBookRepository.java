package com.comtec.ipps.cp.data;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.comtec.ipps.cp.api.PhoneUser;
import com.comtec.ipps.cp.api.PhoneBookRepository;

@Repository
public class ExamplePhoneBookRepository implements PhoneBookRepository {

	static class UserImpl implements PhoneUser {
		String phoneNumber;
		String name;
		String imagePath;
		
		UserImpl(String phoneNumber, String name, String imagePath) {
			this.phoneNumber = phoneNumber;
			this.name = name;
			this.imagePath = imagePath;
		}

		public String getPhoneNumber() {
			return phoneNumber;
		}

		public String getName() {
			return name;
		}

		public String getImagePath() {
			return imagePath;
		}
	}
	

	Map<String, UserImpl> users = new HashMap<>();
	
	public ExamplePhoneBookRepository() {
//		users.put("1001", new UserImpl("1001", "Seong Chunhyang", "SHC1001.png"));
//		users.put("1002", new UserImpl("1002", "Hong Gildong", "SHC1002.png"));
//		users.put("1004", new UserImpl("1004", "Yi Mongryong", "SHC1004.png"));
		users.put("9500", new UserImpl("9500", "Seong Chunhyang", "SHC1001.png"));
		users.put("9501", new UserImpl("9501", "Hong Gildong", "SHC1002.png"));
		users.put("9502", new UserImpl("9502", "Yi Mongryong", "SHC1004.png"));
		users.put("1005", new UserImpl("1005", "Sin Jageum", "SHC1005.png"));
	}
	
	@Override
	public PhoneUser findUserByPhoneNumber(String phoneNumber) {
		return users.get(phoneNumber);
	}
}
