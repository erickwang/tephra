package org.lpw.tephra.office.pptx.parser;

import com.alibaba.fastjson.JSONObject;
import org.apache.poi.sl.usermodel.PaintStyle;
import org.apache.poi.sl.usermodel.StrokeStyle;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSimpleShape;
import org.apache.xmlbeans.XmlObject;
import org.lpw.tephra.office.MediaType;
import org.lpw.tephra.office.MediaWriter;
import org.lpw.tephra.office.OfficeHelper;
import org.lpw.tephra.office.pptx.ReaderContext;
import org.lpw.tephra.office.pptx.WriterContext;
import org.lpw.tephra.util.Image;
import org.lpw.tephra.util.Io;
import org.lpw.tephra.util.Logger;
import org.openxmlformats.schemas.drawingml.x2006.main.CTBlipFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTRelativeRect;
import org.openxmlformats.schemas.drawingml.x2006.main.CTStretchInfoProperties;
import org.openxmlformats.schemas.presentationml.x2006.main.CTBackground;
import org.openxmlformats.schemas.presentationml.x2006.main.CTShape;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author lpw
 */
@Component("tephra.office.pptx.parser.geometry")
public class GeometryImpl implements Simple {
    @Inject
    private Image image;
    @Inject
    private Io io;
    @Inject
    private Logger logger;
    @Inject
    private OfficeHelper officeHelper;

    @Override
    public int getSort() {
        return 5;
    }

    @Override
    public void parseShape(ReaderContext readerContext, XSLFSimpleShape xslfSimpleShape, JSONObject shape) {
        JSONObject geometry = new JSONObject();
        parseLine(xslfSimpleShape, geometry);
        parseFill(xslfSimpleShape, readerContext.getMediaWriter(), geometry);
        if (geometry.isEmpty())
            return;

        String type = xslfSimpleShape.getShapeType() == null ? "rect" : format(xslfSimpleShape.getShapeType().toString());
        geometry.put("type", type);
        String image = readerContext.getGeometryConverter().getGeometryImage(type);
        if (image == null) {
            try {
                image = save(readerContext, xslfSimpleShape, type);
            } catch (IOException e) {
                logger.warn(e, "保存不可解析图形[{}]为PNG图片文件时发生异常！", type);
            }
        }
        geometry.put("image", image);
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
            color.put("alpha", officeHelper.fromPercent(alpha));
    }

    private void parseFill(XSLFSimpleShape xslfSimpleShape, MediaWriter mediaWriter, JSONObject geometry) {
        JSONObject fill = new JSONObject();
        parseColor(xslfSimpleShape, fill);
        parseTexture(xslfSimpleShape, mediaWriter, fill);
        if (!fill.isEmpty())
            geometry.put("fill", fill);
    }

    private void parseColor(XSLFSimpleShape xslfSimpleShape, JSONObject fill) {
        Color fillColor = xslfSimpleShape.getFillColor();
        if (fillColor != null)
            fill.put("color", officeHelper.colorToJson(fillColor));
    }

    private void parseTexture(XSLFSimpleShape xslfSimpleShape, MediaWriter mediaWriter, JSONObject fill) {
        PaintStyle paintStyle = xslfSimpleShape.getFillStyle().getPaint();
        if (!(paintStyle instanceof PaintStyle.TexturePaint))
            return;

        try {
            JSONObject texture = new JSONObject();
            PaintStyle.TexturePaint texturePaint = (PaintStyle.TexturePaint) paintStyle;
            InputStream inputStream = texturePaint.getImageData();
            int[] wh = image.size(inputStream);
            inputStream.reset();
            JSONObject size = new JSONObject();
            size.put("width", wh[0]);
            size.put("height", wh[1]);
            texture.put("size", size);

            texture.put("contentType", texturePaint.getContentType());
            texture.put("alpha", texturePaint.getAlpha() / 100000.0D);
            texture.put("url", mediaWriter.write(MediaType.find(texturePaint.getContentType()), null,
                    texturePaint.getImageData()));
            parseFillRect(xslfSimpleShape, texture);
            fill.put("texture", texture);
        } catch (IOException e) {
            logger.warn(e, "解析填充图片时发生异常！");
        }
    }

    private void parseFillRect(XSLFSimpleShape xslfSimpleShape, JSONObject texture) {
        XmlObject xmlObject = xslfSimpleShape.getXmlObject();
        if (xmlObject instanceof CTBackground)
            parseBlipFill(((CTBackground) xmlObject).getBgPr().getBlipFill(), texture);
        else if (xmlObject instanceof CTShape)
            parseBlipFill(((CTShape) xmlObject).getSpPr().getBlipFill(), texture);
    }

    private void parseBlipFill(CTBlipFillProperties ctBlipFillProperties, JSONObject texture) {
        CTStretchInfoProperties ctStretchInfoProperties = ctBlipFillProperties.getStretch();
        if (ctStretchInfoProperties == null)
            return;

        CTRelativeRect ctRelativeRect = ctStretchInfoProperties.getFillRect();
        if (ctRelativeRect == null)
            return;

        texture.put("left", officeHelper.fromPercent(ctRelativeRect.getL()));
        texture.put("top", officeHelper.fromPercent(ctRelativeRect.getT()));
        texture.put("right", officeHelper.fromPercent(ctRelativeRect.getR()));
        texture.put("bottom", officeHelper.fromPercent(ctRelativeRect.getB()));
    }

    private String save(ReaderContext readerContext, XSLFSimpleShape xslfSimpleShape, String type) throws IOException {
        File file = new File(officeHelper.getTempPath("geometry") + ".png");
        io.mkdirs(file.getParentFile());
        Rectangle2D rectangle2D = xslfSimpleShape.getAnchor();
        BufferedImage bufferedImage = new BufferedImage((int) rectangle2D.getWidth(), (int) rectangle2D.getHeight(),
                BufferedImage.TYPE_4BYTE_ABGR);
        xslfSimpleShape.draw(bufferedImage.createGraphics(), new Rectangle2D.Double(0, 0,
                rectangle2D.getWidth(), rectangle2D.getHeight()));
        ImageIO.write(bufferedImage, "PNG", file);
        String image = readerContext.getGeometryConverter().saveGeometryImage(type, file);
        io.delete(file);

        return image;
    }

    @Override
    public XSLFShape createShape(WriterContext writerContext, JSONObject shape) {
        return null;
    }

    @Override
    public void parseShape(WriterContext writerContext, XSLFSimpleShape xslfSimpleShape, JSONObject shape) {
        if (!shape.containsKey("geometry"))
            return;

        JSONObject geometry = shape.getJSONObject("geometry");
        parseFillToShape(xslfSimpleShape, geometry);
    }

    private void parseFillToShape(XSLFSimpleShape xslfSimpleShape, JSONObject geometry) {
        if (!geometry.containsKey("fill"))
            return;

        JSONObject fill = geometry.getJSONObject("fill");
        if (fill.containsKey("color"))
            xslfSimpleShape.setFillColor(officeHelper.jsonToColor(fill.getJSONObject("color")));
    }
}
