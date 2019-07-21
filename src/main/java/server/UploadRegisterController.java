package server;

import config.ReadConfig;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.NetworkConfigurationException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.junit.Assert;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.model.PreRegister;

import java.io.File;

@RestController
public class UploadRegisterController {
    private final HFClient hfClient;
    private final ReadConfig readConfig;

    UploadRegisterController() throws Exception {
        hfClient = HFClient.createNewInstance();
        hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        readConfig = ReadConfig.fromYamlFile(new File("Resource/network-config.yaml"));
    }


    @RequestMapping("/uploadRegister")
    public void uploadRegister(@ModelAttribute PreRegister preRegister) throws NetworkConfigurationException, InvalidArgumentException {
        Assert.assertNotNull(hfClient);
        Assert.assertNotNull(readConfig);

        hfClient.setUserContext(readConfig.getPeerAdmin());
    }
}
