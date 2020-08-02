package io.slingr.endpoints.googleslides.services;

import com.google.api.client.http.HttpResponseException;
import com.google.api.client.json.GenericJson;
import com.google.api.client.util.DateTime;
import com.google.api.services.slides.GenericGoogleSlidesService;
import com.google.api.services.slides.v1.SlidesRequest;
import io.slingr.endpoints.exceptions.EndpointException;
import io.slingr.endpoints.googleslides.GoogleSlidesEndpoint;
import io.slingr.endpoints.googleslides.services.entities.ApiException;
import io.slingr.endpoints.utils.Json;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * <p>Service class that interacts with the Google Slides API
 *
 * <p>Created by lefunes on 16/06/15.
 * <p>Created by dgaviola on 27/07/20.
 */
public class GoogleSlidesService {
    private static final Logger logger = Logger.getLogger(GoogleSlidesService.class);

    public static final String EXPIRATION_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat(EXPIRATION_TIME_FORMAT);

    private final String userId;
    private final GenericGoogleSlidesService service;
    private final GoogleSlidesEndpoint endpoint;

    public GoogleSlidesService(String userId, String applicationName, String token, GoogleSlidesEndpoint endpoint) {
        this.userId = userId;
        if (StringUtils.isBlank(token)) {
            throw new IllegalArgumentException("Invalid token");
        }
        if (StringUtils.isBlank(applicationName)) {
            applicationName = "Google Slides";
        }

        final GenericGoogleSlidesService service;
        try {
            service = new GenericGoogleSlidesService(applicationName, token);
        } catch (HttpResponseException e) {
            logger.info(String.format("Invalid response when try to build the Google Slides client [%s]", e.getContent() != null ? e.getContent() : e.getMessage()));
            throw ApiException.generate("Invalid response when try to build the Google Slides client", e);
        } catch (Exception e) {
            String cm = String.format("Error building the slides service [%s]", e.getMessage());
            logger.info(cm, e);
            throw ApiException.generate(cm, e);
        }
        this.service = service;
        this.endpoint = endpoint;
    }

    public Json getRequest(String url, Json params, String functionId) {
        try {
            GenericGoogleSlidesService.GenericRequests.GetRequest request = service.generic().get(url);
            applyParams(request, params);
            final GenericJson json = request.execute();
            final Json response = getJson(json);

            logger.info(String.format("Google response [%s]", response));
            return response;
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (HttpResponseException e) {
            return processHttpResponseException(functionId, e);
        } catch (Exception e) {
            return processException(e);
        }
    }

    public Json postRequest(String url, Json params, Json content, String functionId) {
        try {
            GenericGoogleSlidesService.GenericRequests.PostRequest request = service.generic().post(url, content);
            applyParams(request, params);
            final GenericJson json = request.execute();
            final Json response = getJson(json);

            logger.info(String.format("Google response [%s]", response));
            return response;
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (HttpResponseException e) {
            return processHttpResponseException(functionId, e);
        } catch (Exception e) {
            return processException(e);
        }
    }

    public Json putRequest(String url, Json params, Json content, String functionId) {
        try {
            GenericGoogleSlidesService.GenericRequests.PutRequest request = service.generic().put(url, content);
            applyParams(request, params);
            final GenericJson json = request.execute();
            final Json response = getJson(json);

            logger.info(String.format("Google response [%s]", response));
            return response;
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (HttpResponseException e) {
            return processHttpResponseException(functionId, e);
        } catch (Exception e) {
            return processException(e);
        }
    }

    public Json patchRequest(String url, Json params, Json content, String functionId) {
        try {
            GenericGoogleSlidesService.GenericRequests.PatchRequest request = service.generic().patch(url, content);
            applyParams(request, params);
            final GenericJson json = request.execute();
            final Json response = getJson(json);

            logger.info(String.format("Google response [%s]", response));
            return response;
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (HttpResponseException e) {
            return processHttpResponseException(functionId, e);
        } catch (Exception e) {
            return processException(e);
        }
    }

    public Json deleteRequest(String url, Json params, String functionId) {
        try {
            GenericGoogleSlidesService.GenericRequests.DeleteRequest request = service.generic().delete(url);
            applyParams(request, params);
            final GenericJson json = request.execute();
            final Json response = getJson(json);

            logger.info(String.format("Google response [%s]", response));
            return response;
        } catch (EndpointException e) {
            return e.toJson(true);
        } catch (HttpResponseException e) {
            return processHttpResponseException(functionId, e);
        } catch (Exception e) {
            return processException(e);
        }
    }

    public Json getJson(GenericJson genericJson) {
        final Json response = Json.fromMap(genericJson);
        response.traverse(new Json.Visitor() {
            @Override
            public Object convertValue(String key, Object value, String path) {
                if(value instanceof DateTime){
                    return ((DateTime) value).toStringRfc3339();
                }
                return value;
            }
        });
        return response;
    }

    private void applyParams(SlidesRequest request, Json params) {
        if (params != null) {
            for (String key : params.keys()) {
                request.set(key, params.object(key));
            }
        }
    }

    private static Json processException(Exception e) {
        return processException(logger, e);
    }

    private static Json processException(Exception e, String text) {
        return processException(logger, e, text);
    }

    public static Json processException(Logger logger, Exception e) {
        return processException(logger, e, "Exception when execute request");
    }

    public static Json processException(Logger logger, Exception e, String text) {
        final String message = String.format("%s [%s]", text, e.getMessage());
        logger.info(message, e);
        return ApiException.generate(message, e, true);
    }

    private Json processHttpResponseException(String functionId, HttpResponseException e) {
        return processHttpResponseException(endpoint, userId, functionId, e);
    }

    private Json processHttpResponseException(String functionId, HttpResponseException e, String text) {
        return processHttpResponseException(endpoint, userId, functionId, e, text);
    }

    public static Json processHttpResponseException(GoogleSlidesEndpoint endpoint, String userId, String functionId, HttpResponseException e) {
        return processHttpResponseException(endpoint, userId, functionId, e, "Exception when execute request");
    }

    public static Json processHttpResponseException(GoogleSlidesEndpoint endpoint, String userId, String functionId, HttpResponseException e, String text) {
        endpoint.checkDisconnection(userId, e, functionId);

        String message;
        try {
            Json json = Json.fromObject(e.getContent() != null ? e.getContent() : e.getMessage());
            message = String.format("%s [%s]", text, (json.contains("message") ? json.string("message") : json.toString()));
        } catch (Exception ex){
            message = String.format("%s [%s]", text, e.getContent() != null ? e.getContent() : e.getMessage());
        }
        logger.info(message, e);
        return ApiException.generate(message, e, true);
    }
}
