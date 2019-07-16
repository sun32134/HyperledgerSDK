package config.test;

import org.hyperledger.fabric.sdk.ChaincodeEndorsementPolicy;
import org.hyperledger.fabric.sdk.exception.ChaincodeEndorsementPolicyParseException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class PolicyConfigTest {
    @Test
    public void fromYamlFile() throws IOException, ChaincodeEndorsementPolicyParseException {
        ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
        chaincodeEndorsementPolicy.fromYamlFile(new File("Resource/chaincodeendorsementpolicy.yaml"));
    }
}
