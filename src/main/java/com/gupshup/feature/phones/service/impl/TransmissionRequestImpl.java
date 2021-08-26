package com.gupshup.feature.phones.service.impl;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gupshup.feature.phones.constant.TransmissionControl;
import com.gupshup.feature.phones.service.DecodeService;
import com.gupshup.feature.phones.service.LibCorrectService;
import com.gupshup.feature.phones.service.TransmissionRequest;

import libcorrect.reed_solomon.ReedSolomon;

@Component
public class TransmissionRequestImpl implements TransmissionRequest {

	private static final Logger fLogger = LogManager.getFormatterLogger("TransmissionServiceImpl");

	@Autowired
	private LibCorrectService libCorrectService;

	@Autowired
	private DecodeService decodeService;

	@Override
	public int make_request(byte[] inputBuffer, int input_buffer_len, byte[] cmd_identifier, int cmd_identifier_len,
			byte[] outputBuffer) throws FileNotFoundException, IOException {
		byte[] crcBuffer = new byte[TransmissionControl.MAX_INPUT_LENGTH];
		int buffer_with_crc_len = 0;
		int encoded_buf_len = 0;
		int total_data_length = 0;
		int delim_len = TransmissionControl.TRANSMISSION_START_DELIM.length();
		int end_delim_len = TransmissionControl.TRANSMISSION_END_DELIM.length();
		int empty_bytes = TransmissionControl.EMPTY_BYTES_STR.length();
		int data_start_pos = empty_bytes + delim_len + cmd_identifier_len + end_delim_len + empty_bytes;

		if (input_buffer_len != 0) {
			if (TransmissionControl.LIBCORRECT_REED_SOLOMON) {
				/* Create reed solomon codec instance */
				ReedSolomon reedSolomon = new ReedSolomon(TransmissionControl.correctRsPrimitivePolynomialCcsds,
						(byte) 1, (byte) 1, TransmissionControl.MIN_DISTANCE);
				if (reedSolomon != null)
					fLogger.trace("Reed Solomon Codec instance creation Success.");
				else
					fLogger.trace("Reed Solomon Codec instance creation failed.", -1);

				buffer_with_crc_len = libCorrectService.apply_block_id_and_crc(inputBuffer, input_buffer_len,
						crcBuffer);
				// fec_encoded_buf_len = reed_solomon_encode(crs, buffer_with_crc,
				// buffer_with_crc_len, fec_encoded_buffer+start_pos, MIN_DISTANCE,
				// MSG_BLOCK_LENGTH);
				outputBuffer = libCorrectService.reed_solomon_encode_blocks_with_delims(reedSolomon, crcBuffer,
						buffer_with_crc_len, outputBuffer, TransmissionControl.MIN_DISTANCE,
						TransmissionControl.MSG_BLOCK_LENGTH);
				encoded_buf_len = outputBuffer.length;
				libCorrectService.correct_reed_solomon_destroy(reedSolomon);
			} else {
				buffer_with_crc_len = libCorrectService.apply_block_id_and_crc(inputBuffer, input_buffer_len,
						crcBuffer);
				encoded_buf_len = libCorrectService.encode_blocks_with_delims_nofec(crcBuffer, buffer_with_crc_len,
						outputBuffer, TransmissionControl.MSG_BLOCK_LENGTH);
			}

			if (encoded_buf_len == 0) {
				fLogger.trace("Encoding failed.");
				return -1;
			}
		}
		int j = 0;
		byte[] resultBuffer = new byte[outputBuffer.length + data_start_pos];
		// Apply empty bytes
		// buffer starting empty_bytes bytes kept empty for safer side to avoid errors
		// while starting the transmission
		// and '!#!#!#' is placed after these empty_bytes empty bytes
		for (byte emptyStr : TransmissionControl.EMPTY_BYTES_STR.getBytes()) {
			resultBuffer[j] = emptyStr;
			j++;
		}

		// Apply start delimiter
		for (byte transmissionStartDelim : TransmissionControl.TRANSMISSION_START_DELIM.getBytes()) {
			resultBuffer[j] = transmissionStartDelim;
			j++;
		}

		// Apply Command Identifier to udentify the request type
		for (byte cmdIdn : cmd_identifier) {
			resultBuffer[j] = cmdIdn;
			j++;
		}

		// Apply Encoded block
		for (byte output : outputBuffer) {
			resultBuffer[j] = output;
			j++;
		}

		// Apply End delimiter
		for (byte transmissionEndDelim : TransmissionControl.TRANSMISSION_END_DELIM.getBytes()) {
			resultBuffer[j] = transmissionEndDelim;
			j++;
		}
		// buffer 5 last byte is kept empty after palcing '$&$&$&' for safer side to
		// avoid errors while ending transmission

		// Apply empty bytes
		for (byte emptyStr : TransmissionControl.EMPTY_BYTES_STR.getBytes()) {
			resultBuffer[j] = emptyStr;
			j++;
		}
		// start empty bytes + start delimiter bytes + command Identifier
		// +fec_encoded_buf_len + end delimiter bytes + end empty bytes
		total_data_length = empty_bytes + delim_len + cmd_identifier_len + encoded_buf_len + delim_len + empty_bytes;

		try (FileOutputStream stream = new FileOutputStream(
				"D:\\tvs_next\\projects\\features\\features\\features\\src\\main\\resources\\Test1/transmitOutput.txt")) {
			for (byte print : resultBuffer) {
				stream.write((byte) (print & 0xFF));
			}

		}

		return total_data_length;

	}

