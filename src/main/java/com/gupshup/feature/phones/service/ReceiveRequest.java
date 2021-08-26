package com.gupshup.feature.phones.service;

public interface ReceiveRequest {

	int make_request(byte[] input_buffer, int input_buffer_len, byte[] cmd_identifier, int cmd_identifier_len,
			byte[] output_buffer);

}
