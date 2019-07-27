var api = (function() {
/// API Builder javascript library
///
/// This is used by ApiBuilder, to generate out the full javascript file
/// with the various API endpoint initialized.
///
/// Normally this would be wrapped inside a function for the final api.js
/// to prevent uneeded global variable escape

/// The actual api object tor return
	var api = {};

/// api configuration object
	var apiconfig = {
		// baseURL: "//localhost:8080/api/",
		baseURL: "",
		apiKey: null
	};

/// The internal core sub namespace
	var apicore = {};
	api._core = apicore;

/// Function: api.isNodeJS
///
/// @return  Boolean true/false if the runtime environment is node.js compliant (or not)
	apicore.isNodeJS = function isNodeJS() {
		return ((typeof process !== 'undefined') && (typeof process.versions.node !== 'undefined') && require != null);
	}

//---------------------------------------------------------------------------------------
//
//  API client configuration
//
//---------------------------------------------------------------------------------------

/// Function: api._core.baseURL
///
/// @param   [Optional] overwrite the configured baseURL, avoid this, unless you know what your doing
///
/// @return  baseURL to call the api (as a string)
	apicore.baseURL = function baseURL(inBaseURL) {
		// Make configuration change when applicable
		if( inBaseURL ) {
			apiconfig.baseURL = inBaseURL;
		}
		// Return the value
		return apiconfig.baseURL;
	}

/// Function: api._core.apikey
///
/// @param   API key to use, note that using this in the browser side automatically authenticate the user
///          and would make certain functionality (such as logout) non functional
///
/// @return  nothing, intentionally reading the configured key is not made easily accesible
	apicore.apiKey = function apiKey(inKey) {
		if(inKey) {
			apiconfig.apiKey = inKey;
		}
	}

/// Function: api._core.persistentSession
/// @param    True or False value to set the persistency of the request session
///
/// @return  nothing
	apicore.persistentSession = function persistentSession(booleanValue){
		if(apicore.isNodeJS()){
			apicore.persistentSession = booleanValue;
		} else {
			throw "You are not in NodeJS environment.";
		}
	}

/// Function: api._core.setCookieString
/// @param    Takes in a cookieJar to use for the request
///
/// @return  nothing
	apicore.setCookieString = function setCookieString(cookieString){
		if(apicore.isNodeJS()){
			apicore.cookieString = cookieString;
		} else {
			throw "You are not in NodeJS environment.";
		}
	}

	/**
	 * Function: api._core.getCookieString
	 * @return String of cookies
	 */
	apicore.getCookieString = function getCookieString(){
		if(apicore.isNodeJS()){
			return apicore.cookieString;
		} else {
			throw "You are not in NodeJS environment.";
		}
	}

	if(apicore.isNodeJS()){
		var axios = require('axios').default;
	}

	if(axios === undefined){
		throw "Axios is not implemented! Please implement axios library before using.";
	}


//---------------------------------------------------------------------------------------
//
//  AXIOS GET and POST with interceptors
//
//---------------------------------------------------------------------------------------

	apicore.baseURL("SET_SERVER_URL_HERE");

	var instance = axios.create({
		withCredentials : true,
		baseURL: apicore.baseURL()
	});

	// In NodeJS environment, add interceptors to set cookies in requests
	// as well as responses
	if (apicore.isNodeJS()){
		// Set cookies
		instance.interceptors.request.use(function (config) {
			if(apicore.getCookieString() !== undefined){
				// Set Cookie before returning the config
				config.headers['Cookie'] = apicore.getCookieString();
			}

			return config;
		}, function (error) {
			// Do something with request error
			return Promise.reject(error);
		});

		instance.interceptors.response.use(function(response){

			// If there are cookies available, set the cookies
			if (response.headers['set-cookie'] !== undefined){
				apicore.setCookieString(response.headers['set-cookie'].join("; "));
			}

			return response;
		}, function(error){
			return Promise.reject(error);
		})
	}

	apicore.axiosGET = function(reqURI, paramObj, callback){
		var ret = instance.get( reqURI, paramObj );

		// Attach callback
		if( callback != null ) {
			ret.then(callback);
		}

		// Return the promise
		return ret;
	};

	apicore.axiosPOST = function(reqURI, paramObj, callback){
		var ret = instance.post( reqURI, paramObj );

		// Attach callback
		if( callback != null ) {
			ret.then(callback);
		}

		// Return the promise
		return ret;
	}

//---------------------------------------------------------------------------------------
//
//  API endpoint management utilities
//
//  Note: that not all the functions here does parameter safety checks
//
//---------------------------------------------------------------------------------------

/// api endpoint map to config listing
	var apimap = {};

/// Function: normalizeEndpointPath
///
/// Normalize the endpoint to '/' notation from either '.' or '/' notations
/// Eg: user.account.login -> user/account/login
///
/// @param  The path the normalize
///
/// @return  normalized path
	function normalizeEndpointPath(path) {
		path = path.replace(/\./g, '/').trim();

		while( path.charAt(0) == '/' ) {
			path = path.slice(1);
		}

		while( path.charAt(path.length - 1) == '/' ) {
			path = path.slice(0, path.length - 1);
		}

		return path;
	}

/// Function: callSingleEndpoint
///
/// Calls an api endpoint, with the given arguments. Arguments are processed in accordence to the following rules.
///
/// + If there is no argument, no parameters is sent.
/// + If there is only a single argument object, it is assumed to be a parameter object.
///
/// - If the single argument, is a string or number, it is assumed to be a named parameter.
/// - If there is multiple arguments, it is assumed to be a named parameter.
/// - If its a named parameter request, and there is no configuration, an error is thrown via the promise object.
///
/// @param  Endpoint path, must be normalized
/// @param  Array of arguments
///
/// @return  Promise object with the api endpoint result
	function callSingleEndpoint(endpointPath, args) {
		// @TODO: Change the apicore.rawPostRequest
		var config = apimap[endpointPath]

		// @TODO: Think about whether should we assume that no matter the request, if the endpoint has
		// required variables, should we still check for it? If so, alter and shift the checking
		// logic of requiredVar.forEach(function(variable){ here

		// No arguments, nothing to consider
		// Check method in endpoint, if there is GET, do GET, else do POST
		if( args == null || args.length <= 0 ) {
			// Allow GET request since endpoint did not specify any methods
			if( config.methods === undefined ) {
				return apicore.axiosGET(endpointPath);
			}

			// Check that the apimap has GET method
			if(config.methods.length > 0 && config.methods.indexOf("GET") < 0) {
				throw endpointPath + " does not support GET method."
			}

			return apicore.axiosGET(endpointPath);
		}

		// @TODO: Work in progress
		// @TODO: Decide whether do we still need callEndpointWithNamedArguments since server side
		// can already do the filtering the handles it

		// Do the normal raw post request
		// Possible parameter object request
		if( args.length == 1 ) {
			var paramObj = args[0];
			var paramType = (typeof paramObj);

			// Its an object, assume its parameters
			if( paramType == "object" ) {
				// Check the required variables are fulfilled
				var requiredVar = apimap[endpointPath].required;

				if(requiredVar !== undefined){
					requiredVar.forEach(function(variable){
						if(!paramObj.hasOwnProperty(variable)){
							throw "Missing endpoint parameter:" + variable
						}
					});
				}

				return apicore.axiosPOST(endpointPath, paramObj);
			}
		}

		// Assumed named arguments call
		return callEndpointWithNamedArguments(endpointPath, args);
	}

/// Function: callEndpointWithNamedArguments
///
/// Varient of callEndpoint, where it is assumed to be named arguments
/// NOTE: if there is no configuration, an error is thrown via the promise object.
///
/// @param  Endpoint path, must be normalized
/// @param  Array of arguments
///
/// @return  Promise object with the api endpoint result
	function callEndpointWithNamedArguments(endpointPath, args) {
		// Endpoint configuration
		var endpointConfig = apimap[endpointPath];

		// Terminates at invalid name point configration
		if( endpointConfig == null || endpointConfig.argNameList == null || endpointConfig.argNameList.length <= 0 ) {
			return new Promise(function(good,bad) {
				bad("Missing endpoint named parameters configuration for : "+endpointPath);
			});
		}

		// Arguments names list
		var argNameList = endpointConfig.argNameList;

		// Parameter object to build from named arguments
		var paramObject = {};

		// Parmaters names to object mapping
		for(var i=0; i<argNameList.length; ++i) {
			paramObject[argNameList[i]] = args[i];
		}

		// Does the parameter call
		return apicore.axiosPOST(endpointPath, paramObject);
	}

/// Function: setEndpointRaw
///
/// @param   Endpoint path, must be normalized
/// @param   Arg names array, for multiple arguments mode / non object mode
/// @param   Configuration object
	function setEndpointRaw(endpointPath, config) {
		// Normalize config object
		config = config || {};

		// Storing the configuration
		apimap[endpointPath] = config;

		// Split the endpoint path, amd call setup
		var splitEndpointPath = endpointPath.split("/");
		setupEndpointFunction(splitEndpointPath);
	}

/// Function: setupEndpointFunction
///
/// Setup the endpoint function against the "api" object.
/// This is done recursively against pathSuffix
///
/// @param   pathSuffix, array of names, to setup the call function
/// @param   [Optional] pathPrefix that represents the current apiObj
/// @param   [Optional] apiObj to append to, defaults to actual api object
	function setupEndpointFunction(pathSuffix, pathPrefix, apiObj) {
		// Validate path suffix needs processing
		if( pathSuffix == null || pathSuffix.length <= 0 ) {
			return; //terminate
		}

		// Setup optional params if missing
		if( apiObj == null || pathPrefix == null ) {
			apiObj = api;
			pathPrefix = [];
		}

		// clone array, prevent destructive edit on recursive call
		pathSuffix = pathSuffix.slice(0);
		pathPrefix = pathPrefix.slice(0);

		// Setup the new pathPrefix
		var name = pathSuffix.shift();
		pathPrefix.push( name );

		// apiObj[name] previously exists, use it
		if( apiObj[name] ) {
			apiObj = apiObj[name];
		} else {
			apiObj = setupEndpointFunctionStep(apiObj, name, pathPrefix.join('/'));
		}

		// Recursive call
		setupEndpointFunction(pathSuffix, pathPrefix, apiObj);
	}

/// Function: setupEndpointFunctionStep
///
/// @param  apiObj to append to
/// @param  name to attach function with
/// @param  full normalized endpoint path string to use
///
/// @return  The generated function (to append as another apiObj)
	function setupEndpointFunctionStep(apiObj, name, endpointPath) {
		apiObj[name] = function() {
			return callSingleEndpoint(endpointPath, (arguments.length === 1 ? [arguments[0]] : Array.apply(null, arguments)) );
		}
		return apiObj[name];
	}

//---------------------------------------------------------------------------------------
//
//  API endpoint management functions
//
//---------------------------------------------------------------------------------------

/// Function: api._core.callEndpoint
///
/// @param   Endpoint path
/// @param   arguments to pass forward ....
///
/// @return  Promise object for the API request
	apicore.callEndpoint = function callEndpoint(endpointPath) {
		return callSingleEndpoint( normalizeEndpointPath(endpointPath), Array.apply(null, arguments).slice(1) );
	}

/// Function: api._core.setEndpoint
///
/// @param   Endpoint path
/// @param   Arg names array, for multiple arguments mode / non object mode
/// @param   Configuration object
	apicore.setEndpoint = function setEndpoint(endpointPath, config) {
		return setEndpointRaw( normalizeEndpointPath(endpointPath), config );
	}

/// Function: api._core.setEndpointMap
///
/// @param   Object map of [path] = [arguments list]
	apicore.setEndpointMap = function setEndpointMap(pathMap) {
		if( pathMap != null ) {
			for (var path in pathMap) {
				if (pathMap.hasOwnProperty(path)) {
					apicore.setEndpoint( path, pathMap[path] );
				}
			}
		}
	}

	SET_ENDPOINT_MAP_HERE

	return api;
})();

if(api._core.isNodeJS()){
	module.exports=api;
}
