package server;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.model.DCI;

@RestController
public class UploadDCIController {

    @RequestMapping("/uploadDCI")
    public void uploadDCI(@ModelAttribute DCI dci){

    }
}
