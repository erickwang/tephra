package org.lpw.tephra.poi.pptx;

import com.alibaba.fastjson.JSONObject;
import org.apache.poi.sl.usermodel.PaintStyle;
import org.apache.poi.xslf.usermodel.XSLFSimpleShape;
import org.lpw.tephra.bean.BeanFactory;
import org.lpw.tephra.bean.ContextRefreshedListener;
import org.lpw.tephra.util.Image;
import org.lpw.tephra.util.Json;
import org.lpw.tephra.util.Logger;
import org.lpw.tephra.util.Numeric;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lpw
 */
@Component("tephra.poi.pptx.parser-helper")
public class ParserHelperImpl implements ParserHelper, ContextRefreshedListener {
    @Inject
    private Numeric numeric;
    @Inject
    private Json json;
    @Inject
    private Image image;
    @Inject
    private Logger logger;
    private Map<String, Parser> parsers;

    @Override
    public Parser get(String type) {
        return parsers.get(type);
    }

    @Override
    public Rectangle getRectangle(JSONObject object) {
        return new Rectangle(object.getIntValue("x"), object.getIntValue("y"),
                object.getIntValue("width"), object.getIntValue("height"));
    }

    @Override
    public void rotate(XSLFSimpleShape xslfSimpleShape, JSONObject object) {
        if (object.containsKey("rotation") && object.getDoubleValue("rotation") != 0)
            xslfSimpleShape.setRotation(object.getDoubleValue("rotation"));
        if (json.hasTrue(object, "rotationX"))
            xslfSimpleShape.setFlipVertical(true);
        if (json.hasTrue(object, "rotationY"))
            xslfSimpleShape.setFlipHorizontal(true);
    }

    @Override
    public Color getColor(JSONObject object, String key) {
        if (!object.containsKey(key))
            return null;

        String color = object.getString(key);
        int[] ns;
        if (color.charAt(0) == '#') {
            int length = color.length();
            if (length != 4 && length != 7) {
                logger.warn(null, "解析颜色值[{}]失败！", color);

                return null;
            }

            String[] array = new String[3];
            boolean full = length == 7;
            for (int i = 0; i < array.length; i++)
                array[i] = full ? color.substring(2 * i + 1, 2 * i + 3) : (color.substring(i + 1, i + 2) + color.substring(i + 1, i + 2));
            ns = new int[3];
            for (int i = 0; i < ns.length; i++)
                ns[i] = numeric.hexToInt(array[i]);
        } else if (color.indexOf('(') > -1)
            ns = numeric.toInts(color.substring(color.indexOf('(') + 1, color.indexOf(')')));
        else {
            logger.warn(null, "解析颜色值[{}]失败！", color);

            return null;
        }

        return object.containsKey("alpha") ? new Color(ns[0], ns[1], ns[2], numeric.toInt(object.getDoubleValue("alpha") * 255))
                : new Color(ns[0], ns[1], ns[2]);
    }

    @Override
    public String getHexColor(PaintStyle paintStyle, boolean ignoreWhite) {
        return paintStyle instanceof PaintStyle.SolidPaint ?
                toHex(((PaintStyle.SolidPaint) paintStyle).getSolidColor().getColor(), ignoreWhite) : null;
    }

    @Override
    public String toHex(Color color) {
        return toHex(color, false);
    }

    private String toHex(Color color, boolean ignoreWhite) {
        if (color == null || (ignoreWhite && color.getRed() == 255 && color.getGreen() == 255 && color.getBlue() == 255))
            return null;

        return "#" + hex(color.getRed()) + hex(color.getGreen()) + hex(color.getBlue());
    }

    private String hex(int n) {
        String hex = Integer.toHexString(n);

        return hex.length() == 1 ? ("0" + hex) : hex;
    }

    @Override
    public byte[] getImage(JSONObject object, String contentType, ByteArrayOutputStream outputStream) throws IOException {
        if (!object.containsKey("subimage"))
            return outputStream.toByteArray();

        BufferedImage image = this.image.read(outputStream.toByteArray());
        if (image == null)
            return null;

        if (object.containsKey("subimage")) {
            JSONObject subimage = object.getJSONObject("subimage");
            if (json.hasTrue(object, "rotationX"))
                subimage.put("y", image.getHeight() - subimage.getIntValue("y") - subimage.getIntValue("height"));
            if (json.hasTrue(object, "rotationY"))
                subimage.put("x", image.getWidth() - subimage.getIntValue("x") - subimage.getIntValue("width"));
            image = this.image.subimage(image, subimage.getIntValue("x"), subimage.getIntValue("y"),
                    subimage.getIntValue("width"), subimage.getIntValue("height"));
        }
        if (image == null)
            return null;

        return this.image.write(image, this.image.formatFromContentType(contentType));
    }

    @Override
    public int getContextRefreshedSort() {
        return 8;
    }

    @Override
    public void onContextRefreshed() {
        parsers = new HashMap<>();
        BeanFactory.getBeans(Parser.class).forEach(parser -> parsers.put(parser.getType(), parser));
    }
}
