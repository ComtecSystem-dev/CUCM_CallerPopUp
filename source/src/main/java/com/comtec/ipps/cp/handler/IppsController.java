package com.comtec.ipps.cp.handler;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.comtec.ipps.cp.api.CallerPopupService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class IppsController {
	@Autowired
	private CallerPopupService popupService;

	
	@RequestMapping(value="/answer/{connectionId}",
			method=RequestMethod.GET, produces = "text/xml;charset=utf-8")
	public String callAnswer(@PathVariable("connectionId") String connectionId,
			HttpServletRequest request) {
		log.info("callAnswer - {}: {}", connectionId, request.getRemoteAddr());
		popupService.answerCall(connectionId);
		return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
				+ "<CiscoIPPhoneExecute><ExecuteItem Priority=\"2\" URL=\"Init:Services\"/></CiscoIPPhoneExecute>";
	}

	
	@RequestMapping(value="/popup/{connectionId}",
				method=RequestMethod.GET, produces = "text/xml;charset=utf-8")
	public String xmlUserPopup(
			@PathVariable("connectionId") String connectionId,
			@RequestHeader("x-CiscoIPPhoneModelName") String modelName,
			@RequestHeader("x-CiscoIPPhoneDisplay") String display,
			HttpServletRequest request) {
		log.info("xmlUserPopup - {},{},{}: {}", connectionId,
				modelName,
				display,
				request.getRemoteAddr());
		return popupService.getPopupText(connectionId, modelName, display);
	}
}
