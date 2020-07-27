package com.google.api.services.slides;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.slides.v1.Slides;
import com.google.api.services.slides.v1.SlidesRequest;
import io.slingr.endpoints.utils.Json;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Generic service over Google Slides service
 * Created by dgaviola on 26/07/20.
 */
public class GenericGoogleSlidesService extends Slides {

    public GenericGoogleSlidesService(String applicationName, String token) throws GeneralSecurityException, IOException {
        super(
                GoogleNetHttpTransport.newTrustedTransport(),
                new JacksonFactory(),
                new GoogleCredential().setAccessToken(token)
        );
    }

    public GenericRequests generic() {
        return new GenericRequests();
    }

    public class GenericRequests {
        public GetRequest get(String url) throws IOException {
            GetRequest result = new GetRequest(url);
            initialize(result);
            return result;
        }

        public class GetRequest extends SlidesRequest<GenericJson> {
            GetRequest(String url) {
                super(GenericGoogleSlidesService.this, "GET", url, null, GenericJson.class);
            }

            @Override
            public GetRequest set(String parameterName, Object value) {
                return (GetRequest) super.set(parameterName, value);
            }
        }

        public PostRequest post(String url, Json content) throws IOException {
            PostRequest result = new PostRequest(url, content);
            initialize(result);
            return result;
        }

        public class PostRequest extends SlidesRequest<GenericJson> {
            PostRequest(String url, Json content) {
                super(GenericGoogleSlidesService.this, "POST", url, content != null ? content.toMap() : null, GenericJson.class);
            }
        }

        public PutRequest put(String url, Json content) throws IOException {
            PutRequest result = new PutRequest(url, content);
            initialize(result);
            return result;
        }

        public class PutRequest extends SlidesRequest<GenericJson> {
            PutRequest(String url, Json content) {
                super(GenericGoogleSlidesService.this, "PUT", url, content != null ? content.toMap() : null, GenericJson.class);
            }
        }

        public PatchRequest patch(String url, Json content) throws IOException {
            PatchRequest result = new PatchRequest(url, content);
            initialize(result);
            return result;
        }

        public class PatchRequest extends SlidesRequest<GenericJson> {
            PatchRequest(String url, Json content) {
                super(GenericGoogleSlidesService.this, "PATCH", url, content != null ? content.toMap() : null, GenericJson.class);
            }
        }

        public DeleteRequest delete(String url) throws IOException {
            DeleteRequest result = new DeleteRequest(url);
            initialize(result);
            return result;
        }

        public class DeleteRequest extends SlidesRequest<GenericJson> {
            DeleteRequest(String url) {
                super(GenericGoogleSlidesService.this, "DELETE", url, null, GenericJson.class);
            }
        }
    }

}
