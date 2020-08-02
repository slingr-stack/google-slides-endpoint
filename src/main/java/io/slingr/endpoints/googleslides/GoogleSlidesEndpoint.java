package io.slingr.endpoints.googleslides;

import com.google.api.client.http.HttpResponseException;
import io.slingr.endpoints.PerUserEndpoint;
import io.slingr.endpoints.exceptions.EndpointException;
import io.slingr.endpoints.exceptions.ErrorCode;
import io.slingr.endpoints.framework.annotations.*;
import io.slingr.endpoints.googleslides.services.*;
import io.slingr.endpoints.googleslides.services.entities.ValidToken;
import io.slingr.endpoints.services.AppLogs;
import io.slingr.endpoints.services.datastores.DataStore;
import io.slingr.endpoints.services.exchange.ReservedName;
import io.slingr.endpoints.utils.Json;
import io.slingr.endpoints.ws.exchange.FunctionRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * <p>Google Slides endpoint
 *
 * <p>Created by dgaviola on 26/07/20.
 */
@SlingrEndpoint(name = "google-slides")
public class GoogleSlidesEndpoint extends PerUserEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSlidesEndpoint.class);

    private static final String API_URL = "https://slides.googleapis.com/v1";

    // user configuration properties
    private static final String PROPERTY_ID = "_id";
    private static final String PROPERTY_RESULT = "result";
    private static final String PROPERTY_NAME = "name";
    private static final String PROPERTY_PICTURE = "picture";
    public static final String PROPERTY_CODE = "code";
    private static final String PROPERTY_LAST_CODE = "lastCode";
    private static final String PROPERTY_REDIRECT_URI = "redirectUri";
    private static final String PROPERTY_TOKEN = "token";
    private static final String PROPERTY_REFRESH_TOKEN = "refreshToken";
    private static final String PROPERTY_EXPIRATION_TIME = "expirationTime";
    private static final String PROPERTY_TIMEZONE = "timezone";
    private static final String PROPERTY_ERROR = "error";

    @ApplicationLogger
    private AppLogs appLogs;

    @EndpointUserDataStore
    private DataStore usersDataStore;

    @EndpointProperty
    private String clientId;

    @EndpointProperty
    private String clientSecret;

    @EndpointProperty
    private String redirectUri;

    @EndpointProperty(name = "single")
    private String clientType;

    @EndpointProperty
    private String javascriptOrigin;

    private String defaultRedirectUri = "";

    @EndpointConfiguration
    private Json configuration;

    private GoogleClient client = null;

    @Override
    public void endpointStarted() {
        clientType = clientType != null && Arrays.asList("single", "multi").contains(clientType.toLowerCase()) ? clientType.toLowerCase() : "single";
        this.defaultRedirectUri = (
                properties().isLocalDeployment() ? "http://" : "https://"+
                        ("multi".equalsIgnoreCase(clientType) ? "" : properties().getApplicationName()+".")
        )+properties().getBaseDomain()+"/callback";

        // google client
        client = new GoogleClient(properties().getApplicationName(), clientId, clientSecret, redirectUri, ServiceType.values());
    }

    @EndpointFunction(name = ReservedName.CONNECT_USER)
    public Json connectUsers(FunctionRequest request) {
        final String userId = request.getUserId();
        if(StringUtils.isBlank(userId)) {
            throw EndpointException.permanent(ErrorCode.ARGUMENT, "User ID is required").returnCode(400);
        }

        final Json body = request.getJsonParams();
        final String functionId = request.getFunctionId();

        // default values
        final Json configuration = Json.map();
        configuration.set(PROPERTY_RESULT, "An error happened when connecting to Google. Please contact to administrator.");
        configuration.set(PROPERTY_NAME, null);
        configuration.set(PROPERTY_PICTURE, null);
        configuration.set(PROPERTY_CODE, null);
        configuration.set(PROPERTY_LAST_CODE, null);
        configuration.set(PROPERTY_REDIRECT_URI, null);
        configuration.set(PROPERTY_TOKEN, null);
        configuration.set(PROPERTY_REFRESH_TOKEN, null);
        configuration.set(PROPERTY_EXPIRATION_TIME, null);
        configuration.set(PROPERTY_TIMEZONE, null);

        boolean connected = false;
        try {
            // check stored configuration
            try {
                final Json storedConfiguration = usersDataStore.findById(userId);
                if (storedConfiguration != null) {
                    configuration.setIfNotNull(PROPERTY_RESULT, storedConfiguration.string(PROPERTY_RESULT));
                    configuration.setIfNotNull(PROPERTY_NAME, storedConfiguration.string(PROPERTY_NAME));
                    configuration.setIfNotNull(PROPERTY_PICTURE, storedConfiguration.string(PROPERTY_PICTURE));
                    configuration.setIfNotNull(PROPERTY_LAST_CODE, storedConfiguration.string(PROPERTY_LAST_CODE));
                    configuration.setIfNotNull(PROPERTY_TOKEN, storedConfiguration.string(PROPERTY_TOKEN));
                    configuration.setIfNotNull(PROPERTY_REFRESH_TOKEN, storedConfiguration.string(PROPERTY_REFRESH_TOKEN));
                    configuration.setIfNotNull(PROPERTY_EXPIRATION_TIME, storedConfiguration.string(PROPERTY_EXPIRATION_TIME));
                    configuration.setIfNotNull(PROPERTY_TIMEZONE, storedConfiguration.string(PROPERTY_TIMEZONE));
                }
            } catch (Exception ex){
                logger.info(String.format("User configuration not found [%s] [%s]", userId, ex.getMessage()), ex);
            }

            if (body != null) {
                // update new parameters
                configuration.setIfNotNull(PROPERTY_RESULT, body.string(PROPERTY_RESULT));
                configuration.setIfNotNull(PROPERTY_NAME, body.string(PROPERTY_NAME));
                configuration.setIfNotNull(PROPERTY_PICTURE, body.string(PROPERTY_PICTURE));
                configuration.setIfNotNull(PROPERTY_TOKEN, body.string(PROPERTY_TOKEN));
                configuration.setIfNotNull(PROPERTY_CODE, body.string(PROPERTY_CODE));
                configuration.setIfNotNull(PROPERTY_LAST_CODE, body.string(PROPERTY_LAST_CODE));
                configuration.setIfNotNull(PROPERTY_REDIRECT_URI, body.string(PROPERTY_REDIRECT_URI));
                configuration.setIfNotNull(PROPERTY_REFRESH_TOKEN, body.string(PROPERTY_REFRESH_TOKEN));
                configuration.setIfNotNull(PROPERTY_EXPIRATION_TIME, body.string(PROPERTY_EXPIRATION_TIME));
                configuration.setIfNotNull(PROPERTY_TIMEZONE, body.string(PROPERTY_TIMEZONE));
            }

            // generate token from code if code is present
            final String code = configuration.string(PROPERTY_CODE);
            configuration.set(PROPERTY_CODE, null);

            final String redirectUri = configuration.string(PROPERTY_REDIRECT_URI);
            configuration.set(PROPERTY_REDIRECT_URI, null);

            if (StringUtils.isNotBlank(code)) {
                if(!code.equals(configuration.string(PROPERTY_LAST_CODE))) {
                    final Json tokens = client.generateTokensFromCode(code, redirectUri);
                    if (tokens != null && StringUtils.isBlank(tokens.string(PROPERTY_ERROR))) {
                        configuration.set(PROPERTY_RESULT, "Connection established.");
                        configuration.set(PROPERTY_TOKEN, tokens.string(PROPERTY_TOKEN));
                        configuration.set(PROPERTY_REFRESH_TOKEN, tokens.string(PROPERTY_REFRESH_TOKEN));
                        configuration.set(PROPERTY_EXPIRATION_TIME, tokens.string(PROPERTY_EXPIRATION_TIME));
                        configuration.set(PROPERTY_LAST_CODE, code);
                    }
                }
            }

            Json checkedToken = client.checkTokenFromConfiguration(userId, configuration);
            if (StringUtils.isBlank(checkedToken.string(PROPERTY_ERROR))) {
                configuration.set(PROPERTY_TOKEN, checkedToken.string(PROPERTY_TOKEN));
                configuration.set(PROPERTY_REFRESH_TOKEN, checkedToken.string(PROPERTY_REFRESH_TOKEN));
                configuration.set(PROPERTY_EXPIRATION_TIME, checkedToken.string(PROPERTY_EXPIRATION_TIME));
            }

            if (StringUtils.isNotBlank(configuration.string(PROPERTY_TOKEN))) {
                connected = true;
                configuration.set(PROPERTY_RESULT, "Connection established.");

                final GoogleAuthenticationService service = client.getAuthenticationService(configuration.string(PROPERTY_TOKEN));
                if(service != null) {
                    Json user = service.getUserInformation();
                    if(user != null && StringUtils.isBlank(user.string(PROPERTY_TOKEN))){
                        configuration.set(PROPERTY_RESULT, "Connection established as " + user.string(PROPERTY_NAME) + ".");
                        configuration.set(PROPERTY_NAME, user.string(PROPERTY_NAME));
                        configuration.set(PROPERTY_PICTURE, user.string(PROPERTY_PICTURE));
                    }
                }
            }

        } catch (Exception ex){
            final String connectionError = String.format("Error when try to connect user [%s] [%s]", userId, ex.getMessage());
            logger.warn(connectionError, ex);
            appLogs.error(connectionError);
        }

        configuration.set("_id", userId);
        final Json conf = usersDataStore.save(configuration);
        if(connected) {
            final Json event = Json.map()
                    .setIfNotNull("userId", userId)
                    .setIfNotNull("userEmail", request.getUserEmail());

            if (conf != null) {
                logger.info(String.format("User connected [%s] [%s]", conf.string(PROPERTY_ID), conf.toString()));
                event.set("configuration", conf);
            } else {
                configuration.set("_id", userId);
                logger.info(String.format("An error happened when tries to save the new user configuration [%s] [%s]", userId, configuration.toString()));
                event.set("configuration", configuration);
            }

            // sends connected user event
            users().sendUserConnectedEvent(functionId, userId, event);

            return event;
        }

        logger.info(String.format("User [%s] can not be connected to Google", userId));
        return disconnectUser(userId, request.getUserEmail(), functionId, true);
    }

    @EndpointFunction(name = ReservedName.DISCONNECT_USER)
    public Json disconnectUser(FunctionRequest request) {
        final String userId = request.getUserId();
        if(StringUtils.isBlank(userId)) {
            throw EndpointException.permanent(ErrorCode.ARGUMENT, "User ID is required").returnCode(400);
        }

        final String functionId = request.getFunctionId();

        return disconnectUser(userId, request.getUserEmail(), functionId, true);
    }

    /**
     * Disconnect user and invalidate the token
     *
     * @param userId user id
     * @param functionId function id
     * @param revokeToken true if the token must be invalidated
     * @return last user configuration
     */
    public Json disconnectUser(final String userId, final String userEmail, final String functionId, boolean revokeToken){
        logger.info(String.format("Disconnect user [%s] request", userId));

        // default values
        final Json configuration = Json.map();
        configuration.set(PROPERTY_RESULT, "Connection disabled.");
        configuration.set(PROPERTY_NAME, null);
        configuration.set(PROPERTY_PICTURE, null);
        configuration.set(PROPERTY_CODE, null);
        configuration.set(PROPERTY_LAST_CODE, null);
        configuration.set(PROPERTY_TOKEN, null);
        configuration.set(PROPERTY_REFRESH_TOKEN, null);
        configuration.set(PROPERTY_EXPIRATION_TIME, null);
        configuration.set(PROPERTY_TIMEZONE, null);

        if(StringUtils.isNotBlank(userId)) {
            // revoke tokens
            if(revokeToken) {
                final Json storedConfiguration = getUserConfiguration(userId);
                if (storedConfiguration != null && !storedConfiguration.isEmpty()) {
                    client.revokeTokens(storedConfiguration.string(PROPERTY_TOKEN), storedConfiguration.string(PROPERTY_REFRESH_TOKEN));
                    logger.info(String.format("Revoked tokens for user [%s]", userId));
                }
            }
            try {
                final Object response = events().sendSync(ReservedName.USER_DISCONNECTED, null, functionId, userId, null);
                if (response != null) {
                    removeUserConfiguration(userId);
                } else {
                    logger.warn(String.format("The event to disconnect the user fail [%s]", userId));
                }
            } catch(Exception ex){
                logger.warn(String.format("The event to disconnect the user fail [%s]: %s", userId, ex.toString()));
            }
        }

        // sends disconnected user event
        users().sendUserDisconnectedEvent(functionId, userId);

        return Json.map()
                .setIfNotNull("configuration", configuration)
                .setIfNotNull("userId", userId)
                .setIfNotNull("userEmail", userEmail);
    }

    public Json saveUserConfiguration(String userId, Json newConfiguration){
        return saveUserConfiguration(userId, newConfiguration, true);
    }

    public Json saveUserConfiguration(String userId, Json newConfiguration, boolean getConfiguration){
        if(StringUtils.isNotBlank(userId)){
            logger.debug(String.format("Save user configuration [%s]", userId));
            try {
                // check last user configuration
                Json user = getConfiguration ? getUserConfiguration(userId) : null;
                if (user == null) {
                    user = Json.map();
                }

                // check new user configuration
                if (newConfiguration != null) {
                    user.merge(newConfiguration);
                }

                // save configuration
                user.set("_id", userId);
                usersDataStore.save(user);

                logger.debug(String.format("User configuration [%s] was saved [%s]", userId, user.toString()));

                return user;
            } catch (Exception ex){
                logger.warn(String.format("Error when try to save user configuration [%s] [%s]", userId, ex.getMessage()), ex);
            }
        } else {
            logger.warn(String.format("User id is empty [%s]", userId));
        }
        return null;
    }

    public void removeUserConfiguration(String userId){
        if(StringUtils.isNotBlank(userId)){
            logger.debug(String.format("Remove user configuration [%s]", userId));
            try {
                // remove last user configuration
                usersDataStore.removeById(userId);

                logger.debug(String.format("User configuration [%s] was deleted", userId));
            } catch (Exception ex){
                logger.warn(String.format("Error when try to delete user configuration [%s] [%s]", userId, ex.getMessage()), ex);
            }
        }
    }

    public Json getUserConfiguration(String userId){
        Json response = null;
        if(StringUtils.isNotBlank(userId)){
            logger.debug(String.format("Checking user configuration [%s]", userId));
            try {
                // check last user configuration
                response = usersDataStore.findById(userId);

                if(response != null && !response.isEmpty()) {
                    logger.info(String.format("User configuration [%s] was found", userId));
                } else {
                    logger.info(String.format("User configuration [%s] was not found", userId));
                }
            } catch (Exception ex){
                logger.warn(String.format("Error when try to find user configuration [%s] [%s]", userId, ex.getMessage()), ex);
            }
        }
        return response;
    }

    public void checkDisconnection(final String userId, final HttpResponseException httpException, final String functionId){
        final StringBuilder err = new StringBuilder();
        err.append(httpException.getStatusCode()).append(" ");
        if(httpException.getStatusMessage() != null){
            err.append(" - ").append(httpException.getStatusMessage());
        }
        if(httpException.getContent() != null){
            err.append(" - ").append(httpException.getContent());
        }
        if(httpException.getMessage() != null){
            err.append(" - ").append(httpException.getMessage());
        }
        final String message = err.toString().toLowerCase();

        if(message.contains("invalid credentials") || message.contains("authError")){
            if(refreshUserCredentialsById(userId) == null) {
                // Invalid Credentials and it is not possible to generate a new token, disconnect user
                logger.info(String.format("Invalid credentials for user [%s] - disconnecting", userId));
                disconnectUser(userId, null, functionId, true);
            }
        }
    }

    public Json checkUserById(final String userId){
        if(StringUtils.isNotBlank(userId)) {
            Json conf = getUserConfiguration(userId);
            if (conf != null && !conf.isEmpty()) {
                Json checkedToken = client.checkTokenFromConfiguration(userId, conf);
                if (StringUtils.isNotBlank(checkedToken.string(PROPERTY_ERROR))) {
                    logger.info(String.format("Invalid token for user [%s]: %s", userId, checkedToken.string(PROPERTY_ERROR)));
                } else {
                    conf.set(PROPERTY_TOKEN, checkedToken.string(PROPERTY_TOKEN));
                    conf.set(PROPERTY_REFRESH_TOKEN, checkedToken.string(PROPERTY_REFRESH_TOKEN));
                    conf.set(PROPERTY_EXPIRATION_TIME, checkedToken.string(PROPERTY_EXPIRATION_TIME));

                    return saveUserConfiguration(userId, conf, false);
                }
            } else {
                logger.info(String.format("User [%s] is not connected", userId));
            }
        } else {
            logger.info(String.format("Invalid user id [%s]", userId));
        }
        return null;
    }

    public Json checkUserOrDisconnect(final String userId, final String functionId){
        try {
            return checkUserById(userId);
        } catch (Exception ex){
            if(ex.toString().contains("\\\"invalid_grant\\\"") || ex.toString().contains("Token has been revoked.") ||
                    ex.toString().contains("Token has been expired or revoked.") || ex.toString().contains("Error renewing the token [401 Unauthorized]")){
                // token was revoked on Google service
                logger.info(String.format("Token for user [%s] has been revoked. Disconnecting user.", userId));
                disconnectUser(userId, null, functionId, false);
            }
            throw ex;
        }
    }

    public Json refreshUserCredentialsById(final String userId){
        if(StringUtils.isNotBlank(userId)) {
            Json conf = getUserConfiguration(userId);
            if (conf != null && !conf.isEmpty()) {
                Json checkedToken = client.checkToken(userId, null, conf.string(ValidToken.REFRESH_TOKEN), null);
                if (StringUtils.isBlank(checkedToken.string(PROPERTY_ERROR))) {
                    conf.set(PROPERTY_TOKEN, checkedToken.string(PROPERTY_TOKEN));
                    conf.set(PROPERTY_REFRESH_TOKEN, checkedToken.string(PROPERTY_REFRESH_TOKEN));
                    conf.set(PROPERTY_EXPIRATION_TIME, checkedToken.string(PROPERTY_EXPIRATION_TIME));

                    return saveUserConfiguration(userId, conf, false);
                } else {
                    logger.info(String.format("Invalid token for user [%s]: %s", userId, checkedToken.string(PROPERTY_ERROR)));
                }
            } else {
                logger.info(String.format("User [%s] is not connected", userId));
            }
        } else {
            logger.info(String.format("Invalid user id [%s]", userId));
        }
        return null;
    }

    private GoogleSlidesService getService(Json body, String userId, String userEmail, String functionId){
        String token = null;
        Json checkedConf = null;
        if(StringUtils.isNotBlank(userId)){
            checkedConf = checkUserOrDisconnect(userId, functionId);
        } else if(body != null && StringUtils.isNotBlank(body.string(PROPERTY_TOKEN))){
            token = body.string(PROPERTY_TOKEN);
        }
        if(StringUtils.isBlank(token) && StringUtils.isNotBlank(userId)){
            final Json conf = checkedConf != null ? checkedConf : getUserConfiguration(userId);
            if(conf != null && StringUtils.isNotBlank(conf.string(PROPERTY_TOKEN))){
                token = conf.string(PROPERTY_TOKEN);
            }
        }
        if(StringUtils.isNotBlank(token)){
            return client.getService(userId, token, this);
        } else {
            if(StringUtils.isNotBlank(userId)) {
                logger.info(String.format("Token was not generated for user [%s]", userId));
                disconnectUser(userId, userEmail, functionId, false);
            }
            throw EndpointException.permanent(ErrorCode.ARGUMENT, "Invalid user configuration");
        }
    }

    @EndpointFunction(name = "authenticationUrl")
    public Json getAuthenticationUrl(FunctionRequest request){
        return Json.map().setIfNotNull("url", client.generateAuthURL());
    }

    @EndpointWebService(path = "/")
    @EndpointWebService(path = "callback")
    public String callback(){
        return "ok";
    }

    @EndpointFunction(name = "getUserInformation")
    public Json getUserInformation(FunctionRequest request){
        final String userId = request.getUserId();
        appLogs.info(String.format("Request to GET USER INFORMATION received [%s]", userId));

        boolean connected = false;
        Json information = null;

        if(StringUtils.isNotBlank(userId)) {
            final Json configuration = checkUserById(userId);
            final String token = configuration.string(PROPERTY_TOKEN);

            if(StringUtils.isNotBlank(token)) {
                final GoogleAuthenticationService authenticationService = client.getAuthenticationService(token);

                information = authenticationService.getUserInformation();
                connected = true;
            }
        }

        final Json response = Json.map().set("status", connected).setIfNotEmpty("information", information);
        logger.info(String.format("Function GET USER INFORMATION: [%s]", response.toString()));
        return response;
    }

    @EndpointFunction(name = "_getRequest")
    public Json getRequest(FunctionRequest request){
        final Json data = request.getJsonParams();
        final String userId = request.getUserId();
        final String functionId = request.getFunctionId();
        appLogs.info("GET request received", data);

        final GoogleSlidesService service = getService(data, userId, request.getUserEmail(), functionId);

        final Json response = service.getRequest(buildUrl(data.string("path")), data.json("params"), functionId);
        logger.info(String.format("Function GET: [%s]", response.toString()));
        return response;
    }

    @EndpointFunction(name = "_postRequest")
    public Json postRequest(FunctionRequest request){
        final Json data = request.getJsonParams();
        final String userId = request.getUserId();
        final String functionId = request.getFunctionId();
        appLogs.info("POST request received", data);

        final Json content = getContent(data);

        final GoogleSlidesService service = getService(data, userId, request.getUserEmail(), functionId);

        final Json response = service.postRequest(buildUrl(data.string("path")), data.json("params"), content, functionId);
        logger.info(String.format("Function POST: [%s]", response.toString()));
        return response;
    }

    @EndpointFunction(name = "_putRequest")
    public Json putRequest(FunctionRequest request){
        final Json data = request.getJsonParams();
        final String userId = request.getUserId();
        final String functionId = request.getFunctionId();
        appLogs.info("PUT request received", data);

        final Json content = getContent(data);

        final GoogleSlidesService service = getService(data, userId, request.getUserEmail(), functionId);

        final Json response = service.putRequest(buildUrl(data.string("path")), data.json("params"), content, functionId);
        logger.info(String.format("Function PUT: [%s]", response.toString()));
        return response;
    }

    @EndpointFunction(name = "_patchRequest")
    public Json patchRequest(FunctionRequest request){
        final Json data = request.getJsonParams();
        final String userId = request.getUserId();
        final String functionId = request.getFunctionId();
        appLogs.info("PATCH request received", data);

        final Json content = getContent(data);

        final GoogleSlidesService service = getService(data, userId, request.getUserEmail(), functionId);

        final Json response = service.patchRequest(buildUrl(data.string("path")), data.json("params"), content, functionId);
        logger.info(String.format("Function PATCH: [%s]", response.toString()));
        return response;
    }

    @EndpointFunction(name = "_deleteRequest")
    public Json deleteRequest(FunctionRequest request){
        final Json data = request.getJsonParams();
        final String userId = request.getUserId();
        final String functionId = request.getFunctionId();
        appLogs.info("DELETE request received", data);

        final GoogleSlidesService service = getService(data, userId, request.getUserEmail(), functionId);

        final Json response = service.deleteRequest(buildUrl(data.string("path")), data.json("params"), functionId);
        logger.info(String.format("Function DELETE: [%s]", response.toString()));
        return response;
    }

    private Json getContent(Json body) {
        Json content = body.json("body");
        if(content == null) {
            content = body.json("params");
        }
        if(content == null) {
            content = Json.map();
        }
        return content;
    }

    private String buildUrl(String path) {
        if (path != null) {
            if (path.startsWith("https://")) {
                return path;
            } else if (path.startsWith("/")) {
                return API_URL+path;
            } else {
                return API_URL+"/"+path;
            }
        } else {
            return API_URL;
        }
    }
}
