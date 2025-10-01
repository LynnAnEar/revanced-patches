package app.revanced.extension.shared.innertube.utils;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.youtubeapi.app.playerdata.PlayerDataExtractor;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.revanced.extension.shared.innertube.client.YouTubeClient;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * The functions used in this class are referenced below:
 * - <a href="https://github.com/felipeucelli/JavaTube/blob/ec9011fa2ed584b867d276e683c421059b87bec5/src/main/java/com/github/felipeucelli/javatube/Youtube.java">JavaTube</a>
 * - <a href="https://github.com/TeamNewPipe/NewPipeExtractor/blob/68b4c9acbae2d167e7b1209bb6bf0ae086dd427e/extractor/src/main/java/org/schabi/newpipe/extractor/services/youtube/YoutubeJavaScriptExtractor.java">NewPipeExtractor</a>
 */
public class ThrottlingParameterUtils {
    /**
     * Regular expression pattern to find the signature timestamp.
     */
    private static final Pattern SIGNATURE_TIMESTAMP_PATTERN = Pattern.compile("signatureTimestamp[=:](\\d+)");
    /**
     * Regular expression pattern to find the 'n' parameter in streamingUrl.
     */
    private static final Pattern THROTTLING_PARAM_N_PATTERN = Pattern.compile("[&?]n=([^&]+)");
    /**
     * Regular expression pattern to find the 's' parameter in signatureCipher.
     */
    private static final Pattern THROTTLING_PARAM_S_PATTERN = Pattern.compile("s=([^&]+)");
    /**
     * Regular expression pattern to find the 'url' parameter in signatureCipher.
     */
    private static final Pattern THROTTLING_PARAM_URL_PATTERN = Pattern.compile("&url=([^&]+)");
    /**
     * JavaScript url format (Mobile Web).
     */
    private static final String PLAYER_JS_URL_FORMAT_MOBILE_WEB =
            "https://m.youtube.com/s/player/%s/player-plasma-ias-phone-en_US.vflset/base.js";
    /**
     * JavaScript url format (TV).
     */
    private static final String PLAYER_JS_URL_FORMAT_TV =
            "https://www.youtube.com/s/player/%s/tv-player-ias.vflset/tv-player-ias.js";
    /**
     * Hardcoded javascript url path (Mobile Web).
     */
    private static final String PLAYER_JS_HARDCODED_URL_PATH_MOBILE_WEB = "0004de42";
    /**
     * Hardcoded javascript url path (TV).
     */
    private static final String PLAYER_JS_HARDCODED_URL_PATH_TV = "0004de42";
    /**
     * Regular expression pattern to find variables used in JavaScript url.
     */
    private static final Pattern PLAYER_JS_IDENTIFIER_PATTERN =
            Pattern.compile("player\\\\/([a-z0-9]{8})\\\\/");
    /**
     * Service worker url (Mobile Web).
     */
    private static final String SERVICE_WORKER_URL_MOBILE_WEB =
            "https://m.youtube.com/sw.js_data";
    /**
     * Service worker url (TV).
     */
    private static final String SERVICE_WORKER_URL_TV =
            "https://www.youtube.com/tv/sw.js_data";
    /**
     * Url used to find variables used in JavaScript url.
     */
    private static final String IFRAME_API_URL =
            "https://www.youtube.com/iframe_api";
    /**
     * User-agent (Mobile Web).
     */
    private static final String USER_AGENT_MOBILE_WEB =
            "Mozilla/5.0 (Android 16; Mobile; rv:140.0) Gecko/140.0 Firefox/140.0";
    /**
     * User-agent (TV).
     */
    private static final String USER_AGENT_TV =
            "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; Xbox)";
    /**
     * Client version from service worker (Mobile Web).
     */
    @Nullable
    private volatile static String clientVersionMobileWeb = null;
    /**
     * Client version from service worker (TV).
     */
    @Nullable
    private volatile static String clientVersionTV = null;
    /**
     * Class used to deobfuscate, powered by SmartTube (Mobile Web).
     */
    @Nullable
    private volatile static PlayerDataExtractor extractorMobileWeb = null;
    /**
     * Class used to deobfuscate, powered by SmartTube (TV).
     */
    @Nullable
    private volatile static PlayerDataExtractor extractorTV = null;
    /**
     * Javascript contents (Mobile Web).
     */
    @Nullable
    private volatile static String playerJsMobileWeb = null;
    /**
     * Javascript contents (TV).
     */
    @Nullable
    private volatile static String playerJsTV = null;
    /**
     * Javascript url (Mobile Web).
     */
    @Nullable
    private volatile static String playerJsUrlMobileWeb = null;
    /**
     * Javascript url (TV).
     */
    @Nullable
    private volatile static String playerJsUrlTV = null;
    /**
     * Service worker Json Array (Mobile Web).
     */
    @Nullable
    private volatile static JSONArray serviceWorkerJsonArrayMobileWeb = null;
    /**
     * Service worker Json Array (TV).
     */
    @Nullable
    private volatile static JSONArray serviceWorkerJsonArrayTV = null;
    /**
     * Field value included when sending a request (Mobile Web).
     */
    @Nullable
    private volatile static Integer signatureTimestampMobileWeb = null;
    /**
     * Field value included when sending a request (TV).
     */
    @Nullable
    private volatile static Integer signatureTimestampTV = null;
    /**
     * Visitor data from service worker (Mobile Web).
     */
    @Nullable
    private volatile static String visitorIdMobileWeb = null;
    /**
     * Visitor data from service worker (TV).
     */
    @Nullable
    private volatile static String visitorIdTV = null;

