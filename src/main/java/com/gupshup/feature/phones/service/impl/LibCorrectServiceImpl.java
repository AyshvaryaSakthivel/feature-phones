package com.gupshup.feature.phones.service.impl;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.gupshup.feature.phones.constant.TransmissionControl;
import com.gupshup.feature.phones.service.LibCorrectService;

import libcorrect.reed_solomon.ReedSolomon;

@Service
public class LibCorrectServiceImpl implements LibCorrectService {

	private static final Logger fLogger = LogManager.getFormatterLogger("LibCorrectServiceImpl");

	private boolean as_block_start_delim = false;
	private boolean as_block_end_delim = false;
	Queue<Integer> byte_queue = new LinkedList<>();

	/*
	 * @Override public int apply_block_id_and_crc(byte[] input_buffer, int
	 * input_buffer_len, byte[] buffer_with_crc) { int i = 0, j = 0; int chunk_size
	 * = 0; byte checksum; byte block_id = 1;
	 * 
	 * for (i = 0; i < input_buffer_len; i +=
	 * TransmissionControl.MSG_BLOCK_LEN_BEORE_CRC_AND_BLK_ID) { if
	 * (input_buffer_len - i >=
	 * TransmissionControl.MSG_BLOCK_LEN_BEORE_CRC_AND_BLK_ID) { chunk_size =
	 * TransmissionControl.MSG_BLOCK_LEN_BEORE_CRC_AND_BLK_ID; } else { chunk_size =
	 * input_buffer_len - i; }
	 * 
	 * // Put the block id buffer_with_crc[j] = block_id; block_id++;
	 * 
	 * // Put data System.arraycopy(buffer_with_crc[buffer_with_crc.length + j + 1],
	 * 0, input_buffer[input_buffer.length + i], 0, input_buffer.length);
	 * 
	 * // Put checksum // checksum = checksum_generate_key(inbuf+i,chunk_size);
	 * checksum = decodeService.checksum_generate_key(buffer_with_crc, chunk_size +
	 * 1); j += chunk_size + 1;
	 * 
	 * buffer_with_crc[j] = checksum; j += 1; // Increment as checksum also placed
	 * in outbuf }
	 * 
	 * return j; }
	 */

	@Override
	public int apply_block_id_and_crc(byte[] inputBuffer, int input_buffer_len, byte[] crcBuffer) {
		int i = 0, j = 0;
		int chunk_size = 0;
		byte checksum;
		byte block_id = 1;

		for (i = 0; i < input_buffer_len; i += TransmissionControl.MSG_BLOCK_LEN_BEORE_CRC_AND_BLK_ID) {
			if (input_buffer_len - i >= TransmissionControl.MSG_BLOCK_LEN_BEORE_CRC_AND_BLK_ID) {
				chunk_size = TransmissionControl.MSG_BLOCK_LEN_BEORE_CRC_AND_BLK_ID;
			} else {
				chunk_size = input_buffer_len - i;
			}

			// Put the block id
			crcBuffer[j] = block_id;
			block_id++;

			// Put data
			for (byte input : inputBuffer) {
				crcBuffer[++j] = input;
			}

			// Put checksum
			// checksum = checksum_generate_key(inbuf+i,chunk_size);
			checksum = checksum_generate_key(crcBuffer, chunk_size + 1);
			j = chunk_size + 1;

			crcBuffer[j] = checksum;
			j += 1; // Increment as checksum also placed in outbuf
		}

		return j;
	}

