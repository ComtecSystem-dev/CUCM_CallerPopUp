package com.comtec.ipps.cp.api.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.telephony.CallObserver;
import javax.telephony.InvalidArgumentException;
import javax.telephony.InvalidStateException;
import javax.telephony.JtapiPeer;
import javax.telephony.JtapiPeerFactory;
import javax.telephony.MethodNotSupportedException;
import javax.telephony.Provider;
import javax.telephony.ProviderObserver;
import javax.telephony.Terminal;
import javax.telephony.TerminalConnection;
import javax.telephony.TerminalObserver;
import javax.telephony.callcontrol.CallControlCallObserver;
import javax.telephony.callcontrol.CallControlTerminalConnection;
import javax.telephony.callcontrol.events.CallCtlConnDisconnectedEv;
import javax.telephony.callcontrol.events.CallCtlConnEv;
import javax.telephony.callcontrol.events.CallCtlConnOfferedEv;
import javax.telephony.events.CallEv;
import javax.telephony.events.ProvEv;
import javax.telephony.events.ProvInServiceEv;
import javax.telephony.events.TermConnCreatedEv;
import javax.telephony.events.TermConnEv;
import javax.telephony.events.TermEv;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cisco.jtapi.extensions.CiscoCall;
import com.cisco.jtapi.extensions.CiscoConnection;
import com.cisco.jtapi.extensions.CiscoProvider;
import com.cisco.jtapi.extensions.CiscoTerminal;
import com.cisco.jtapi.extensions.CiscoTerminalObserver;
import com.comtec.ipps.cp.api.Tapi;
import com.comtec.ipps.cp.api.TapiCall;
import com.comtec.ipps.cp.api.TapiListener;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author comtec
 *
 */
@Slf4j
@Component
public class TapiImpl implements Tapi {
	@Value("${ipps.cucm.providerString}")
	private String providerString;
	private Provider provider;
	private CallObserver callObserver = new MyCallObserver();
	private TerminalObserver terminalObserver = new MyTerminalObserver();
	private Map<String, CallInfo> calls = new ConcurrentHashMap<>();
	private List<TapiListener> listeners = new CopyOnWriteArrayList<>();
	
	@Data
	static class CallInfo implements TapiCall {
		private final String id;
		private final String extension;
		private String callingPartyNumber;
		private String calledPartyNumber;
		private String terminalName;
		
		CallInfo(String id, String extension) {
			this.id = id;
			this.extension = extension;
		}

		
	}
	/**
	 * Provider observer
	 * 
	 * @author leebh
	 */
	class ProviderObserverImpl implements ProviderObserver {
		@Override
		public void providerChangedEvent(ProvEv[] args) {
			for (ProvEv ev: args) {
	        	log.info("Provider event: " + ev);
	        	if (ev.getID() == ProvInServiceEv.ID) {
	        		observeAll();
				}
			}
		}
		
	}
	
	/**
	 * Terminal observer
	 */
    private class MyTerminalObserver implements CiscoTerminalObserver {
		public void terminalChangedEvent (TermEv[] eventList) {
		}
	}
    

	/**
	 * Call observer
	 * 
	 */
	private class MyCallObserver implements CallControlCallObserver {
		public void callChangedEvent(CallEv[] eventList) {
			for (CallEv event : eventList) {
				if (log.isDebugEnabled())
					log.debug("CallEvent: {} - {}", event.toString(), event.getID());
				switch (event.getID()) {
				case TermConnCreatedEv.ID:
					processTermCreateEvent((TermConnCreatedEv) event);
					break;
				case CallCtlConnDisconnectedEv.ID:
					processCallDisconnectedEvent((CallCtlConnEv) event);
					break;
				case CallCtlConnOfferedEv.ID:
					processCallOfferedEvent((CallCtlConnEv)event);
					break;
				default:
					break;
				}
			}
		}
	}
	
