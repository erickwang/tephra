package org.lpw.tephra.chrome.runner;

import com.alibaba.fastjson.JSONObject;

/**
 * @author lpw
 */
public class Image extends Support {
    private double x = 0;
    private double y = 0;
    private String format = "png";
    private int quality = 100;

    private Image(String[] args) {
        super(args);
    }

    @Override
    void parseArg(String arg) {
        if (arg.startsWith("-x="))
            x = Double.parseDouble(arg.substring(3));
        else if (arg.startsWith("-y="))
            y = Double.parseDouble(arg.substring(3));
        else if (arg.startsWith("-format="))
            format = arg.substring(8);
        else if (arg.startsWith("-quality="))
            quality = Integer.parseInt(arg.substring(9));
    }

    @Override
    String getMethod() {
        return "Page.captureScreenshot";
    }

    @Override
    JSONObject getParams() {
        JSONObject params = new JSONObject();
        params.put("format", format);
        if (format.equals("jpeg"))
            params.put("quality", quality);
        JSONObject clip = new JSONObject();
        clip.put("x", x);
        clip.put("y", y);
        clip.put("width", x + width);
        clip.put("height", y + height);
        clip.put("scale", 1);
        params.put("clip", clip);

        return params;
    }

    public static void main(String[] args) throws Exception {
        new Image(args).execute();
    }
}
