package com.gupshup.feature.phones.enums;

public enum TransmissionStates {

	GUPSHUP_DATA_INVALID(0), GUPSHUP_TRANSMISSTION_END_REQUEST(1), GUPSHUP_RETRANSMISSTION_REQUEST(2),
	GUPSHUP_SEND_END_SIGNAL_ACK(3), GUPSHUP_NEED_DISCONNECTION_CALL(4), GUPSHUP_RESULT_MAX(5);

	public final int label;

	private TransmissionStates(int label) {
		this.label = label;
	}

}
