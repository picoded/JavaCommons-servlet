LifeCycle of a HTTP Request
Matching the correct endpoint

DStackPage -> BaseUtilPage -> BasePage -> CoreUtilPage -> CorePage

## CorePage
CorePage is the lowest level of implementation that interacts with the native Java Servlet class. Its main purpose is to alter the flow of the how requests are being processed.

JavaCommons.servlet are all designed to be re initiated for each thread request, ensuring class instance isolation between various request by default.

For all requests, be it `GET`, `POST` or others, it will go through this flow diagram.

> Note that internally, doPost, doGet creates a new class instance for each call/request it recieves.
> As such, all subclass built can consider all servlet instances are fresh instances on process request.

```java
/**
 * [CorePage request process flow]
 *
 * doGet ------------+--> spawnInstance().setupInstance(...).processChain(...)
 *                   |            |
 * doPost -----------+       doSharedSetup
 *                   |     && doRequestSetup
 * doPut ------------+            |
 *                   |        doRequest
 * doDelete ---------+            |
 *                   |     doRequestTearDown
 * #doOption --------+    && doSharedTeardown
 *                   |
 * #doHead ----------/
 *
 * #doOption and #doHead is not yet supported
 */
```

In terms of its lifecycle, CorePage overrides the native Java servlet `contextInitialized` with its own implementation
```java
/**
 * [CorePage lifecycle process flow]
 *
 * contextInitialized --> doSharedSetup -----> initializeContext
 * contextDestroyed ----> doSharedTeardown --> destroyContext
 */
```

### Methods to take note of
For developers extending CorePage, `doSharedSetup` and `initializeContext` are the methods to take note of.

From the two text diagrams, one main takeaway is that the method `initializeContext` is only called once. This is the usually used for the initial set up such as declaring of database connections or other one time setup.

The other main takeaway is the `doSharedSetup`. This particular method will be invoked every single time a request is being made to the server. The usual practice is to populate API endpoint maps inside `doSharedSetup` before the request is processed further down the chain. 

#### Other useful methods

- getHttpServletRequest()
- getHttpServletResponse()
- requestParameterMap()
- requestHeaderMap()
- requestCookieMap()
- requestWildcardUri()
- requestWildcardUriArray()

## CoreUtilPage

## BasePage

## BaseUtilPage

## DStackPage

BasePageClassMap

EndpointMap


private methods do not get recognized by `BasePageClassMap.registerClassMethods` method
