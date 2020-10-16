# cawdy

[![Clojars Project](https://img.shields.io/clojars/v/cawdy.svg)](https://clojars.org/cawdy)
[![CircleCI](https://circleci.com/gh/victorb/cawdy/tree/master.svg?style=svg)](https://circleci.com/gh/victorb/cawdy/tree/master)

Clojure library for interacting with Caddy via the HTTP Admin API

Tested with Caddy v2.2.1

## Installation

### Leiningen

```
[cawdy "0.2.0"]
```

### deps.edn

```
cawdy {:mvn/version "0.2.0"}
```

## Usage

### Quickstart

Creates a new server that listens on :2015 and serves two domains. One that
gives a static response (`:static`) for all requests and one that serves files
from a directory (`:files`)

```clojure
(require '[cawdy.core :as cawdy])
(def conn (cawdy/connect "http://localhost:2019")
(cawdy/create-server conn ":2015")
(cawdy/add-route conn :my-id "cawdy-response.example" :static {:body "hello"}))
(cawdy/add-route conn :my-id "cawdy-files.example" :files {:root "/etc"}))
```

### Connect to running Caddy server

```clojure
(require '[cawdy.core :as cawdy])
(def conn (cawdy/connect "http://localhost:2019")
```

### Get Current Configuration

```clojure
(cawdy/config conn)
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
(cawdy/create-server conn ":2019")
(cawdy/add-route conn :my-id "localhost" :static {:body "This gets returned"}))
```

```shellsession
$ curl --silent localhost:2019
This gets returned
```

Options for :static handler can be:

- `:body` - What to send in the response body

### Add File Server Handler

```clojure
(cawdy/create-server conn ":2020")
(cawdy/add-route conn :my-id "localhost" :static {:root "/etc"}))
```

```shellsession
$ curl --silent localhost:2020/hosts
# Static table lookup for hostnames.
...
```
Options for :files handler are:

- `:root` - What directory should act as the root directory

## Tests

Run tests with `make test` that will automatically download a compatible
Caddy version to run the tests with. It'll also start caddy with no config,
and turn it off after the tests.

Don't run the tests with a caddy instance whos config is important as the
tests will remove the existing config before each test.

## License

Copyright © 2020 Victor Bjelkholm under MIT license, see `LICENSE`
