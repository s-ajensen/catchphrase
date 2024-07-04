(ns catchphrase.config
  (:require [c3kit.apron.app :as app]))

(def ^:private base
  {
   :analytics-code "console.log('google analytics would have loaded for this page');"
   :log-level      :info})

(def bucket {:impl        :memory
             :full-schema 'catchphrase.schema.full/full-schema})

(def development
  (assoc base
    :database bucket
    :host "http://localhost:8123"
    :log-level :trace
    :jwt-secret "catchphrase_DEV_SECRET"))

(def staging
  (assoc base
    :database bucket
    :host "https://catchphrase-staging.com"
    :log-level :trace
    :jwt-secret "catchphrase_STAGING_SECRET"))

(def production
  (assoc base
    :database bucket
    :host "https://catchphrase.com"
    :jwt-secret "catchphrase_PRODUCTION_SECRET"))

(def environment (app/find-env "catchphrase.env" "catchphrase_ENV"))
(def development? (= "development" environment))

(def env
  (case environment
    "staging" staging
    "production" production
    development))

(def host (:host env))
