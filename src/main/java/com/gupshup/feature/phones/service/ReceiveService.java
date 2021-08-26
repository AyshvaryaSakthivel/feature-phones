package com.gupshup.feature.phones.service;

public interface ReceiveService {

	public int parseRequestAtReceiverSide(byte[] input_buffer, int length, byte[] output_buffer, byte[] request_buffer,
			int output_length);

}
