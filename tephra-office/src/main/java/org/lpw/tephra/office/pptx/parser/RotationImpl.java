package org.lpw.tephra.office.pptx.parser;

import com.alibaba.fastjson.JSONObject;
import org.apache.poi.xslf.usermodel.XSLFSimpleShape;
import org.lpw.tephra.office.pptx.MediaWriter;
import org.springframework.stereotype.Component;

/**
 * @author lpw
 */
@Component("tephra.office.pptx.parser.rotation")
public class RotationImpl implements Simple {
    @Override
    public int getSort() {
        return 2;
    }

    @Override
    public void parse(XSLFSimpleShape xslfSimpleShape, MediaWriter mediaWriter, JSONObject shape, boolean layout) {
        if (xslfSimpleShape.getRotation() != 0.0D)
            shape.put("rotation", xslfSimpleShape.getRotation());
    }
}