	@Override
	public int make_retransmission_request_response(byte[] requestInputBuf, int len, byte[] transmissionDataBuf,
			int transBufLen, byte[] outputBuffer) throws FileNotFoundException, IOException {
		byte[] data_output = new byte[TransmissionControl.MAX_OUTPUT_LENGTH];
		byte[] decoded_buffer_with_crc = new byte[TransmissionControl.MAX_FEC_BUF_LENGTH];
		int data_with_crc_len = 0;
		int actual_data_len = 0;
		int i = 0;
		int index = 0;
		int index_in_transmission_buf = 0;
		int blk_start_pos = TransmissionControl.EMPTY_BYTES_STR.length()
				+ TransmissionControl.TRANSMISSION_START_DELIM.length() + TransmissionControl.CMD_IDENTIFIER_LEN;
		int retrans_data_len = 0;
		int block_len = 0;
		int block_id_index = 0;
		int chunk_size = 0;
		if (TransmissionControl.LIBCORRECT_REED_SOLOMON) {
			/* Create reed solomon codec instance */
			ReedSolomon reedSolomon = new ReedSolomon(TransmissionControl.correctRsPrimitivePolynomialCcsds, (byte) 1,
					(byte) 1, TransmissionControl.MIN_DISTANCE);
			if (reedSolomon != null)
				fLogger.trace("Reed Solomon Codec instance creation Success.");
			else
				fLogger.trace("Reed Solomon Codec instance creation failed.", -1);

			// Decode with fec
			outputBuffer = libCorrectService.reed_solomon_decode_blocks_with_delims(reedSolomon, requestInputBuf, len,
					outputBuffer, TransmissionControl.MIN_DISTANCE, TransmissionControl.MSG_BLOCK_LENGTH);
			data_with_crc_len = outputBuffer.length;
			// destory crs
			libCorrectService.correct_reed_solomon_destroy(reedSolomon);
			block_len = TransmissionControl.BLOCK_START_DELIM.length() + TransmissionControl.MSG_BLOCK_LENGTH
					+ TransmissionControl.MIN_DISTANCE + TransmissionControl.BLOCK_END_DELIM.length();
		} else
			data_with_crc_len = libCorrectService.decode_blocks_with_delims_nofec(requestInputBuf, len,
					decoded_buffer_with_crc, TransmissionControl.MSG_BLOCK_LENGTH);

		block_len = TransmissionControl.BLOCK_START_DELIM.length() + TransmissionControl.MSG_BLOCK_LENGTH
				+ TransmissionControl.BLOCK_END_DELIM.length();

		if (data_with_crc_len <= 0) {
			fLogger.trace("Fec decoding failed.");
			return -1;
		}
		// Remove checksum from data
		actual_data_len = libCorrectService.validate_and_remove_crc_block_id(decoded_buffer_with_crc, data_with_crc_len,
				data_output);
		// data_output contains now block ids seperated by command
		// Example: 5,9,14 etc...

		if (actual_data_len == -1) {
			fLogger.trace("CRC validation failed.");
			return -1;
		}

		// remove transmission end delims and empty bytes from trans_buf_len
		transBufLen = transBufLen - TransmissionControl.EMPTY_BYTES_STR.length()
				+ TransmissionControl.TRANSMISSION_END_DELIM.length();
		for (i = 0; i < actual_data_len; i = i + 2) {
			fLogger.trace("Block Id = %d " + data_output[i]);
			index_in_transmission_buf = blk_start_pos + (data_output[i] - 1) * block_len;
			block_id_index = index_in_transmission_buf + TransmissionControl.BLOCK_START_DELIM.length();// block Id
																										// index in
																										// transmission
			// data
			if (block_id_index < transBufLen && data_output[i] == transmissionDataBuf[block_id_index] >> 4) {
				chunk_size = (transBufLen >= index_in_transmission_buf + block_len) ? block_len
						: (transBufLen - index_in_transmission_buf);
				// memcpy(output_buffer + retrans_data_len + blk_start_pos,
				// transmissionDataBuf + index_in_transmission_buf, chunk_size);
				retrans_data_len += chunk_size;
			}
		}

		// For time being passing ff01, in future will replace with actual retry number
		return apply_delimiters("ff01".getBytes(), TransmissionControl.CMD_IDENTIFIER_LEN, retrans_data_len,
				outputBuffer);

	}