	public void addListener(TapiListener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(TapiListener listener) {
		listeners.remove(listener);
	}

    private void processTermCreateEvent(TermConnEv event) {
    	CiscoCall call = (CiscoCall)event.getCall();
    	log.debug("TermCreateEvent - {} -> {}: {}",
    			call.getCurrentCallingAddress(),
				call.getCurrentCalledAddress(),
				event.getTerminalConnection().getConnection().getAddress());    	
    	String id = event.getTerminalConnection().getConnection().getAddress()
    				+ "."
    				+ ((CiscoConnection)event.getTerminalConnection().getConnection())
    					.getConnectionID().intValue();
    	CallInfo callInfo = calls.get(id); 
    	if (callInfo != null) {
    		callInfo.setCallingPartyNumber(call.getCurrentCallingAddress().getName());
    		callInfo.setCalledPartyNumber(call.getCurrentCalledAddress().getName());
    		callInfo.setTerminalName(event.getTerminalConnection().getTerminal().getName());
    		log.info("Deliver a CallOfferedEvent - {} -> {}: {}",
    					call.getCurrentCallingAddress(),
    					call.getCurrentCalledAddress(),
    					event.getTerminalConnection().getConnection().getAddress());   
    		for (TapiListener listener: listeners) {
    			listener.callOffered(callInfo);
    		}
    	}
    }
    
    private void processCallOfferedEvent(CallCtlConnEv event) {
    	CiscoCall call = (CiscoCall)event.getCall();
  	    CiscoConnection connection = (CiscoConnection)event.getConnection();
    	log.debug("CallOfferedEvent - {} -> {}: {}", 
    				call.getCurrentCallingAddress(),
    				call.getCurrentCalledAddress(),
    				connection.getAddress()	);
  	    int intConnectionId = connection.getConnectionID().intValue();
  	    if (intConnectionId != 0) {
  	    	String id = connection.getAddress().getName() + "." +  intConnectionId;
  	    	calls.put(id, new CallInfo(id, connection.getAddress().getName()));
  	    }
    }
    
    private void processCallDisconnectedEvent(CallCtlConnEv event) {
    	CiscoCall call = (CiscoCall)event.getCall();
  	    CiscoConnection connection = (CiscoConnection)event.getConnection();
    	log.debug("CallDisconnectedEvent - {} -> {}: {}", call.getCurrentCallingAddress(),
				call.getCurrentCalledAddress(), connection.getAddress()	);  	    
  	    String id = connection.getAddress().getName() + "." +  connection.getConnectionID().intValue();
  	    CallInfo callInfo = calls.remove(id); 
		if (callInfo != null) {
    		log.info("Deliver a CallDisconnectedEvent - {} -> {}: {}",
					call.getCurrentCallingAddress(),
					call.getCurrentCalledAddress(),
					connection.getAddress());   			
    		for (TapiListener listener: listeners) {
    			listener.callDisconnected(callInfo);
    		}
		}
    }
	
	
	private void addObserver(Terminal terminal) {
        try {
        	terminal.addObserver(terminalObserver);
        	terminal.addCallObserver(callObserver);
            log.info("Add observer to " + terminal.getName());
        } catch (Exception e) {
            log.error("Failed to add observer to " + terminal.getName(), e);
        }
	
	}
	
	protected void observeAll() {
        try {
            Terminal terminals[] = provider.getTerminals();
           	for (Terminal term : terminals) {
           		addObserver(term);
           	}
        } catch(Exception e) {
            log.error("observeAll exception", e);
        }		
	}
	
	
	@PostConstruct
	public void init() {
        try {
            JtapiPeer peer = JtapiPeerFactory.getJtapiPeer(null);
            provider = (CiscoProvider)peer.getProvider(providerString);
            provider.addObserver(new ProviderObserverImpl());
        } catch(Exception e) {
            log.error("ProviderInitException: " + e.toString());
            new RuntimeException("ProviderInitException", e);
        }
	}
	
	@PreDestroy
	public void detory() {
		provider.shutdown();
	}

	@Override
	public void sendData(String terminalName, String data) {
//		log.info("Send xml data to {} {}", terminalName, data);
		log.info("Send xml data to {}", terminalName);
		CiscoTerminal terminal;
		try {
			terminal = (CiscoTerminal)provider.getTerminal(terminalName);
			terminal.sendData(data);
		} catch (InvalidArgumentException e) {
			log.info("Not found terminal: {}", terminalName);
		} catch (InvalidStateException | MethodNotSupportedException e) {
			log.info("Faild to send xmldata to " + terminalName, e);
		}
	}
	
	@Override
	public void answerCall(String id) {
		log.info("Answer a call - {}", id);
		CallInfo callInfo = calls.get(id);
		if (callInfo != null) {
			CiscoTerminal terminal;
			try {
				terminal = (CiscoTerminal)provider.getTerminal(callInfo.terminalName);
				if (terminal != null) {
					for (TerminalConnection tconn : terminal.getTerminalConnections()) {
						try {
							if (id.endsWith("." + ((CiscoConnection)tconn.getConnection())
										.getConnectionID().intValue())) {
								((CallControlTerminalConnection)tconn).answer();
							}
						} catch (Exception e) {
							log.info("Answer a call error ", e);
						}
					}
				}				
			} catch (InvalidArgumentException e) {
				log.info("Not found terminal: {}", callInfo.terminalName);
			}
		}
	}

}
