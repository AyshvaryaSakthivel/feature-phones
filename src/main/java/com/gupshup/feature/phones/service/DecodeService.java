package com.gupshup.feature.phones.service;

import java.io.FileNotFoundException;
import java.io.IOException;

public interface DecodeService {

	int decode_data(byte[] input_buffer, int length, byte[] output_buffer, byte[] invalid_blk_buffer,
			int is_retransmission) throws FileNotFoundException, IOException;

	byte[] minimodemDecode(byte[] input_buffer);

}
