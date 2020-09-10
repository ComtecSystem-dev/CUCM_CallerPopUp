package com.comtec.ipps.cp.api;

public interface Tapi {
	/**
	 * Add event listener.
	 */
	void addListener(TapiListener listener);
	/**
	 * Remove event listener.
	 */
	void removeListener(TapiListener listener);
	/**
	 * Send XML data to the terminal.
	 * 
	 * @param terminalName device name
	 * @param data xml data
	 */
	void sendData(String terminalName, String data);
	/**
	 * Answer a call.
	 * 
	 * @param id TapCall's ID.
	 */
	void answerCall(String id);
}