	@Override
	public byte[] reed_solomon_encode_blocks_with_delims(ReedSolomon reedSolomon, byte[] crcBuffer,
			int buffer_with_crc_len, byte[] outputBuffer, int minDistance, int msgBlockLength) {
		int i = 0;
		int chunk_size = 0;
		int j = 0;

		for (i = 0; i < buffer_with_crc_len; i += msgBlockLength) {
			if (buffer_with_crc_len - i >= msgBlockLength) {
				chunk_size = msgBlockLength;
			} else {
				chunk_size = buffer_with_crc_len - i;
			}
			outputBuffer = new byte[TransmissionControl.BLOCK_START_DELIM.length() + chunk_size + minDistance
					+ TransmissionControl.BLOCK_END_DELIM.length()];

			// Put start delim of the block
			for (byte startdelim : TransmissionControl.BLOCK_START_DELIM.getBytes()) {
				outputBuffer[j] = startdelim;
				j++;
			}

			crcBuffer = Arrays.copyOf(crcBuffer, chunk_size);

			// Put the fec encoded block
			byte[] encodeBufferWithFec = reedSolomon.encode(crcBuffer);
			if (encodeBufferWithFec != null)
				for (byte encodefec : encodeBufferWithFec) {
					outputBuffer[j] = encodefec;
					j++;
				}

			// Put end delim of the block
			for (byte enddelim : TransmissionControl.BLOCK_END_DELIM.getBytes()) {
				outputBuffer[j] = enddelim;
				j++;
			}
		}
		return outputBuffer;

	}

	@Override
	public void correct_reed_solomon_destroy(ReedSolomon reedSolomon) {
		// TODO Auto-generated method stub

	}

	@Override
	public int encode_blocks_with_delims_nofec(byte[] buffer_with_crc, int buffer_with_crc_len, byte[] encoded,
			int msgBlockLength) {
		int i = 0;
		int chunk_size = 0;
		int encoded_buf_len = 0;
		int j = 0;

		for (i = 0; i < buffer_with_crc_len; i += msgBlockLength) {
			if (buffer_with_crc_len - i >= msgBlockLength) {
				chunk_size = msgBlockLength;
			} else {
				chunk_size = buffer_with_crc_len - i;
			}

			encoded = new byte[TransmissionControl.BLOCK_START_DELIM.length() + chunk_size
					+ TransmissionControl.BLOCK_END_DELIM.length()];

			// Put start delim of the block
			for (byte startdelim : TransmissionControl.BLOCK_START_DELIM.getBytes()) {
				encoded[j] = startdelim;
				j++;
			}

			// Put start delim of the block
			encoded = Arrays.copyOf(TransmissionControl.BLOCK_START_DELIM.getBytes(),
					TransmissionControl.BLOCK_START_DELIM.length());
			encoded_buf_len += TransmissionControl.BLOCK_START_DELIM.length();

			// Put the encoded block
			for (byte crcBuffer : buffer_with_crc) {
				encoded[j] = crcBuffer;
				j++;
			}

			// Put end delim of the block
			for (byte enddelim : TransmissionControl.BLOCK_END_DELIM.getBytes()) {
				encoded[j] = enddelim;
				j++;
			}
		}
		return encoded.length;
	}

