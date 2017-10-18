package org.lpw.tephra.poi.pptx;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.poi.sl.usermodel.Insets2D;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.lpw.tephra.util.Numeric;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.awt.Color;

/**
 * @author lpw
 */
@Component("tephra.poi.pptx.text")
public class TextParserImpl implements Parser {
    @Inject
    private Numeric numeric;
    @Inject
    private ParserHelper parserHelper;

    @Override
    public String getType() {
        return "text";
    }

    @Override
    public boolean parse(XMLSlideShow xmlSlideShow, XSLFSlide xslfSlide, JSONObject object) {
        XSLFTextBox xslfTextBox = xslfSlide.createTextBox();
        xslfTextBox.clearText();
        xslfTextBox.setAnchor(parserHelper.getRectangle(object));
        xslfTextBox.setInsets(new Insets2D(0.0D, 0.0D, 0.0D, 0.0D));
        parserHelper.rotate(xslfTextBox, object);
        XSLFTextParagraph xslfTextParagraph = xslfTextBox.addNewTextParagraph();
        align(xslfTextParagraph, object);
        if (object.containsKey("texts")) {
            JSONArray texts = object.getJSONArray("texts");
            for (int i = 0, size = texts.size(); i < size; i++)
                add(xslfTextParagraph, texts.getJSONObject(i));
        } else if (object.containsKey("text"))
            add(xslfTextParagraph, object);


        return true;
    }

    private void add(XSLFTextParagraph xslfTextParagraph, JSONObject object) {
        XSLFTextRun xslfTextRun = xslfTextParagraph.addNewTextRun();
        xslfTextRun.setText(object.getString("text"));
        font(xslfTextParagraph, xslfTextRun, object);
        color(xslfTextRun, object);
        if (hasTrue(object, "bold"))
            xslfTextRun.setBold(true);
        if (hasTrue(object, "underline"))
            xslfTextRun.setUnderlined(true);
        if (hasTrue(object, "italic"))
            xslfTextRun.setItalic(true);
        if (object.containsKey("spacing"))
            xslfTextRun.setCharacterSpacing(object.getDoubleValue("spacing"));
    }

    private void align(XSLFTextParagraph xslfTextParagraph, JSONObject object) {
        if (!object.containsKey("align"))
            return;

        String align = object.getString("align");
        if (align.equals("left"))
            xslfTextParagraph.setTextAlign(TextParagraph.TextAlign.LEFT);
        else if (align.equals("center"))
            xslfTextParagraph.setTextAlign(TextParagraph.TextAlign.CENTER);
        else if (align.equals("right"))
            xslfTextParagraph.setTextAlign(TextParagraph.TextAlign.RIGHT);
    }

    private void font(XSLFTextParagraph xslfTextParagraph, XSLFTextRun xslfTextRun, JSONObject object) {
        if (!object.containsKey("font"))
            return;

        JSONObject font = object.getJSONObject("font");
        if (font.containsKey("family"))
            xslfTextRun.setFontFamily(font.getString("family"));
        if (font.containsKey("size")) {
            double height = 0.0D;
            if (font.containsKey("height"))
                height = Math.max(0.0D, font.getDoubleValue("height") - 1);
            double size = numeric.toDouble(font.getDoubleValue("size"));
            double space = size * height / 2;
            xslfTextParagraph.setSpaceBefore(space);
            xslfTextParagraph.setSpaceAfter(space);
            xslfTextRun.setFontSize(size);
        }
    }

    private void color(XSLFTextRun xslfTextRun, JSONObject object) {
        Color color = parserHelper.getColor(object, "color");
        if (color != null)
            xslfTextRun.setFontColor(color);
    }

    private boolean hasTrue(JSONObject object, String key) {
        return object.containsKey(key) && object.getBoolean(key);
    }
}
