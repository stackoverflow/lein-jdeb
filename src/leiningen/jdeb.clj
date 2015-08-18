(ns leiningen.jdeb
  (:require [leiningen.core.main :as lm]
            [clojure.java.io :as io]
            [clojure.walk :refer [stringify-keys]]
            [me.raynes.fs :refer [temp-dir file]])
  (:import [org.vafer.jdeb DebMaker Console]
           [org.vafer.jdeb.mapping Mapper PermMapper NullMapper LsMapper]
           [org.vafer.jdeb.producers DataProducerDirectory DataProducerPathTemplate]
           [org.vafer.jdeb.utils MapVariableResolver]
           [leiningen DataProducerResolvedFile])
  (:gen-class))

(def console
  (reify Console
    (info [_ m]
      (lm/info m))
    (warn [_ m]
      (lm/warn m))
    (debug [_ m]
      (lm/debug m))))

(defn get-properties [project]
  (stringify-keys project))

(defn create-temp-control
  "Create control file in temporary directory with minimum fields"
  [a m p v]
  (let [td (temp-dir "lein-deb")]
    (with-open [f (io/writer (file td "control"))]
      (.write f (str "Architecture: " a))
      (.newLine f)
      (.write f (str "Maintainer: " m))
      (.newLine f)
      (.write f (str "Priority: " p))
      (.newLine f)
      (.write f (str "Version: " v))
      (.newLine f)
      (.flush f))
    td))

(defn deb-pkg-name
  "Build debian package name"
  [p v a]
  (str p "_" v "_" a ".deb"))

(defn string-array [coll]
  (into-array String coll))

(defn mapper-array [coll]
  (into-array Mapper coll))

;; Mappers
(defmulti mapper :type)

(defmethod mapper :perm [m]
  (PermMapper. (:uid m -1) (:gid m -1) (:user m) (:group m) (:filemode m "644") (:dirmode m "755") (:strip m 0) (:prefix m)))

(defmethod mapper :ls [m]
  (LsMapper. (io/input-stream (:src m))))

(defmethod mapper :default [_]
  NullMapper/INSTANCE)

;; Data Producers
(defmulti process-data (fn [m _] (:type m)))

(defmethod process-data :file [data resolver]
  (DataProducerResolvedFile. (io/file (:src data))
                             (:dst data)
                             (string-array (:includes data))
                             (string-array (:excludes data))
                             (mapper-array [(mapper (:mapper data))])
                             resolver
                             (or (:conffile data) false)))

(defmethod process-data :directory [data resolver]
  (DataProducerDirectory. (io/file (:src data))
                          (string-array (or (:includes data) ["**"]))
                          (string-array (:excludes data))
                          (mapper-array [(mapper (:mapper data))])))

(defmethod process-data :template [data resolver]
  (DataProducerPathTemplate. (string-array (:paths data))
                             (string-array (:includes data))
                             (string-array (:excludes data))
                             (mapper-array [(mapper (:mapper data))])))

(defn jdeb
  "Create debian package from project.clj configuration"
  [project & args]
  (let [conf (project :jdeb)
        package (project :name)
        version (project :version)
        description (project :description)
        homepage (project :url)
        control-dir (:deb-control-dir conf)
        maintainer (:deb-maintainer conf)
        architecture (:deb-architecture conf "all")
        section (:deb-section conf "java")
        depends (:deb-depends conf)
        priority (:deb-priority conf "optional")
        resolver (MapVariableResolver. (get-properties project))
        pkg-name (.getPath (io/file "target" (deb-pkg-name package version architecture)))
        producers (mapv #(process-data % resolver) (:data-set conf))
        confs (mapv #(process-data % resolver) (filter :conffile (:data-set conf)))
        dm (doto (DebMaker. console producers confs)
             (.setOpenReplaceToken "[[")
             (.setCloseReplaceToken "]]")
             (.setResolver resolver))]
    ;; If user specified control dir use that, else create control in temp
    ;; directory with minimum required control fields
    (if control-dir
      (.setControl dm (file control-dir))
      (.setControl dm
                   (create-temp-control
                    architecture maintainer priority version)))
    (.setDeb dm (file pkg-name))
    (if depends
      (.setDepends dm depends))
    (doto dm
      (.setDescription description)
      (.setHomepage homepage)
      (.setPackage package)
      (.setSection section)
      (.validate)
      (.makeDeb))))
