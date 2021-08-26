package com.gupshup.feature.phones.service;

import java.io.FileNotFoundException;
import java.io.IOException;

public interface TransmissionRequest {

	int make_retransmission_request_response(byte[] requestInputBuf, int i, byte[] transmissionDataBuf, int transBufLen,
			byte[] outputBuffer) throws FileNotFoundException, IOException;

	int make_request(byte[] inputBuffer, int input_buffer_len, byte[] cmd_identifier, int cmd_identifier_len,
			byte[] outputBuffer) throws FileNotFoundException, IOException;

}
