package io.github.x5897415746.mybalance.util;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.Log;

import androidx.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import io.github.x5897415746.mybalance.R;

public class NfcTechFilterXmlParser {
    private static final String LOG_TAG = "NfcTechFilterXmlParser";

    private static final int XML_RES_ID = R.xml.nfc_tech_filter;
    private static final List<String> VALID_TAG_NAME_LIST = Arrays.asList("resources", "tech-list", "tech");

    private static boolean parsed = false; // works as isParsed
    private static String[][] parsedData = new String[0][0];

    private static int log(int priority, @NonNull XmlResourceParser parser, String msg) {
        if (msg == null) {
            msg = "(null)";
        } else if (msg.isEmpty()) {
            msg = "(empty)";
        }
        return Log.println(priority, LOG_TAG,
                "at Line " + parser.getLineNumber()
                        + ", Column " + parser.getColumnNumber()
                        + ": " + msg);
    }

    @NonNull
    public static String[][] parse(Context context) {
        if (parsed) {
            Log.i(LOG_TAG, "Using cached data");
            return parsedData;
        }

        List<String[]> techLists = new ArrayList<>();
        List<String> currentTechList = null;
        Stack<String> tagStack = new Stack<>();

        XmlResourceParser parser = context.getResources().getXml(XML_RES_ID);

        try {
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = null;
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        tagName = parser.getName();
                        if (tagStack.isEmpty() && !"resources".equals(tagName)) {
                            log(Log.ERROR, parser, "Root node must be <resources>");
                            return parsedData;
                        }
                        tagStack.push(tagName);
                        if ("tech-list".equals(tagName)) {
                            if (currentTechList != null) {
                                log(Log.ERROR, parser, "Nested <tech-list> is not allowed");
                                return parsedData;
                            }
                            currentTechList = new ArrayList<>();
                        } else if ("tech".equals(tagName)) {
                            if (currentTechList == null) {
                                log(Log.ERROR, parser, "<tech> must be in <tech-list>");
                                return parsedData;
                            }
                            String tech = parser.nextText();
                            tagStack.pop(); // </tech> is consumed in nextText()
                            currentTechList.add(tech);
                        } else if (!VALID_TAG_NAME_LIST.contains(tagName)) {
                            log(Log.ERROR, parser, "Unexpected start tag: " + tagName);
                            return parsedData;
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        tagName = parser.getName();
                        if (tagStack.isEmpty() || !tagStack.peek().equals(tagName)) {
                            log(Log.ERROR, parser, "Mismatched end tag: " + tagName);
                            return parsedData;
                        }
                        tagStack.pop();
                        if ("tech-list".equals(tagName)) {
                            techLists.add(currentTechList.toArray(new String[0]));
                            currentTechList = null;
                        } else if (!VALID_TAG_NAME_LIST.contains(tagName)) {
                            log(Log.ERROR, parser, "Unexpected end tag: " + tagName);
                            return parsedData;
                        }
                        break;
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException e) {
            log(Log.ERROR, parser, "Failed to parse XML file: " + e.getMessage());
            return parsedData;
        } catch (IOException e) {
            log(Log.ERROR, parser, "Failed to read XML file: " + e.getMessage());
            return parsedData;
        }

        parsed = true;
        parsedData = techLists.toArray(new String[0][0]);
        return parsedData;
    }
}
