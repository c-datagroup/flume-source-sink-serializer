package net.dataservice.flume.http.source;

/**
 * Created by zhengwx on 7/11/17.
 */

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.event.EventBuilder;
import org.apache.flume.event.JSONEvent;
import org.apache.flume.source.http.HTTPBadRequestException;
import org.apache.flume.source.http.HTTPSourceHandler;
import org.apache.flume.source.http.JSONHandler;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.lang.reflect.Type;
import java.nio.charset.UnsupportedCharsetException;
import java.util.*;

/**
 * JSONHandler for HTTPSource that accepts an array of events.
 *
 * This handler throws exception if the deserialization fails because of bad
 * format or any other reason and will put the HTTP request headers into the headers.
 *
 * Each event must be encoded as a map with two key-value pairs. <p> 1. headers
 * - the key for this key-value pair is "headers". The value for this key is
 * another map, which represent the event headers. These headers are inserted
 * into the Flume event as is. <p> 2. body - The body is a string which
 * represents the body of the event. The key for this key-value pair is "body".
 * All key-value pairs are considered to be headers. An example: <p> [{"headers"
 * : {"a":"b", "c":"d"},"body": "random_body"}, {"headers" : {"e": "f"},"body":
 * "random_body2"}] <p> would be interpreted as the following two flume events:
 * <p> * Event with body: "random_body" (in UTF-8/UTF-16/UTF-32 encoded bytes)
 * and headers : (a:b, c:d) <p> *
 * Event with body: "random_body2" (in UTF-8/UTF-16/UTF-32 encoded bytes) and
 * headers : (e:f) <p>
 *
 * The charset of the body is read from the request and used. If no charset is
 * set in the request, then the charset is assumed to be JSON's default - UTF-8.
 * The JSON handler supports UTF-8, UTF-16 and UTF-32.
 *
 * To set the charset, the request must have content type specified as
 * "application/json; charset=UTF-8" (replace UTF-8 with UTF-16 or UTF-32 as
 * required).
 *
 * One way to create an event in the format expected by this handler, is to
 * use {@linkplain JSONEvent} and use {@linkplain Gson} to create the JSON
 * string using the
 * {@linkplain Gson#toJson(java.lang.Object, java.lang.reflect.Type) }
 * method. The type token to pass as the 2nd argument of this method
 * for list of events can be created by: <p>
 * {@code
 * Type type = new TypeToken<List<JSONEvent>>() {}.getType();
 * }
 */

public class CustomizedJSONHandler implements HTTPSourceHandler{
    private static final Logger LOG = LoggerFactory.getLogger(JSONHandler.class);
    private final Type listType = new TypeToken<List<JSONEvent>>() {}.getType();
    private final Gson gson;

    private static final String USER_AGENT = "User-Agent";
    private static final String REFERER = "Referer";
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String DATE_TIME = "Date-Time";
    private static final String DATE_TIME_FORMAT = "yyyyMMdd HH:mm:ss";
    private static final String CID = "uuid_tt_dd";
    private static final String SID = "dc_session_id";
    private static final String DEFAULT_PATH = "/";
    private static final String DEFAULT_DOMAIN = "";
    private static final int SECONDS_PER_YEAR = 60 * 60 * 24 * 365;
    private static final int SECONDS_HALF_HOUR = 60 * 30;

    public CustomizedJSONHandler(){
        gson = new GsonBuilder().disableHtmlEscaping().create();
    }

    /**
     * {@inheritDoc}
     */
    public List<Event> getEvents(HttpServletRequest request) throws Exception {
        return getEvents(request, null);
    }

    public List<Event> getEvents(HttpServletRequest request, HttpServletResponse response) throws Exception {
        BufferedReader reader = request.getReader();
        String charset = request.getCharacterEncoding();
        //UTF-8 is default for JSON. If no charset is specified, UTF-8 is to
        //be assumed.
        if (charset == null) {
            LOG.debug("Charset is null, default charset of UTF-8 will be used.");
            charset = "UTF-8";
        }
        else if (!(charset.equalsIgnoreCase("utf-8")
                || charset.equalsIgnoreCase("utf-16")
                || charset.equalsIgnoreCase("utf-32"))) {
            LOG.error("Unsupported character set in request {}. "
                    + "JSON handler supports UTF-8, "
                    + "UTF-16 and UTF-32 only.", charset);
            throw new UnsupportedCharsetException("JSON handler supports UTF-8, "
                    + "UTF-16 and UTF-32 only.");
        }

    /*
     * Gson throws Exception if the data is not parseable to JSON.
     * Need not catch it since the source will catch it and return error.
     */
        List<Event> eventList = new ArrayList<Event>(0);
        try {
            eventList = gson.fromJson(reader, listType);
        } catch (JsonSyntaxException ex) {
            throw new HTTPBadRequestException("Request has invalid JSON Syntax.", ex);
        }

        Map<String, String> requestHeaders = getRequestHeaders(request, response);
        for (Event e : eventList) {
            ((JSONEvent) e).setCharset(charset);
            e.getHeaders().putAll(requestHeaders);
        }
        return getSimpleEvents(eventList);
    }

