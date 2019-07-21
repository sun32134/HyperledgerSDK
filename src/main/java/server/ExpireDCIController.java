package server;

import org.hyperledger.fabric.sdk.HFClient;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.model.ExpiredDCI;

@RestController
public class ExpireDCIController {

    @RequestMapping("/expiredDCI")
    public void expiredDCI(@ModelAttribute ExpiredDCI expiredDCI){

    }
}
