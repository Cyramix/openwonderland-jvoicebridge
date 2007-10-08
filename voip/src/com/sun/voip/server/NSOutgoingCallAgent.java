/*
 * Copyright 2007 Sun Microsystems, Inc.
 *
 * This file is part of jVoiceBridge.
 *
 * jVoiceBridge is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License version 2 as 
 * published by the Free Software Foundation and distributed hereunder 
 * to you.
 *
 * jVoiceBridge is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Sun designates this particular file as subject to the "Classpath"
 * exception as provided by Sun in the License file that accompanied this 
 * code. 
 */

package com.sun.voip.server;

import com.sun.voip.CallParticipant;
import com.sun.voip.CallState;
import com.sun.voip.Logger;
import com.sun.voip.MediaInfo;
import com.sun.voip.RtpPacket;
import com.sun.voip.SdpInfo;
import com.sun.voip.TreatmentManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.ParseException;

/**
 * Non-Signaling handles calls setup by an external source.
 */
public class NSOutgoingCallAgent extends CallSetupAgent {

    private SipUtil sipUtil;

    private CallParticipant cp;
 
    private boolean callAnswered = false;

    public NSOutgoingCallAgent(CallHandler callHandler) {
	super(callHandler);

	cp = callHandler.getCallParticipant();

        MediaInfo mixerMediaPreference =
            callHandler.getConferenceManager().getMediaInfo();

        sipUtil = new SipUtil(mixerMediaPreference);
    }

    public void initiateCall() throws IOException {
	InetSocketAddress isa = callHandler.getReceiveAddress();

        if (isa == null) {
            throw new IOException("can't get receiver socket!");
        }

	setState(CallState.INVITED);

	if (cp.getInputTreatment() != null) {
	    try {
		/*
		 * Just make sure we can open the file
		 */
		new TreatmentManager(cp.getInputTreatment(),
                    0, 8000, 1);
	    } catch (IOException e) {
		Logger.println("Invalid input treatment:  " + cp.getInputTreatment());
		throw new IOException(
		    "Invalid input treatment:  " + cp.getInputTreatment());
	    }

	    /*
	     * This is a special call which is used to play
	     * an input treatment as its input.
	     * There is no remote endpoint.
	     */
	    try {
	        setRemoteMediaInfo();
	    } catch (ParseException e) {
		throw new IOException(e.getMessage());
	    }
	}
    }

    public String getSdp() {
        InetSocketAddress isa;

	if (cp.getInputTreatment() != null) {
	    isa = new InetSocketAddress(Bridge.getLocalHost(), 0);
	} else {
	    isa = callHandler.getReceiveAddress();
	}

	return sipUtil.generateSdp(cp, isa);
    }

    public void setRemoteMediaInfo(String sdp) throws ParseException {
	if (getState() != CallState.INVITED) {
	    Logger.println("Call " + cp
		+ ":  NSOutgoingCallAgent:  bad state "
		+ getState());
	    return;
	}

	sdp = sdp.replaceAll("\\+", "\r\n");

	Logger.println("Call " + cp
	    + ":  NSOutgoingCallAgent:  remote SDP:  " + sdp);

	SdpInfo sdpInfo = sipUtil.getSdpInfo(sdp, false);

	MediaInfo mediaInfo = sdpInfo.getMediaInfo();

        InetSocketAddress isa = new InetSocketAddress(
	    sdpInfo.getRemoteHost(), sdpInfo.getRemotePort());

	Logger.println("Call " + cp
	    + ":  NSOutgoingCallAgent:  " + mediaInfo
	    + " remote " + isa);

        setEndpointAddress(isa, mediaInfo.getPayload(), 
	    sdpInfo.getTransmitMediaInfo().getPayload(),
	    sdpInfo.getTelephoneEventPayload());

	if (callAnswered) {
	    Logger.writeFile("Call " + cp 
		+ ":  NSOutgoingCallAgent: done remote SDP");
	    return;
	}

	/*
	 * The CallParticipant has answered.
	 * If join confirmation is required, we remain in the
	 * INVITED state.  
	 */
	callAnswered = true;

	if (cp.getJoinConfirmationTimeout() == 0) {
	    setState(CallState.ANSWERED);
	}

	/*
	 * Start treatment if any and wait for it to finish.
	 * When the treatment finishes, notification will
	 * be delivered to our parent which will indicate
	 * we're ready for the conference.
	 *
	 * If there's no treatment to be played, we're ready now
	 * unless we're waiting for join confirmation..
	 */
	initializeCallAnsweredTreatment();

	if (callAnsweredTreatment != null) {
	    startCallAnsweredTreatment();
	} else {
	    if (cp.getJoinConfirmationTimeout() == 0) {
                setState(CallState.ESTABLISHED);
	    }
	}
    }

    /*
     * This is called for calls with an input treatment.
     * There is really no endpoint
     * and no data is ever sent to the call.
     * The looks like any other call but is used only as a source of sound.
     */
    public void setRemoteMediaInfo() throws ParseException {
	if (getState() != CallState.INVITED) {
	    Logger.println("Call " + cp
		+ ":  NSOutgoingCallAgent:  bad state "
		+ getState());
	    return;
	}

	MediaInfo mediaInfo = callHandler.getConferenceManager().getMediaInfo();

        InetSocketAddress isa = 
	    callHandler.getMember().getMemberReceiver().getReceiveAddress();

	Logger.println("Call " + cp
	    + ":  NSOutgoingCallAgent:  " + mediaInfo
	    + " remote " + isa);

        setEndpointAddress(isa, mediaInfo.getPayload(), 
	    mediaInfo.getPayload(), (byte) 0);

	if (callAnswered) {
	    Logger.writeFile("Call " + cp 
		+ ":  NSOutgoingCallAgent: done remote SDP");
	    return;
	}

	/*
	 * The CallParticipant has answered.
	 * If join confirmation is required, we remain in the
	 * INVITED state.  
	 */
	callAnswered = true;

	if (cp.getJoinConfirmationTimeout() == 0) {
	    setState(CallState.ANSWERED);
	}

	/*
	 * Start treatment if any and wait for it to finish.
	 * When the treatment finishes, notification will
	 * be delivered to our parent which will indicate
	 * we're ready for the conference.
	 *
	 * If there's no treatment to be played, we're ready now
	 * unless we're waiting for join confirmation..
	 */
	initializeCallAnsweredTreatment();

	if (callAnsweredTreatment != null) {
	    startCallAnsweredTreatment();
	} else {
	    if (cp.getJoinConfirmationTimeout() == 0) {
                setState(CallState.ESTABLISHED);
	    }
	}
    }

    public void terminateCall() {
    }

}