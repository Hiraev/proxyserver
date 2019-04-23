package http.proxy.utils;

public interface Callback {

    void onFailure(Request request, Exception exception);

    void onSuccess(Request request, Response response);

}