    private volatile static boolean isInitialized = false;
    private volatile static boolean useEJS = false;

    /**
     * Typically, there are 10 to 30 available formats for a video.
     * Each format has a different streaming url, but the 'n' parameter in the response is the same.
     * If the obfuscated 'n' parameter and the deobfuscated 'n' parameter are put in a Map,
     * the remaining 9 to 29 streaming urls can be deobfuscated quickly using the values put in the Map.
     */
    private static final Map<String, String> nParamMap = new LinkedHashMap<>() {
        private static final int NUMBER_OF_N_PARAM = 50;

        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > NUMBER_OF_N_PARAM;
        }
    };

    public static void initializeJavascript(boolean useEJS, boolean useMobileWeb) {
        if (isInitialized) {
            return;
        }
        if (!Utils.isNetworkConnected()) {
            return;
        }
        isInitialized = true;
        ThrottlingParameterUtils.useEJS = useEJS;

        if (!useEJS) {
            playerJsUrlMobileWeb = String.format(PLAYER_JS_URL_FORMAT_MOBILE_WEB, PLAYER_JS_HARDCODED_URL_PATH_MOBILE_WEB);
            playerJsUrlTV = String.format(PLAYER_JS_URL_FORMAT_TV, PLAYER_JS_HARDCODED_URL_PATH_TV);
        }

        extractorTV = getExtractor(true);
        playerJsTV = getPlayerJs(true);
        playerJsUrlTV = getPlayerJsUrl(true);
        signatureTimestampTV = getSignatureTimestamp(true);

        if (useEJS) {
            clientVersionTV = getClientVersion(true);
            serviceWorkerJsonArrayTV = getServiceWorkerJsonArray(true);
        }

        if (useMobileWeb) {
            extractorMobileWeb = getExtractor(false);
            playerJsMobileWeb = getPlayerJs(false);
            playerJsUrlMobileWeb = getPlayerJsUrl(false);
            signatureTimestampMobileWeb = getSignatureTimestamp(false);
            visitorIdMobileWeb = getVisitorId(false);
            serviceWorkerJsonArrayMobileWeb = getServiceWorkerJsonArray(false);
            if (useEJS) {
                clientVersionMobileWeb = getClientVersion(false);
            }
        }
    }

    private static void resetAll() {
        isInitialized = false;

        clientVersionMobileWeb = null;
        clientVersionTV = null;
        extractorMobileWeb = null;
        extractorTV = null;
        playerJsMobileWeb = null;
        playerJsTV = null;
        playerJsUrlMobileWeb = null;
        playerJsUrlTV = null;
        serviceWorkerJsonArrayMobileWeb = null;
        serviceWorkerJsonArrayTV = null;
        signatureTimestampMobileWeb = null;
        signatureTimestampTV = null;
        visitorIdMobileWeb = null;
        visitorIdTV = null;
    }

    @Nullable
    private static Integer setSignatureTimestamp(boolean isTV) {
        try {
            String playerJs = getPlayerJs(isTV);
            if (playerJs != null) {
                Matcher matcher = SIGNATURE_TIMESTAMP_PATTERN.matcher(playerJs);
                if (matcher.find()) {
                    String signatureTimestamp = matcher.group(1);
                    if (StringUtils.isNotEmpty(signatureTimestamp)) {
                        Logger.printDebug(() -> "signatureTimestamp: " + signatureTimestamp);
                        return Helpers.parseInt(signatureTimestamp);
                    }
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "setSignatureTimestamp failed", ex);
        }
        Logger.printDebug(() -> "signatureTimestamp not found");
        return null;
    }

    @Nullable
    public static Integer getSignatureTimestamp(boolean isTV) {
        Integer signatureTimestamp = isTV
                ? signatureTimestampTV
                : signatureTimestampMobileWeb;
        if (signatureTimestamp == null) {
            signatureTimestamp = setSignatureTimestamp(isTV);
            if (isTV) {
                signatureTimestampTV = signatureTimestamp;
            } else {
                signatureTimestampMobileWeb = signatureTimestamp;
            }
        }
        return signatureTimestamp;
    }

    private static JSONArray setServiceWorkerJsonArray(boolean isTV) {
        String jsonString = fetch(isTV
                        ? SERVICE_WORKER_URL_TV
                        : SERVICE_WORKER_URL_MOBILE_WEB,
                isTV
        );
        if (jsonString != null) {
            if (jsonString.startsWith(")]}'"))
                jsonString = jsonString.substring(4);

            try {
                JSONArray jsonArray = new JSONArray(jsonString);

                //jsonArray[0][2][0][0]
                return jsonArray
                        .getJSONArray(0)
                        .getJSONArray(2)
                        .getJSONArray(0)
                        .getJSONArray(0);
            } catch (Exception ex) {
                Logger.printException(() -> "setServiceWorkerJsonArray failed", ex);
            }
        }
        return null;
    }

    private static JSONArray getServiceWorkerJsonArray(boolean isTV) {
        JSONArray serviceWorkerJsonArray = isTV
                ? serviceWorkerJsonArrayTV
                : serviceWorkerJsonArrayMobileWeb;
        if (serviceWorkerJsonArray == null) {
            serviceWorkerJsonArray = setServiceWorkerJsonArray(isTV);
            if (isTV) {
                serviceWorkerJsonArrayTV = serviceWorkerJsonArray;
            } else {
                serviceWorkerJsonArrayMobileWeb = serviceWorkerJsonArray;
            }
        }
        return serviceWorkerJsonArray;
    }

    private static String setClientVersion(boolean isTV) {
        JSONArray serviceWorkerJsonArray = getServiceWorkerJsonArray(isTV);

        if (serviceWorkerJsonArray != null) {
            try {
                String clientVersion = serviceWorkerJsonArray
                        .getString(16);
                if (StringUtils.isNotEmpty(clientVersion)) {
                    Logger.printDebug(() -> "clientVersion: " + clientVersion + ", isTV: " + isTV);
                }
                return clientVersion;
            } catch (Exception ex) {
                Logger.printException(() -> "setClientVersion failed", ex);
            }
        }

        return null;
    }

    public static String getClientVersion(boolean isTV) {
        String clientVersion = isTV
                ? clientVersionTV
                : clientVersionMobileWeb;
        if (clientVersion == null) {
            clientVersion = setClientVersion(isTV);
            if (isTV) {
                clientVersionTV = clientVersion;
            } else {
                clientVersionMobileWeb = clientVersion;
            }
        }

        return clientVersion;
    }

    public static String getClientVersion(YouTubeClient.ClientType clientType) {
        String hardCodedClientVersion = clientType.getClientVersion();
        if (useEJS) {
            if (clientType == YouTubeClient.ClientType.TV || clientType == YouTubeClient.ClientType.MWEB) {
                boolean isTV = clientType == YouTubeClient.ClientType.TV;
                String clientVersion = getClientVersion(isTV);
                if (clientVersion != null) {
                    return clientVersion;
                }
            }
        }

        return hardCodedClientVersion;
    }

    private static String setVisitorId(boolean isTV) {
        JSONArray serviceWorkerJsonArray = getServiceWorkerJsonArray(isTV);

        if (serviceWorkerJsonArray != null) {
            try {
                String visitorId = serviceWorkerJsonArray
                        .getString(13);
                if (StringUtils.isNotEmpty(visitorId)) {
                    Logger.printDebug(() -> "visitorId: " + visitorId + ", isTV: " + isTV);
                }
                return visitorId;
            } catch (Exception ex) {
                Logger.printException(() -> "setVisitorId failed", ex);
            }
        }

        return null;
    }

    public static String getVisitorId(boolean isTV) {
        String visitorId = isTV
                ? visitorIdTV
                : visitorIdMobileWeb;
        if (visitorId == null) {
            visitorId = setVisitorId(isTV);
            if (isTV) {
                visitorIdTV = visitorId;
            } else {
                visitorIdMobileWeb = visitorId;
            }
        }

        return visitorId;
    }


    @Nullable
    private static String setPlayerJsUrl(boolean isTV) {
        String iframeContent = fetch(IFRAME_API_URL, isTV);
        if (iframeContent != null) {
            Matcher matcher = PLAYER_JS_IDENTIFIER_PATTERN.matcher(iframeContent);
            if (matcher.find()) {
                try {
                    return String.format(
                            isTV ? PLAYER_JS_URL_FORMAT_TV : PLAYER_JS_URL_FORMAT_MOBILE_WEB,
                            matcher.group(1)
                    );
                } catch (Exception ex) {
                    Logger.printException(() -> "setPlayerJsUrl failed", ex);
                    resetAll();
                }
            }
        }
        Logger.printDebug(() -> "iframeContent not found");
        return null;
    }

    @Nullable
    private static String getPlayerJsUrl(boolean isTV) {
        String playerJsUrl = isTV
                ? playerJsUrlTV
                : playerJsUrlMobileWeb;
        if (playerJsUrl == null) {
            playerJsUrl = setPlayerJsUrl(isTV);
            if (isTV) {
                playerJsUrlTV = playerJsUrl;
            } else {
                playerJsUrlMobileWeb = playerJsUrl;
            }
        }
        return playerJsUrl;
    }

    @Nullable
    private static String setPlayerJs(boolean isTV) {
        String playerJsUrl = getPlayerJsUrl(isTV);
        if (playerJsUrl != null) {
            return fetch(playerJsUrl, isTV);
        }
        return null;
    }

    @Nullable
    private static String getPlayerJs(boolean isTV) {
        String playerJs = isTV
                ? playerJsTV
                : playerJsMobileWeb;
        if (playerJs == null) {
            playerJs = setPlayerJs(isTV);
            if (isTV) {
                playerJsTV = playerJs;
            } else {
                playerJsMobileWeb = playerJs;
            }
        }
        return playerJs;
    }

    @Nullable
    private static PlayerDataExtractor setExtractor(boolean isTV) {
        String playerJs = getPlayerJs(isTV);
        if (playerJs != null) {
            return new PlayerDataExtractor(playerJs, useEJS);
        }
        return null;
    }

    @Nullable
    private static PlayerDataExtractor getExtractor(boolean isTV) {
        PlayerDataExtractor extractor = isTV
                ? extractorTV
                : extractorMobileWeb;
        if (extractor == null) {
            extractor = setExtractor(isTV);
            if (isTV) {
                extractorTV = extractor;
            } else {
                extractorMobileWeb = extractor;
            }
        }
        return extractor;
    }

    private static void handleConnectionError(String toastMessage, @Nullable Exception ex) {
        Logger.printDebug(() -> toastMessage, ex);
    }

    @Nullable
    public static String fetch(@NonNull String url, boolean isTV) {
        try {
            return Utils.submitOnBackgroundThread(() -> fetchUrl(url, isTV)).get();
        } catch (ExecutionException | InterruptedException ex) {
            Logger.printDebug(() -> "Could not fetch url: " + url, ex);
        }

        return null;
    }

    @Nullable
    private static String fetchUrl(@NonNull String uri, boolean isTV) {
        Utils.verifyOffMainThread();

        final long startTime = System.currentTimeMillis();
        Logger.printDebug(() -> "fetching url: " + uri);

        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(uri)
                    .header("Accept-Language", "en-US,en")
                    .header("User-Agent", isTV ? USER_AGENT_TV : USER_AGENT_MOBILE_WEB)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    ResponseBody responseBody = response.body();
                    if (responseBody != null) {
                        return responseBody.string();
                    }
                } else {
                    handleConnectionError("API not available with response code: "
                            + response.code() + " message: " + response.message(), null);
                }
            }
        } catch (SocketTimeoutException ex) {
            handleConnectionError("Connection timeout", ex);
        } catch (IOException ex) {
            handleConnectionError("Network error", ex);
        } catch (Exception ex) {
            Logger.printException(() -> "fetching url failed", ex);
        } finally {
            Logger.printDebug(() -> "fetched url: " + uri + " took: " + (System.currentTimeMillis() - startTime) + "ms");
        }

        return null;
    }

    @Nullable
    public static String deobfuscateStreamingUrl(
            @NonNull String videoId,
            @NonNull String cpn,
            @Nullable String url,
            @Nullable String signatureCipher,
            @Nullable String poToken,
            boolean isTV) {
        String streamUrl = null;
        if (StringUtils.isNotEmpty(url)) {
            streamUrl = url;
        } else if (StringUtils.isNotEmpty(signatureCipher)) {
            streamUrl = getUrlWithThrottlingParameterObfuscated(
                    videoId,
                    signatureCipher,
                    isTV
            );
        }
        if (StringUtils.isNotEmpty(streamUrl)) {
            String deobfuscatedUrl = getUrlWithThrottlingParameterDeobfuscated(
                    videoId,
                    streamUrl,
                    isTV
            );
            if (StringUtils.isNotEmpty(cpn)) {
                deobfuscatedUrl += "&cpn=" + cpn;
            }
            if (StringUtils.isNotEmpty(poToken)) {
                deobfuscatedUrl += "&pot=" + poToken;
            }
            return deobfuscatedUrl;
        }
        return null;
    }

    /**
     * Convert signatureCipher to streaming url with obfuscated 'n' parameter.
     * <p>
     *
     * @param videoId         Current video id.
     * @param signatureCipher The 'signatureCipher' included in the response.
     * @return Streaming url with obfuscated 'n' parameter.
     */
    @Nullable
    private static String getUrlWithThrottlingParameterObfuscated(@NonNull String videoId, @NonNull String signatureCipher,
                                                                 boolean isTV) {
        try {
            PlayerDataExtractor extractor = getExtractor(isTV);
            if (extractor != null) {
                Matcher paramSMatcher = THROTTLING_PARAM_S_PATTERN.matcher(signatureCipher);
                Matcher paramUrlMatcher = THROTTLING_PARAM_URL_PATTERN.matcher(signatureCipher);
                if (paramSMatcher.find() && paramUrlMatcher.find()) {
                    // The 's' parameter from signatureCipher.
                    String sParam = paramSMatcher.group(1);
                    // The 'url' parameter from signatureCipher.
                    String urlParam = paramUrlMatcher.group(1);
                    if (StringUtils.isNotEmpty(sParam) && StringUtils.isNotEmpty(urlParam)) {
                        // The 'sig' parameter converted by javascript rules.
                        String decodedSigParm = extractor.extractSig(Helpers.decode(sParam));
                        if (StringUtils.isNotEmpty(decodedSigParm)) {
                            String decodedUriParm = Helpers.decode(urlParam);
                            Logger.printDebug(() -> "Converted signatureCipher to obfuscatedUrl, videoId: " + videoId);
                            return decodedUriParm + "&sig=" + decodedSigParm;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "getUrlWithThrottlingParameterObfuscated failed", ex);
        }

        Logger.printDebug(() -> "Failed to convert signatureCipher, videoId: " + videoId);
        return null;
    }

    /**
     * Deobfuscates the obfuscated 'n' parameter to a valid streaming url.
     * <p>
     *
     * @param videoId       Current video id.
     * @param obfuscatedUrl Streaming url with obfuscated 'n' parameter.
     * @return Deobfuscated streaming url.
     */
    @Nullable
    private static String getUrlWithThrottlingParameterDeobfuscated(@NonNull String videoId, @NonNull String obfuscatedUrl,
                                                                   boolean isTV) {
        try {
            // Obfuscated url is empty.
            if (StringUtils.isEmpty(obfuscatedUrl)) {
                Logger.printDebug(() -> "obfuscatedUrl is empty, videoId: " + videoId);
                return obfuscatedUrl;
            }

            // The 'n' parameter from obfuscatedUrl.
            String obfuscatedNParams = getThrottlingParameterFromStreamingUrl(obfuscatedUrl);

            // The 'n' parameter is null or empty.
            if (StringUtils.isEmpty(obfuscatedNParams)) {
                Logger.printDebug(() -> "'n' parameter not found in obfuscated streaming url, videoId: " + videoId);
                return obfuscatedUrl;
            }

            // If the deobfuscated 'n' parameter is in the Map, return it.
            String deobfuscatedNParam = nParamMap.get(obfuscatedNParams);
            if (deobfuscatedNParam != null) {
                Logger.printDebug(() -> "Cached 'n' parameter found, videoId: " + videoId + ", deobfuscatedNParams: " + deobfuscatedNParam);
                return replaceNParam(obfuscatedUrl, obfuscatedNParams, deobfuscatedNParam);
            }

            // Deobfuscate the 'n' parameter.
            Pair<String, String> deobfuscatedNParamPairs = decodeNParam(obfuscatedUrl, obfuscatedNParams, isTV);
            String deobfuscatedUrl = deobfuscatedNParamPairs.first;
            String deobfuscatedNParams = deobfuscatedNParamPairs.second;
            if (!deobfuscatedNParams.isEmpty()) {
                // If the 'n' parameter obfuscation was successful, put it in the map.
                nParamMap.put(obfuscatedNParams, deobfuscatedNParams);
                Logger.printDebug(() -> "Deobfuscated the 'n' parameter, videoId: " + videoId + ", obfuscatedNParams: " + obfuscatedNParams + ", deobfuscatedNParams: " + deobfuscatedNParams);
                return deobfuscatedUrl;
            }
        } catch (Exception ex) {
            Logger.printException(() -> "getUrlWithThrottlingParameterDeobfuscated failed", ex);
        }

        Logger.printDebug(() -> "Failed to obfuscate 'n' parameter, videoId: " + videoId);
        return obfuscatedUrl;
    }

    /**
     * Extract the 'n' parameter from the streaming Url.
     * <p>
     *
     * @param streamingUrl The streaming url.
     * @return The 'n' parameter.
     */
    @Nullable
    private static String getThrottlingParameterFromStreamingUrl(@NonNull String streamingUrl) {
        if (streamingUrl.contains("&n=") || streamingUrl.contains("?n=")) {
            final Matcher matcher = THROTTLING_PARAM_N_PATTERN.matcher(streamingUrl);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "";
    }

    /**
     * Replace the 'n' parameter.
     *
     * @param obfuscatedUrl       Streaming url with obfuscated 'n' parameter.
     * @param obfuscatedNParams   Obfuscated 'n' parameter.
     * @param deObfuscatedNParams Deobfuscated 'n' parameter.
     * @return Deobfuscated streaming url.
     */
    @NonNull
    private static String replaceNParam(@NonNull String obfuscatedUrl, @NonNull String obfuscatedNParams, @NonNull String deObfuscatedNParams) {
        return obfuscatedUrl.replaceFirst("n=" + obfuscatedNParams, "n=" + deObfuscatedNParams);
    }

    /**
     * Deobfuscate the 'n' parameter.
     * <p>
     *
     * @param obfuscatedUrl     Streaming url with obfuscated 'n' parameter.
     * @param obfuscatedNParams Obfuscated 'n' parameter.
     * @return Deobfuscated Pair(Deobfuscated streaming url, Deobfuscated 'n' parameter).
     */
    @NonNull
    private static Pair<String, String> decodeNParam(@NonNull String obfuscatedUrl, @NonNull String obfuscatedNParams,
                                                     boolean isTV) {
        try {
            PlayerDataExtractor extractor = getExtractor(isTV);
            if (extractor != null) {
                // The 'n' parameter deobfuscated by javascript rules.
                String deObfuscatedNParams = extractor.extractNSig(obfuscatedNParams);
                if (StringUtils.isNotEmpty(deObfuscatedNParams)) {
                    String deObfuscatedUrl = replaceNParam(obfuscatedUrl, obfuscatedNParams, deObfuscatedNParams);
                    return new Pair<>(deObfuscatedUrl, deObfuscatedNParams);
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "decodeNSig failed", ex);
        }

        return new Pair<>(obfuscatedUrl, "");
    }
}
