package qz.integration.services;

import java.security.Provider;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

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

	@DataProvider(name = "providers")
	public Object[][] providers() {
		return new Object[][] {
			{ "BC", BouncyCastleProvider.class.getName() },
			{ "BCPQC", BouncyCastlePQCProvider.class.getName() }
		};
	}

	@Test(dataProvider = "providers")
	public void testProviderRegistration(String providerName, String className) {
		Provider p = Security.getProvider(providerName);
		Assert.assertNotNull(p, String.format("Provider %s was not found!", providerName));
		Assert.assertEquals(p.getClass().getName(), className, String.format("Provider class mismatch for %s!", providerName));
	}

}
