package com.comtec.ipps.cp.api;

public interface PhoneBookRepository {

	public PhoneUser findUserByPhoneNumber(String phoneNumber);

}
