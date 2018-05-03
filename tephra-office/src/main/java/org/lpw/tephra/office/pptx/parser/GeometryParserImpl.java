package org.lpw.tephra.office.pptx.parser;

import com.alibaba.fastjson.JSONObject;
import org.apache.poi.sl.usermodel.PaintStyle;
import org.apache.poi.sl.usermodel.StrokeStyle;
import org.apache.poi.xslf.usermodel.XSLFSimpleShape;
import org.apache.xmlbeans.XmlObject;
import org.lpw.tephra.office.OfficeHelper;
import org.lpw.tephra.office.pptx.MediaWriter;
import org.openxmlformats.schemas.drawingml.x2006.main.CTBlipFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTRelativeRect;
import org.openxmlformats.schemas.drawingml.x2006.main.CTStretchInfoProperties;
import org.openxmlformats.schemas.presentationml.x2006.main.CTBackground;
import org.openxmlformats.schemas.presentationml.x2006.main.CTShape;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.awt.Color;

/**
 * @author lpw
 */
@Component("tephra.office.pptx.parser.geometry")
public class GeometryParserImpl implements Parser {
    @Inject
    private OfficeHelper officeHelper;

    @Override
    public int getSort() {
        return 6;
    }

    @Override
    public void parse(XSLFSimpleShape xslfSimpleShape, MediaWriter mediaWriter, JSONObject shape) {
        JSONObject geometry = new JSONObject();
        parseLine(xslfSimpleShape, geometry);
        parseFill(xslfSimpleShape, mediaWriter, geometry, shape);
        if (geometry.isEmpty())
            return;

        geometry.put("type", xslfSimpleShape.getShapeType() == null ? "rect" : format(xslfSimpleShape.getShapeType().toString()));
        shape.put("geometry", geometry);
    }

    private void parseLine(XSLFSimpleShape xslfSimpleShape, JSONObject geometry) {
        if (xslfSimpleShape.getLineWidth() == 0.0D || xslfSimpleShape.getLineColor() == null)
            return;

        JSONObject line = new JSONObject();
        line.put("width", officeHelper.pointToPixel(xslfSimpleShape.getLineWidth()));
        line.put("style", getLineStyle(xslfSimpleShape.getLineDash()));
        line.put("color", officeHelper.colorToJson(xslfSimpleShape.getLineColor()));
        parseAlpha(xslfSimpleShape.getStrokeStyle().getPaint(), line.getJSONObject("color"));
        geometry.put("line", line);
    }

    private String getLineStyle(StrokeStyle.LineDash lineDash) {
        return lineDash == null ? "solid" : format(lineDash.toString());
    }

    private String format(String string) {
        return string.toLowerCase().replace('_', '-');
    }

    private void parseAlpha(PaintStyle paintStyle, JSONObject color) {
        if (!(paintStyle instanceof PaintStyle.SolidPaint))
            return;

        int alpha = ((PaintStyle.SolidPaint) paintStyle).getSolidColor().getAlpha();
        if (alpha > -1)
            color.put("alpha", officeHelper.fromPercent(255, alpha));
    }

    private void parseFill(XSLFSimpleShape xslfSimpleShape, MediaWriter mediaWriter, JSONObject geometry, JSONObject shape) {
        JSONObject fill = new JSONObject();
        parseColor(xslfSimpleShape, fill);
        parseTexture(xslfSimpleShape, mediaWriter, fill, shape);
        if (!fill.isEmpty())
            geometry.put("fill", fill);

    }

    private void parseColor(XSLFSimpleShape xslfSimpleShape, JSONObject fill) {
        Color fillColor = xslfSimpleShape.getFillColor();
        if (fillColor != null)
            fill.put("color", officeHelper.colorToJson(fillColor));
    }

    private void parseTexture(XSLFSimpleShape xslfSimpleShape, MediaWriter mediaWriter, JSONObject fill, JSONObject shape) {
        PaintStyle paintStyle = xslfSimpleShape.getFillStyle().getPaint();
        if (!(paintStyle instanceof PaintStyle.TexturePaint))
            return;

        JSONObject texture = new JSONObject();
        PaintStyle.TexturePaint texturePaint = (PaintStyle.TexturePaint) paintStyle;
        texture.put("contentType", texturePaint.getContentType());
        texture.put("alpha", texturePaint.getAlpha() / 100000.0D);
        texture.put("uri", mediaWriter.write(MediaWriter.Type.Image, texturePaint.getContentType(), texturePaint.getImageData()));
        parseFillRect(xslfSimpleShape, texture, shape);
        fill.put("texture", texture);
    }

    private void parseFillRect(XSLFSimpleShape xslfSimpleShape, JSONObject texture, JSONObject shape) {
        XmlObject xmlObject = xslfSimpleShape.getXmlObject();
        if (xmlObject instanceof CTBackground)
            parseBlipFill(((CTBackground) xmlObject).getBgPr().getBlipFill(), texture, shape);
        else if (xmlObject instanceof CTShape)
            parseBlipFill(((CTShape) xmlObject).getSpPr().getBlipFill(), texture, shape);
    }

    private void parseBlipFill(CTBlipFillProperties ctBlipFillProperties, JSONObject texture, JSONObject shape) {
        CTStretchInfoProperties ctStretchInfoProperties = ctBlipFillProperties.getStretch();
        if (ctStretchInfoProperties == null)
            return;

        CTRelativeRect ctRelativeRect = ctStretchInfoProperties.getFillRect();
        if (ctRelativeRect == null)
            return;

        JSONObject anchor = shape.getJSONObject("anchor");
        int width = anchor.getIntValue("width");
        int height = anchor.getIntValue("height");
        texture.put("left", officeHelper.fromPercent(width, ctRelativeRect.getL()));
        texture.put("top", officeHelper.fromPercent(height, ctRelativeRect.getT()));
        texture.put("right", officeHelper.fromPercent(width, ctRelativeRect.getR()));
        texture.put("bottom", officeHelper.fromPercent(height, ctRelativeRect.getB()));
    }
}