    public void configure(Context context) {
    }

    private Map<String, String> getRequestHeaders(HttpServletRequest request, HttpServletResponse response){
        Map<String, String> requestHeaders = new HashMap<String, String>();

        String datetime = new DateTime().toString(DATE_TIME_FORMAT);
        requestHeaders.put(DATE_TIME, datetime);
        requestHeaders.put(USER_AGENT, getRequestHeader(request, USER_AGENT, "-"));
        requestHeaders.put(REFERER, getRequestHeader(request, REFERER, "-"));
        requestHeaders.put(X_FORWARDED_FOR, getIPAddress(getRequestHeader(request, X_FORWARDED_FOR)));
        requestHeaders.put(CID, getCookieID(request, response, datetime));
        requestHeaders.put(SID, getSessionID(request, response));
        return requestHeaders;
    }

    private String getIPAddress(String xForwardedFor){
        if (xForwardedFor != null && xForwardedFor.length() > 0) {
            return xForwardedFor.split(",")[0];
        } else {
            return "127.0.0.1";
        }
    }

    private String getRequestHeader(HttpServletRequest request, String headerName){
        return request.getHeader(headerName);
    }

    private String getRequestHeader(HttpServletRequest request, String headerName, String defaultValue){
        String value = request.getHeader(headerName);
        return value != null? value: defaultValue;
    }

    private String getCookieID(HttpServletRequest request, HttpServletResponse response, String currentDateTime){
        String cid = getCookie(request, CID);
        if(isEmpty(cid)){
            cid = UUID.randomUUID().getMostSignificantBits() + "_" + currentDateTime;
            setCookie(response, ImmutableMap.of("name", CID, "value", cid, "max_age", SECONDS_PER_YEAR,  "path", DEFAULT_PATH, "domain", DEFAULT_DOMAIN));
            LOG.debug("set the cookie id {} with value {}", CID, cid);
        }
        return cid;
    }

    private String getSessionID(HttpServletRequest request, HttpServletResponse response){
        String sid = getCookie(request, SID);
        if(isEmpty(sid)){
            sid = String.valueOf(new Date().getTime());
        }
        else{
            long nowTimeInMillSeconds = new Date().getTime();
            if (nowTimeInMillSeconds - Long.parseLong(sid) > SECONDS_HALF_HOUR * 1000){
                sid = String.valueOf(nowTimeInMillSeconds);
                setCookie(response, ImmutableMap.of("name", SID, "value", sid, "max_age", SECONDS_HALF_HOUR,  "path", DEFAULT_PATH, "domain", DEFAULT_DOMAIN));
                LOG.debug("set the session id {} with value {}", SID, sid);
            }
        }
        return sid;
    }

    private String getCookie(HttpServletRequest request, String cookieName){
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length != 0){
            for(Cookie cookie: cookies){
                if (cookie.getName().equalsIgnoreCase(cookieName)){
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void setCookie(HttpServletResponse response, Map cookieInfo){
        Cookie cookie = new Cookie((String)cookieInfo.get("name"), (String)cookieInfo.get("value"));
        if(cookieInfo.containsKey("domain")) {
            cookie.setDomain((String)cookieInfo.get("domain"));
        }

        if(cookieInfo.containsKey("max_age")) {
            cookie.setMaxAge(((Integer)cookieInfo.get("max_age")).intValue());
        }

        if(cookieInfo.containsKey("path")) {
            cookie.setPath((String)cookieInfo.get("path"));
        }

        if(cookieInfo.containsKey("secure")) {
            cookie.setSecure(((Boolean)cookieInfo.get("secure")).booleanValue());
        }

        if(cookieInfo.containsKey("version")) {
            cookie.setVersion(((Integer)cookieInfo.get("version")).intValue());
        }
        response.addCookie(cookie);
    }

    private List<Event> getSimpleEvents(List<Event> events) {
        List<Event> newEvents = new ArrayList<Event>(events.size());
        for (Event e:events) {
            newEvents.add(EventBuilder.withBody(e.getBody(), e.getHeaders()));
        }
        return newEvents;
    }

    private static boolean isEmpty(String value){
        return value == null || value.length() == 0;
    }
}
