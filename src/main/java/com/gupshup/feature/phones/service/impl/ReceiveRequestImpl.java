package com.gupshup.feature.phones.service.impl;

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gupshup.feature.phones.constant.TransmissionControl;
import com.gupshup.feature.phones.service.LibCorrectService;
import com.gupshup.feature.phones.service.ReceiveRequest;

import libcorrect.reed_solomon.ReedSolomon;

@Service
public class ReceiveRequestImpl implements ReceiveRequest {

	private static final Logger fLogger = LogManager.getFormatterLogger("TransmissionServiceImpl");

	@Autowired
	private LibCorrectService libCorrectService;

	@Override
	public int make_request(byte[] input_buffer, int input_buffer_len, byte[] cmd_identifier, int cmd_identifier_len,
			byte[] output_buffer) {
		byte[] buffer_with_crc = { '\0' };
		int buffer_with_crc_len = 0;
		int encoded_buf_len = 0;
		int total_data_length = 0;
		int delim_len = TransmissionControl.TRANSMISSION_START_DELIM.length();
		int empty_bytes = TransmissionControl.EMPTY_BYTES_STR.length();
		int data_start_pos = empty_bytes + delim_len + cmd_identifier_len;

		if (input_buffer_len != 0) {
			if (TransmissionControl.LIBCORRECT_REED_SOLOMON) {
				/* Create reed solomon codec instance */
				ReedSolomon reedSolomon = new ReedSolomon(TransmissionControl.correctRsPrimitivePolynomialCcsds,
						(byte) 1, (byte) 1, TransmissionControl.MIN_DISTANCE);
				if (reedSolomon == null) {
					fLogger.trace("Reed Solomon Codec instance creation failed.");
					return -1;
				}

				buffer_with_crc_len = libCorrectService.apply_block_id_and_crc(input_buffer, input_buffer_len,
						buffer_with_crc);
				// fec_encoded_buf_len = reed_solomon_encode(crs, buffer_with_crc,
				// buffer_with_crc_len, fec_encoded_buffer+start_pos, MIN_DISTANCE,
				// MSG_BLOCK_LENGTH);
				output_buffer = libCorrectService.reed_solomon_encode_blocks_with_delims(reedSolomon, buffer_with_crc,
						buffer_with_crc_len, output_buffer, TransmissionControl.MIN_DISTANCE,
						TransmissionControl.MSG_BLOCK_LENGTH);
				encoded_buf_len = output_buffer.length;
				libCorrectService.correct_reed_solomon_destroy(reedSolomon);
			} else {
				buffer_with_crc_len = libCorrectService.apply_block_id_and_crc(input_buffer, input_buffer_len,
						buffer_with_crc);
				encoded_buf_len = libCorrectService.encode_blocks_with_delims_nofec(buffer_with_crc,
						buffer_with_crc_len, output_buffer, TransmissionControl.MSG_BLOCK_LENGTH);
			}

			if (encoded_buf_len == -1) {
				fLogger.trace("Encoding failed.");
				return -1;
			}
		}

		// Apply empty bytes
		// buffer starting empty_bytes bytes kept empty for safer side to avoid errors
		// while starting the transmission
		// and '!#!#!#' is placed after these empty_bytes empty bytes
		output_buffer = Arrays.copyOf(TransmissionControl.EMPTY_BYTES_STR.getBytes(), empty_bytes);
		// strncpy(output_buffer, EMPTY_BYTES_STR, empty_bytes);

		// Apply start delimiter
		output_buffer = Arrays.copyOf(output_buffer, empty_bytes);
		output_buffer = Arrays.copyOf(TransmissionControl.TRANSMISSION_START_DELIM.getBytes(), delim_len);
		// strncpy(output_buffer + empty_bytes, TRANSMISSION_START_DELIM, delim_len);

		// Apply Command Identifier to udentify the request type
		Arrays.copyOf(output_buffer, empty_bytes);
		Arrays.copyOf(output_buffer, delim_len);
		output_buffer = Arrays.copyOf(cmd_identifier, cmd_identifier_len);
		// strncpy(output_buffer + empty_bytes + delim_len, cmd_identifier,
		// cmd_identifier_len);

		// Apply End delimiter
		Arrays.copyOf(output_buffer, data_start_pos);
		Arrays.copyOf(output_buffer, encoded_buf_len);
		output_buffer = Arrays.copyOf(TransmissionControl.TRANSMISSION_END_DELIM.getBytes(), delim_len);
		// strncpy(output_buffer + data_start_pos + encoded_buf_len,
		// TRANSMISSION_END_DELIM, delim_len);
		// buffer 5 last byte is kept empty after palcing '$&$&$&' for safer side to
		// avoid errors while ending transmission

		// Apply empty bytes
		Arrays.copyOf(output_buffer, data_start_pos);
		Arrays.copyOf(output_buffer, encoded_buf_len);
		Arrays.copyOf(output_buffer, delim_len);
		output_buffer = Arrays.copyOf(TransmissionControl.EMPTY_BYTES_STR.getBytes(), empty_bytes);
		// strncpy(output_buffer + data_start_pos + encoded_buf_len + delim_len,
		// EMPTY_BYTES_STR, empty_bytes);

		// start empty bytes + start delimiter bytes + command Identifier
		// +fec_encoded_buf_len + end delimiter bytes + end empty bytes
		total_data_length = empty_bytes + delim_len + cmd_identifier_len + encoded_buf_len + delim_len + empty_bytes;

		return total_data_length;

	}

}
