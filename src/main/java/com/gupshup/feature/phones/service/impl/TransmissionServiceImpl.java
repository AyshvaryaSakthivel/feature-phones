package com.gupshup.feature.phones.service.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gupshup.feature.phones.constant.TransmissionControl;
import com.gupshup.feature.phones.enums.TransmissionStates;
import com.gupshup.feature.phones.service.DecodeService;
import com.gupshup.feature.phones.service.TransmissionRequest;
import com.gupshup.feature.phones.service.TransmissionService;

@Service
public class TransmissionServiceImpl implements TransmissionService {

	private static final Logger fLogger = LogManager.getFormatterLogger("TransmissionServiceImpl");

	@Autowired
	private DecodeService decodeService;

	@Autowired
	private TransmissionRequest transmissionRequest;

	private byte b;

	@Override
	public int parseRequestAtSenderSide(byte[] requestInputBuf, int length, byte[] transmissionDataBuf, int transBufLen,
			byte[] outputBuffer, int outputLength) throws FileNotFoundException, IOException {
		byte[] cmdIdentifier = new byte[TransmissionControl.CMD_IDENTIFIER_LEN];
		cmdIdentifier = Arrays.copyOf(requestInputBuf, TransmissionControl.CMD_IDENTIFIER_LEN);
		requestInputBuf = Arrays.copyOfRange(requestInputBuf, TransmissionControl.CMD_IDENTIFIER_LEN,
				requestInputBuf.length);

		if ((Arrays.equals(TransmissionControl.DATA_CMD_IDENTIFIER.getBytes(), cmdIdentifier)
				&& cmdIdentifier.length == TransmissionControl.CMD_IDENTIFIER_LEN)) {
			outputLength = transmissionRequest.make_request(requestInputBuf, requestInputBuf.length,
					TransmissionControl.DATA_CMD_IDENTIFIER.getBytes(), TransmissionControl.CMD_IDENTIFIER_LEN,
					outputBuffer);
			fLogger.trace("Transmission End Signal Ack response.");
			return TransmissionStates.GUPSHUP_SEND_END_SIGNAL_ACK.label;
		}
		if ((Arrays.equals(TransmissionControl.TRANSMISSION_END_SIGNAL_CMD_IDENTIFIER.getBytes(), cmdIdentifier)
				&& cmdIdentifier.length == TransmissionControl.CMD_IDENTIFIER_LEN)) {
			outputLength = transmissionRequest.make_request(null, 0,
					TransmissionControl.TRANSMISSION_END_ACK_SIGNAL_CMD_IDENTIFIER.getBytes(),
					TransmissionControl.CMD_IDENTIFIER_LEN, outputBuffer);
			fLogger.trace("Transmission End Signal Ack response.");
			return TransmissionStates.GUPSHUP_SEND_END_SIGNAL_ACK.label;

		} else if ((Arrays.equals(TransmissionControl.RETRANSMISSION_CMD_IDENTIFIER.getBytes(), cmdIdentifier)
				&& cmdIdentifier.length == 2)) {
			outputLength = transmissionRequest.make_retransmission_request_response(requestInputBuf,
					length - TransmissionControl.CMD_IDENTIFIER_LEN, transmissionDataBuf, transBufLen, outputBuffer);
			fLogger.trace("Retransmission request response.");
			return TransmissionStates.GUPSHUP_RETRANSMISSTION_REQUEST.label;
		} else if ((Arrays.equals(TransmissionControl.TRANSMISSION_SIGNAL_CMD_NO_DATA_PRAE.getBytes(), cmdIdentifier)
				&& cmdIdentifier.length == TransmissionControl.CMD_IDENTIFIER_LEN))
		// Retransmission request
		{
			fLogger.trace("Retransmission all data request response.");
			return TransmissionStates.GUPSHUP_DATA_INVALID.label;
		}

		// Can be extended to handle more requests if required
		fLogger.trace("Invalid data.");

		return TransmissionStates.GUPSHUP_DATA_INVALID.label;

	}
}
