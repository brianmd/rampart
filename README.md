# rampart

generated using Luminus version "2.9.10.97"
lein new luminus +auth +mysql +cljs

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein run
    
## Description

Framework to provide authentication/authorization to backend servers.
    
## Process description

The central component is the system is this function:

```clojure
(defn process [query]
  (->>
   query
   prepare-query
   pre-validate
   pre-authorize
   proxy-request
   add-body-object
   post-validate
   post-authorize
   finalize-query
   format-response))
```
   
The query parameter is a map which is returned from each function, and in
turn passed to the next function. It starts with a :query key,
which is also a map, containing :subsystem, :query-name,
and :params keys.

The subsystem is a psuedo self-contained system that can operate independently.
Each subsystem will typically be instantiated in a service, but several subsystems
can be contained in a monolithic program.

Rampart itself is an example of a subsystem, as it owns the data used for it's
business rules. There are of course links from the customer-ids and account-numbers
in rampart to the full objects in another system, but rampart itself does not
access this other data.

The query-name refers to the query that is to be run on the backend server.
A definition of the query is associated with this query name.

Finally the params are parameters to be passed to the remote query.

Pre-authorize typically authorizes the :query values, while post-authorize
typically authorizes the body of response from the backend server.

## License

Copyright © 2016 FIXME
