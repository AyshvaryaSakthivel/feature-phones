package com.gupshup.feature.phones.service.impl;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gupshup.feature.phones.constant.TransmissionControl;
import com.gupshup.feature.phones.service.DecodeService;
import com.gupshup.feature.phones.service.LibCorrectService;

import libcorrect.reed_solomon.ReedSolomon;

@Service
public class DecodeServiceImpl implements DecodeService {

	private static final Logger fLogger = LogManager.getFormatterLogger("DecodeServiceImpl");
	private boolean asEmptyDelim = false;
	private boolean asTransmissionStartDelim = false;
	private boolean asTransmissionEndDelim = false;

	@Autowired
	private LibCorrectService libCorrectService;

	@Override
	public int decode_data(byte[] input_buffer, int length, byte[] output_buffer, byte[] invalid_blk_buffer,
			int is_retransmission) throws FileNotFoundException, IOException {
		int actual_data_len = 0;

		if (TransmissionControl.LIBCORRECT_REED_SOLOMON) {
			/* Create reed solomon codec instance */
			ReedSolomon reedSolomon = new ReedSolomon(TransmissionControl.correctRsPrimitivePolynomialCcsds, (byte) 1,
					(byte) 1, TransmissionControl.MIN_DISTANCE);
			if (reedSolomon == null) {
				fLogger.trace("Reed Solomon Codec instance creation failed.");
				return -1;
			}

			// Decode with fec
			// data_with_crc_len = reed_solomon_decode(crs, decoded_buffer_with_fec, len,
			// decoded_buffer_with_crc, MIN_DISTANCE,MSG_BLOCK_LENGTH);
			output_buffer = libCorrectService.reed_solomon_decode_blocks_with_delims_local(reedSolomon, input_buffer,
					length, output_buffer, invalid_blk_buffer, TransmissionControl.MIN_DISTANCE,
					TransmissionControl.MSG_BLOCK_LENGTH, is_retransmission);
			actual_data_len = output_buffer.length;
			libCorrectService.correct_reed_solomon_destroy(reedSolomon);
		} else {
			// Decode data no used fec
			actual_data_len = decode_blocks_with_delims_local_nofec(input_buffer, length, output_buffer,
					invalid_blk_buffer, TransmissionControl.MSG_BLOCK_LENGTH, is_retransmission);
		}

		try (FileOutputStream stream = new FileOutputStream(
				"D:\\tvs_next\\projects\\features\\features\\features\\src\\main\\resources\\Test1/receiveOutput.txt")) {
			for (byte print : output_buffer) {
				stream.write((byte) (print & 0xFF));
			}

		}

		return actual_data_len;

	}

	private int decode_blocks_with_delims_local_nofec(byte[] input_buffer, int length, byte[] output_buffer,
			byte[] invalid_blk_buffer, int msgBlockLength, int is_retransmission) {
		return is_retransmission;
	}

	@Override
	public byte[] minimodemDecode(byte[] input_buffer) {
		return removeLeadingEmptyDelimiter(input_buffer);
	}

	private byte[] removeLeadingEmptyDelimiter(byte[] input_buffer) {
		int i = 0;
		for (byte input : input_buffer) {
			if ((byte) TransmissionControl.EMPTY_BYTES_STR.charAt(i) == input)
				asEmptyDelim = true;
		}
		if (asEmptyDelim) {
			input_buffer = Arrays.copyOfRange(input_buffer, TransmissionControl.EMPTY_BYTES_STR.length(),
					input_buffer.length);
			input_buffer = Arrays.copyOf(input_buffer,
					input_buffer.length - TransmissionControl.EMPTY_BYTES_STR.length());
			input_buffer = removeTransmissionStartDelim(input_buffer);
			return removeTransmissionEndDelim(input_buffer);
		}
		return null;

	}

	private byte[] removeTransmissionEndDelim(byte[] input_buffer) {
		int i = 0;
		for (byte input : input_buffer) {
			if ((byte) TransmissionControl.TRANSMISSION_END_DELIM.charAt(i) == input)
				asTransmissionEndDelim = true;
		}
		if (asTransmissionEndDelim) {
			input_buffer = Arrays.copyOf(input_buffer,
					input_buffer.length - TransmissionControl.TRANSMISSION_END_DELIM.length());
		}
		return input_buffer;

	}

	private byte[] removeTransmissionStartDelim(byte[] input_buffer) {
		int i = 0;
		for (byte input : input_buffer) {
			if ((byte) TransmissionControl.TRANSMISSION_START_DELIM.charAt(i) == input)
				asTransmissionStartDelim = true;
		}
		if (asTransmissionStartDelim) {
			input_buffer = Arrays.copyOfRange(input_buffer, TransmissionControl.TRANSMISSION_START_DELIM.length(),
					input_buffer.length);
		}
		return input_buffer;

	}

}