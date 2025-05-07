package com.github.tvbox.osc.util.js;

import com.google.common.util.concurrent.SettableFuture;
import com.whl.quickjs.wrapper.JSCallFunction;
import com.whl.quickjs.wrapper.JSFunction;
import com.whl.quickjs.wrapper.JSObject;

public class Async {

    private final SettableFuture<Object> future;

    public static SettableFuture<Object> run(JSObject object, String name, Object[] args) {
        return new Async().call(object, name, args);
    }

    private Async() {
        this.future = SettableFuture.create();
    }

    private SettableFuture<Object> call(JSObject object, String name, Object[] args) {
        try {
            JSFunction function = object.getJSFunction(name);
            if (function == null) {
                future.set(null);
                return future;
            }
            Object result = function.call(args);
            if (result instanceof JSObject) {
                then(result);
            } else {
                future.set(result);
            }
        } catch (Throwable t) {
            future.setException(t);
        }
        return future;
    }

    private void then(Object result) {
        JSObject promise = (JSObject) result;
        JSFunction thenFn = promise.getJSFunction("then");
        if (thenFn != null) {
            thenFn.call(callback);
        } else {
            // If there's no then, complete immediately
            future.set(result);
        }
    }

    private final JSCallFunction callback = new JSCallFunction() {
        @Override
        public Object call(Object... args) {
            // args[0] holds the resolved value from the JS promise
            future.set(args.length > 0 ? args[0] : null);
            return null;
        }
    };
}
