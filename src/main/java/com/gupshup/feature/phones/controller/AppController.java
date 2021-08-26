package com.gupshup.feature.phones.controller;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.gupshup.feature.phones.enums.TransmissionStates;
import com.gupshup.feature.phones.response.ResponseMessage;
import com.gupshup.feature.phones.service.DecodeService;
import com.gupshup.feature.phones.service.ReceiveService;
import com.gupshup.feature.phones.service.TransmissionService;

import libcorrect.reed_solomon.ReedSolomon;
import minimodem.Minimodem;
import picocli.CommandLine;

@RestController
@RequestMapping(path = "/api")
public class AppController {

	@Autowired
	private Environment env;

	@Autowired
	private TransmissionService transmissionService;

	@Autowired
	private ReceiveService receiveService;

	@Autowired
	private DecodeService decodeService;

	@Value("${receiver.output.file}")
	private String receiverResult;

	private final String baseFileDestination = "D:\\tvs_next\\projects\\features\\features\\features\\src\\main\\resources\\Test1\\";
	public static final short correctRsPrimitivePolynomialCcsds = 0x187; // x^8 + x^7 + x^2 + x + 1

	public static byte[] transmissionDataBuf;
	public static int transBufLen;
	public static byte[] outputBuffer;
	public static int outputLength;