	@Override
	public byte[] reed_solomon_decode_blocks_with_delims_local(ReedSolomon reedSolomon, byte[] encoded_msg,
			int encoded_length, byte[] decoded_msg, byte[] invalid_blk_ids_buf, int minDistance, int msgBlockLength,
			int is_retransmission) {

		int data_bytes = 0;
		int block_length = msgBlockLength + minDistance;
		byte[] input_start = encoded_msg;
		byte[] removed_blck_delim = encoded_msg;
		byte[] input_end = null;
		byte[] block_delim_arr = null;
		byte[] encoded_msg_end = new byte[encoded_length];
		byte[] decoded_buf = new byte[msgBlockLength + 1];
		int block_id = 0;
		int max_block_id = 0;
		int res = 0;
		int i = 0;
		int remove_block_index = 0;

		while (input_start.length <= encoded_msg_end.length) {
			for (byte input : input_start) {
				if ((byte) TransmissionControl.BLOCK_START_DELIM.charAt(i) == input)
					as_block_start_delim = true;
			}
			if (as_block_start_delim)
				input_start = Arrays.copyOfRange(input_start, TransmissionControl.BLOCK_START_DELIM.length(),
						input_start.length);

			for (byte input : input_start) {
				if ((byte) TransmissionControl.BLOCK_END_DELIM.charAt(i) == input)
					as_block_end_delim = true;
			}
			if (as_block_end_delim)
				input_end = Arrays.copyOf(input_start,
						input_start.length - TransmissionControl.BLOCK_END_DELIM.length());

			if (input_end != null) {
				if (input_end.length > 0 && input_end.length <= block_length) {
					decoded_buf = reedSolomon.decode(input_end);
					block_id = input_start[0];
					fLogger.trace("fec decode len = %d", decoded_buf.length);

					if (decoded_buf != null && decoded_buf.length != 0) {
						res = validate_crc(decoded_buf, decoded_buf.length - 1);
					}

					if (is_retransmission == 1 || GupshupIsNeedRetransmission()) {
						if (decoded_buf != null && res != -1) {
							block_id = (byte) (block_id >> 4);

							// Remove block id from queue
							GupshupDeleteOneRetransmissionBlockIdFromQueue(block_id);
							// Not taking block Id and crc into actual decoded data
							// memcpy(decoded_msg + ((block_id - 1) * (MSG_BLOCK_LENGTH - 2)), decoded_buf +
							// 1, len - 2);
							data_bytes += decoded_buf.length - 2;
						}
					} else {
						if (decoded_buf != null && res != -1) {
							int j = 0;

							max_block_id = block_id & 0x0f;
							// block_id = (byte) (block_id >> 4);

							// save all block-id
							for (i = 1; i <= max_block_id; i++) {
								// add retransmission block id into queue
								GupshupAddRetransmissionBlockIdToQueue(block_id);
							}

							// Remove block id from queue
							GupshupDeleteOneRetransmissionBlockIdFromQueue(block_id);
							// Not taking block Id and crc into actual decoded data
							// memcpy(decoded_msg + ((block_id - 1) * (MSG_BLOCK_LENGTH - 2)), decoded_buf +
							// 1, len - 2);
							data_bytes += decoded_buf.length - 2;
						}
					}
					input_start = new byte[input_end.length + TransmissionControl.BLOCK_START_DELIM.length()
							+ TransmissionControl.BLOCK_END_DELIM.length() + 1];
				}
			}
		}
		/*
		 * // decode data failed if (is_retransmission == 0 &&
		 * !GupshupIsNeedRetransmission()) { return -1; }
		 */

		decoded_buf = Arrays.copyOfRange(decoded_buf, 1, decoded_buf.length - 1);

		// get all need retransmission block id from queue
		GupshupGetAllNeedRetransmissionBlockID(invalid_blk_ids_buf);

		return decoded_buf;
	}

	private int GupshupGetAllNeedRetransmissionBlockID(byte[] invalid_blk_ids_buf) {
		return 0;

	}

	private boolean GupshupIsNeedRetransmission() {
		// return gupshup_thread_info_ptr -> is_need_retransmission;
		return false;
	}

	private void GupshupAddRetransmissionBlockIdToQueue(int block_id) {
		byte_queue.add(block_id);
	}

	private void GupshupDeleteOneRetransmissionBlockIdFromQueue(int block_id) {
		byte_queue.remove(block_id);
	}

	private int validate_crc(byte[] decoded_buf, int decoded_buf_len) {
		byte checksum;
		// checksum calculated on block id and block data both
		checksum = checksum_generate_key(decoded_buf, decoded_buf_len);

		if (checksum != decoded_buf[decoded_buf.length - 1]) {
			fLogger.trace("Checksum validation failed.");
			return -1;
		}

		return 0;
	}

	public byte checksum_generate_key(byte[] decoded_buf, int decoded_buf_len) {
		int i, sum = 0;
		byte key;
		for (i = 0; i < decoded_buf_len; i++)
			sum += (decoded_buf[i]);

		// mask and convert to 2's complement
		key = (byte) (~(sum & 0x00ff) + 1);

		return key;
	}

