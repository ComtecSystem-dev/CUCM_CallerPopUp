package com.comtec.ipps.cp.api;

public interface TapiListener {
	void callOffered(TapiCall call);
	void callDisconnected(TapiCall call);
}
