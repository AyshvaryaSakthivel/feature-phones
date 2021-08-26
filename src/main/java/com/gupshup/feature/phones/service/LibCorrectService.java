package com.gupshup.feature.phones.service;

import libcorrect.reed_solomon.ReedSolomon;

public interface LibCorrectService {

	int apply_block_id_and_crc(byte[] input_buffer, int input_buffer_len, byte[] buffer_with_crc);

	byte[] reed_solomon_encode_blocks_with_delims(ReedSolomon reedSolomon, byte[] buffer_with_crc,
			int buffer_with_crc_len, byte[] concat, int minDistance, int msgBlockLength);

	void correct_reed_solomon_destroy(ReedSolomon reedSolomon);

	int encode_blocks_with_delims_nofec(byte[] buffer_with_crc, int buffer_with_crc_len, byte[] concat,
			int msgBlockLength);

	byte[] reed_solomon_decode_blocks_with_delims(ReedSolomon reedSolomon, byte[] requestInputBuf, int len,
			byte[] outputBuffer, int minDistance, int msgBlockLength);

	int decode_blocks_with_delims_nofec(byte[] requestInputBuf, int len, byte[] decoded_buffer_with_crc,
			int msgBlockLength);

	int validate_and_remove_crc_block_id(byte[] decoded_buffer_with_crc, int data_with_crc_len, byte[] data_output);

	byte[] reed_solomon_decode_blocks_with_delims_local(ReedSolomon reedSolomon, byte[] input_buffer, int length,
			byte[] output_buffer, byte[] invalid_blk_buffer, int minDistance, int msgBlockLength,
			int is_retransmission);

}
