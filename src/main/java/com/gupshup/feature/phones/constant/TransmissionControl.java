package com.gupshup.feature.phones.constant;

public class TransmissionControl {

	public static final int MIN_DISTANCE = 16;
	public static final int MIN_DISTANCE_1 = 4;
	public static final int MSG_BLOCK_LENGTH = 48;
	public static final int MSG_BLOCK_LEN_BEORE_CRC_AND_BLK_ID = MSG_BLOCK_LENGTH - 2; // 1 byte for block Id and 1
																						// byte for checksum
	public static final int MAX_OUTPUT_LENGTH = 256;
	public static final int MAX_INPUT_LENGTH = 512;
	public static final int MAX_FEC_BUF_LENGTH = 256;
	public static int CMD_IDENTIFIER_LEN = 4;

	public static final int DELIM_SUB_LEN = 2;
	public static final int DELIM_LEN = 4;
	public static final String TRANSMISSION_START_SEQ = "!#";
	public static final String TRANSMISSION_END_SEQ = "$&";

	public static final String TRANSMISSION_START_DELIM = "!#!#";
	public static final String TRANSMISSION_END_DELIM = "$&$&";
	public static final String TRANSMISSION_END_SIGNAL_CMD_IDENTIFIER = "ENDS";
	public static final String TRANSMISSION_END_ACK_SIGNAL_CMD_IDENTIFIER = "ACKS";
	public static final String TRANSMISSION_SIGNAL_CMD_NO_DATA_PRAE = "NDAT";
	public static final String DATA_CMD_IDENTIFIER = "0000";
	public static final String RETRANSMISSION_CMD_IDENTIFIER = "ff";
	public static final String EMPTY_BYTES_STR = "#@#";

	public static final boolean LIBCORRECT_REED_SOLOMON = true;
	public static final String BLOCK_START_DELIM = "@%";
	public static final String BLOCK_END_DELIM = "^*";

	public static final short correctRsPrimitivePolynomialCcsds = 0x187;

}
