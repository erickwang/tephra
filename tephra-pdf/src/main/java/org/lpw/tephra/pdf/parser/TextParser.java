package org.lpw.tephra.pdf.parser;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;
import org.lpw.tephra.pdf.PdfHelper;

import java.io.IOException;

/**
 * @author lpw
 */
public class TextParser extends PDFTextStripper {
    private PdfHelper pdfHelper;
    private JSONArray array;

    public TextParser(PdfHelper pdfHelper) throws IOException {
        super();

        this.pdfHelper = pdfHelper;
        array = new JSONArray();

        setSortByPosition(true);
    }

    @Override
    protected void processTextPosition(TextPosition textPosition) {
        JSONObject object = new JSONObject();
        JSONObject anchor = new JSONObject();
        Matrix matrix = textPosition.getFont().getFontMatrix();
        anchor.put("x", pdfHelper.pointToPixel(matrix.getTranslateX()));
        anchor.put("y", pdfHelper.pointToPixel(matrix.getTranslateY()));
        anchor.put("width", pdfHelper.pointToPixel(matrix.getScalingFactorX()));
        anchor.put("height", pdfHelper.pointToPixel(matrix.getScalingFactorY()));
        object.put("anchor", anchor);


        JSONArray words = new JSONArray();
        JSONObject word = new JSONObject();
        word.put("word", textPosition.getUnicode());
        words.add(word);

        JSONObject paragraph = new JSONObject();
        paragraph.put("words", words);
        JSONArray paragraphs = new JSONArray();
        paragraphs.add(paragraph);
        JSONObject text = new JSONObject();
        text.put("fontSize", pdfHelper.pointToPixel(textPosition.getFontSizeInPt()));
        text.put("paragraphs", paragraphs);
        object.put("text", text);
        array.add(object);
    }

    public JSONArray getArray() {
        return array;
    }
}
