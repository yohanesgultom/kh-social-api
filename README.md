# KH Social API Integration
Kawal/Pantau Harga integration with social media using awesome libraries [Facebook4j](http://facebook4j.org) and [Twitter4j](http://twitter4j.org). Using internal library [kh-database](https://github.com/yohanesgultom/kh-database) to access Kawal/Pantau Harga DB 

## Features

Facebook
* Post daily price info in fan page
* Check reaction to daily price info posts (currently only likes)

Twitter
* Tweet daily price info
* Check price report through mention `@pantauharga lapor harga (nama komoditas) Rp (harga)/kg di (lokasi)`

## Running

On successful Gradle build, get the distributable binary (tar/zip) in `build/distributions/` sub-directory. To run the program, extract it and go inside `$ cd kh-social-api-XX/bin` and run script using below format:

```
$ kh-social-api [action] [database config properties path]
```
Actions:

* `fb-page-single-post` : post price info to fb page
* `fb-page-update` : get fb page post reactions
* `tw-single-post` : tweet price info
* `tw-check-report` : get price report from mentions
