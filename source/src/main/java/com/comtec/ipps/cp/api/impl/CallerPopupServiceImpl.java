package com.comtec.ipps.cp.api.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.comtec.ipps.cp.api.CallerPopupService;
import com.comtec.ipps.cp.api.PhoneBookRepository;
import com.comtec.ipps.cp.api.PhoneUser;
import com.comtec.ipps.cp.api.Tapi;
import com.comtec.ipps.cp.api.TapiCall;
import com.comtec.ipps.cp.api.TapiListener;

@Service
public class CallerPopupServiceImpl implements CallerPopupService, TapiListener {
	@Autowired
	private Tapi tapi;
	@Autowired
	private PhoneBookRepository userRepository;
	@Value("${ipps.server.url}")
	private String serverUrl;
	@Value("${ipps.path.popup}")
	private String pathPopup;
	@Value("${ipps.path.images}")
	private String pathImages;
	@Value("${ipps.path.answer}")
	private String pathAnswer;
	
	private Map<String, PopupInfo> popupList = new ConcurrentHashMap<>();
	
	
	static class PopupInfo {
		private final PhoneUser user;
		private final TapiCall call;
		
		PopupInfo(TapiCall call, PhoneUser user) {
			this.call = call;
			this.user = user;
		}
		public PhoneUser getUser() {
			return user;
		}
		public TapiCall getCall() {
			return call;
		}
	}
	
	@PostConstruct
	public void init() {
		tapi.addListener(this);
	}
	
	@PreDestroy
	public void destory() {
		tapi.removeListener(this);
	}

	@Override
	public void callOffered(TapiCall call) {
		PhoneUser user = userRepository.findUserByPhoneNumber(call.getCallingPartyNumber());
		if (user != null) {
			PopupInfo popupInfo = new PopupInfo(call, user);
			popupList.put(call.getId(), popupInfo);
			sendXml(popupInfo);
		}
	}

  private final String XML_CLOSE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<CiscoIPPhoneExecute>"
					+ "<ExecuteItem Priority=\"2\" URL=\"Init:Services\"/>"
			+ "</CiscoIPPhoneExecute>";

	@Override
	public void callDisconnected(TapiCall call) {
		PopupInfo popupInfo = popupList.remove(call.getId());
		if (popupInfo != null) {
			tapi.sendData(popupInfo.getCall().getTerminalName(), XML_CLOSE);
		}
	}
	
	private void sendXml(PopupInfo popupInfo) {
		StringBuilder buf = new StringBuilder(512);
		buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<CiscoIPPhoneExecute>")
			.append("<ExecuteItem Priority=\"0\" URL=\"")
			.append(serverUrl).append(pathPopup).append("/").append(popupInfo.getCall().getId()).append( "\"/>")
			.append("</CiscoIPPhoneExecute>");
		tapi.sendData(popupInfo.getCall().getTerminalName(), buf.toString());				
	}

	@Override
	public void answerCall(String connectionId) {
		PopupInfo popupInfo = popupList.get(connectionId);
		if (popupInfo != null) {
			tapi.answerCall(connectionId);
		}
	}

	@Override
	public String getPopupText(String connectionId, String modelName, String display) {
		PopupInfo popupInfo = popupList.get(connectionId);
		if (popupInfo == null) {
			return XML_CLOSE;
		}
		if (modelName.startsWith("CP-88")) {
			return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
					+ "<CiscoIPPhoneImageFile>"
					+ "<Title>Caller Information</Title>"
					+ "<Prompt>Caller: " + popupInfo.getCall().getCallingPartyNumber()  + "</Prompt>"
			+ "<LocationX>0</LocationX>"
			+ "<LocationY>0</LocationY>"
			+ "<URL>" + serverUrl + pathImages + "/" + popupInfo.getUser().getImagePath() + "</URL>"
			+ "<SoftKeyItem>"
			+ "<Name>Answer</Name>"
			+ "<URL>" + serverUrl + pathAnswer + "/" + popupInfo.getCall().getId() + "</URL>"
			+ "<Position>1</Position>"
			+ "</SoftKeyItem>"
			+ "<SoftKeyItem>"
			+ "<Name>Close</Name>"
			+ "<URL>SoftKey:Exit</URL>"
			+ "<Position>2</Position>"
			+ "</SoftKeyItem>"
			+ "</CiscoIPPhoneImageFile>";
		}  else {
			String s = "Name: " + popupInfo.getUser().getName();
			return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
					+ "<CiscoIPPhoneText>"
					+ "<Title>Caller Information</Title>"
					+ "<Prompt>Caller: " + popupInfo.getCall().getCallingPartyNumber() + "</Prompt>"
					+ "<Text>" + s  + "</Text>"
					+ "<SoftKeyItem>"
					+ "<Name>Answer</Name>"
					+ "<URL>" + serverUrl + pathAnswer + "/" + connectionId + "</URL>"
					+ "<Position>1</Position>"
					+ "</SoftKeyItem>"
					+ "<SoftKeyItem>"
					+ "<Name>Close</Name>"
					+ "<URL>SoftKey:Exit</URL>"
					+ "<Position>2</Position>"
					+ "</SoftKeyItem>"
					+ "</CiscoIPPhoneText>";
		}
	}
}

