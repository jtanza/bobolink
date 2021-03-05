# bobolink

bobolink helps user's store bookmarks and easily search for them later. In a nutshell, bobolink provides full text search on the HTML documents associated with user's bookmarks.

For more information on bobolink in general, users should refer to the documentation hosted on the [website](http://bobolink.me)

This repository houses the API which runs the public instance of the bobolink backend. For user's wishing to use bobolink,
please refer to the [bobolink-cli](https://github.com/jtanza/bobolink-cli).

### Running Locally

Running bobolink locally requires some minor adjustments to account for the local environment. User's will also need to 
install some prerequistes before starting:

Users will need both Java and [Leiningen](https://github.com/technomancy/leiningen) 2.0.0 or above installed in order to run locally.

A running postgres instance is needed as well. User's should refer to the [init-db.clj](https://github.com/jtanza/bobolink/blob/client-server/resources/init-db.sql) file for schema information and
[db.clj](https://github.com/jtanza/bobolink/blob/client-server/src/bobolink/db.clj) for additional info on connection values.

It will also be necessary for users to have an AWS account and have it configured locally in order for bobolink to access
the AWS API. There are a number of ways this can be achieved (e.g. `aws configure`). User's should refer to AWS docs for 
more info. Keep in mind bobolink will write to `s3://bobo-index/` in the users AWS account. S3 is the only AWS service used.

To start the web server for the application, run:

```
$ lein ring server-headless
```

User's can now clone into the user [cli](https://github.com/jtanza/bobolink-cli). Once doing so, it will be necessary to export the [api url environment](https://github.com/jtanza/bobolink-cli/blob/master/bobolink/api.py#L5) variable in order to point the cli at their locally running server, i.e.:

```
$ export BOBO_URL=http://localhost:3000/v1/
```

That's it. Refer to the [cli](https://github.com/jtanza/bobolink-cli) for additional info on usage.

### API

The bobolink API consists of the basic CRUD operations around bookmark/user creation. User's wishing for more information
on the available endpoints and their payloads should consult [handler.clj](https://github.com/jtanza/bobolink/blob/client-server/src/bobolink/handler.clj) and [api.clj](https://github.com/jtanza/bobolink/blob/client-server/src/bobolink/api.clj). 
	
### Some Notes

bobolink uses [Lucene](https://lucene.apache.org/) to store and search the HTML documents associated with user's stored bookmarks. 
Lucene indexes are cached locally for active users and persisted to AWS S3 for permanent storage. Indexes are written
to disk in `/opt/bobolink`. These files are periodically purged by a background thread when the memory representation of
the index has been evicted from our cache.





