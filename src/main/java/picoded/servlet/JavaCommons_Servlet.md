LifeCycle of a HTTP Request
Matching the correct endpoint

DStackPage -> BaseUtilPage -> BasePage -> CoreUtilPage -> CorePage


# Summary
After reading through this documentation (or just skip to the class that you want to know more about), you should be able to implement classes that extends from the JavaCommons-servlet classes as well as understanding the functions of the utility classes.

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

@TOOD: Add more details to these methods

- getHttpServletRequest()
- getHttpServletResponse()
- requestParameterMap()
- requestHeaderMap()
- requestCookieMap()
- requestWildcardUri()
- requestWildcardUriArray()

## CoreUtilPage
`CoreUtilPage` is an extended from `CorePage`. This class contains additional utility functions to provide the bare minimum to serve static pages.

`outputFileServlet` is the method that `CoreUtilPage` uses to process any requests to get static pages.

@TODO: To have a wrapper function that accepts a parameter to dictate the location of the files to be served.

## BasePage
`BasePage` is an extension class from `CoreUtilPage`. Its main purpose is to override the `doRequest` method from CorePage so that it will utilize `BasePageClassMap`. 

Apart from that, `BasePage` is the layer where it will intercept any `ApiException` from the endpoints and return a standardized error format.

The error format is
```javascript
	{
		"ERROR" : {
			"code" : "MISSING_PARAM",
			"message": "Missing `name` parameter."
			"stack": "<a stacktrace of the exception>"
		}
	}
``` 

## BaseUtilPage
`BaseUtilPage` is extended from `BasePage`. `BaseUtilPage` contains more utility functions such as getting specific file paths.
- getWebInfPath()
- getClassesPath()
- getLibraryPath()
- getConfigPath() 

A most commonly used function in this class will be the `configFileSet()`. This function returns the configuration folder with all the setting files as a `GenericConvertMap`. All the file settings are in `.json` files. 
```
File Directory
config/
	- sys/
		account.json
		dstack.json
	- site/
		email.json
```

The file directory is then convert to `GenericConvertMap` where you can retrieve the settings as such:


```java
// Getting the dstack as a GenericConvertStringMap
configFileSet().getGenericConvertStringMap("sys.dstack", null);
	
// Getting a single string
// Given that you have an attribute `name` in account.json
configFileSet().getString("sys.account.name", "");
```


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

> Within each request handling method, it follows a general structure of
> 1. Find the main endpoints (If more than 1 is return, it will always get the first in the list)
> 2. Find and execute any matched before EndpointMap
> 3. Execute the main Endpoint
> 4. Find and execute any matched after EndpointMap  

`request_api` and `request_path` is fairly straightforward whereby upon finding match endpoints, it will just execute them.

`request_methodReroute` and `request_fieldReroute` methods are more tricky, see the next section on `How does BasePageClassMap set up reroute paths` for more information.
 
#### How does BasePageClassMap set up reroute paths
The BasePageClassMap does not explicitly set up reroute paths. It will only attempt to find the relevant reroute paths during handling of requests. 
 
`request_methodReroute` and `request_filedReroute` methods are suppose to contain endpoint paths that end with `/*`, if not it will throw an error. Once the path is valid, it will find and execute all relevant `before` endpoint paths.

##### request_methodReroute
After the execution of the `before` paths, it will invoke the main method. The object that is returned from this method should be of a `BasePage` class. It will then performs the `setupAndCache` of this returned object and checks if it supports the request path.

If it does support, `request_methodReroute` will then transfer all the params from itself to the `BasePage` class and execute the `handleRequest` method. 

##### request_fieldReroute 

After the execution of the `before` paths, it will obtain the data type of the variable, which is the `reroute class`.

Next, it will construct a `BasePageClassMap` based on the `reroute class` and find the relevant paths through the use of `supportRequestPath` method. Once the `reroute class` supports the path, the request will be passed over to the `reroute class` and the same process occurs again.  

## EndpointMap

private methods do not get recognized by `BasePageClassMap.registerClassMethods` method