	private int apply_delimiters(byte[] cmd_identifier, int cmd_identifier_len, int fec_encoded_buf_len,
			byte[] outputBuffer) throws FileNotFoundException, IOException {
		int delim_len = TransmissionControl.TRANSMISSION_START_DELIM.length();
		int end_delim_len = TransmissionControl.TRANSMISSION_END_DELIM.length();
		int empty_bytes = TransmissionControl.EMPTY_BYTES_STR.length();
		int data_start_pos = empty_bytes + delim_len + cmd_identifier_len + end_delim_len + empty_bytes;
		int total_data_length = 0;

		int j = 0;
		byte[] resultBuffer = new byte[outputBuffer.length + data_start_pos];
		// Apply empty bytes
		// buffer starting empty_bytes bytes kept empty for safer side to avoid errors
		// while starting the transmission
		// and '!#!#!#' is placed after these empty_bytes empty bytes
		for (byte emptyStr : TransmissionControl.EMPTY_BYTES_STR.getBytes()) {
			resultBuffer[j] = emptyStr;
			j++;
		}

		// Apply start delimiter
		for (byte transmissionStartDelim : TransmissionControl.TRANSMISSION_START_DELIM.getBytes()) {
			resultBuffer[j] = transmissionStartDelim;
			j++;
		}

		// Apply Command Identifier to udentify the request type
		for (byte cmdIdn : cmd_identifier) {
			resultBuffer[j] = cmdIdn;
			j++;
		}

		// Apply Encoded block
		for (byte output : outputBuffer) {
			resultBuffer[j] = output;
			j++;
		}

		// Apply End delimiter
		for (byte transmissionEndDelim : TransmissionControl.TRANSMISSION_END_DELIM.getBytes()) {
			resultBuffer[j] = transmissionEndDelim;
			j++;
		}
		// buffer 5 last byte is kept empty after palcing '$&$&$&' for safer side to
		// avoid errors while ending transmission

		// Apply empty bytes
		for (byte emptyStr : TransmissionControl.EMPTY_BYTES_STR.getBytes()) {
			resultBuffer[j] = emptyStr;
			j++;
		}
		// start empty bytes + start delimiter bytes + command Identifier
		// +fec_encoded_buf_len + end delimiter bytes + end empty bytes
		total_data_length = empty_bytes + delim_len + cmd_identifier_len + fec_encoded_buf_len + delim_len
				+ empty_bytes;

		try (FileOutputStream stream = new FileOutputStream(
				"D:\\tvs_next\\projects\\features\\features\\features\\src\\main\\resources\\Test1/retransmitOutput.txt")) {
			for (byte print : resultBuffer) {
				stream.write((byte) (print & 0xFF));
			}

		}

		return total_data_length;

	}

}