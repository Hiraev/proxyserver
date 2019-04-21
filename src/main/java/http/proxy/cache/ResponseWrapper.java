package http.proxy.cache;

import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Обертка над Response, позволяет копировать его
 * храненит длину и время создания
 *
 * Поле valid говорит о том, что обертка была создана успешна,
 * если во время создания, что-то пошло не так, то
 * valid будет false
 */
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

    private ResponseWrapper(ResponseWrapper responseWrapper) {
        this.valid = responseWrapper.valid;
        this.length = responseWrapper.length;
        this.response = cloneResponse(responseWrapper.response, responseWrapper.body);
        this.createdTime = responseWrapper.createdTime;
        this.body = responseWrapper.body;
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

    /**
     * Создает копию
     * @param response ответ
     * @param body тело ответа
     * @return новый экземпляр Response со скопированным телом, либо
     * null, если что-то пошло не так
     */
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
        return new ResponseWrapper(this);
    }
}