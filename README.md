# This is a work in progress. Functionality listed below is not yet supported.

# bobolink

bobolink is a small tool that helps user's store bookmarks and search for them later.
More specifically, bobolink provides full text search on the body of HTML documents associated with the bookmarks that you've added to your store.

For more information on bobolink in general, please refer to the [website](https://bobolink.me)

This repository houses the API which runs the public instance of the bobolink backend. For user's wishing to use bobolink,
please refer to the [bobolink-cli]().

### Running

While it is certainly possible to run bobolink locally, some adjustments will need to be made in order 

Users will need [Leiningen](https://github.com/technomancy/leiningen) 2.0.0 or above installed in order to run locally.

It will also be necessary for users to have an AWS account and have it configured locally in order for bobolink to access
the AWS API. There are a number of ways this can be achieved (e.g. `aws configure`). User's should refer to AWS docs for 
more info. Keep in mind bobolink will write to `s3://bobo-index/` in the users AWS account. S3 is the only AWS service used.

To start the web server for the application, run:

```
$ lein ring server-headless
```

User's can now clone into the user [cli](). Once doing so, it will be necessary to export the [api url environment]() variable in order to point the cli at their locally running server, i.e.:

```
$ export BOBO_URL=http://localhost:3000/v1/
```

That's it. Refer to the [cli]() for additional info on usage.
	
### Architecture

bobolink uses [Lucene]() to store and search the corresponding HTML documents associated with user's stored bookmarks. 
Lucene indexes are cached locally for active users and persisted to AWS S3 for permanent storage. While indexes
of active user's are kept on disk locally, a daemon thread will delete any lingering indexes from disk if they've since
been evicted from our cache in favor of more recently active users.

### API

The bobolink API consists of the basic CRUD operations around bookmark/user creation. User's wishing for more information
on the available endpoints and their payloads should consult [handler.clj]() and [api.clj](). 

## License

Copyright © 2021 FIXME
