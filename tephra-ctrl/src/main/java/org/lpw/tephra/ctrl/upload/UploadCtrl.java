package org.lpw.tephra.ctrl.upload;

import org.lpw.tephra.ctrl.context.Request;
import org.lpw.tephra.ctrl.execute.Execute;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;

/**
 * @author lpw
 */
@Controller(UploadService.PREFIX + "ctrl")
@Execute(name = "/tephra/ctrl/", key = "tephra.ctrl")
public class UploadCtrl {
    @Inject
    private Request request;
    @Inject
    private UploadService uploadService;

    @Execute(name = "uploads")
    public Object uploads() {
        return uploadService.uploads(request.getFromInputStream());
    }

    @Execute(name = "upload")
    public Object upload() {
        return uploadService.upload(request.get("name"), request.get("fileName"), request.get("contentType"),
                request.get("base64"), request.get("string"));
    }
}
