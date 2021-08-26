package com.gupshup.feature.phones.service.impl;

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gupshup.feature.phones.constant.TransmissionControl;
import com.gupshup.feature.phones.enums.TransmissionStates;
import com.gupshup.feature.phones.service.DecodeService;
import com.gupshup.feature.phones.service.ReceiveRequest;
import com.gupshup.feature.phones.service.ReceiveService;

@Service
public class ReceiveServiceImpl implements ReceiveService {

	private static final Logger fLogger = LogManager.getFormatterLogger("TransmissionServiceImpl");

	@Autowired
	private DecodeService decodeService;

	@Autowired
	private ReceiveRequest receiveRequest;

	@Override
	public int parseRequestAtReceiverSide(byte[] input_buffer, int length, byte[] output_buffer, byte[] request_buffer,
			int output_length) {
		int is_retransmission = 0;
		byte[] cmdIdentifier;
		byte[] invalid_blk_buffer = null;
		try {
			cmdIdentifier = Arrays.copyOf(input_buffer, TransmissionControl.CMD_IDENTIFIER_LEN);
			input_buffer = Arrays.copyOfRange(input_buffer, TransmissionControl.CMD_IDENTIFIER_LEN,
					input_buffer.length);
			length -= TransmissionControl.CMD_IDENTIFIER_LEN; // Skip command identifier
			if ((Arrays.equals(TransmissionControl.DATA_CMD_IDENTIFIER.getBytes(), cmdIdentifier)
					&& cmdIdentifier.length == TransmissionControl.CMD_IDENTIFIER_LEN
					&& TransmissionControl.DATA_CMD_IDENTIFIER
							.getBytes().length == TransmissionControl.CMD_IDENTIFIER_LEN)
					|| (Arrays.equals(TransmissionControl.DATA_CMD_IDENTIFIER.getBytes(), cmdIdentifier)
							&& cmdIdentifier.length == TransmissionControl.CMD_IDENTIFIER_LEN
							&& TransmissionControl.DATA_CMD_IDENTIFIER
									.getBytes().length == TransmissionControl.CMD_IDENTIFIER_LEN)) {
				if (Arrays.equals(TransmissionControl.RETRANSMISSION_CMD_IDENTIFIER.getBytes(), cmdIdentifier)
						&& cmdIdentifier.length == TransmissionControl.CMD_IDENTIFIER_LEN
						&& TransmissionControl.RETRANSMISSION_CMD_IDENTIFIER
								.getBytes().length == TransmissionControl.CMD_IDENTIFIER_LEN) {
					is_retransmission = 1;
				}

				if (decodeService.decode_data(input_buffer, length, output_buffer, invalid_blk_buffer,
						is_retransmission) == -1) {
					fLogger.trace("decoded_buf_len val = -1");
					return TransmissionStates.GUPSHUP_DATA_INVALID.label;
				} else {
					fLogger.trace("Transmission End request.");
					return TransmissionStates.GUPSHUP_TRANSMISSTION_END_REQUEST.label;
				}

				/*
				 * fLogger.
				 * trace("parse_data_and_make_request_at_receiver_side:invalid_blk_buffer len =%d"
				 * , invalid_blk_buffer.length);
				 * 
				 * if (invalid_blk_buffer.length == 0) { // Transmission End Request
				 * fLogger.trace("Data received successfully, no invalid block."); output_length
				 * = receiveRequest.make_request(null, 0,
				 * TransmissionControl.TRANSMISSION_END_SIGNAL_CMD_IDENTIFIER.getBytes(),
				 * TransmissionControl.CMD_IDENTIFIER_LEN, request_buffer);
				 * fLogger.trace("Transmission End request."); return
				 * TransmissionStates.GUPSHUP_TRANSMISSTION_END_REQUEST.label;
				 * 
				 * } else { // Retransmission request // For time being use ff01, in future it
				 * will be ff{retry_num} output_length =
				 * receiveRequest.make_request(invalid_blk_buffer, invalid_blk_buffer.length,
				 * "ff01".getBytes(), TransmissionControl.CMD_IDENTIFIER_LEN, request_buffer);
				 * fLogger.trace("Retransmission request."); if (output_length == -1) { return
				 * TransmissionStates.GUPSHUP_DATA_INVALID.label; } else { return
				 * TransmissionStates.GUPSHUP_RETRANSMISSTION_REQUEST.label; } }
				 */
			} else if (Arrays.equals(TransmissionControl.TRANSMISSION_END_ACK_SIGNAL_CMD_IDENTIFIER.getBytes(),
					cmdIdentifier) && cmdIdentifier.length == TransmissionControl.CMD_IDENTIFIER_LEN
					&& TransmissionControl.TRANSMISSION_END_ACK_SIGNAL_CMD_IDENTIFIER
							.getBytes().length == TransmissionControl.CMD_IDENTIFIER_LEN)// End Signal Ack
			{
				// Disconnect Call
				fLogger.trace("Disconnect Call request.");
				return TransmissionStates.GUPSHUP_NEED_DISCONNECTION_CALL.label;
			}
			fLogger.trace("Invalid data.");
			return TransmissionStates.GUPSHUP_DATA_INVALID.label;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return is_retransmission;

	}

}
