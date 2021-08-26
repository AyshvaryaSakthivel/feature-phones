package com.gupshup.feature.phones.service;

import java.io.FileNotFoundException;
import java.io.IOException;

public interface TransmissionService {

	public int parseRequestAtSenderSide(byte[] requestInputBuf, int length, byte[] transmissionDataBuf, int transBufLen,
			byte[] outputBuffer, int outputLength) throws FileNotFoundException, IOException;

}
