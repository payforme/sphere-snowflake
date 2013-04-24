SPHERE.IO - Snowflake
=====================

This is a fully functional example web store for [SPHERE.IO](http://sphere.io).

Have fun!

## Get data into Sphere

```
$ sphere -u hajo.eichler@commercetools.de login
$ sphere project select payforme
$ sphere types create @data/legoType.json
$ sphere categories import data/categories.csv
$ sphere products import data/products.csv
```

## Development

[![Build Status](https://hajo.ci.cloudbees.com/job/payforme/badge/icon)](https://hajo.ci.cloudbees.com/job/payforme/)
