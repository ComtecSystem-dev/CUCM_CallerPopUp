package com.comtec.ipps.cp.api;

public interface CallerPopupService {

	void answerCall(String connectionId);
	String getPopupText(String connectionId, String modelName, String display);
}
