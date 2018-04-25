package org.lpw.tephra.office.pptx.parser;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.poi.sl.usermodel.PaintStyle;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.lpw.tephra.office.pptx.MediaWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lpw
 */
@Component("tephra.office.pptx.parser.text")
public class TextParserImpl implements Parser {
    private String[] merges = {"align", "fontFamily", "fontSize", "color", "bold", "italic", "underline", "strikethrough",
            "subscript", "superscript"};

    @Override
    public int getSort() {
        return 9;
    }

    @Override
    public void parse(XSLFShape xslfShape, MediaWriter mediaWriter, JSONObject shape) {
        if (!(xslfShape instanceof XSLFTextShape))
            return;

        XSLFTextShape xslfTextShape = (XSLFTextShape) xslfShape;
        JSONObject text = new JSONObject();
        parseVerticalAlignment(xslfTextShape, text);
        JSONArray paragraphs = new JSONArray();
        xslfTextShape.getTextParagraphs().forEach(xslfTextParagraph -> {
            JSONObject paragraph = new JSONObject();
            parseAlign(xslfTextParagraph, paragraph);
            JSONArray words = new JSONArray();
            xslfTextParagraph.getTextRuns().forEach(xslfTextRun -> {
                JSONObject word = new JSONObject();
                word.put("fontFamily", xslfTextRun.getFontFamily());
                word.put("fontSize", xslfTextRun.getFontSize());
                word.put("bold", xslfTextRun.isBold());
                word.put("italic", xslfTextRun.isItalic());
                word.put("underline", xslfTextRun.isUnderlined());
                word.put("strikethrough", xslfTextRun.isStrikethrough());
                word.put("subscript", xslfTextRun.isSubscript());
                word.put("superscript", xslfTextRun.isSuperscript());
                parseColor(xslfTextRun.getFontColor(), word);
                word.put("word", xslfTextRun.getRawText());
                words.add(word);
            });
            merge(paragraph, words);
            paragraph.put("words", words);
            paragraphs.add(paragraph);
        });
        merge(text, paragraphs);
        text.put("paragraphs", paragraphs);
        shape.put("text", text);
    }

    private void parseVerticalAlignment(XSLFTextShape xslfTextShape, JSONObject text) {
        switch (xslfTextShape.getVerticalAlignment()) {
            case TOP:
                text.put("verticalAlign", "top");
                return;
            case MIDDLE:
                text.put("verticalAlign", "middle");
                return;
            case BOTTOM:
                text.put("verticalAlign", "bottom");
        }
    }

    private void parseAlign(XSLFTextParagraph xslfTextParagraph, JSONObject paragraph) {
        switch (xslfTextParagraph.getTextAlign()) {
            case LEFT:
                paragraph.put("align", "left");
                return;
            case CENTER:
                paragraph.put("align", "center");
                return;
            case RIGHT:
                paragraph.put("align", "right");
                return;
            case JUSTIFY:
                paragraph.put("align", "justify");
        }
    }

    private void parseColor(PaintStyle paintStyle, JSONObject word) {
        if (!(paintStyle instanceof PaintStyle.SolidPaint))
            return;

        PaintStyle.SolidPaint solidPaint = (PaintStyle.SolidPaint) paintStyle;
        Color solidColor = solidPaint.getSolidColor().getColor();
        JSONObject color = new JSONObject();
        color.put("red", solidColor.getRed());
        color.put("green", solidColor.getGreen());
        color.put("blue", solidColor.getBlue());
        color.put("alpha", solidColor.getAlpha());
        word.put("color", color);
    }

    private void merge(JSONObject object, JSONArray array) {
        if (array.isEmpty())
            return;

        int size = array.size();
        if (size == 1) {
            JSONObject obj = array.getJSONObject(0);
            for (String key : merges)
                if (obj.containsKey(key))
                    object.put(key, obj.remove(key));

            return;
        }

        for (String key : merges) {
            Map<Object, Integer> map = new HashMap<>();
            for (int i = 0; i < size; i++) {
                JSONObject obj = array.getJSONObject(i);
                if (!obj.containsKey(key))
                    break;

                Object value = obj.get(key);
                map.put(value, map.getOrDefault(value, 0) + 1);
            }
            if (map.isEmpty())
                continue;

            Object value = null;
            int count = 0;
            for (Object k : map.keySet()) {
                int v = map.get(k);
                if (v <= count)
                    continue;

                count = v;
                value = k;
            }
            object.put(key, value);
            for (int i = 0; i < size; i++) {
                JSONObject obj = array.getJSONObject(i);
                if (!obj.containsKey(key))
                    break;

                if (obj.get(key).equals(value))
                    obj.remove(key);
            }
        }
    }
}
