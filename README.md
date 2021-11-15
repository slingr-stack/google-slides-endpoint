---
title: Google Slides endpoint
keywords: 
last_updated: July 28, 2020
tags: []
summary: "Detailed description of the API of the Google Slides endpoint."
---

## Overview

The Google Slides endpoint is a user endpoint (see [Global vs user endpoints](app_development_model_endpoints.html#global-vs-user-endpoints)), 
which means that each user should connect to the endpoint.

This endpoint allows direct access to the [Google Slides API](https://developers.google.com/slides/reference/rest),
however it provides shortcuts and helpers for most common use cases.

Some features available in this endpoint are:

- Authentication and authorization
- Direct access to the Google Slides API
- Helpers for most common use cases like creating or updating a presentation

## Configuration

In order to use the Google Slides endpoint you must create an app in the [Google Developer Console](https://console.developers.google.com)
by following these instructions:

- Access to Google Developer Console
- Access to `API Manager > Library`. Enable `Slides API`.
- Access to `API Manager > Credentials > OAuth consent screen`. Complete the form as you prefer and save it.
- Access to `API Manager > Credentials > Credentials`, then `Create credentials > OAuth client ID`.
- Select `Web application` as `Application Type` and add your domain as `Authorized Javascript Origins` (per example 
  `https://myapp.slingrs.io` or you custom domain if you have one), and add a `Authorized redirect URIs` 
  with your domain and `/callback`, like `https://myapp.slingrs.io/callback` or `https://mycustomdomain.com/callback`.
  If you plan to use the app as a template, you should select 'Multi Application' as 'Client type' in order to use the
  platform main domain, like `https://slingrs.io` and `https://slingrs.io/callback`.
- Then click on `Create`.
- That will give you the `Client ID` and `Client Secret` values.  

### Client ID

As explained above, this value comes from the app created in the Google Developer Console.

### Client secret

As explained above, this value comes from the app created in the Google Developer Console.

### Client type

This field determines what kind of URIs will be used on the client configuration on Google.
Select 'Multi Application' when you want to use your application as a template in order to
use the platform main domain.

### Javascript origin

This URL has to be configured in the app created in the Google Developer Console as a valid
origin for OAuth in the field `Authorized JavaScript origins`.

### Registered URI

This URL has to be configured in the app created in the Google Developer Console in the field
`Authorized redirect URIs`.

## Quick start

You can create a new presentation like this:

```js
var res = app.endpoints.googleSlides.presentations.create({
    title: 'Test Presentation'
});
log('presentation: '+JSON.stringify(res));
```

## Javascript API

The Google Slides endpoint allows direct access to the API. This means you can make HTTP requests
to access the API documented [here](https://developers.google.com/slides/reference/rest).

Additionally, the endpoint provides shortcuts and helpers for the most common use cases.

### HTTP requests

You can make `GET`, `POST`, `PUT`, and `DELETE` request to the 
[Google Slides API](https://developers.google.com/slides/reference/rest) like this:

```js
var presentation = app.endpoints.googleSlides.get('/presentations/oiewDSOew32iwdlee');
```

Please take a look at the documentation of the [HTTP endpoint]({{site.baseurl}}/endpoints_http.html#javascript-api)
for more information.

### Shortcuts

These are the shortcuts available for the Google Slides API:

```
endpoint.presentations.batchUpdate = function(presentationId, body) { ... }
endpoint.presentations.create = function(body) { ... }
endpoint.presentations.get = function(presentationId) { ... }
endpoint.presentations.pages.get = function(presentationId, pageId) { ... }
endpoint.presentations.pages.getThumbnail = function(presentationId, pageId, params) { ... }
```

## Events

There are no events for this endpoint.

## About SLINGR

SLINGR is a low-code rapid application development platform that accelerates development, with robust architecture for integrations and executing custom workflows and automation.

[More info about SLINGR](https://slingr.io)

## License

This endpoint is licensed under the Apache License 2.0. See the `LICENSE` file for more details.


