package org.lpw.tephra.office.pptx.parser;

import com.alibaba.fastjson.JSONObject;
import org.apache.poi.xslf.usermodel.XSLFGraphicFrame;
import org.apache.poi.xslf.usermodel.XSLFSimpleShape;
import org.lpw.tephra.office.pptx.MediaWriter;
import org.lpw.tephra.util.Numeric;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.awt.geom.Rectangle2D;

/**
 * @author lpw
 */
@Component("tephra.office.pptx.parser.anchor")
public class AnchorImpl implements Simple, Graphic {
    @Inject
    private Numeric numeric;

    @Override
    public int getSort() {
        return 0;
    }

    @Override
    public void parse(XSLFSimpleShape xslfSimpleShape, MediaWriter mediaWriter, JSONObject shape, boolean layout) {
        parse(xslfSimpleShape.getAnchor(), shape);
    }

    @Override
    public void parse(XSLFGraphicFrame xslfGraphicFrame, MediaWriter mediaWriter, JSONObject shape) {
        parse(xslfGraphicFrame.getAnchor(), shape);
    }

    private void parse(Rectangle2D rectangle2D, JSONObject shape) {
        JSONObject anchor = new JSONObject();
        anchor.put("x", numeric.toInt(rectangle2D.getX()));
        anchor.put("y", numeric.toInt(rectangle2D.getY()));
        anchor.put("width", numeric.toInt(rectangle2D.getWidth()));
        anchor.put("height", numeric.toInt(rectangle2D.getHeight()));
        shape.put("anchor", anchor);
    }
}
