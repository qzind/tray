package qz.integration.services;

import java.security.Provider;
import java.security.Security;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;

public class SecuritySpiTests {

	@BeforeMethod
    public void beforeMethod() {
		if (Security.getProvider("BC") == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
		if (Security.getProvider("BCPQC") == null) {
			Security.addProvider(new BouncyCastlePQCProvider());
		}
	}

	@Test
	public void testBouncyCastleProvidersRegistered() {

		Provider bc = Security.getProvider("BC");
		Assert.assertNotNull(bc, "BouncyCastleProvider should be registered by PDFBox!");

		Provider bcpqc = Security.getProvider("BCPQC");
		Assert.assertNotNull(bcpqc, "BouncyCastlePQCProvider hould be registered by PDFBox!");

	}

}