	@Override
	public byte[] reed_solomon_decode_blocks_with_delims(ReedSolomon reedSolomon, byte[] encoded_msg, int encoded_len,
			byte[] decoded_msg, int minDistance, int msgBlockLength) {
		int decoded_bytes = 0;
		int block_length = msgBlockLength + minDistance;
		byte[] input_start = encoded_msg;
		byte[] input_end = null;
		int encoded_msg_end = encoded_msg.length + encoded_len - 1;
		int i = 0;

		while (input_start.length <= encoded_msg_end) {
			for (byte input : input_start) {
				if ((byte) TransmissionControl.BLOCK_START_DELIM.charAt(i) == input)
					as_block_start_delim = true;
			}
			if (as_block_start_delim)
				input_start = Arrays.copyOfRange(input_start, TransmissionControl.BLOCK_START_DELIM.length(),
						input_start.length);

			for (byte input : input_start) {
				if ((byte) TransmissionControl.BLOCK_END_DELIM.charAt(i) == input)
					as_block_end_delim = true;
			}
			if (as_block_end_delim)
				input_end = Arrays.copyOf(input_start,
						input_start.length - TransmissionControl.BLOCK_END_DELIM.length());

			if (input_end != null) {
				if (input_end.length > 0 && input_end.length <= block_length) {
					decoded_msg = reedSolomon.decode(input_end);
					if (decoded_msg == null) {
						// for testing purpose not returning -1 now, so that further blocks can be
						// decoded
						// return res;
					} else
						decoded_bytes += decoded_msg.length;
					// input_start = input_end + TransmissionControl.BLOCK_END_DELIM.length();
				}
			} else
				break;
		}
		return decoded_msg;
	}

	@Override
	public int decode_blocks_with_delims_nofec(byte[] encoded_msg, int encoded_length, byte[] decoded_msg,
			int msgBlockLength) {
		int chunk_size = 0;
		int decoded_bytes = 0;
		int block_length = msgBlockLength;
		byte[] input_start = encoded_msg;
		byte[] input_end = null;
		int encoded_msg_end = encoded_msg.length + encoded_length - 1;
		int i = 0;

		while (input_start.length <= encoded_msg_end) {
			for (byte input : input_start) {
				if ((byte) TransmissionControl.BLOCK_START_DELIM.charAt(i) == input)
					as_block_start_delim = true;
			}
			if (as_block_start_delim)
				input_start = Arrays.copyOfRange(input_start, TransmissionControl.BLOCK_START_DELIM.length(),
						input_start.length);

			for (byte input : input_start) {
				if ((byte) TransmissionControl.BLOCK_END_DELIM.charAt(i) == input)
					as_block_end_delim = true;
			}
			if (as_block_end_delim)
				input_end = Arrays.copyOf(input_start,
						input_start.length - TransmissionControl.BLOCK_END_DELIM.length());

			if (input_end != null) {
				if (input_end.length > 0 && input_end.length <= block_length) {
					chunk_size = input_end.length - input_start.length;
					// memcpy(decoded_msg + decoded_bytes, input_start, chunk_size);
					decoded_bytes += chunk_size;

					// input_start = input_end + TransmissionControl.BLOCK_END_DELIM.length();
				}
			} else
				break;
		}

		return decoded_bytes;
	}

	@Override
	public int validate_and_remove_crc_block_id(byte[] inbuf, int inbuf_length, byte[] outbuf) {
		int i = 0, j = 0;
		int chunk_size = 0;
		byte checksum;

		for (i = 0; i < inbuf_length; i += TransmissionControl.MSG_BLOCK_LENGTH) {
			if (inbuf_length - i >= TransmissionControl.MSG_BLOCK_LENGTH) {
				chunk_size = TransmissionControl.MSG_BLOCK_LEN_BEORE_CRC_AND_BLK_ID;
			} else {
				chunk_size = (inbuf_length - i - 2);
			}
			// To add block id length also to chunk_size as checksum was calculated on block
			// id and block data both
			checksum = checksum_generate_key(inbuf, chunk_size + 1);

			if (checksum != inbuf[i + chunk_size + 1]) {
				fLogger.trace("Checksum validation failed.");
				return -1;
			}

			// memcpy(outbuf + j, inbuf + i + 1, chunk_size);
			j += chunk_size;
		}

		return j;

	}

}
