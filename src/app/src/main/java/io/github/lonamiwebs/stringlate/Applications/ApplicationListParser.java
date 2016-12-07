package io.github.lonamiwebs.stringlate.Applications;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;

// TODO Maybe a base class which has readText and that?
public class ApplicationListParser {
    // We don't use namespaces
    private static final String ns = null;

    private static final String ID = "id";
    private static final String LAST_UPDATED = "lastupdated";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "summary";
    private static final String ICON = "icon";
    private static final String SOURCE_URL = "source";

    // We will only parse https GitHub urls
    private static final String HTTPS_GITHUB = "https://github.com/";

    private static final HashSet<String> mWantedFields = new HashSet<String>(
            Arrays.asList(ID, LAST_UPDATED, NAME, DESCRIPTION, ICON, SOURCE_URL));

    //region Xml -> ApplicationsList

    public static HashSet<Application> parseFromXml(InputStream in)
            throws XmlPullParserException, IOException {

        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readFdroid(parser);
        } finally {
            try {
                in.close();
            } catch (IOException e) { }
        }
    }

    // Reads the <fdroid> tag and returns a list of its <application> tags
    private static HashSet<Application> readFdroid(XmlPullParser parser)
            throws XmlPullParserException, IOException {

        HashSet<Application> apps = new HashSet<>();

        parser.require(XmlPullParser.START_TAG, ns, "fdroid");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG)
                continue;

            String name = parser.getName();
            if (name.equals("application")) {
                Application app = readApplication(parser);
                if (app.mSourceCodeUrl.startsWith(HTTPS_GITHUB))
                    apps.add(app);
            }
            else
                skip(parser);
        }
        return apps;
    }

    // Reads a <application id="...">...</application> tag from the xml
    private static Application readApplication(XmlPullParser parser)
            throws XmlPullParserException, IOException {

        String packageName, lastUpdated, name, description, iconName, sourceCodeUrl;
        packageName = lastUpdated = name = description = iconName = sourceCodeUrl = null;

        parser.require(XmlPullParser.START_TAG, ns, "application");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG)
                continue;

            switch (parser.getName()) {
                case ID:
                    packageName = readText(parser);
                    break;
                case LAST_UPDATED:
                    lastUpdated = readText(parser);
                    break;
                case NAME:
                    name = readText(parser);
                    break;
                case DESCRIPTION:
                    description = readText(parser);
                    break;
                case ICON:
                    iconName = readText(parser);
                    break;
                case SOURCE_URL:
                    sourceCodeUrl = readText(parser);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, ns, "application");

        return new Application(packageName, lastUpdated,
                name, description, iconName, sourceCodeUrl);
    }

    // Reads the text from an xml tag
    private static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    // Skips a tag in the xml
    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG)
            throw new IllegalStateException();

        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG: --depth; break;
                case XmlPullParser.START_TAG: ++depth; break;
            }
        }
    }

    //endregion
}
