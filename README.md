# cawdy

[![Clojars Project](https://img.shields.io/clojars/v/cawdy.svg)](https://clojars.org/cawdy)

Clojure library for interacting with Caddy via the HTTP Admin API

Tested with Caddy v2.2.0

## Installation

### Leiningen

```
[cawdy "0.1.0"]
```

### deps.edn

```
cawdy {:mvn/version "0.1.0"}
```

## Usage

### Get Current Configuration

```clojure
(require '[cawdy.core :as cawdy])
(cawdy/config)
=> {:apps
    {:http
     {:servers
      {:my-id
       {:listen ["localhost:2016"],
        :routes
        [{:handle
          [{:handler "file_server", :root "/tmp/cawdytest2"}]}]}}}}})
```

### Add Static Response Handler

```clojure
(cawdy/add-server "http://localhost:2019"
                  :my-server-id
                  :static
                  {:body "This gets returned"})
```

```shellsession
$ curl --silent localhost:2019
This gets returned
```

Options for :static handler can be:

- `:listen` - Listen address of the server, defaults to ":2015"
- `:body` - What to send in the response body
- `:host` - What host header is needed for getting the response, defaults to "localhost"

### Add File Server Handler

```clojure
(cawdy/add-server "http://localhost:2019"
                  :my-server-id
                  :files
                  {:directory "/etc"})
```

```shellsession
$ curl --silent localhost:2019/hosts
# Static table lookup for hostnames.
...
```
Options for :files handler are:

- `:listen` - Listen address of the server, defaults to ":2015"
- `:directory` - What directory should act as the root directory
- `:host` - What host header is needed for getting the response, defaults to "localhost"

## Tests

Make sure you have caddy running (via `caddy run`) before trying to run tests.

Also, don't run the tests with a caddy instance whos config is important as the
tests will remove the existing config before each test.

## License

Copyright © 2020 Victor Bjelkholm under MIT license, see `LICENSE`
