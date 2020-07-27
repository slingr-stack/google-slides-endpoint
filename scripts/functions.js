/////////////////////
// Public API
/////////////////////

// Presentations

endpoint.presentations = {};

endpoint.presentations.batchUpdate = function(presentationId, body) {
    return endpoint.post({
        path: '/presentations/'+presentationId+':batchUpdate',
        body: body
    });
};

endpoint.presentations.create = function(body) {
    return endpoint.post({
        path: '/presentations',
        body: body
    });
};

endpoint.presentations.get = function(presentationId) {
    return endpoint.get('/presentations/'+presentationId);
};

// Presentations - Pages

endpoint.presentations.pages = {};

endpoint.presentations.pages.get = function(presentationId, pageId) {
    return endpoint.get('/presentations/'+presentationId+'/pages/'+pageId);
};

endpoint.presentations.pages.getThumbnail = function(presentationId, pageId, params) {
    return endpoint.get({
        path: '/presentations/'+presentationId+'/pages/'+pageId+'/thumbnail',
        params: params
    });
};

/////////////////////
// Public API - Generic Functions
/////////////////////

endpoint.get = function (url) {
    options = checkHttpOptions(url, {});
    return endpoint._getRequest(options);
};

endpoint.post = function (url, options) {
    options = checkHttpOptions(url, options);
    return endpoint._postRequest(options);
};

endpoint.put = function (url, options) {
    options = checkHttpOptions(url, options);
    return endpoint._putRequest(options);
};

endpoint.patch = function (url, options) {
    options = checkHttpOptions(url, options);
    return endpoint._patchRequest(options);
};

endpoint.delete = function (url) {
    var options = checkHttpOptions(url, {});
    return endpoint._deleteRequest(options);
};

/////////////////////
// Utilities
/////////////////////

var checkHttpOptions = function (url, options) {
    options = options || {};
    if (!!url) {
        if (isObject(url)) {
            // take the 'url' parameter as the options
            options = url || {};
        } else {
            if (!!options.path || !!options.params || !!options.body) {
                // options contains the http package format
                options.path = url;
            } else {
                // create html package
                options = {
                    path: url,
                    body: options
                }
            }
        }
    }
    return options;
};

var isObject = function (obj) {
    return !!obj && stringType(obj) === '[object Object]'
};

var stringType = Function.prototype.call.bind(Object.prototype.toString);