	@PostMapping("/transmit")
	public ResponseEntity<?> transmitMessage() throws IOException {
		byte[] requestInputBuf = null;
		try {
			File file = ResourceUtils.getFile(
					"D:\\tvs_next\\projects\\features\\features\\features\\src\\main\\resources\\Test1/gupshup_audio_input.txt");
			System.setIn(new FileInputStream(file));
			requestInputBuf = Files.readAllBytes(file.toPath());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		File fIn = new File(
				"D:\\tvs_next\\projects\\features\\features\\features\\src\\main\\resources\\Test1/tmp.wav");

		Minimodem minimodem = new Minimodem();
		CommandLine cmd = new CommandLine(minimodem);
		final String[] args = { "--tx", "300", "-f", fIn.getPath() };
		cmd.parseArgs(args);

		if (minimodem.configure() == 0) {
			int parseRequestAtSenderSide = transmissionService.parseRequestAtSenderSide(requestInputBuf,
					requestInputBuf.length, transmissionDataBuf, transBufLen, outputBuffer, outputLength);
			if (parseRequestAtSenderSide == TransmissionStates.GUPSHUP_SEND_END_SIGNAL_ACK.label) {
				try {
					File file = ResourceUtils.getFile(
							"D:\\tvs_next\\projects\\features\\features\\features\\src\\main\\resources\\Test1/transmitOutput.txt");
					System.setIn(new FileInputStream(file));
					requestInputBuf = Files.readAllBytes(file.toPath());
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				if (minimodem.transmit() == 0)
					return ResponseEntity.ok(ResponseMessage.builder().status(HttpStatus.OK.value())
							.message(env.getProperty("transmit.success")).build());
				else
					return ResponseEntity.ok(ResponseMessage.builder().status(HttpStatus.PARTIAL_CONTENT.value())
							.message(env.getProperty("transmit.failed")).build());

			}
		}
		return null;

	}

	@PostMapping("/receive")
	public ResponseEntity<?> receiveMessage() throws IOException {
		try {
			PrintStream out = new PrintStream(new FileOutputStream(
					"D:\\tvs_next\\projects\\features\\features\\features\\target\\classes\\Test1/tmp.txt"));
			System.setOut(out);
		} catch (IOException e) {
			e.printStackTrace();
		}
		File fIn = new File(
				"D:\\tvs_next\\projects\\features\\features\\features\\src\\main\\resources\\Test1/tmp.wav");

		Minimodem minimodem = new Minimodem();
		CommandLine cmd = new CommandLine(minimodem);
		final String[] args = { "--rx", "300", "-f", fIn.getPath() };
		cmd.parseArgs(args);
		if (minimodem.configure() == 0) {
			if (minimodem.receive() == 0) {
				File audioText = ResourceUtils.getFile("classpath:Test1/tmp.txt");
				byte[] input_buffer = Files.readAllBytes(audioText.toPath());
				byte[] minimodemDecode = decodeService.minimodemDecode(input_buffer);
				int parseRequestAtReceiverSide = receiveService.parseRequestAtReceiverSide(minimodemDecode,
						minimodemDecode.length, null, null, 0);
				if (parseRequestAtReceiverSide == TransmissionStates.GUPSHUP_TRANSMISSTION_END_REQUEST.label) {
					return ResponseEntity.ok(ResponseMessage.builder().status(HttpStatus.OK.value())
							.message(env.getProperty("receive.exact.success")).build());
				}
			}
		}
		return ResponseEntity.ok(ResponseMessage.builder().status(HttpStatus.PARTIAL_CONTENT.value())
				.message(env.getProperty("compare.failed")).build());
	}

	@PostMapping("/encode")
	public boolean encodeFiles() {
		final long minDistance = 16;
		byte[] encodedInput = null;
		byte[] encodedOutput = null;
		try {
			File audioText = ResourceUtils.getFile("classpath:Test1/gupshup_audio_input.txt");
			File audioEncodedText = ResourceUtils.getFile("classpath:Test1/tmp.txt");
			byte[] input = Files.readAllBytes(audioText.toPath());
			byte[] output = Files.readAllBytes(audioEncodedText.toPath());
			ReedSolomon rs = new ReedSolomon(correctRsPrimitivePolynomialCcsds, (byte) 1, (byte) 1, minDistance);
			encodedInput = rs.encode(input);
			encodedOutput = rs.encode(output);
			FileUtils.writeByteArrayToFile(new File(baseFileDestination + "encodedinput.txt"), encodedInput);
			FileUtils.writeByteArrayToFile(new File(baseFileDestination + "encodedoutput.txt"), encodedOutput);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Arrays.equals(encodedInput, encodedOutput);
	}

	@PostMapping("/decode")
	public boolean decodeFiles() {
		final long minDistance = 16;
		byte[] input = null;
		byte[] output = null;
		try {
			File audioText = ResourceUtils.getFile("classpath:Test1/encodedinput.txt");
			File audioEncodedText = ResourceUtils.getFile("classpath:Test1/encodedoutput.txt");
			input = Files.readAllBytes(audioText.toPath());
			output = Files.readAllBytes(audioEncodedText.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		ReedSolomon rs = new ReedSolomon(correctRsPrimitivePolynomialCcsds, (byte) 1, (byte) 1, minDistance);
		byte[] decodedInput = rs.decode(input);
		byte[] decodedOutput = rs.decode(output);

		return Arrays.equals(decodedInput, decodedOutput);
	}

	@PostMapping("/compare/bytes")
	public boolean compareFileBytes() throws IOException {
		final long minDistance = 16;
		File audioText = ResourceUtils.getFile("classpath:Test1/gupshup_audio_input.txt");
		File audioEncodedText = ResourceUtils.getFile("classpath:Test1/tmp.txt");
		byte[] input = Files.readAllBytes(audioText.toPath());
		byte[] sample = Files.readAllBytes(audioEncodedText.toPath());
		ReedSolomon rs = new ReedSolomon(correctRsPrimitivePolynomialCcsds, (byte) 1, (byte) 1, minDistance);
		byte[] encoded = rs.encode(input);
		System.out.println(String.valueOf(encoded));
		return Arrays.equals(sample, encoded);
	}

	@PostMapping("/compare/files")
	public boolean compareFiles(@RequestParam("file1") MultipartFile file1, @RequestParam("file2") MultipartFile file2)
			throws FileNotFoundException {
		File input = ResourceUtils.getFile("classpath:Test1/gupshup_audio_input.txt");
		File result = ResourceUtils.getFile("classpath:Test1/tmp.txt");
		return compareFiles(input, result);
	}

	private boolean compareFiles(File file1, File file2) {
		if (file1.length() != file2.length()) {
			return false;
		}

		try (InputStream in1 = new BufferedInputStream(new FileInputStream(file1));
				InputStream in2 = new BufferedInputStream(new FileInputStream(file2));) {
			int value1, value2, i;
			i = 0;
			do {
				// since we're buffered read() isn't expensive
				value1 = in1.read();
				value2 = in2.read();
				i++;
				if (value1 != value2) {
					return false;
				}
			} while (value1 >= 0);

			// since we already checked that the file sizes are equal
			// if we're here we reached the end of both files without a mismatch
			return true;
		} catch (Exception ignored) {
		}
		return false;
	}

}
