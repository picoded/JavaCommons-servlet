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

## BasePageClassMap
BasePageClassMap is an utility class that developers can use to map Annotations to endpoints of a given class object. After which, it can be used to handle request/api calls.
 
#### Parameter
- `BasePage` class or any other classes

#### How to use BasePageClassMap
BasePageClassMap is the main class that can help facilitate the matching of requests to the endpoints as well as registering endpoints of a Class.

In all cases, the `setupAndCache` method will be called first to generate a BasePageClassMap itself with all its paths that existed in the class extracted.

It is true that you can immediately initialize BasePageClassMap with `new BasePageClassMap(<Your class>)`. However, the purpose of using `setupAndCache` method is that it can return existing objects rather than constantly creating a new `BasePageClassMap`.

```java
	
	// Generate a BasePageClassMap
	BasePageClassMap basePageClassMap = BasePageClassMap.setupAndCache(this);
``` 

In the constructor method of `BasePageClassMap`, 
1. It will call upon `registerClass` method that will retrieve all the methods and fields in the class, 
2. Register those with Annotations and populate the relevant `EndpointMaps` (Look at `EndpointMap` for its definitions and what is it for).

The list of `EndpointMap` consists of
- beforeMap - contains all paths to process before executing the main endpoint.
- pathMap   - contains all paths that is not returning a JSON response.
- apiMap    - contains all paths that returns a JSON response.
- rerouteFieldMap - contains all rerouting paths that is derived from variables.
- rerouteMethodMap - contains all rerouting paths that required some initialization steps and return a BasePage class 
- afterMap  - contains all paths to process after the execution of the main endpoint.

#### How does BasePageClassMap handle requests 
Once the set up is completed, the `handleRequest` method will be called to process the requests. 

```java
	// Try to use the various routing options
	if (request_api(page, routePath)) {
		return;
	}
	
	if (request_path(page, routePath)) {
		return;
	}
	
	if (request_methodReroute(page, routePath)) {
		return;
	}
	
	if (request_fieldReroute(page, routePath)) {
		return;
	}
```

It will follow this specific order and upon matching at any point, it will stop the subsequent processing and return back to the caller.

Within each request handling method, it follows a general structure of
1. Find and execute any matched before EndpointMap
2. Find and execute any matched main EndpointMap
3. Find and execute any matched after EndpointMap  


#### How does BasePageClassMap set up reroute paths
The BasePageClassMap does not explicitly set up reroute paths. It will only attempt to find the relevant reroute paths during handling of requests.


## EndpointMap


private methods do not get recognized by `BasePageClassMap.registerClassMethods` method
