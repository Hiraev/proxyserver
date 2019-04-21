package http.proxy.cache;

import okhttp3.Response;
import okhttp3.ResponseBody;

public class ResponseWrapper {

    private boolean valid = true;
    private long length;
    private final Response response;
    private final long createdTime;
    private byte[] body;

    public ResponseWrapper(Response response, byte[] body) {
        this.response = cloneResponse(response, body);
        createdTime = System.currentTimeMillis();
        this.body = body;
        try {
            length = this.response.body().source().getBuffer().size();
            if (length < 1) valid = false;
        } catch (NullPointerException e) {
            valid = false;
        }
    }

    public boolean isValid() {
        return valid;
    }

    public long length() {
        return length;
    }

    public Response getResponse() {
        return response;
    }

    public long getCreatedTime() {
        return createdTime;
    }


    private Response cloneResponse(Response response, byte[] body) {
        try {
            return new Response.Builder()
                    .body(ResponseBody.create(response.body().contentType(), body))
                    .request(response.request())
                    .headers(response.headers())
                    .code(response.code())
                    .message(response.message())
                    .handshake(response.handshake())
                    .protocol(response.protocol())
                    .networkResponse(response.networkResponse())
                    .cacheResponse(response.cacheResponse())
                    .priorResponse(response.priorResponse())
                    .build();
        } catch (NullPointerException e) {
            return null;
        }
    }

    public ResponseWrapper copy() {
        return new ResponseWrapper(this.response, body);
    }
}