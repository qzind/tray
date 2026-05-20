package qz.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

public class TcvnCharsetTests {

	private static final Logger log = LogManager.getLogger(TcvnCharsetTests.class);

	private static final String CHARSET_NAME = "TCVN-3-1";

	@Test
	public void testCharsetAvailability() {
		log.trace("Checking if {} is supported by the JVM provider", CHARSET_NAME);
		try {
			Charset cs = Charset.forName(CHARSET_NAME);
			Assert.assertNotNull(cs, "Charset should not be null if registered");
		} catch (UnsupportedCharsetException e) {
			Assert.fail(CHARSET_NAME + " charset provider is not registered or supported in this runtime.");
		}
	}

	@DataProvider(name = "tcvnData")
	public Object[][] tcvnData() {
		return new Object[][] {
			// Basic ASCII characters (Should be a 1:1 mapping in TCVN-3)
			{
				"Viet Nam",
				new byte[] {
					(byte) 0x56,
					(byte) 0x69,
					(byte) 0x65,
					(byte) 0x74,
					(byte) 0x20,
					(byte) 0x4E,
					(byte) 0x61,
					(byte) 0x6D
				}
			},
			// Literal Vietnamese input to 8-bit TCVN byte equivalents
			{
				"Tiếng Việt Nam",
				new byte[] {
					(byte) 0x54, // "Tiếng"
					(byte) 0x69,
					(byte) 0xD5,
					(byte) 0x6E,
					(byte) 0x67,
					(byte) 0x20, // " "
					(byte) 0x56, // "Việt"
					(byte) 0x69,
					(byte) 0xD6,
					(byte) 0x74,
					(byte) 0x20, // " "
					(byte) 0x4E, // "Nam"
					(byte) 0x61,
					(byte) 0x6D
				}
			}
		};
	}

	@Test(dataProvider = "tcvnData")
	public void testTcvnEncoding(String input, byte[] expectedBytes) throws Exception {
		log.trace("Encoding string '{}' to {}", input, CHARSET_NAME);

		byte[] actualBytes = input.getBytes(CHARSET_NAME);

		log.trace("Comparing byte arrays. Expected length: {}, Actual length: {}",
			expectedBytes.length, actualBytes.length);

		Assert.assertEquals(actualBytes, expectedBytes, "Byte arrays do not match for input: " + input);
	}

	@Test
	public void testDecoding() throws Exception {
		byte[] rawTcvnBytes = new byte[] {
			(byte) 0x56,
			(byte) 0x69,
			(byte) 0x65,
			(byte) 0x74,
			(byte) 0x20,
			(byte) 0x4E,
			(byte) 0x61,
			(byte) 0x6D
		};
		String expectedString = "Viet Nam";

		String actualString = new String(rawTcvnBytes, CHARSET_NAME);
		log.trace("Decoded string actual: '{}', expected: '{}'", actualString, expectedString);

		Assert.assertEquals(actualString, expectedString);
	}
}
