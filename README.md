# lein-jdeb

A Leiningen plugin to create debian package from your project's uberjar.
Uses [jdeb](https://github.com/tcurdt/jdeb) to build the debian packages.

##  Leiningen

[![Clojars Project](https://clojars.org/stackoverflow/lein-jdeb/latest-version.svg)](https://clojars.org/stackoverflow/lein-jdeb)

## Installation

###With Leiningen 2

Add `[stackoverflow/lein-jdeb "1.0.0"]` to your project's `:plugins`.

###With Leiningen 1

Add `[stackoverflow/lein-jdeb "1.0.0"]` to your project's `:dev-dependencies`.

##  Usage

Add a `:deb` entry to your `project.clj`. It should be similar to jdeb maven plugin:

    :jdeb {:deb-control-dir "src/deb/control"
           :data-set [{:src "target/my-project.jar"
                       :type :file
                       :mapper {:type :perm
                                :prefix "/opt/company/my-project"
                                :user "myuser"
                                :group "mygroup"}}
                      {:src "src/deb/default"
                       :type :directory
                       :conffile true
                       :mapper {:type :perm
                                :prefix "/etc/default"
                                :filemode "644"
                                :user "myuser"
                                :group "mygroup"}}
                      {:src "src/deb/init.d"
                       :type :directory
                       :mapper {:type :perm
                                :prefix "/etc/init.d"
                                :filemode "755"
                                :user "root"
                                :group "root"}}
                      {:paths ["/var/log/company" "/var/run/company"]
                       :type :template
                       :mapper {:type :perm
                                :user "myuser"
                                :group "mygroup"}}]}

Note that `:type :directory` is not yet working.

Invoke via:

    $ lein jdeb

Other settings that are available and their defaults
* `:deb-architecture` Sets [Architecture](https://www.debian.org/doc/debian-policy/ch-controlfields.html#s-f-Architecture). Set to `all` by default.
* `:deb-priority` Sets [Priority](https://www.debian.org/doc/debian-policy/ch-controlfields.html#s-f-Priority). Set to `optional` by default.
* `:deb-section` Sets [Section](https://www.debian.org/doc/debian-policy/ch-controlfields.html#s-f-Section). Set to `java` by default.
* `:deb-depends` Sets [Depends](). Not set by default

## Future Work

- Fix :directory type
- Support more types
- Add wildcards so you can do things like `:src "${build.directory}/${build.finalName}.jar"

## License

Copyright © 2015 Islon Scherer

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
